package com.nogeon.economyland.network;

import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.menu.ShopOpener;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.ExtendedInventoryDelivery;
import com.nogeon.economyland.shop.ShopEntry;
import com.nogeon.economyland.shop.ShopItemProtection;
import com.nogeon.economyland.shop.Shops;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.state.TraderShopState;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public final class BuyCartItemsPacket {
    public static record CartItem(String entryId, int quantity) {}

    private final String kindId;
    private final String traderDatabaseId;
    private final List<CartItem> items;

    public BuyCartItemsPacket(String kindId, String traderDatabaseId, List<CartItem> items) {
        this.kindId = kindId;
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.items = items;
    }

    public static void encode(BuyCartItemsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.kindId);
        buffer.writeUtf(packet.traderDatabaseId);
        buffer.writeVarInt(packet.items.size());
        for (CartItem item : packet.items) {
            buffer.writeUtf(item.entryId);
            buffer.writeVarInt(item.quantity);
        }
    }

    public static BuyCartItemsPacket decode(FriendlyByteBuf buffer) {
        String kindId = buffer.readUtf();
        String traderDatabaseId = buffer.readUtf();
        int size = buffer.readVarInt();
        List<CartItem> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(new CartItem(buffer.readUtf(), buffer.readVarInt()));
        }
        return new BuyCartItemsPacket(kindId, traderDatabaseId, items);
    }

    public static void handle(BuyCartItemsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (packet.items.isEmpty()) return;

            EconomyState state = EconomyState.get(player.server);
            TraderShopState traderState = TraderShopState.get(player.server);
            TraderKind kind = TraderKind.byId(packet.kindId);
            
            traderState.refreshShopDay(state, player.server.overworld().getDayTime() / 24000L);
            PlayerProfile profile = state.profile(player.getUUID());

            long totalPrice = 0L;
            List<ShopEntry> matchedEntries = new ArrayList<>();
            
            // 1. 유효성 검사 루프 (재고 및 가격 검사)
            for (CartItem item : packet.items) {
                List<ShopEntry> availableEntries = ShopOpener.entriesFor(player, kind, packet.traderDatabaseId);
                ShopEntry entry = Shops.find(availableEntries, item.entryId);
                
                if (entry == null) {
                    player.displayClientMessage(Component.literal("§c상점에서 잘못된 아이템을 감지했습니다."), false);
                    return;
                }

                int remaining = traderState.remaining(state, packet.traderDatabaseId, entry);
                if (remaining < item.quantity) {
                    player.displayClientMessage(Component.literal("§c" + entry.stack().getHoverName().getString() + "의 재고가 부족합니다 (남은 재고: " + remaining + "개)"), false);
                    return;
                }

                long adjusted = traderState.adjustedPrice(kind, packet.traderDatabaseId, entry, false);
                totalPrice += adjusted * item.quantity;
                matchedEntries.add(entry);
            }

            // 2. 잔액 검사
            if (profile.credits() < totalPrice) {
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.shop.no_money"), false);
                return;
            }

            // 3. 일괄 결제 및 지급 진행
            profile.spendCredits(totalPrice);
            for (int i = 0; i < packet.items.size(); i++) {
                CartItem item = packet.items.get(i);
                ShopEntry entry = matchedEntries.get(i);

                traderState.recordPurchase(state, packet.traderDatabaseId, entry, item.quantity);
                ItemStack stack = entry.stack().copy();
                stack.setCount(entry.count() * item.quantity);
                ShopItemProtection.markPurchased(stack);
                ExtendedInventoryDelivery.giveOrDrop(player, stack);
            }

            SyncCreditsPacket.send(player, profile.credits());
            state.setDirty();

            NumberFormat format = NumberFormat.getIntegerInstance(Locale.KOREA);
            player.displayClientMessage(Component.literal("§a장바구니 상품 일괄 구매 성공! 총 §e" + format.format(totalPrice) + " C§a가 차감되었습니다."), false);
            
            // 상점 화면 갱신
            if (kind == TraderKind.SMITH) {
                List<ShopEntry> entries = ShopOpener.entriesFor(player, TraderKind.SMITH, packet.traderDatabaseId);
                ShopOpener.openShop(player, TraderKind.SMITH, packet.traderDatabaseId, entries);
            } else if (kind == TraderKind.HUNTER) {
                List<ShopEntry> entries = ShopOpener.entriesFor(player, TraderKind.HUNTER, packet.traderDatabaseId);
                ShopOpener.openShop(player, TraderKind.HUNTER, packet.traderDatabaseId, entries);
            } else {
                ShopOpener.open(player, kind, packet.traderDatabaseId);
            }
        });
        context.setPacketHandled(true);
    }
}
