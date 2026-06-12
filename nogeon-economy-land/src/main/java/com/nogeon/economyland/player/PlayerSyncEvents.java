package com.nogeon.economyland.player;

import com.nogeon.economyland.item.ModItems;
import com.nogeon.economyland.item.ReforgeService;
import com.nogeon.economyland.network.SyncCosmeticArmorPacket;
import com.nogeon.economyland.network.SyncCreditsPacket;
import com.nogeon.economyland.state.EconomyState;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class PlayerSyncEvents {
    private static final int STARTER_PACKAGE_VERSION = 1;
    private static final UUID STARTER_BOOTS_SPEED_UUID = UUID.fromString("f00f3f35-3baf-4d0e-a72e-bf51cc5705ab");
    private static final String STARTER_CLIMATE_TAG = "NoGeonStarterClimateGuard";

    private PlayerSyncEvents() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EconomyState state = EconomyState.get(player.server);
            state.rememberPlayer(player.getUUID(), player.getGameProfile().getName());
            state.deliverPendingRewards(player);
            PlayerProfile profile = state.profile(player.getUUID());
            grantStarterPackage(player, profile, state);
            migrateReforgedItems(player, profile, state);
            PvpFlagBridge.setPvpEnabled(player, !profile.peacefulFlag());
            SyncCreditsPacket.send(player, profile.credits());
            syncCosmeticArmor(player, state);
            SyncCosmeticArmorPacket.broadcast(player.server, player.getUUID(), profile);
            
            // 광부 스킬 상태 클라이언트 동기화
            int stoneSkinLevel = profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_STONE_SKIN);
            int eyeLevel = profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_EYE_OPENING);
            boolean minerSelected = profile.selectedJob() == JobType.MINER;
            if (minerSelected && stoneSkinLevel > 0 && !profile.minerBodyActive()
                && !player.getPersistentData().getBoolean("nogeon_miner_state_repaired_v1")) {
                profile.setMinerBodyActive(true);
                player.getPersistentData().putBoolean("nogeon_miner_state_repaired_v1", true);
                state.setDirty();
            }
            boolean bodyActive = minerSelected && stoneSkinLevel > 0 && profile.minerBodyActive();
            boolean eyeActive = minerSelected && eyeLevel > 0 && profile.minerEyeActive();
            if (!minerSelected && (profile.minerBodyActive() || profile.minerEyeActive())) {
                profile.setMinerBodyActive(false);
                profile.setMinerEyeActive(false);
                state.setDirty();
            }
            com.nogeon.economyland.network.ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new com.nogeon.economyland.network.SyncMinerAbilityPacket(bodyActive, eyeActive, eyeLevel > 0 ? Math.min(28, 8 + eyeLevel * 2) : 0)
            );
            int hunterSenseLevel = profile.job(JobType.HUNTER).nodeLevel(SkillNode.HUNTER_QUICK_DRAW);
            boolean hunterSelected = profile.selectedJob() == JobType.HUNTER;
            boolean hunterSenseActive = hunterSelected && hunterSenseLevel > 0 && profile.hunterSenseActive();
            if (!hunterSelected && (profile.hunterSenseActive() || !profile.hunterPreyMarkedUUID().isEmpty())) {
                profile.setHunterSenseActive(false);
                profile.setHunterSenseTicks(0);
                profile.setHunterPreyMarkedUUID("");
                state.setDirty();
            }
            int hunterRadius = hunterSenseActive ? Math.min(42, 12 + hunterSenseLevel * 3) : 0;
            com.nogeon.economyland.network.ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new com.nogeon.economyland.network.SyncHunterAbilityPacket(hunterSenseActive, hunterRadius,
                    hunterSenseActive ? profile.hunterPreyMarkedUUID() : "")
            );
            com.nogeon.economyland.job.JobEvents.syncFisherDataToPlayer(player);

            PlayerDisplayNameManager.refresh(player, profile);
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EconomyState.get(player.server).cancelTrade(player.getUUID(), "logout");
        }
    }

    private static void grantStarterPackage(ServerPlayer player, PlayerProfile profile, EconomyState state) {
        if (profile.starterPackageVersion() >= STARTER_PACKAGE_VERSION) {
            return;
        }
        
        if (!profile.starterLedgerGranted()) {
            giveItem(player, new ItemStack(ModItems.ECONOMY_LEDGER.get()));
            profile.setStarterLedgerGranted(true);
        }

        profile.addCredits(30000);
        giveItem(player, starterWeapon());
        giveItem(player, starterTool(new ItemStack(Items.IRON_PICKAXE), true));
        giveItem(player, starterTool(new ItemStack(Items.IRON_AXE), false));
        giveItem(player, starterTool(new ItemStack(Items.IRON_SHOVEL), false));
        giveItem(player, starterTool(new ItemStack(Items.IRON_HOE), false));
        giveItem(player, starterArmor(new ItemStack(Items.IRON_HELMET), EquipmentSlot.HEAD));
        giveItem(player, starterArmor(new ItemStack(Items.IRON_CHESTPLATE), EquipmentSlot.CHEST));
        giveItem(player, starterArmor(new ItemStack(Items.IRON_LEGGINGS), EquipmentSlot.LEGS));
        giveItem(player, starterBoots());
        giveItem(player, new ItemStack(Items.BREAD, 16));
        giveItem(player, new ItemStack(Items.COOKED_BEEF, 8));
        giveItem(player, new ItemStack(Items.CARROT, 16));

        // 5. Welcome Message
        player.displayClientMessage(Component.literal("[NoGeon] 패치 스타터팩을 지급했습니다. 초반용 철 장비, 음식, 기후 보호 장비가 포함되어 있습니다.").withStyle(ChatFormatting.GOLD), false);
        
        profile.setStarterPackageVersion(STARTER_PACKAGE_VERSION);
        profile.setStarterLedgerGranted(true);
        state.setDirty();
    }

    private static void giveItem(ServerPlayer player, ItemStack stack) {
        ExtendedInventoryDelivery.giveOrDrop(player, stack);
    }

    public static boolean isStarterClimateArmor(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrCreateTag().getBoolean(STARTER_CLIMATE_TAG);
    }

    private static ItemStack starterWeapon() {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        stack.enchant(Enchantments.SHARPNESS, 1);
        stack.enchant(Enchantments.UNBREAKING, 1);
        markStarter(stack, "초심자의 철검");
        return stack;
    }

    private static ItemStack starterTool(ItemStack stack, boolean mainPickaxe) {
        stack.enchant(Enchantments.BLOCK_EFFICIENCY, mainPickaxe ? 2 : 1);
        stack.enchant(Enchantments.UNBREAKING, mainPickaxe ? 2 : 1);
        markStarter(stack, mainPickaxe ? "초심자의 철 곡괭이" : "초심자의 철 도구");
        return stack;
    }

    private static ItemStack starterArmor(ItemStack stack, EquipmentSlot slot) {
        stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 1);
        stack.enchant(Enchantments.UNBREAKING, 1);
        stack.getOrCreateTag().putBoolean(STARTER_CLIMATE_TAG, true);
        markStarter(stack, starterArmorName(slot));
        return stack;
    }

    private static ItemStack starterBoots() {
        ItemStack stack = starterArmor(new ItemStack(Items.IRON_BOOTS), EquipmentSlot.FEET);
        stack.addAttributeModifier(Attributes.MOVEMENT_SPEED,
            new AttributeModifier(STARTER_BOOTS_SPEED_UUID, "nogeon.starter_boots_speed", 0.05D, AttributeModifier.Operation.MULTIPLY_TOTAL),
            EquipmentSlot.FEET);
        return stack;
    }

    private static String starterArmorName(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "초심자의 방한 투구";
            case CHEST -> "초심자의 방열 흉갑";
            case LEGS -> "초심자의 방호 각반";
            case FEET -> "초심자의 여행 장화";
            default -> "초심자의 방어구";
        };
    }

    private static void markStarter(ItemStack stack, String name) {
        stack.setHoverName(Component.literal(name).withStyle(ChatFormatting.AQUA));
        ListTag lore = new ListTag();
        lore.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7[스타터팩] 초반 정착용 장비"))));
        if (stack.getItem() instanceof ArmorItem) {
            lore.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("§b추위와 더위를 약하게 막아줍니다."))));
        }
        stack.getOrCreateTagElement("display").put("Lore", lore);
    }

    private static void syncCosmeticArmor(ServerPlayer target, EconomyState state) {
        for (ServerPlayer player : target.server.getPlayerList().getPlayers()) {
            SyncCosmeticArmorPacket.send(target, player.getUUID(), state.profile(player.getUUID()));
        }
    }

    private static void migrateReforgedItems(ServerPlayer player, PlayerProfile profile, EconomyState state) {
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            changed |= ReforgeService.migrateBalance(player.getInventory().getItem(slot));
        }

        ItemStack[] extItems = ExtendedInventoryDelivery.load(profile.extInventoryData());
        boolean extChanged = false;
        int unlockedSlots = Math.min(extItems.length, Math.max(0, profile.inventoryExtLevel() * 9));
        for (int slot = 0; slot < unlockedSlots; slot++) {
            extChanged |= ReforgeService.migrateBalance(extItems[slot]);
        }
        if (extChanged) {
            profile.setExtInventoryData(ExtendedInventoryDelivery.save(extItems));
            changed = true;
        }

        for (ItemStack backpack : ExtendedInventoryDelivery.findAllBackpacks(player)) {
            changed |= migrateBackpack(backpack);
        }

        if (changed) {
            player.inventoryMenu.broadcastChanges();
            state.setDirty();
        }
    }

    private static boolean migrateBackpack(ItemStack backpack) {
        if (backpack.isEmpty()) {
            return false;
        }
        var capability = backpack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
        if (!capability.isPresent()) {
            return false;
        }
        var handler = capability.orElse(null);
        if (!(handler instanceof net.minecraftforge.items.IItemHandlerModifiable modifiable)) {
            return false;
        }

        boolean changed = false;
        for (int slot = 0; slot < modifiable.getSlots(); slot++) {
            ItemStack stored = modifiable.getStackInSlot(slot);
            if (ReforgeService.migrateBalance(stored)) {
                modifiable.setStackInSlot(slot, stored);
                changed = true;
            }
        }
        return changed;
    }

    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer targetPlayer && event.getEntity() instanceof ServerPlayer player) {
            EconomyState state = EconomyState.get(targetPlayer.server);
            PlayerProfile targetProfile = state.profile(targetPlayer.getUUID());
            SyncCosmeticArmorPacket.send(player, targetPlayer.getUUID(), targetProfile);
        }
    }
}
