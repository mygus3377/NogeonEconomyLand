package com.nogeon.economyland.player;

import com.nogeon.economyland.state.EconomyState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class InventoryKeepService {
    public static final java.util.Set<java.util.UUID> ACTIVE_KEEP_PLAYERS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final String KEEP_INVENTORY_TAG = "NoGeonKeepInventory";
    private static final String SAVED_INVENTORY_TAG = "NoGeonKeepInventoryItems";

    private InventoryKeepService() {
    }

    public static void backupDroneData(ServerPlayer player) {
        CompoundTag persistedData = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag droneBackup = new CompoundTag();
        CompoundTag originalRoot = player.getPersistentData();
        
        for (String key : originalRoot.getAllKeys()) {
            if (key.startsWith("nogeon_")) {
                droneBackup.put(key, originalRoot.get(key).copy());
            }
        }
        
        if (!droneBackup.isEmpty()) {
            persistedData.put("NoGeonDroneBackup", droneBackup);
            if (!player.getPersistentData().contains(Player.PERSISTED_NBT_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
            }
            System.out.println("[InventoryKeepService] Backed up drone data (" + droneBackup.getAllKeys().size() + " keys) to Persisted NBT for " + player.getName().getString());
        }
    }

    public static void restoreDroneData(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.contains("NoGeonDroneBackup", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            CompoundTag droneBackup = persisted.getCompound("NoGeonDroneBackup");
            CompoundTag playerRoot = player.getPersistentData();
            
            int restoredCount = 0;
            for (String key : droneBackup.getAllKeys()) {
                playerRoot.put(key, droneBackup.get(key).copy());
                restoredCount++;
            }
            
            persisted.remove("NoGeonDroneBackup");
            System.out.println("[InventoryKeepService] Restored drone data (" + restoredCount + " keys) from backup for " + player.getName().getString());
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 1. 엔지니어 드론 관련 데이터 대피 (사망 리스폰 시 정보 소실 버그 완벽 방지)
        backupDroneData(player);

        if (player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
            return;
        }

        CompoundTag persistedData = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persistedData.getBoolean(KEEP_INVENTORY_TAG)) {
            System.out.println("[InventoryKeepService] Already processed keep inventory for " + player.getName().getString() + ", skipping duplicate check.");
            return;
        }

        if (hasAndConsumeScroll(player)) {
            ACTIVE_KEEP_PLAYERS.add(player.getUUID());
            ListTag savedInventory = player.getInventory().save(new ListTag());
            if (!player.getPersistentData().contains(Player.PERSISTED_NBT_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
            }
            persistedData.putBoolean(KEEP_INVENTORY_TAG, true);
            persistedData.put(SAVED_INVENTORY_TAG, savedInventory);
            
            // 경험치 백업 추가
            persistedData.putInt("NoGeonKeepXpLevel", player.experienceLevel);
            persistedData.putFloat("NoGeonKeepXpProgress", player.experienceProgress);
            persistedData.putInt("NoGeonKeepTotalXp", player.totalExperience);
            
            // NBT가 확실하게 보존되도록 강제 저장 조치
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistedData);
            
            System.out.println("[InventoryKeepService] Saved " + savedInventory.size() + " inventory items and experience (Level " + player.experienceLevel + ") for " + player.getName().getString() + " and consumed 1 scroll.");
            player.getInventory().clearContent();
            
            // 경험치 드롭 방지를 위해 0으로 만듦
            player.experienceLevel = 0;
            player.experienceProgress = 0.0F;
            player.totalExperience = 0;
        }
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        Player original = event.getOriginal();
        Player player = event.getEntity();

        // 1. 엔지니어 드론 데이터 백업 복구 (사망 리스폰 시 드론 초기화 및 무기/스탯/보관함 소실 해결)
        // Forge가 original의 PlayerPersisted를 새 플레이어에게 복사해 주므로 player 객체에서 바로 복구할 수 있음.
        restoreDroneData(player);

        // 만약 original 객체에 여전히 백업이 남아있다면 안전하게 직접 이식 시도
        CompoundTag originalPersisted = original.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (originalPersisted.contains("NoGeonDroneBackup", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            CompoundTag droneBackup = originalPersisted.getCompound("NoGeonDroneBackup");
            CompoundTag playerRoot = player.getPersistentData();
            for (String key : droneBackup.getAllKeys()) {
                playerRoot.put(key, droneBackup.get(key).copy());
            }
            originalPersisted.remove("NoGeonDroneBackup");
        }

        // 기존의 루트 NBT 직접 복사 방식도 안전망으로 유지
        net.minecraft.nbt.CompoundTag originalRoot = original.getPersistentData();
        net.minecraft.nbt.CompoundTag playerRoot = player.getPersistentData();
        for (String key : originalRoot.getAllKeys()) {
            if (key.startsWith("nogeon_")) {
                playerRoot.put(key, originalRoot.get(key).copy());
            }
        }

        ACTIVE_KEEP_PLAYERS.remove(original.getUUID());
        ACTIVE_KEEP_PLAYERS.remove(player.getUUID());

        if (player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
            return;
        }

        if (originalPersisted.getBoolean(KEEP_INVENTORY_TAG)) {
            if (originalPersisted.contains(SAVED_INVENTORY_TAG, net.minecraft.nbt.Tag.TAG_LIST)) {
                player.getInventory().load(originalPersisted.getList(SAVED_INVENTORY_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND));
            }
            
            // 경험치 복구
            if (originalPersisted.contains("NoGeonKeepXpLevel")) {
                player.experienceLevel = originalPersisted.getInt("NoGeonKeepXpLevel");
                player.experienceProgress = originalPersisted.getFloat("NoGeonKeepXpProgress");
                player.totalExperience = originalPersisted.getInt("NoGeonKeepTotalXp");
            }
            
            originalPersisted.remove(KEEP_INVENTORY_TAG);
            originalPersisted.remove(SAVED_INVENTORY_TAG);
            originalPersisted.remove("NoGeonKeepXpLevel");
            originalPersisted.remove("NoGeonKeepXpProgress");
            originalPersisted.remove("NoGeonKeepTotalXp");
            
            // 신규 플레이어에 복사된 persistent data에서도 확실히 제거
            CompoundTag playerPersisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            playerPersisted.remove(KEEP_INVENTORY_TAG);
            playerPersisted.remove(SAVED_INVENTORY_TAG);
            playerPersisted.remove("NoGeonKeepXpLevel");
            playerPersisted.remove("NoGeonKeepXpProgress");
            playerPersisted.remove("NoGeonKeepTotalXp");
        }
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
                return;
            }

            CompoundTag persistedData = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            if (persistedData.getBoolean(KEEP_INVENTORY_TAG)) {
                event.getDrops().clear();
                event.setCanceled(true);
            }
        }
    }

    private static boolean hasAndConsumeScroll(ServerPlayer player) {
        EconomyState state = EconomyState.get(player.server);
        boolean consumed = state.profile(player.getUUID()).consumeInventoryKeepCharge();
        if (consumed) {
            state.setDirty();
        }
        return consumed;
    }

    public static void onPlayerRevived(team.creative.playerrevive.api.event.PlayerRevivedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 1. 드론 데이터 백업 복구 (빈사 부활 시 드론 초기화 방지)
        restoreDroneData(player);

        // 2. 인벤토리 보호권으로 백업된 인벤토리가 있다면 복구
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(KEEP_INVENTORY_TAG)) {
            if (persisted.contains(SAVED_INVENTORY_TAG, net.minecraft.nbt.Tag.TAG_LIST)) {
                player.getInventory().load(persisted.getList(SAVED_INVENTORY_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND));
                System.out.println("[InventoryKeepService] Restored " + persisted.getList(SAVED_INVENTORY_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND).size() + " items upon revive for " + player.getName().getString());
            }
            
            // 경험치 복구
            if (persisted.contains("NoGeonKeepXpLevel")) {
                player.experienceLevel = persisted.getInt("NoGeonKeepXpLevel");
                player.experienceProgress = persisted.getFloat("NoGeonKeepXpProgress");
                player.totalExperience = persisted.getInt("NoGeonKeepTotalXp");
            }
            
            persisted.remove(KEEP_INVENTORY_TAG);
            persisted.remove(SAVED_INVENTORY_TAG);
            persisted.remove("NoGeonKeepXpLevel");
            persisted.remove("NoGeonKeepXpProgress");
            persisted.remove("NoGeonKeepTotalXp");
            
            // 변경된 persisted NBT를 플레이어의 Persistent NBT에 반영
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
            ACTIVE_KEEP_PLAYERS.remove(player.getUUID());

            // 3. 인벤토리 동기화 패킷 전송 (컨테이너 및 인벤토리 양방향 강제 동기화)
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();
            com.nogeon.economyland.network.SyncPlayerInventoryPacket.send(player);
        }
    }

    public static void onExperienceDrop(net.minecraftforge.event.entity.living.LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            if (persisted.getBoolean(KEEP_INVENTORY_TAG)) {
                event.setDroppedExperience(0);
                event.setCanceled(true);
            }
        }
    }
}
