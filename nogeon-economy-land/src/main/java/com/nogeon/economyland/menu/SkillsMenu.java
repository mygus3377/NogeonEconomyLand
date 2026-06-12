package com.nogeon.economyland.menu;

import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.SkillNode;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class SkillsMenu extends AbstractContainerMenu {
    private final String jobId;
    private final int jobLevel;
    private final int skillPoints;
    private final LinkedHashMap<String, Integer> nodeLevels = new LinkedHashMap<>();

    public SkillsMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.SKILLS.get(), containerId);
        jobId = buffer.readUtf();
        jobLevel = buffer.readVarInt();
        skillPoints = buffer.readVarInt();
        int nodeCount = buffer.readVarInt();
        for (int index = 0; index < nodeCount; index++) {
            nodeLevels.put(buffer.readUtf(), buffer.readVarInt());
        }
    }

    public SkillsMenu(int containerId, PlayerProfile profile) {
        super(ModMenus.SKILLS.get(), containerId);
        JobType job = profile.selectedJob();
        jobId = job.id();
        jobLevel = profile.job(job).level();
        skillPoints = profile.job(job).skillPoints();
        for (SkillNode node : SkillNode.forJob(job)) {
            nodeLevels.put(node.id(), profile.job(job).nodeLevel(node));
        }
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(jobId);
        buffer.writeVarInt(jobLevel);
        buffer.writeVarInt(skillPoints);
        buffer.writeVarInt(nodeLevels.size());
        for (Map.Entry<String, Integer> entry : nodeLevels.entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeVarInt(entry.getValue());
        }
    }

    public String jobId() {
        return jobId;
    }

    public int jobLevel() {
        return jobLevel;
    }

    public int skillPoints() {
        return skillPoints;
    }

    public int level(SkillNode node) {
        return nodeLevels.getOrDefault(node.id(), 0);
    }

    public int upgradeCost(SkillNode node) {
        return node.large() ? 3 : 1;
    }

    public boolean canUpgrade(SkillNode node) {
        if (!jobId.equals(node.job().id())) {
            return false;
        }
        int cost = upgradeCost(node);
        if (skillPoints < cost) {
            return false;
        }
        if (level(node) >= node.maxLevel()) {
            return false;
        }
        if (node.large()) {
            if (level(node) >= jobLevel / 5) {
                return false;
            }
        }
        for (SkillNode prerequisite : node.prerequisites()) {
            if (level(prerequisite) <= 0) {
                return false;
            }
        }
        return true;
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
