package com.nogeon.economyland.state;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.network.SyncCreditsPacket;
import com.nogeon.economyland.player.ExtendedInventoryDelivery;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.SocialClass;
import com.nogeon.economyland.shop.ShopItemProtection;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public final class AuctionState extends SavedData {
    private static final String STATE_ID = NoGeonEconomyLand.MOD_ID + "_auction_state";

    private final List<AuctionEntry> auctionEntries = new ArrayList<>();
    private int nextAuctionId = 1;

    public static AuctionState get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded");
        }
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(AuctionState::fromNbt, AuctionState::new, STATE_ID);
    }

    public List<AuctionEntry> auctions() {
        return List.copyOf(auctionEntries);
    }

    public String listInventoryAuction(ServerPlayer seller, int slot, int count, long price) {
        if (price <= 0L) {
            return "message.nogeon_economy_land.auction.price_invalid";
        }
        if (slot < 0 || slot >= seller.getInventory().items.size()) {
            return "message.nogeon_economy_land.auction.select_item";
        }
        ItemStack selectedStack = seller.getInventory().getItem(slot);
        if (selectedStack.isEmpty()) {
            return "message.nogeon_economy_land.auction.select_item";
        }
        if (ShopItemProtection.isShopPurchased(selectedStack)) {
            return "message.nogeon_economy_land.auction.shop_item_blocked";
        }
        if (count <= 0 || count > selectedStack.getCount()) {
            return "message.nogeon_economy_land.auction.quantity_invalid";
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(selectedStack.getItem());
        if (itemId == null) {
            return "message.nogeon_economy_land.auction.unavailable";
        }

        ItemStack listedStack = seller.getInventory().removeItem(slot, count);
        if (listedStack.isEmpty()) {
            return "message.nogeon_economy_land.auction.unavailable";
        }

        auctionEntries.add(new AuctionEntry(
            nextAuctionId++,
            seller.getUUID(),
            seller.getGameProfile().getName(),
            itemId.toString(),
            listedStack.getCount(),
            price,
            listedStack.serializeNBT()
        ));
        seller.displayClientMessage(Component.translatable("message.nogeon_economy_land.auction.listed", listedStack.getHoverName(), price), false);
        setDirty();
        return null;
    }

    public String buyAuction(ServerPlayer buyer, int auctionId) {
        EconomyState economyState = EconomyState.get(buyer.server);
        for (int index = 0; index < auctionEntries.size(); index++) {
            AuctionEntry entry = auctionEntries.get(index);
            if (entry.id() != auctionId) {
                continue;
            }
            if (entry.sellerId().equals(buyer.getUUID())) {
                return "message.nogeon_economy_land.auction.own_listing";
            }

            ItemStack stack = auctionStack(entry);
            if (stack.isEmpty()) {
                return "message.nogeon_economy_land.auction.unavailable";
            }

            PlayerProfile buyerProfile = economyState.profile(buyer.getUUID());
            if (!buyerProfile.spendCredits(entry.price())) {
                return "message.nogeon_economy_land.shop.no_money";
            }

            PlayerProfile sellerProfile = economyState.profile(entry.sellerId());
            SocialClass sellerClass = sellerProfile.socialClass();
            long fee = entry.price() * sellerClass.auctionFeePercent() / 100L;
            long sellerAmount = entry.price() - fee;
            sellerProfile.addCredits(sellerAmount);
            auctionEntries.remove(index);
            giveItemStack(buyer, stack);
            buyer.displayClientMessage(Component.translatable("message.nogeon_economy_land.auction.bought", stack.getHoverName(), entry.price()), false);
            ServerPlayer seller = buyer.server.getPlayerList().getPlayer(entry.sellerId());
            if (seller != null) {
                seller.displayClientMessage(Component.translatable("message.nogeon_economy_land.auction.sold", stack.getHoverName(), sellerAmount), false);
                SyncCreditsPacket.send(seller, economyState.profile(seller.getUUID()).credits());
            }
            SyncCreditsPacket.send(buyer, buyerProfile.credits());
            economyState.setDirty();
            setDirty();
            return null;
        }
        return "message.nogeon_economy_land.auction.not_found";
    }

    public String cancelAuction(ServerPlayer seller, int auctionId) {
        for (int index = 0; index < auctionEntries.size(); index++) {
            AuctionEntry entry = auctionEntries.get(index);
            if (entry.id() != auctionId) {
                continue;
            }
            if (!entry.sellerId().equals(seller.getUUID())) {
                return "message.nogeon_economy_land.auction.not_found";
            }
            ItemStack stack = auctionStack(entry);
            auctionEntries.remove(index);
            if (!stack.isEmpty()) {
                giveItemStack(seller, stack);
            }
            seller.displayClientMessage(Component.translatable("message.nogeon_economy_land.auction.cancelled"), false);
            setDirty();
            return null;
        }
        return "message.nogeon_economy_land.auction.not_found";
    }

    private static ItemStack auctionStack(AuctionEntry entry) {
        ItemStack savedStack = entry.stack();
        if (!savedStack.isEmpty()) {
            return savedStack;
        }
        ResourceLocation itemId = ResourceLocation.tryParse(entry.itemId());
        if (itemId == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, entry.count());
    }

    private static void giveItemStack(ServerPlayer player, ItemStack stack) {
        ExtendedInventoryDelivery.giveOrDrop(player, stack);
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        CompoundTag auctionsNbt = new CompoundTag();
        int auctionIndex = 0;
        for (AuctionEntry entry : auctionEntries) {
            auctionsNbt.put(String.valueOf(auctionIndex++), entry.toNbt());
        }
        auctionsNbt.putInt("count", auctionIndex);
        auctionsNbt.putInt("nextId", nextAuctionId);
        nbt.put("auctions", auctionsNbt);
        return nbt;
    }

    public static AuctionState fromNbt(CompoundTag nbt) {
        AuctionState state = new AuctionState();
        CompoundTag auctionsNbt = nbt.getCompound("auctions");
        state.nextAuctionId = Math.max(1, auctionsNbt.getInt("nextId"));
        int auctionCount = auctionsNbt.getInt("count");
        for (int index = 0; index < auctionCount; index++) {
            state.auctionEntries.add(AuctionEntry.fromNbt(auctionsNbt.getCompound(String.valueOf(index))));
        }
        return state;
    }
}
