package com.nogeon.economyland.player;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;

public final class JobProgress {
    public static final int MAX_LEVEL = 300;
    private final JobType jobType;
    private long totalExp; // 누적 경험치를 기준값으로 사용
    private final EnumMap<SkillNode, Integer> skillNodes = new EnumMap<>(SkillNode.class);

    public JobProgress(JobType jobType) {
        this.jobType = jobType;
        for (SkillNode node : SkillNode.values()) {
            skillNodes.put(node, 0);
        }
    }

    public int level() {
        return calculateLevel(this.totalExp);
    }

    public int exp() {
        int level = level();
        if (level >= MAX_LEVEL) return 0;
        return (int) (this.totalExp - totalExpForLevel(level));
    }

    public int skillPoints() {
        int spent = 0;
        for (Map.Entry<SkillNode, Integer> entry : skillNodes.entrySet()) {
            if (entry.getKey().job() == jobType) {
                spent += entry.getValue() * upgradeCost(entry.getKey());
            }
        }
        return Math.max(0, (level() - 1) - spent);
    }

    public int nodeLevel(SkillNode node) {
        return skillNodes.getOrDefault(node, 0);
    }

    public boolean hasUnlocked(SkillNode node) {
        return nodeLevel(node) > 0;
    }

    public int bonusPercent(SkillNodeStat stat) {
        int total = 0;
        for (Map.Entry<SkillNode, Integer> entry : skillNodes.entrySet()) {
            if (entry.getKey().job() == jobType && entry.getKey().stat() == stat) {
                int level = Math.min(entry.getValue(), entry.getKey().maxLevel());
                if (entry.getKey() == SkillNode.ENGINEER_ROTATION_ROUTINE) {
                    // 회전 속도 학습 스킬은 스펙상 레벨당 1.5% 증가 (30레벨 완충 시 45% 증가)
                    total += (int) Math.round(level * 1.5D);
                } else {
                    total += level * entry.getKey().statValue();
                }
            }
        }
        return total;
    }

    public int upgradeCost(SkillNode node) {
        return node.large() ? 3 : 1;
    }

    public boolean canUpgrade(SkillNode node) {
        if (node.job() != jobType) {
            return false;
        }
        int cost = upgradeCost(node);
        if (skillPoints() < cost) {
            return false;
        }
        if (nodeLevel(node) >= node.maxLevel()) {
            return false;
        }
        if (node.large()) {
            if (nodeLevel(node) >= level() / 5) {
                return false;
            }
        }
        for (SkillNode prerequisite : node.prerequisites()) {
            if (!hasUnlocked(prerequisite)) {
                return false;
            }
        }
        return true;
    }

    public boolean upgrade(SkillNode node) {
        if (!canUpgrade(node)) {
            return false;
        }
        skillNodes.put(node, nodeLevel(node) + 1);
        return true;
    }

    public int skillLevel(SkillType type) {
        return nodeLevel(switch (type) {
            case DELIVERY_BONUS -> SkillNode.deliveryNode(jobType);
            case EFFICIENCY -> SkillNode.expNode(jobType);
            case CONVENIENCE -> SkillNode.specialtyNode(jobType);
        });
    }

    public boolean upgrade(SkillType type) {
        return upgrade(switch (type) {
            case DELIVERY_BONUS -> SkillNode.deliveryNode(jobType);
            case EFFICIENCY -> SkillNode.expNode(jobType);
            case CONVENIENCE -> SkillNode.specialtyNode(jobType);
        });
    }

    public void addExp(int amount) {
        this.totalExp += Math.max(0, amount);
        long maxExp = totalExpForLevel(MAX_LEVEL);
        if (this.totalExp > maxExp) {
            this.totalExp = maxExp;
        }
    }

    // 새로운 레벨업 필요 경험치 공식 (더 완만하게 수정)
    public int expToNextLevel() {
        return expToNextLevel(level());
    }

    public int expToNextLevel(int level) {
        if (level >= MAX_LEVEL) return 0;
        // 새 공식: 150 + (L-1)*100 + (L-1)^2 * 5
        return 150 + (level - 1) * 100 + (level - 1) * (level - 1) * 5;
    }

