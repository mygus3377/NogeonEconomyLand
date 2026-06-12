package com.nogeon.economyland.menu;

import com.nogeon.economyland.state.GachaRewardResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class GachaMenu extends AbstractContainerMenu {
    private final String actionId;
    private final String traderDatabaseId;
    private final String categoryId;
    private final long pricePerRoll;
    private final int selectedCount;
    private final List<GachaRewardResult> results;
    private final UUID celebrationToken;

    public GachaMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.GACHA.get(), containerId);
        actionId = buffer.readUtf();
        traderDatabaseId = buffer.readUtf();
        categoryId = buffer.readUtf();
        pricePerRoll = buffer.readLong();
        selectedCount = buffer.readVarInt();
        int resultCount = buffer.readVarInt();
        List<GachaRewardResult> decoded = new ArrayList<>(resultCount);
        for (int index = 0; index < resultCount; index++) {
            decoded.add(GachaRewardResult.read(buffer));
        }
        results = List.copyOf(decoded);
        celebrationToken = buffer.readBoolean() ? buffer.readUUID() : null;
    }

    public GachaMenu(int containerId, String actionId, String categoryId, long pricePerRoll, int selectedCount) {
        this(containerId, "", actionId, categoryId, pricePerRoll, selectedCount, List.of(), null);
    }

    public GachaMenu(int containerId, String actionId, String categoryId, long pricePerRoll, int selectedCount, List<GachaRewardResult> results) {
        this(containerId, "", actionId, categoryId, pricePerRoll, selectedCount, results, null);
    }

    public GachaMenu(int containerId, String actionId, String categoryId, long pricePerRoll, int selectedCount, List<GachaRewardResult> results, UUID celebrationToken) {
        this(containerId, "", actionId, categoryId, pricePerRoll, selectedCount, results, celebrationToken);
    }

    public GachaMenu(int containerId, String traderDatabaseId, String actionId, String categoryId, long pricePerRoll, int selectedCount, List<GachaRewardResult> results, UUID celebrationToken) {
        super(ModMenus.GACHA.get(), containerId);
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.actionId = actionId;
        this.categoryId = categoryId;
        this.pricePerRoll = pricePerRoll;
        this.selectedCount = selectedCount;
        this.results = List.copyOf(results);
        this.celebrationToken = celebrationToken;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(actionId);
        buffer.writeUtf(traderDatabaseId);
        buffer.writeUtf(categoryId);
        buffer.writeLong(pricePerRoll);
        buffer.writeVarInt(selectedCount);
        buffer.writeVarInt(results.size());
        for (GachaRewardResult result : results) {
            result.write(buffer);
        }
        buffer.writeBoolean(celebrationToken != null);
        if (celebrationToken != null) {
            buffer.writeUUID(celebrationToken);
        }
    }

    public String actionId() {
        return actionId;
    }

    public String traderDatabaseId() {
        return traderDatabaseId;
    }

    public String categoryId() {
        return categoryId;
    }

    public long pricePerRoll() {
        return pricePerRoll;
    }

    public int selectedCount() {
        return selectedCount;
    }

    public List<GachaRewardResult> results() {
        return results;
    }

    public boolean hasResults() {
        return !results.isEmpty();
    }

    public boolean hasCelebrationToken() {
        return celebrationToken != null;
    }

    public UUID celebrationToken() {
        return celebrationToken;
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
