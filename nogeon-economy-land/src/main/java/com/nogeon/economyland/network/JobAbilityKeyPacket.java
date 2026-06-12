package com.nogeon.economyland.network;

import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.SkillNode;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public final class JobAbilityKeyPacket {
    private final int slot;

    public JobAbilityKeyPacket(int slot) {
        this.slot = slot;
    }

    public static void encode(JobAbilityKeyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.slot);
    }

    public static JobAbilityKeyPacket decode(FriendlyByteBuf buffer) {
        return new JobAbilityKeyPacket(buffer.readVarInt());
    }

    public static void handle(JobAbilityKeyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            EconomyState state = EconomyState.get(sender.server);
            PlayerProfile profile = state.profile(sender.getUUID());
            JobType job = profile.selectedJob();
            if (job == JobType.MINER) {
                toggleMiner(sender, state, profile, packet.slot);
            } else if (job == JobType.HUNTER) {
                if (packet.slot == 1) {
                    toggleHunterSense(sender, state, profile);
                } else if (packet.slot == 2) {
                    // 사냥감의 표식 (MB4로 통합)
                    MarkHunterPreyPacket.handle(new MarkHunterPreyPacket(), contextSupplier);
                }
            } else if (job == JobType.FISHER) {
                if (packet.slot == 1) {
                    // 미끼 뿌리기 (MB3로 통합)
                    RequestCastBaitPacket.handle(new RequestCastBaitPacket(), contextSupplier);
                }
            } else if (job == JobType.FARMER) {
                if (packet.slot == 1) {
                    // 신선한 참 (MB3로 통합)
                    handleFarmerSnack(sender, state, profile);
                }
            } else if (job == JobType.COOK) {
                if (packet.slot == 2) {
                    // 나만의 레시피 화면 열기 (MB4로 통합)
                    RequestOpenCookRecipePacket.handle(new RequestOpenCookRecipePacket(), contextSupplier);
                }
            } else if (job == JobType.ENGINEER) {
                if (packet.slot == 1) {
                    // Mouse 5 triggers Compression GUI
                    com.nogeon.economyland.job.JobEvents.handleEngineerSkill(sender, state, profile, 1);
                } else if (packet.slot == 2) {
                    // Mouse 4 triggers Perpetual Engine (Kinetic Boost)
                    com.nogeon.economyland.job.JobEvents.handleEngineerSkill(sender, state, profile, 2);
                }
            } else {
                sender.displayClientMessage(Component.literal("§7현재 직업에는 이 단축키에 배정된 스킬이 없습니다."), true);
            }
        });
        context.setPacketHandled(true);
    }

    private static void handleFarmerSnack(ServerPlayer sender, EconomyState state, PlayerProfile profile) {
        int snackLevel = profile.job(JobType.FARMER).nodeLevel(SkillNode.FARMER_FIELD_SNACK);
        if (snackLevel <= 0) {
            sender.displayClientMessage(Component.literal("§c먼저 [신선한 참] 스킬을 배워야 합니다."), true);
            return;
        }
        net.minecraft.nbt.CompoundTag nbt = sender.getPersistentData();
        int energy = nbt.getInt("nogeon_farmer_energy");
        if (energy < 100) {
            sender.displayClientMessage(Component.literal("§c[신선한 참] 에너지가 아직 부족합니다! (§e" + energy + "%§f / 100%)"), true);
            return;
        }
        nbt.putInt("nogeon_farmer_energy", 0);
        sender.getFoodData().eat(20, 1.0F);
        sender.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 20 * 60, 2));
        sender.displayClientMessage(Component.literal("§6[신선한 참] §f체력이 보충되고 기운이 솟아납니다!"), false);
        sender.level().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
            net.minecraft.sounds.SoundEvents.PLAYER_BURP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void toggleMiner(ServerPlayer sender, EconomyState state, PlayerProfile profile, int slot) {
        int eyeLevel = profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_EYE_OPENING);
        int eyeRadius = eyeLevel > 0 ? Math.min(28, 8 + eyeLevel * 2) : 0;
        if (slot == 1) {
            if (profile.job(JobType.MINER).nodeLevel(SkillNode.MINER_STONE_SKIN) <= 0) {
                sender.displayClientMessage(Component.literal("§c먼저 [우월한 신체] 스킬을 배워야 합니다."), true);
                return;
            }
            boolean next = !profile.minerBodyActive();
            profile.setMinerBodyActive(next);
            state.setDirty();
            sender.displayClientMessage(Component.literal("§6[우월한 신체] §f스킬이 " + (next ? "§a활성화(ON)" : "§c비활성화(OFF)") + "§f되었습니다."), true);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender),
                new SyncMinerAbilityPacket(next, profile.minerEyeActive(), eyeRadius));
        } else if (slot == 2) {
            if (eyeLevel <= 0) {
                sender.displayClientMessage(Component.literal("§c먼저 [개안] 스킬을 배워야 합니다."), true);
                return;
            }
            boolean next = !profile.minerEyeActive();
            if (next && sender.getHealth() <= sender.getMaxHealth() * 0.05F) {
                sender.displayClientMessage(Component.literal("§c[개안] §f체력이 부족하여 개안을 활성화할 수 없습니다."), true);
                return;
            }
            profile.setMinerEyeActive(next);
            state.setDirty();
            sender.displayClientMessage(Component.literal("§b[개안] §f스킬이 " + (next ? "§a활성화(ON)" : "§c비활성화(OFF)") + "§f되었습니다."), true);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender),
                new SyncMinerAbilityPacket(profile.minerBodyActive(), next, eyeRadius));
        }
    }

    private static void toggleHunterSense(ServerPlayer sender, EconomyState state, PlayerProfile profile) {
        int quickDrawLevel = profile.job(JobType.HUNTER).nodeLevel(SkillNode.HUNTER_QUICK_DRAW);
        if (quickDrawLevel <= 0) {
            sender.displayClientMessage(Component.literal("§c먼저 [추적자의 감각] 스킬을 배워야 합니다."), true);
            return;
        }
        boolean next = !profile.hunterSenseActive();
        if (next && sender.getFoodData().getFoodLevel() <= 2) {
            sender.displayClientMessage(Component.literal("§c허기가 너무 부족하여 추적 상태에 진입할 수 없습니다!"), true);
            return;
        }
        profile.setHunterSenseActive(next);
        if (next) {
            profile.setHunterSenseTicks(0);
        }
        state.setDirty();
        int radius = Math.min(42, 12 + quickDrawLevel * 3);
        if (next) {
            com.nogeon.economyland.job.JobEvents.applyHunterSenseGlow(sender, radius);
        } else {
            com.nogeon.economyland.job.JobEvents.clearHunterSenseGlow(sender, radius);
        }
        sender.displayClientMessage(Component.literal("§2[추적자의 감각] §f스킬이 " + (next ? "§a활성화(ON)" : "§c비활성화(OFF)") + "§f되었습니다."), true);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender),
            new SyncHunterAbilityPacket(next, radius, profile.hunterPreyMarkedUUID()));
    }
}
