package com.nogeon.economyland.item;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;

import java.lang.reflect.Field;

/**
 * TaCZ 총기의 재장전 속도를 "reload_mastery" 재련 스탯에 맞게 가속시키는 핸들러.
 *
 * 기존 접근(ModernKineticGunScriptAPI.getReloadTime()을 Mixin으로 후킹)은
 * TaCZ 1.1.5에서 거의 모든 총이 Lua 스크립트 경로를 사용하기 때문에 효과가 없었습니다.
 * Lua 스크립트 경로에서는 defaultTickReload()가 호출되지 않으므로 getReloadTime() 믹스인이 무시됩니다.
 *
 * 이 핸들러는 서버 틱마다 재장전 중인 플레이어의 ShooterDataHolder.reloadTimestamp를
 * 직접 리플렉션으로 과거로 밀어서, 경과 시간을 부풀리는 방식으로 재장전을 가속합니다.
 * reloadTimestamp는 Lua 스크립트/기본 로직 모두에서 공통으로 사용되는 필드이므로
 * 어떤 경로에서든 일관되게 가속 효과가 적용됩니다.
 */
public final class ReloadBoostHandler {

    // 리플렉션 캐시
    private static Field reloadTimestampField;
    private static Field dataHolderField;
    private static boolean reflectionFailed;
    private static long lastDebugLog;

    private ReloadBoostHandler() {}

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                boostReloadForPlayer(player);
            } catch (Exception ignored) {
                // 개별 플레이어 처리 실패는 무시하고 계속 진행
            }
        }
    }

    private static void boostReloadForPlayer(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) {
            return;
        }

        // reload_mastery 재련 값 확인
        double boost = SmithEvents.reforgeValue(mainHand, "reload_mastery");
        if (boost <= 0) {
            boost = 0;
        }

        try {
            // IGunOperator를 통해 내부 데이터에 접근
            IGunOperator gunOperator = IGunOperator.fromLivingEntity(player);
            if (gunOperator == null) {
                return;
            }

            // ShooterDataHolder에서 reloadTimestamp 가져오기
            ShooterDataHolder dataHolder = gunOperator.getDataHolder();
            if (dataHolder == null) {
                return;
            }

            ItemStack gunStack = dataHolder.currentGunItem != null ? dataHolder.currentGunItem.get() : ItemStack.EMPTY;
            if (!gunStack.isEmpty()) {
                boost = Math.max(boost, SmithEvents.reforgeValue(gunStack, "reload_mastery"));
            }
            
            // 영구 기관 작동 시 재장전 속도 30% 추가 보너스
            if (player.getPersistentData().contains("nogeon_engineer_kinetic_boost_ticks")) {
                boost += 0.30D;
            }
            
            if (boost <= 0) {
                return;
            }

            long reloadTimestamp = dataHolder.reloadTimestamp;
            if (reloadTimestamp == -1L || dataHolder.reloadStateType == null || !dataHolder.reloadStateType.isReloading()) {
                // 재장전 중이 아님
                return;
            }

            // 현재 경과 시간 확인
            long elapsed = System.currentTimeMillis() - reloadTimestamp;
            if (elapsed <= 0) {
                return;
            }

            // boost에 비례한 시간 가속
            // 매 서버 틱(50ms)마다 boost 비율만큼 추가 경과시간을 부여
            // 예: boost = 0.275 (27.5%) → 매 틱 50ms * 0.275 = 13.75ms 추가 가속
            double clampedBoost = Math.min(0.8D, boost);
            long now = System.currentTimeMillis();
            if (now - lastDebugLog >= 1000L) {
                lastDebugLog = now;
                NoGeonEconomyLand.LOGGER.info(
                    "TaCZ reload server debug: player={}, item={}, boost={}, state={}, elapsed={}",
                    player.getGameProfile().getName(),
                    gunStack.getHoverName().getString(),
                    boost,
                    dataHolder.reloadStateType,
                    elapsed
                );
            }
            long adjustment = Math.round(50.0D * clampedBoost / (1.0D - clampedBoost));
            if (adjustment > 0) {
                // reloadTimestamp를 과거로 밀어서 경과시간 부풀리기
                dataHolder.reloadTimestamp = reloadTimestamp - adjustment;
            }
        } catch (Exception ignored) {
            // 리플렉션 실패 시 조용히 무시
        }
    }

    /**
     * IGunOperator.fromLivingEntity(player) 호출
     */
    private static Object getGunOperator(ServerPlayer player) {
        try {
            Class<?> iGunOperator = Class.forName("com.tacz.guns.api.entity.IGunOperator");
            java.lang.reflect.Method fromLivingEntity = iGunOperator.getMethod("fromLivingEntity", net.minecraft.world.entity.LivingEntity.class);
            return fromLivingEntity.invoke(null, player);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * LivingEntityShootManager (IGunOperator 구현체)에서 ShooterDataHolder를 가져옴
     */
    private static Object getDataHolder(Object gunOperator) {
        if (dataHolderField != null) {
            try {
                return dataHolderField.get(gunOperator);
            } catch (Exception e) {
                return null;
            }
        }

        // ShooterDataHolder 타입의 필드 찾기
        try {
            Class<?> shooterDataHolderClass = Class.forName("com.tacz.guns.entity.shooter.ShooterDataHolder");
            for (Field field : gunOperator.getClass().getDeclaredFields()) {
                if (shooterDataHolderClass.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    dataHolderField = field;
                    return field.get(gunOperator);
                }
            }
        } catch (Exception e) {
            // 실패 시 null 반환
        }
        return null;
    }

    /**
     * ShooterDataHolder.reloadTimestamp 필드 값 가져오기
     */
    private static long getReloadTimestamp(Object dataHolder) {
        try {
            ensureReloadTimestampField(dataHolder);
            if (reloadTimestampField != null) {
                return reloadTimestampField.getLong(dataHolder);
            }
        } catch (Exception ignored) {}
        return -1L;
    }

    /**
     * ShooterDataHolder.reloadTimestamp 필드 값 설정
     */
    private static void setReloadTimestamp(Object dataHolder, long value) {
        try {
            ensureReloadTimestampField(dataHolder);
            if (reloadTimestampField != null) {
                reloadTimestampField.setLong(dataHolder, value);
            }
        } catch (Exception ignored) {}
    }

    private static void ensureReloadTimestampField(Object dataHolder) {
        if (reloadTimestampField != null) {
            return;
        }
        try {
            reloadTimestampField = dataHolder.getClass().getDeclaredField("reloadTimestamp");
            reloadTimestampField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // 필드명이 다를 수 있으므로 long 타입 필드 중 이름에 reload와 timestamp가 포함된 것 탐색
            for (Field field : dataHolder.getClass().getDeclaredFields()) {
                if (field.getType() == long.class) {
                    String name = field.getName().toLowerCase();
                    if (name.contains("reload") && name.contains("timestamp")) {
                        field.setAccessible(true);
                        reloadTimestampField = field;
                        return;
                    }
                }
            }
            // 실패 시 long 필드를 순서대로 탐색 (reloadTimestamp가 첫 번째 long 필드일 가능성)
            // 바이트코드에서 확인: reloadTimestamp는 ShooterDataHolder의 주요 필드
            reflectionFailed = true;
        } catch (Exception e) {
            reflectionFailed = true;
        }
    }
}
