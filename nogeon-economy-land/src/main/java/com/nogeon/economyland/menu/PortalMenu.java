package com.nogeon.economyland.menu;

import com.nogeon.economyland.player.HomeEntry;
import com.nogeon.economyland.player.PlayerProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class PortalMenu extends AbstractContainerMenu {
    private final List<HomeSummary> homes;

    // 클라이언트 사이드 생성자 (버퍼로부터 읽음)
    public PortalMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.PORTAL.get(), containerId);
        this.homes = new ArrayList<>();
        int count = buffer.readVarInt();
        for (int i = 0; i < count; i++) {
            this.homes.add(new HomeSummary(
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf()
            ));
        }
    }

    // 서버 사이드 생성자
    public PortalMenu(int containerId, PlayerProfile profile) {
        super(ModMenus.PORTAL.get(), containerId);
        this.homes = new ArrayList<>();
        for (Map.Entry<String, HomeEntry> entry : profile.homes().entrySet()) {
            HomeEntry home = entry.getValue();
            this.homes.add(new HomeSummary(
                entry.getKey(),
                home.worldKey().location().toString(),
                home.pos().getX(),
                home.pos().getY(),
                home.pos().getZ(),
                home.memo()
            ));
        }
    }

    // 클라이언트로 직렬화하여 전송할 때 사용
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.homes.size());
        for (HomeSummary home : this.homes) {
            buffer.writeUtf(home.name());
            buffer.writeUtf(home.world());
            buffer.writeVarInt(home.x());
            buffer.writeVarInt(home.y());
            buffer.writeVarInt(home.z());
            buffer.writeUtf(home.memo());
        }
    }

    public List<HomeSummary> homes() {
        return this.homes;
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
