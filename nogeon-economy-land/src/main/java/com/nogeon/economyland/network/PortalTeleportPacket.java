package com.nogeon.economyland.network;

import com.nogeon.economyland.entity.ModEntities;
import com.nogeon.economyland.entity.PortalEntity;
import com.nogeon.economyland.item.ModItems;
import com.nogeon.economyland.player.HomeEntry;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

public final class PortalTeleportPacket {
    private final String homeName;

    public PortalTeleportPacket(String homeName) {
        this.homeName = homeName;
    }

    public static void encode(PortalTeleportPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.homeName);
    }

    public static PortalTeleportPacket decode(FriendlyByteBuf buffer) {
        return new PortalTeleportPacket(buffer.readUtf());
    }

    public static void handle(PortalTeleportPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || packet.homeName.isBlank()) {
                return;
            }

            // 1. 주문서 소지 여부 검증 및 소모
            ItemStack scrollStack = ItemStack.EMPTY;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(ModItems.PORTAL_SCROLL.get())) {
                    scrollStack = stack;
                    break;
                }
            }

            if (scrollStack.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.nogeon_economy_land.shop.no_money")
                    .withStyle(ChatFormatting.RED));
                return;
            }

            // 2. 홈 목록 조회
            EconomyState state = EconomyState.get(player.server);
            PlayerProfile profile = state.profile(player.getUUID());
            HomeEntry home = profile.homes().get(packet.homeName);
            if (home == null) {
                return;
            }

            ServerLevel targetLevel = player.server.getLevel(home.worldKey());
            if (targetLevel == null) {
                player.sendSystemMessage(Component.translatable("command.nogeon_economy_land.home.dimension_missing", home.name())
                    .withStyle(ChatFormatting.RED));
                return;
            }

            // 3. 포탈 엔티티 생성 및 연동
            ServerLevel currentLevel = player.serverLevel();
            BlockPos currentPos = player.blockPosition();
            Vec3 currentVec = player.position();

            PortalEntity originPortal = ModEntities.PORTAL_ENTITY.get().create(currentLevel);
            if (originPortal == null) return;
            originPortal.moveTo(currentVec.x, currentVec.y + 0.1D, currentVec.z, player.getYRot(), player.getXRot());
            UUID originUuid = originPortal.getUUID();

            PortalEntity targetPortal = ModEntities.PORTAL_ENTITY.get().create(targetLevel);
            if (targetPortal == null) {
                originPortal.discard();
                return;
            }
            targetPortal.moveTo(home.pos().getX() + 0.5D, home.pos().getY() + 0.1D, home.pos().getZ() + 0.5D, player.getYRot(), player.getXRot());
            UUID targetUuid = targetPortal.getUUID();

            // 포탈 셋업 (양방향 연결)
            originPortal.setup(home.worldKey().location().toString(), home.pos(), player.getUUID(), targetUuid);
            targetPortal.setup(currentLevel.dimension().location().toString(), currentPos, player.getUUID(), originUuid);

            // 월드에 엔티티 배치
            currentLevel.addFreshEntity(originPortal);
            targetLevel.addFreshEntity(targetPortal);

            // 주문서 1개 소모
            scrollStack.shrink(1);

            // 4. 플레이어 이동
            levelSound(currentLevel, currentVec.x, currentVec.y, currentVec.z);
            player.teleportTo(targetLevel, home.pos().getX() + 0.5D, home.pos().getY() + 0.1D, home.pos().getZ() + 0.5D, player.getYRot(), player.getXRot());
            levelSound(targetLevel, home.pos().getX() + 0.5D, home.pos().getY() + 0.1D, home.pos().getZ() + 0.5D);
        });
        context.setPacketHandled(true);
    }

    private static void levelSound(ServerLevel level, double x, double y, double z) {
        level.playSound(null, x, y, z, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.5F, 1.2F);
    }
}