    // 특정 레벨에 도달하기 위한 누적 경험치 총합
    public long totalExpForLevel(int targetLevel) {
        long total = 0;
        for (int i = 1; i < targetLevel; i++) {
            total += expToNextLevel(i);
        }
        return total;
    }

    private int calculateLevel(long totalExp) {
        int left = 1;
        int right = MAX_LEVEL;
        int result = 1;
        
        while (left <= right) {
            int mid = (left + right) / 2;
            if (totalExpForLevel(mid) <= totalExp) {
                result = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return result;
    }

    public void setLevel(int level) {
        this.totalExp = totalExpForLevel(Math.max(1, Math.min(MAX_LEVEL, level)));
    }

    public void reset() {
        this.totalExp = 0;
        for (SkillNode node : SkillNode.forJob(jobType)) {
            skillNodes.put(node, 0);
        }
    }

    public void resetSkills() {
        for (SkillNode node : SkillNode.forJob(jobType)) {
            skillNodes.put(node, 0);
        }
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("totalExp", totalExp); // totalExp만 저장
        CompoundTag skillsNbt = new CompoundTag();
        for (Map.Entry<SkillNode, Integer> entry : skillNodes.entrySet()) {
            if (entry.getKey().job() == jobType && entry.getValue() > 0) {
                skillsNbt.putInt(entry.getKey().id(), entry.getValue());
            }
        }
        nbt.put("skillNodes", skillsNbt);
        return nbt;
    }

    public static JobProgress fromNbt(JobType jobType, CompoundTag nbt) {
        JobProgress progress = new JobProgress(jobType);
        
        // 1. 새로운 데이터 포맷(totalExp)이 있는 경우
        if (nbt.contains("totalExp")) {
            progress.totalExp = nbt.getLong("totalExp");
        } 
        // 2. 과거 데이터 포맷만 있는 경우 마이그레이션 수행
        else if (nbt.contains("level")) {
            int oldLevel = nbt.getInt("level");
            int oldExp = nbt.getInt("exp");
            
            // 과거 공식 기반으로 누적 경험치 역산
            // 과거 공식: f(L) = 100 + (L-1)^2 * 25 + (L-1) * 100
            long oldTotal = 0;
            for (int i = 1; i < oldLevel; i++) {
                oldTotal += (100 + (i - 1) * (i - 1) * 25 + (i - 1) * 100);
            }
            oldTotal += oldExp;
            progress.totalExp = oldTotal;
        }

        CompoundTag nodesNbt = nbt.contains("skillNodes") ? nbt.getCompound("skillNodes") : new CompoundTag();
        if (!nodesNbt.isEmpty()) {
            for (SkillNode node : SkillNode.forJob(jobType)) {
                progress.skillNodes.put(node, Math.min(node.maxLevel(), Math.max(0, nodesNbt.getInt(node.id()))));
            }
        } else if (nbt.contains("skills")) {
            CompoundTag skillsNbt = nbt.getCompound("skills");
            SkillNode delivery = SkillNode.deliveryNode(jobType);
            SkillNode expNode = SkillNode.expNode(jobType);
            SkillNode specialty = SkillNode.specialtyNode(jobType);
            progress.skillNodes.put(delivery, Math.min(delivery.maxLevel(), Math.max(0, skillsNbt.getInt("delivery_bonus"))));
            progress.skillNodes.put(expNode, Math.min(expNode.maxLevel(), Math.max(0, skillsNbt.getInt("efficiency"))));
            progress.skillNodes.put(specialty, Math.min(specialty.maxLevel(), Math.max(0, skillsNbt.getInt("convenience"))));
            
            if (skillsNbt.getInt("convenience") >= 1) progress.skillNodes.put(SkillNode.primaryEffectNode(jobType), 1);
            if (skillsNbt.getInt("convenience") >= 2) progress.skillNodes.put(SkillNode.secondaryEffectNode(jobType), 1);
            if (skillsNbt.getInt("convenience") >= 3) progress.skillNodes.put(SkillNode.tertiaryEffectNode(jobType), 1);
        }
        
        return progress;
    }
}
