package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.SlotMachineOpener;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.state.SlotMachineResult;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.network.NetworkEvent;

public final class SlotMachineActionPacket {
    private final long stake;

    public SlotMachineActionPacket(long stake) {
        this.stake = stake;
    }

    public static void encode(SlotMachineActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.stake);
    }

    public static SlotMachineActionPacket decode(FriendlyByteBuf buffer) {
        return new SlotMachineActionPacket(buffer.readLong());
    }

    public static void handle(SlotMachineActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            EconomyState state = EconomyState.get(player.server);
            PlayerProfile profile = state.profile(player.getUUID());
            
            // 배팅 한도 검사
            long baseCap = profile.socialClass().maxBetCap();
            long maxCap = Math.min(1000000L, Math.round(baseCap * (1.0D + Math.min(10, profile.gambleStreak()) * 0.1D)));
            if (packet.stake > maxCap) {
                player.displayClientMessage(Component.literal("배팅 한도를 초과했습니다. (최대 " + maxCap + " C)"), false);
                return;
            }

            if (packet.stake <= 0L || !profile.spendCredits(packet.stake)) {
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.shop.no_money"), false);
                return;
            }
            SlotMachineResult result = roll(player.getRandom(), packet.stake);
            long finalPayout = result.payout();
            if (finalPayout > 0L) {
                double bonus = 1.0D + Math.min(10, profile.gambleStreak()) * 0.05D;
                finalPayout = Math.round(finalPayout * bonus);
                profile.addCredits(finalPayout);
                profile.incrementGambleStreak();
            } else {
                profile.resetGambleStreak();
            }

            SlotMachineResult finalResult = new SlotMachineResult(
                result.stake(), result.leftSymbol(), result.middleSymbol(), result.rightSymbol(),
                finalPayout, result.resultKey()
            );

            SyncCreditsPacket.send(player, profile.credits());
            state.setDirty();
            SlotMachineOpener.open(player, finalResult);
        });
        context.setPacketHandled(true);
    }

    private static SlotMachineResult roll(RandomSource random, long stake) {
        int left = weightedSymbol(random);
        int middle = weightedSymbol(random);
        int right = weightedSymbol(random);
        long payout = 0L;
        String resultKey = "gui.nogeon_economy_land.slot_machine_result_lose";
        if (left == middle && middle == right) {
            payout = stake * tripleMultiplier(left);
            resultKey = left == 5
                ? "gui.nogeon_economy_land.slot_machine_result_jackpot"
                : "gui.nogeon_economy_land.slot_machine_result_triple";
        } else if (left == middle || middle == right || left == right) {
            payout = stake * 13L / 10L;
            resultKey = "gui.nogeon_economy_land.slot_machine_result_pair";
        }
        return new SlotMachineResult(stake, left, middle, right, payout, resultKey);
    }

    private static int weightedSymbol(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 28) {
            return 0;
        }
        if (roll < 52) {
            return 1;
        }
        if (roll < 72) {
            return 2;
        }
        if (roll < 87) {
            return 3;
        }
        if (roll < 97) {
            return 4;
        }
        return 5;
    }

    private static long tripleMultiplier(int symbol) {
        return switch (symbol) {
            case 5 -> 20L;
            case 4 -> 10L;
            case 3 -> 7L;
            case 2 -> 5L;
            default -> 4L;
        };
    }
}
