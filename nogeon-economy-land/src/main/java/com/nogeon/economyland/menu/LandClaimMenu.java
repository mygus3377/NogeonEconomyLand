package com.nogeon.economyland.menu;

import com.nogeon.economyland.land.LandSelection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class LandClaimMenu extends AbstractContainerMenu {
    private final String modeId;
    private final boolean hasSelection;
    private final String typeKey;
    private final long blocks;
    private final long price;
    private final int discountPercent;
    private final int cuboidCount;
    private String memo = "";

    public LandClaimMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.LAND_CLAIM.get(), containerId);
        modeId = buffer.readUtf();
        hasSelection = buffer.readBoolean();
        typeKey = buffer.readUtf();
        blocks = buffer.readLong();
        price = buffer.readLong();
        discountPercent = buffer.readVarInt();
        cuboidCount = buffer.readVarInt();
        memo = buffer.readUtf();
    }

    public LandClaimMenu(int containerId, Mode mode, LandSelection selection, long price, int discountPercent) {
        super(ModMenus.LAND_CLAIM.get(), containerId);
        modeId = mode.id;
        hasSelection = selection != null && !selection.cuboids().isEmpty();
        typeKey = selection == null ? "" : selection.type().translationKey();
        blocks = selection == null ? 0L : selection.blocks();
        this.price = price;
        this.discountPercent = discountPercent;
        this.cuboidCount = selection == null ? 0 : selection.cuboids().size();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(modeId);
        buffer.writeBoolean(hasSelection);
        buffer.writeUtf(typeKey);
        buffer.writeLong(blocks);
        buffer.writeLong(price);
        buffer.writeVarInt(discountPercent);
        buffer.writeVarInt(cuboidCount);
        buffer.writeUtf(memo);
    }

    public Mode mode() { return Mode.byId(modeId); }
    public boolean hasSelection() { return hasSelection; }
    public String typeKey() { return typeKey; }
    public long blocks() { return blocks; }
    public long price() { return price; }
    public int discountPercent() { return discountPercent; }
    public int cuboidCount() { return cuboidCount; }
    public String memo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public enum Mode {
        PROMPT("prompt"),
        OPTIONS("options");

        private final String id;

        Mode(String id) {
            this.id = id;
        }

        public static Mode byId(String id) {
            for (Mode mode : values()) {
                if (mode.id.equals(id)) {
                    return mode;
                }
            }
            return OPTIONS;
        }
    }
}
