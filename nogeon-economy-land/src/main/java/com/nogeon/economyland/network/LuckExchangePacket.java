package com.nogeon.economyland.network;

import com.nogeon.economyland.item.ModItems;
import com.nogeon.economyland.menu.LuckExchangeOffer;
import com.nogeon.economyland.menu.LuckExchangeOpener;
import com.nogeon.economyland.player.ExtendedInventoryDelivery;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public final class LuckExchangePacket {
    private final String offerId;

    public LuckExchangePacket(String offerId) {
        this.offerId = offerId == null ? "" : offerId;
    }

    public static void encode(LuckExchangePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.offerId);
    }

    public static LuckExchangePacket decode(FriendlyByteBuf buffer) {
        return new LuckExchangePacket(buffer.readUtf());
    }

    public static void handle(LuckExchangePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if ("deposit".equals(packet.offerId)) {
                int deposited = 0;
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    ItemStack stack = player.getInventory().getItem(slot);
                    if (stack.is(ModItems.UNLUCKY_TOKEN.get())) {
                        deposited += stack.getCount();
                        stack.setCount(0);
                    }
                }
                if (deposited > 0) {
                    com.nogeon.economyland.state.EconomyState state = com.nogeon.economyland.state.EconomyState.get(player.server);
                    com.nogeon.economyland.player.PlayerProfile profile = state.profile(player.getUUID());
                    profile.addUnluckyTokens(deposited);
                    state.setDirty();
                    player.displayClientMessage(Component.translatable("message.nogeon_economy_land.luck_exchange.deposited", deposited), false);
                } else {
                    player.displayClientMessage(Component.translatable("message.nogeon_economy_land.luck_exchange.no_deposit_item"), false);
                }
                LuckExchangeOpener.open(player);
                return;
            }
            LuckExchangeOffer offer = LuckExchangeOpener.findOffer(packet.offerId);
            if (offer == null) {
                return;
            }
            com.nogeon.economyland.state.EconomyState state = com.nogeon.economyland.state.EconomyState.get(player.server);
            com.nogeon.economyland.player.PlayerProfile profile = state.profile(player.getUUID());
            if (profile.unluckyTokens() < offer.tokenCost()) {
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.luck_exchange.no_token"), false);
                LuckExchangeOpener.open(player);
                return;
            }
            profile.spendUnluckyTokens(offer.tokenCost());
            state.setDirty();

            ResourceLocation rewardId = ResourceLocation.tryParse(offer.rewardItemId());
            Item reward = rewardId == null ? ModItems.BASIC_GACHA_TICKET.get() : BuiltInRegistries.ITEM.get(rewardId);
            ItemStack stack = new ItemStack(reward, offer.rewardCount());
            ExtendedInventoryDelivery.giveOrDrop(player, stack);
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.luck_exchange.done", stack.getHoverName(), offer.rewardCount()), false);
            LuckExchangeOpener.open(player);
        });
        context.setPacketHandled(true);
    }
}
