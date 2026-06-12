package com.nogeon.economyland.network;

import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.player.ExtendedInventoryDelivery;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public final class CompressionActionPacket {
    private final boolean isResponse;
    private final int materialType; // 1: Cobble, 2: Deepslate, 3: Limestone, 4: Raw Metal, 5: Metal Ingot
    private final ItemStack rolledGem;
    private final double rolledPercent;
    private final int expGained;
    private final int creditsGained;
    private final int count; // Client request count

    // Client Request Constructor
    public CompressionActionPacket(int materialType, int count) {
        this.isResponse = false;
        this.materialType = materialType;
        this.count = count;
        this.rolledGem = ItemStack.EMPTY;
        this.rolledPercent = 0.0D;
        this.expGained = 0;
        this.creditsGained = 0;
    }

    // Server Response Constructor
    public CompressionActionPacket(int materialType, ItemStack rolledGem, double rolledPercent, int expGained, int creditsGained) {
        this.isResponse = true;
        this.materialType = materialType;
        this.count = 1;
        this.rolledGem = rolledGem;
        this.rolledPercent = rolledPercent;
        this.expGained = expGained;
        this.creditsGained = creditsGained;
    }

    public static void encode(CompressionActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.isResponse);
        buffer.writeInt(packet.materialType);
        if (packet.isResponse) {
            buffer.writeItem(packet.rolledGem);
            buffer.writeDouble(packet.rolledPercent);
            buffer.writeInt(packet.expGained);
            buffer.writeInt(packet.creditsGained);
        } else {
            buffer.writeInt(packet.count);
        }
    }

    public static CompressionActionPacket decode(FriendlyByteBuf buffer) {
        boolean isResponse = buffer.readBoolean();
        int materialType = buffer.readInt();
        if (isResponse) {
            ItemStack rolledGem = buffer.readItem();
            double rolledPercent = buffer.readDouble();
            int expGained = buffer.readInt();
            int creditsGained = buffer.readInt();
            return new CompressionActionPacket(materialType, rolledGem, rolledPercent, expGained, creditsGained);
        }
        int count = buffer.readInt();
        return new CompressionActionPacket(materialType, count);
    }

    public static void handle(CompressionActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (packet.isResponse) {
                // Client Side Handling
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.nogeon.economyland.client.ClientPacketHandler.handleCompressionResult(
                        packet.materialType, packet.rolledGem, packet.rolledPercent, packet.expGained, packet.creditsGained
                    );
                });
            } else {
                // Server Side Handling
                ServerPlayer player = context.getSender();
                if (player == null) return;

                EconomyState state = EconomyState.get(player.server);
                PlayerProfile profile = state.profile(player.getUUID());

                if (profile.selectedJob() != JobType.ENGINEER) {
                    return;
                }

                int compressionLevel = profile.job(JobType.ENGINEER).nodeLevel(com.nogeon.economyland.player.SkillNode.ENGINEER_COMPRESSION);
                if (compressionLevel <= 0) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c먼저 [자원 압축] 스킬을 배워야 합니다."), true);
                    return;
                }

                // Determine material identifiers and quantities
                int requiredCount = 0;
                int baseExp = 0;
                int baseCredits = 0;

                java.util.List<ItemStack> targetStacks = new java.util.ArrayList<>();

                switch (packet.materialType) {
                    case 1 -> { // Cobble
                        targetStacks.add(new ItemStack(net.minecraft.world.item.Items.COBBLESTONE));
                        requiredCount = 500;
                        baseExp = 10;
                        baseCredits = 50;
                    }
                    case 2 -> { // Deepslate
                        targetStacks.add(new ItemStack(net.minecraft.world.item.Items.DEEPSLATE));
                        requiredCount = 500;
                        baseExp = 10;
                        baseCredits = 50;
                    }
                    case 3 -> { // Limestone
                        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation("create:limestone"));
                        if (item != net.minecraft.world.item.Items.AIR) {
                            targetStacks.add(new ItemStack(item));
                        }
                        requiredCount = 300;
                        baseExp = 20;
                        baseCredits = 100;
                    }
                    case 4 -> { // Raw Metals (Copper or Zinc)
                        targetStacks.add(new ItemStack(net.minecraft.world.item.Items.RAW_COPPER));
                        net.minecraft.world.item.Item zinc = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation("create:raw_zinc"));
                        if (zinc != net.minecraft.world.item.Items.AIR) {
                            targetStacks.add(new ItemStack(zinc));
                        }
                        requiredCount = 100;
                        baseExp = 40;
                        baseCredits = 200;
                    }
                    case 5 -> { // Metal Ingots (Copper or Zinc)
                        targetStacks.add(new ItemStack(net.minecraft.world.item.Items.COPPER_INGOT));
                        net.minecraft.world.item.Item zinc = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation("create:zinc_ingot"));
                        if (zinc != net.minecraft.world.item.Items.AIR) {
                            targetStacks.add(new ItemStack(zinc));
                        }
                        requiredCount = 100;
                        baseExp = 40;
                        baseCredits = 200;
                    }
                }

                if (targetStacks.isEmpty()) return;

                // Count total owned
                int totalOwned = 0;
                for (ItemStack target : targetStacks) {
                    totalOwned += ExtendedInventoryDelivery.countAllOwned(player, target);
                }

                int maxCompressCount = totalOwned / requiredCount;
                int actualCount = Math.min(packet.count, maxCompressCount);
                if (actualCount <= 0) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c압축할 재료가 부족합니다."), true);
                    return;
                }

                ItemStack bestGem = ItemStack.EMPTY;
                double bestRoll = 99999.0D;
                int bestTier = 999;
                
                int totalExp = 0;
                int totalCredits = 0;

                double rareBoost = 1.0D + 0.02D * Math.min(10, Math.max(0, compressionLevel));

                for (int i = 0; i < actualCount; i++) {
                    // Consume resources
                    int remainingToConsume = requiredCount;
                    for (ItemStack target : targetStacks) {
                        if (remainingToConsume <= 0) break;
                        int consumed = ExtendedInventoryDelivery.consumeAllOwned(player, target, remainingToConsume);
                        remainingToConsume -= consumed;
                    }

                    // Roll gem
                    double roll = player.getRandom().nextDouble() * 10000.0D;
                    ItemStack gemResult;

                    if (roll < 0.5D * rareBoost) {
                        gemResult = new ItemStack(com.nogeon.economyland.item.ModItems.PERFECT_ENHANCEMENT_GEM.get());
                    } else if (roll < 3.0D * rareBoost) {
                        gemResult = new ItemStack(com.nogeon.economyland.item.ModItems.FLAWLESS_ENHANCEMENT_GEM.get());
                    } else if (roll < 12.0D * rareBoost) {
                        gemResult = new ItemStack(com.nogeon.economyland.item.ModItems.ENHANCEMENT_GEM.get());
                    } else if (roll < 50.0D * rareBoost) {
                        gemResult = new ItemStack(com.nogeon.economyland.item.ModItems.FLAWED_ENHANCEMENT_GEM.get());
                    } else if (roll < 150.0D + 3.0D * compressionLevel) {
                        gemResult = new ItemStack(com.nogeon.economyland.item.ModItems.SPLIT_ENHANCEMENT_GEM.get());
                    } else if (roll < 500.0D + 10.0D * compressionLevel) {
                        gemResult = new ItemStack(com.nogeon.economyland.item.ModItems.CRACKED_ENHANCEMENT_GEM.get());
                    } else {
                        gemResult = com.nogeon.economyland.job.JobEvents.createRandomApotheosisGem(player);
                        if (gemResult.isEmpty()) {
                            gemResult = new ItemStack(com.nogeon.economyland.item.ModItems.CRACKED_ENHANCEMENT_GEM.get());
                        }
                    }

                    if (!gemResult.isEmpty()) {
                        ExtendedInventoryDelivery.giveOrDrop(player, gemResult);
                    }

                    // Check if this is the best gem rolled
                    int tier = 6;
                    if (gemResult.getItem() == com.nogeon.economyland.item.ModItems.PERFECT_ENHANCEMENT_GEM.get()) {
                        tier = 1;
                    } else if (gemResult.getItem() == com.nogeon.economyland.item.ModItems.FLAWLESS_ENHANCEMENT_GEM.get()) {
                        tier = 2;
                    } else if (gemResult.getItem() == com.nogeon.economyland.item.ModItems.ENHANCEMENT_GEM.get()) {
                        tier = 3;
                    } else if (gemResult.getItem() == com.nogeon.economyland.item.ModItems.FLAWED_ENHANCEMENT_GEM.get()) {
                        tier = 4;
                    } else if (gemResult.getItem() == com.nogeon.economyland.item.ModItems.SPLIT_ENHANCEMENT_GEM.get()) {
                        tier = 5;
                    } else if (gemResult.getItem() == com.nogeon.economyland.item.ModItems.CRACKED_ENHANCEMENT_GEM.get()) {
                        tier = 6;
                    } else {
                        tier = 0; // Apotheosis Gem!
                    }

                    if (tier < bestTier || (tier == bestTier && roll < bestRoll)) {
                        bestTier = tier;
                        bestRoll = roll;
                        bestGem = gemResult;
                    }

                    totalExp += baseExp;
                    totalCredits += baseCredits;
                }

                // Apply rewards
                com.nogeon.economyland.job.JobEvents.addExp(player, JobType.ENGINEER, totalExp);
                profile.addCredits(totalCredits);
                state.setDirty();

                // Play sound on server for ambiance
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.GRINDSTONE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.4F);

                // Send response back to client to start the mechanical gacha roll animation
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new CompressionActionPacket(packet.materialType, bestGem, bestRoll, totalExp, totalCredits)
                );
            }
        });
        context.setPacketHandled(true);
    }
}
