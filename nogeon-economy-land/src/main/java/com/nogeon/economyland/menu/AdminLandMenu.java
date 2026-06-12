package com.nogeon.economyland.menu;

import com.nogeon.economyland.land.LandFlag;
import com.nogeon.economyland.land.LandRegion;
import com.nogeon.economyland.state.EconomyState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class AdminLandMenu extends AbstractContainerMenu {
    private final List<LandSummary> lands;

    public AdminLandMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.ADMIN_LAND.get(), containerId);
        lands = new ArrayList<>();
        int landCount = buffer.readVarInt();
        for (int i = 0; i < landCount; i++) {
            Map<String, Boolean> flags = new HashMap<>();
            int flagCount = buffer.readVarInt();
            for (int j = 0; j < flagCount; j++) {
                flags.put(buffer.readUtf(), buffer.readBoolean());
            }

            Map<String, String> permissions = new HashMap<>();
            int permCount = buffer.readVarInt();
            for (int j = 0; j < permCount; j++) {
                permissions.put(buffer.readUtf(), buffer.readUtf());
            }

            lands.add(new LandSummary(
                buffer.readVarInt(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readLong(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf(),
                flags,
                permissions
            ));
        }
    }

    public AdminLandMenu(int containerId, EconomyState state) {
        super(ModMenus.ADMIN_LAND.get(), containerId);
        lands = new ArrayList<>();
        for (LandRegion land : state.adminLands()) {
            Map<String, Boolean> flags = new HashMap<>();
            for (Map.Entry<LandFlag, Boolean> entry : land.flags().entrySet()) {
                flags.put(entry.getKey().id(), entry.getValue());
            }
            lands.add(new LandSummary(land.id(), land.type().translationKey(), land.world().location().toString(),
                land.blocks(), land.min().getX(), land.min().getY(), land.min().getZ(), land.memo(), flags, new HashMap<>()));
        }
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(lands.size());
        for (LandSummary land : lands) {
            buffer.writeVarInt(land.flags().size());
            for (Map.Entry<String, Boolean> entry : land.flags().entrySet()) {
                buffer.writeUtf(entry.getKey());
                buffer.writeBoolean(entry.getValue());
            }
            buffer.writeVarInt(land.permissions().size());
            for (Map.Entry<String, String> entry : land.permissions().entrySet()) {
                buffer.writeUtf(entry.getKey());
                buffer.writeUtf(entry.getValue());
            }
            buffer.writeVarInt(land.id());
            buffer.writeUtf(land.typeKey());
            buffer.writeUtf(land.world());
            buffer.writeLong(land.blocks());
            buffer.writeVarInt(land.x());
            buffer.writeVarInt(land.y());
            buffer.writeVarInt(land.z());
            buffer.writeUtf(land.memo());
        }
    }

    public List<LandSummary> lands() {
        return lands;
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
