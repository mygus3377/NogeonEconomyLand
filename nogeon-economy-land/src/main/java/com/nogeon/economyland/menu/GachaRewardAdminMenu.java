package com.nogeon.economyland.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GachaRewardAdminMenu extends AbstractContainerMenu {
    private final String traderDatabaseId;
    private final String categoryId;
    private final int page;
    private final int totalCount;
    private final List<ShopLine> lines;

    public GachaRewardAdminMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.GACHA_REWARD_ADMIN.get(), containerId);
        traderDatabaseId = buffer.readUtf();
        categoryId = buffer.readUtf();
        page = buffer.readVarInt();
        totalCount = buffer.readVarInt();
        int count = buffer.readVarInt();
        lines = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String kindId = buffer.readUtf();
            String entryId = buffer.readUtf();
            Item item = BuiltInRegistries.ITEM.get(buffer.readResourceLocation());
            int stackCount = buffer.readVarInt();
            ItemStack stack = new ItemStack(item == null ? Items.AIR : item, Math.max(1, stackCount));
            CompoundTag tag = buffer.readNbt();
            if (tag != null && !tag.isEmpty()) {
                stack.setTag(tag);
            }
            lines.add(new ShopLine(kindId, entryId, stack, buffer.readLong(), buffer.readVarInt(), false));
        }
    }

    public GachaRewardAdminMenu(int containerId, String traderDatabaseId, List<ShopLine> lines) {
        this(containerId, traderDatabaseId, GachaCategory.ITEM.id(), 0, lines.size(), lines);
    }

    public GachaRewardAdminMenu(int containerId, String traderDatabaseId, String categoryId, List<ShopLine> lines) {
        this(containerId, traderDatabaseId, categoryId, 0, lines.size(), lines);
    }

    public GachaRewardAdminMenu(int containerId, String traderDatabaseId, String categoryId, int page, int totalCount, List<ShopLine> lines) {
        super(ModMenus.GACHA_REWARD_ADMIN.get(), containerId);
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.categoryId = categoryId == null || categoryId.isBlank() ? GachaCategory.ITEM.id() : GachaCategory.byId(categoryId).id();
        this.page = Math.max(0, page);
        this.totalCount = Math.max(0, totalCount);
        this.lines = List.copyOf(lines);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(traderDatabaseId);
        buffer.writeUtf(categoryId);
        buffer.writeVarInt(page);
        buffer.writeVarInt(totalCount);
        buffer.writeVarInt(lines.size());
        for (ShopLine line : lines) {
            ItemStack stack = line.stack();
            buffer.writeUtf(line.kindId());
            buffer.writeUtf(line.id());
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            buffer.writeResourceLocation(itemId == null ? BuiltInRegistries.ITEM.getKey(Items.AIR) : itemId);
            buffer.writeVarInt(stack.getCount());
            buffer.writeNbt(stack.getTag());
            buffer.writeLong(line.price());
            buffer.writeVarInt(line.remaining());
        }
    }

    public String traderDatabaseId() {
        return traderDatabaseId;
    }

    public String categoryId() {
        return categoryId;
    }

    public int page() {
        return page;
    }

    public int totalCount() {
        return totalCount;
    }

    public List<ShopLine> lines() {
        return lines;
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
