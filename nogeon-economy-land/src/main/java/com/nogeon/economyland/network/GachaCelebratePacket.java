package com.nogeon.economyland.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

public final class GachaCelebratePacket {
    private static final Map<UUID, PendingCelebration> PENDING = new ConcurrentHashMap<>();

    private final UUID token;

    public GachaCelebratePacket(UUID token) {
        this.token = token;
    }

    public static UUID queue(ServerPlayer player, ItemStack rewardStack) {
        UUID token = UUID.randomUUID();
        PENDING.put(token, new PendingCelebration(player.getUUID(), rewardStack.copy()));
        return token;
    }

    public static void encode(GachaCelebratePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.token);
    }

    public static GachaCelebratePacket decode(FriendlyByteBuf buffer) {
        return new GachaCelebratePacket(buffer.readUUID());
    }

    public static void handle(GachaCelebratePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            PendingCelebration pending = PENDING.remove(packet.token);
            if (pending == null || !pending.playerId().equals(player.getUUID())) {
                return;
            }
            celebrate(player, pending.rewardStack());
        });
        context.setPacketHandled(true);
    }

    public static Component getGachaItemName(ItemStack stack) {
        if (stack.isEmpty()) {
            return Component.literal("");
        }
        net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null && "tacz".equals(id.getNamespace()) && "modern_kinetic_gun".equals(id.getPath())) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("GunId", 8)) {
                String gunId = tag.getString("GunId");
                if (gunId.contains(":")) {
                    String[] split = gunId.split(":", 2);
                    return Component.translatable(split[0] + ".gun." + split[1] + ".name");
                } else {
                    return Component.translatable("tacz.gun." + gunId + ".name");
                }
            }
        }
        return stack.getHoverName();
    }

    private static void celebrate(ServerPlayer player, ItemStack rewardStack) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.6F, 0.95F);
        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.4F, 1.0F);
        launchFirework(level, player, new int[] {0xF8D86B, 0xFFF27A, 0xFFC94C});
        launchFirework(level, player, new int[] {0xFFE38A, 0xFFF6AD, 0xE8B84E});
        player.server.getPlayerList().broadcastSystemMessage(
            Component.translatable(
                "message.nogeon_economy_land.gacha.jackpot_broadcast",
                player.getDisplayName().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                getGachaItemName(rewardStack).copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal(String.valueOf(rewardStack.getCount())).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            ).withStyle(ChatFormatting.YELLOW),
            false
        );
    }

    private static void launchFirework(ServerLevel level, ServerPlayer player, int[] colors) {
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag fireworks = rocket.getOrCreateTagElement("Fireworks");
        fireworks.putByte("Flight", (byte) 0);
        ListTag explosions = new ListTag();
        CompoundTag explosion = new CompoundTag();
        explosion.putByte("Type", (byte) 1);
        explosion.putIntArray("Colors", colors);
        explosion.putBoolean("Trail", true);
        explosion.putBoolean("Flicker", true);
        explosions.add(explosion);
        fireworks.put("Explosions", explosions);

        FireworkRocketEntity entity = new FireworkRocketEntity(level, player.getX(), player.getY() + 2.2D, player.getZ(), rocket);
        entity.setDeltaMovement((player.getRandom().nextDouble() - 0.5D) * 0.06D, 0.05D, (player.getRandom().nextDouble() - 0.5D) * 0.06D);
        level.addFreshEntity(entity);
    }

    private record PendingCelebration(UUID playerId, ItemStack rewardStack) {
    }
}
