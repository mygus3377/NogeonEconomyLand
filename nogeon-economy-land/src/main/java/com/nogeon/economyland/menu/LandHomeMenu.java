package com.nogeon.economyland.menu;

import com.nogeon.economyland.land.LandFlag;
import com.nogeon.economyland.land.LandRegion;
import com.nogeon.economyland.player.HomeEntry;
import com.nogeon.economyland.player.PlayerProfile;
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

public final class LandHomeMenu extends AbstractContainerMenu {
    private final List<HomeSummary> homes;
    private final List<LandSummary> lands;
    private final List<String> knownPlayers;
    private final boolean operator;

    public LandHomeMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.LAND_HOME.get(), containerId);
        int count = buffer.readVarInt();
        homes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            homes.add(new HomeSummary(
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf()
            ));
        }
        int landCount = buffer.readVarInt();
        lands = new ArrayList<>();
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
        int playerCount = buffer.readVarInt();
        knownPlayers = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            knownPlayers.add(buffer.readUtf());
        }
        operator = buffer.readBoolean();
    }

    public LandHomeMenu(int containerId, PlayerProfile profile, EconomyState state, java.util.UUID owner, boolean operator) {
        super(ModMenus.LAND_HOME.get(), containerId);
        homes = new ArrayList<>();
        for (Map.Entry<String, HomeEntry> entry : profile.homes().entrySet()) {
            HomeEntry home = entry.getValue();
            homes.add(new HomeSummary(entry.getKey(), home.worldKey().location().toString(),
                home.pos().getX(), home.pos().getY(), home.pos().getZ(), home.memo()));
        }
        lands = new ArrayList<>();
        for (LandRegion land : state.landsOf(owner)) {
            Map<String, Boolean> flags = new HashMap<>();
            for (Map.Entry<LandFlag, Boolean> entry : land.flags().entrySet()) {
                flags.put(entry.getKey().id(), entry.getValue());
            }
            Map<String, String> permissions = new HashMap<>();
            for (Map.Entry<java.util.UUID, com.nogeon.economyland.land.LandPermission> entry : land.permissions().entrySet()) {
                if (entry.getValue() != com.nogeon.economyland.land.LandPermission.NONE) {
                    String name = state.knownPlayers().get(entry.getKey());
                    if (name == null) {
                        name = entry.getKey().toString().substring(0, 8);
                    }
                    permissions.put(name, entry.getValue().id());
                }
            }
            lands.add(new LandSummary(land.id(), land.type().translationKey(), land.world().location().toString(),
                land.blocks(), land.min().getX(), land.min().getY(), land.min().getZ(), land.memo(), flags, permissions));
        }
        knownPlayers = new ArrayList<>(state.knownPlayers().values());
        knownPlayers.sort(String::compareToIgnoreCase);
        this.operator = operator;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(homes.size());
        for (HomeSummary home : homes) {
            buffer.writeUtf(home.name());
            buffer.writeUtf(home.world());
            buffer.writeVarInt(home.x());
            buffer.writeVarInt(home.y());
            buffer.writeVarInt(home.z());
            buffer.writeUtf(home.memo());
        }
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
        buffer.writeVarInt(knownPlayers.size());
        for (String name : knownPlayers) {
            buffer.writeUtf(name);
        }
        buffer.writeBoolean(operator);
    }

    public List<HomeSummary> homes() {
        return homes;
    }

    public List<LandSummary> lands() {
        return lands;
    }

    public List<String> knownPlayers() {
        return knownPlayers;
    }

    public boolean operator() {
        return operator;
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
