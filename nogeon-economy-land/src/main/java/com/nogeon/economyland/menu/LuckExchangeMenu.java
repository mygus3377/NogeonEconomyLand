package com.nogeon.economyland.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class LuckExchangeMenu extends AbstractContainerMenu {
    private final int virtualTokenCount;
    private final int itemTokenCount;
    private final List<LuckExchangeOffer> offers;

    public LuckExchangeMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.LUCK_EXCHANGE.get(), containerId);
        virtualTokenCount = buffer.readVarInt();
        itemTokenCount = buffer.readVarInt();
        int offerCount = buffer.readVarInt();
        List<LuckExchangeOffer> decoded = new ArrayList<>(offerCount);
        for (int index = 0; index < offerCount; index++) {
            decoded.add(new LuckExchangeOffer(buffer.readUtf(), buffer.readUtf(), buffer.readVarInt(), buffer.readUtf(), buffer.readVarInt()));
        }
        offers = List.copyOf(decoded);
    }

    public LuckExchangeMenu(int containerId, int virtualTokenCount, int itemTokenCount, List<LuckExchangeOffer> offers) {
        super(ModMenus.LUCK_EXCHANGE.get(), containerId);
        this.virtualTokenCount = virtualTokenCount;
        this.itemTokenCount = itemTokenCount;
        this.offers = List.copyOf(offers);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(virtualTokenCount);
        buffer.writeVarInt(itemTokenCount);
        buffer.writeVarInt(offers.size());
        for (LuckExchangeOffer offer : offers) {
            buffer.writeUtf(offer.id());
            buffer.writeUtf(offer.labelKey());
            buffer.writeVarInt(offer.tokenCost());
            buffer.writeUtf(offer.rewardItemId());
            buffer.writeVarInt(offer.rewardCount());
        }
    }

    public int tokenCount() {
        return virtualTokenCount;
    }

    public int virtualTokenCount() {
        return virtualTokenCount;
    }

    public int itemTokenCount() {
        return itemTokenCount;
    }

    public List<LuckExchangeOffer> offers() {
        return offers;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
