package com.nogeon.economyland.player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum SkillNode {
    FARMER_MARKET_SENSE("farmer_market_sense", JobType.FARMER, false, 30, 420, 24, SkillNodeStat.DELIVERY_PRICE, 1),
    FARMER_FIELD_ROUTINE("farmer_field_routine", JobType.FARMER, false, 30, 205, 108, SkillNodeStat.EXP_GAIN, 1, "farmer_market_sense"),
    FARMER_SEED_SELECTION("farmer_seed_selection", JobType.FARMER, false, 30, 635, 108, SkillNodeStat.SPECIAL_CHANCE, 1, "farmer_market_sense"),
    FARMER_CART_TRAINING("farmer_cart_training", JobType.FARMER, false, 30, 120, 218, SkillNodeStat.DELIVERY_PRICE, 1, "farmer_field_routine"),
    FARMER_SOIL_STUDY("farmer_soil_study", JobType.FARMER, false, 30, 720, 218, SkillNodeStat.SPECIAL_CHANCE, 1, "farmer_seed_selection"),
    FARMER_BOUNTIFUL_HARVEST("farmer_bountiful_harvest", JobType.FARMER, true, 10, 70, 338, SkillNodeStat.NONE, 0, "farmer_cart_training"),
    FARMER_SUNLIT_STEP("farmer_sunlit_step", JobType.FARMER, true, 10, 390, 338, SkillNodeStat.NONE, 0, "farmer_field_routine", "farmer_seed_selection"),
    FARMER_FIELD_SNACK("farmer_field_snack", JobType.FARMER, true, 10, 700, 338, SkillNodeStat.NONE, 0, "farmer_soil_study"),
    FARMER_EARTH_MIRACLE("farmer_earth_miracle", JobType.FARMER, true, 10, 390, 440, SkillNodeStat.NONE, 0, "farmer_sunlit_step"),

    FISHER_MARKET_SENSE("fisher_market_sense", JobType.FISHER, false, 30, 420, 24, SkillNodeStat.DELIVERY_PRICE, 1),
    FISHER_LINE_ROUTINE("fisher_line_routine", JobType.FISHER, false, 30, 205, 108, SkillNodeStat.EXP_GAIN, 1, "fisher_market_sense"),
    FISHER_LURE_TUNING("fisher_lure_tuning", JobType.FISHER, false, 30, 635, 108, SkillNodeStat.SPECIAL_CHANCE, 1, "fisher_market_sense"),
    FISHER_NET_SORTING("fisher_net_sorting", JobType.FISHER, false, 30, 120, 218, SkillNodeStat.DELIVERY_PRICE, 1, "fisher_line_routine"),
    FISHER_CURRENT_READING("fisher_current_reading", JobType.FISHER, false, 30, 720, 218, SkillNodeStat.SPECIAL_CHANCE, 1, "fisher_lure_tuning"),
    FISHER_DOUBLE_HOOK("fisher_double_hook", JobType.FISHER, true, 10, 70, 338, SkillNodeStat.NONE, 0, "fisher_net_sorting"),
    FISHER_TIDAL_STEP("fisher_tidal_step", JobType.FISHER, true, 10, 390, 338, SkillNodeStat.NONE, 0, "fisher_line_routine", "fisher_lure_tuning"),
    FISHER_CALM_WATER("fisher_calm_water", JobType.FISHER, true, 10, 700, 338, SkillNodeStat.NONE, 0, "fisher_current_reading"),
    FISHER_TREASURE_HUNT("fisher_treasure_hunt", JobType.FISHER, true, 10, 390, 440, SkillNodeStat.NONE, 0, "fisher_tidal_step"),

    MINER_MARKET_SENSE("miner_market_sense", JobType.MINER, false, 30, 420, 24, SkillNodeStat.DELIVERY_PRICE, 1),
    MINER_TUNNEL_ROUTINE("miner_tunnel_routine", JobType.MINER, false, 30, 205, 108, SkillNodeStat.EXP_GAIN, 1, "miner_market_sense"),
    MINER_ORE_SENSE("miner_ore_sense", JobType.MINER, false, 30, 635, 108, SkillNodeStat.SPECIAL_CHANCE, 1, "miner_market_sense"),
    MINER_CART_LOADING("miner_cart_loading", JobType.MINER, false, 30, 120, 218, SkillNodeStat.DELIVERY_PRICE, 1, "miner_tunnel_routine"),
    MINER_FAULT_READING("miner_fault_reading", JobType.MINER, false, 30, 720, 218, SkillNodeStat.SPECIAL_CHANCE, 1, "miner_ore_sense"),
    MINER_VEIN_STRIKE("miner_vein_strike", JobType.MINER, true, 10, 70, 338, SkillNodeStat.NONE, 0, "miner_cart_loading"),
    MINER_STONE_SKIN("miner_stone_skin", JobType.MINER, true, 10, 390, 338, SkillNodeStat.NONE, 0, "miner_tunnel_routine", "miner_ore_sense"),
    MINER_DEEP_BREATH("miner_deep_breath", JobType.MINER, true, 10, 700, 338, SkillNodeStat.NONE, 0, "miner_fault_reading"),
    MINER_EYE_OPENING("miner_eye_opening", JobType.MINER, true, 10, 390, 440, SkillNodeStat.NONE, 0, "miner_stone_skin"),

    COOK_MARKET_SENSE("cook_market_sense", JobType.COOK, false, 30, 420, 24, SkillNodeStat.DELIVERY_PRICE, 1),
    COOK_PREP_ROUTINE("cook_prep_routine", JobType.COOK, false, 30, 205, 108, SkillNodeStat.EXP_GAIN, 1, "cook_market_sense"),
    COOK_SEASONING("cook_seasoning", JobType.COOK, false, 30, 635, 108, SkillNodeStat.SPECIAL_CHANCE, 1, "cook_market_sense"),
    COOK_KITCHEN_SERVICE("cook_kitchen_service", JobType.COOK, false, 30, 120, 218, SkillNodeStat.DELIVERY_PRICE, 1, "cook_prep_routine"),
    COOK_TASTE_MEMORY("cook_taste_memory", JobType.COOK, false, 30, 720, 218, SkillNodeStat.SPECIAL_CHANCE, 1, "cook_seasoning"),
    COOK_HEARTY_PORTION("cook_hearty_portion", JobType.COOK, true, 10, 70, 338, SkillNodeStat.NONE, 0, "cook_kitchen_service"),
    COOK_WARM_MEAL("cook_warm_meal", JobType.COOK, true, 10, 390, 338, SkillNodeStat.NONE, 0, "cook_prep_routine", "cook_seasoning"),
    COOK_CHEFS_SNACK("cook_chefs_snack", JobType.COOK, true, 10, 700, 338, SkillNodeStat.NONE, 0, "cook_taste_memory"),
    COOK_MASTER_RECIPE("cook_master_recipe", JobType.COOK, true, 10, 390, 440, SkillNodeStat.NONE, 0, "cook_warm_meal"),

    HUNTER_MARKET_SENSE("hunter_market_sense", JobType.HUNTER, false, 30, 420, 24, SkillNodeStat.DELIVERY_PRICE, 1),
    HUNTER_HUNT_ROUTINE("hunter_hunt_routine", JobType.HUNTER, false, 30, 205, 108, SkillNodeStat.EXP_GAIN, 1, "hunter_market_sense"),
    HUNTER_WEAPON_TUNING("hunter_weapon_tuning", JobType.HUNTER, false, 30, 635, 108, SkillNodeStat.SPECIAL_CHANCE, 1, "hunter_market_sense"),
    HUNTER_TROPHY_SENSE("hunter_trophy_sense", JobType.HUNTER, false, 30, 120, 218, SkillNodeStat.DELIVERY_PRICE, 1, "hunter_hunt_routine"),
    HUNTER_VITAL_READING("hunter_vital_reading", JobType.HUNTER, false, 30, 720, 218, SkillNodeStat.SPECIAL_CHANCE, 1, "hunter_weapon_tuning"),
    HUNTER_QUICK_DRAW("hunter_quick_draw", JobType.HUNTER, true, 10, 70, 338, SkillNodeStat.NONE, 0, "hunter_trophy_sense"),
    HUNTER_STEADY_AIM("hunter_steady_aim", JobType.HUNTER, true, 10, 390, 338, SkillNodeStat.NONE, 0, "hunter_hunt_routine", "hunter_weapon_tuning"),
    HUNTER_WILD_STEP("hunter_wild_step", JobType.HUNTER, true, 10, 700, 338, SkillNodeStat.NONE, 0, "hunter_vital_reading"),
    HUNTER_APEX_PREDATOR("hunter_apex_predator", JobType.HUNTER, true, 10, 390, 440, SkillNodeStat.NONE, 0, "hunter_steady_aim"),
    
    ENGINEER_MARKET_SENSE("engineer_market_sense", JobType.ENGINEER, false, 30, 420, 24, SkillNodeStat.DELIVERY_PRICE, 1),
    ENGINEER_ROTATION_ROUTINE("engineer_rotation_routine", JobType.ENGINEER, false, 30, 205, 108, SkillNodeStat.EXP_GAIN, 1, "engineer_market_sense"),
    ENGINEER_GEAR_SENSE("engineer_gear_sense", JobType.ENGINEER, false, 30, 635, 108, SkillNodeStat.SPECIAL_CHANCE, 1, "engineer_market_sense"),
    ENGINEER_SU_TRAINING("engineer_su_training", JobType.ENGINEER, false, 30, 120, 218, SkillNodeStat.DELIVERY_PRICE, 1, "engineer_rotation_routine"),
    ENGINEER_ASSEMBLY_STUDY("engineer_assembly_study", JobType.ENGINEER, false, 30, 720, 218, SkillNodeStat.SPECIAL_CHANCE, 1, "engineer_gear_sense"),
    ENGINEER_COMPRESSION("engineer_compression", JobType.ENGINEER, true, 10, 70, 338, SkillNodeStat.NONE, 0, "engineer_su_training"),
    ENGINEER_PERFECT_ASSEMBLY("engineer_perfect_assembly", JobType.ENGINEER, true, 10, 700, 338, SkillNodeStat.NONE, 0, "engineer_assembly_study"),
    ENGINEER_PROCESS_OPTIMIZATION("engineer_process_optimization", JobType.ENGINEER, true, 10, 390, 338, SkillNodeStat.NONE, 0, "engineer_rotation_routine", "engineer_gear_sense"),
    ENGINEER_KINETIC_BOOST("engineer_kinetic_boost", JobType.ENGINEER, true, 10, 390, 440, SkillNodeStat.NONE, 0, "engineer_process_optimization");

    private static final Map<String, SkillNode> BY_ID = new HashMap<>();
    private static final Map<JobType, List<SkillNode>> BY_JOB = new EnumMap<>(JobType.class);

    static {
        for (JobType job : JobType.values()) {
            BY_JOB.put(job, new ArrayList<>());
        }
        for (SkillNode node : values()) {
            BY_ID.put(node.id, node);
            BY_JOB.get(node.job).add(node);
        }
    }

    private final String id;
    private final JobType job;
    private final boolean large;
    private final int maxLevel;
    private final int x;
    private final int y;
    private final SkillNodeStat stat;
    private final int statValue;
    private final String[] prerequisites;

    SkillNode(String id, JobType job, boolean large, int maxLevel, int x, int y, SkillNodeStat stat, int statValue,
        String... prerequisites) {
        this.id = id;
        this.job = job;
        this.large = large;
        this.maxLevel = maxLevel;
        this.x = x;
        this.y = y;
        this.stat = stat;
        this.statValue = statValue;
        this.prerequisites = prerequisites;
    }

    public String id() {
        return id;
    }

    public JobType job() {
        return job;
    }

    public boolean large() {
        return large;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public SkillNodeStat stat() {
        return stat;
    }

    public int statValue() {
        return statValue;
    }

    public List<SkillNode> prerequisites() {
        List<SkillNode> nodes = new ArrayList<>(prerequisites.length);
        for (String prerequisite : prerequisites) {
            nodes.add(byId(prerequisite));
        }
        return nodes;
    }

    public String titleKey() {
        return "skill_node.nogeon_economy_land." + id;
    }

    public String descriptionKey() {
        return titleKey() + ".desc";
    }

    public static SkillNode byId(String id) {
        SkillNode node = BY_ID.get(id.toLowerCase(Locale.ROOT));
        if (node == null) {
            throw new IllegalArgumentException("Unknown skill node: " + id);
        }
        return node;
    }

    public static List<SkillNode> forJob(JobType job) {
        return List.copyOf(BY_JOB.get(job));
    }

    public static List<SkillNode> forJobId(String jobId) {
        return forJob(JobType.byId(jobId));
    }

    public static SkillNode deliveryNode(JobType job) {
        if (job == JobType.FARMER) return FARMER_MARKET_SENSE;
        if (job == JobType.FISHER) return FISHER_MARKET_SENSE;
        if (job == JobType.MINER) return MINER_MARKET_SENSE;
        if (job == JobType.COOK) return COOK_MARKET_SENSE;
        if (job == JobType.HUNTER) return HUNTER_MARKET_SENSE;
        if (job == JobType.ENGINEER) return ENGINEER_MARKET_SENSE;
        throw new IllegalArgumentException("Unknown job: " + job);
    }

    public static SkillNode expNode(JobType job) {
        if (job == JobType.FARMER) return FARMER_FIELD_ROUTINE;
        if (job == JobType.FISHER) return FISHER_LINE_ROUTINE;
        if (job == JobType.MINER) return MINER_TUNNEL_ROUTINE;
        if (job == JobType.COOK) return COOK_PREP_ROUTINE;
        if (job == JobType.HUNTER) return HUNTER_HUNT_ROUTINE;
        if (job == JobType.ENGINEER) return ENGINEER_ROTATION_ROUTINE;
        throw new IllegalArgumentException("Unknown job: " + job);
    }

    public static SkillNode specialtyNode(JobType job) {
        if (job == JobType.FARMER) return FARMER_SEED_SELECTION;
        if (job == JobType.FISHER) return FISHER_LURE_TUNING;
        if (job == JobType.MINER) return MINER_ORE_SENSE;
        if (job == JobType.COOK) return COOK_SEASONING;
        if (job == JobType.HUNTER) return HUNTER_WEAPON_TUNING;
        if (job == JobType.ENGINEER) return ENGINEER_GEAR_SENSE;
        throw new IllegalArgumentException("Unknown job: " + job);
    }

    public static SkillNode primaryEffectNode(JobType job) {
        if (job == JobType.FARMER) return FARMER_BOUNTIFUL_HARVEST;
        if (job == JobType.FISHER) return FISHER_DOUBLE_HOOK;
        if (job == JobType.MINER) return MINER_VEIN_STRIKE;
        if (job == JobType.COOK) return COOK_HEARTY_PORTION;
        if (job == JobType.HUNTER) return HUNTER_QUICK_DRAW;
        if (job == JobType.ENGINEER) return ENGINEER_COMPRESSION;
        throw new IllegalArgumentException("Unknown job: " + job);
    }

    public static SkillNode secondaryEffectNode(JobType job) {
        if (job == JobType.FARMER) return FARMER_FIELD_SNACK;
        if (job == JobType.FISHER) return FISHER_CALM_WATER;
        if (job == JobType.MINER) return MINER_DEEP_BREATH;
        if (job == JobType.COOK) return COOK_CHEFS_SNACK;
        if (job == JobType.HUNTER) return HUNTER_WILD_STEP;
        if (job == JobType.ENGINEER) return ENGINEER_PERFECT_ASSEMBLY;
        throw new IllegalArgumentException("Unknown job: " + job);
    }

    public static SkillNode tertiaryEffectNode(JobType job) {
        if (job == JobType.FARMER) return FARMER_SUNLIT_STEP;
        if (job == JobType.FISHER) return FISHER_TIDAL_STEP;
        if (job == JobType.MINER) return MINER_STONE_SKIN;
        if (job == JobType.COOK) return COOK_WARM_MEAL;
        if (job == JobType.HUNTER) return HUNTER_STEADY_AIM;
        if (job == JobType.ENGINEER) return ENGINEER_PROCESS_OPTIMIZATION;
        throw new IllegalArgumentException("Unknown job: " + job);
    }
}
