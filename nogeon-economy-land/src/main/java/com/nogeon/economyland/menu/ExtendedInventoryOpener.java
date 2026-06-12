package com.nogeon.economyland.menu;

import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

public final class ExtendedInventoryOpener {
    private ExtendedInventoryOpener() {
    }

    public static void open(ServerPlayer player) {
        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());

        // SimpleContainer 생성 (최대 10페이지 = 270칸)
        SimpleContainer extInventory = new SimpleContainer(270);

        // NBT로부터 아이템 로드
        CompoundTag extNbt = profile.extInventoryData();
        if (extNbt != null && extNbt.contains("Items", Tag.TAG_LIST)) {
            ListTag listTag = extNbt.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag itemNbt = listTag.getCompound(i);
                int slot = itemNbt.getInt("Slot");
                if (slot >= 0 && slot < extInventory.getContainerSize()) {
                    extInventory.setItem(slot, ItemStack.of(itemNbt));
                }
            }
        }

        int extLevel = Math.max(0, profile.inventoryExtLevel());
        // 초기에는 0페이지부터 시작
        int initialPage = 0;

        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, playerInventory, opener) -> new ExtendedInventoryMenu(containerId, playerInventory, extInventory, extLevel, initialPage),
            Component.translatable("screen.nogeon_economy_land.extended_inventory")
        ), (FriendlyByteBuf buffer) -> {
            buffer.writeVarInt(extLevel);
            buffer.writeVarInt(initialPage);
        });
    }
}
