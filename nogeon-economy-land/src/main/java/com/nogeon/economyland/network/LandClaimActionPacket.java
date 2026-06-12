package com.nogeon.economyland.network;

import com.nogeon.economyland.item.ModItems;
import com.nogeon.economyland.land.LandSelection;
import com.nogeon.economyland.land.LandSelectionManager;
import com.nogeon.economyland.land.LandSelectionValidator;
import com.nogeon.economyland.land.LandType;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public final class LandClaimActionPacket {
    private final String action;
    private final String memo;

    public LandClaimActionPacket(String action) {
        this(action, "");
    }

    public LandClaimActionPacket(String action, String memo) {
        this.action = action;
        this.memo = memo;
    }

    public static void encode(LandClaimActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.action);
        buffer.writeUtf(packet.memo);
    }

    public static LandClaimActionPacket decode(FriendlyByteBuf buffer) {
        return new LandClaimActionPacket(buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(LandClaimActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if ("enter_selection".equals(packet.action)) {
                LandSelectionManager.enterDesignationMode(player);
                LandSelection selection = LandSelectionManager.get(player);
                SyncLandSelectionPacket.send(player, selection, null, false);
                player.closeContainer();
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.designation_started"), true);
                return;
            }
            if ("close".equals(packet.action)) {
                player.closeContainer();
                return;
            }

            if ("undo".equals(packet.action)) {
                LandSelection restored = LandSelectionManager.undo(player);
                SyncLandSelectionPacket.send(player, restored, null, false);
                player.closeContainer();
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.undo_done"), true);
                return;
            }

            if ("reset".equals(packet.action)) {
                LandSelection reset = LandSelectionManager.reset(player);
                SyncLandSelectionPacket.send(player, reset, null, false);
                player.closeContainer();
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.reset_done"), true);
                return;
            }

            LandSelection selection = LandSelectionManager.get(player);
            if (selection == null || selection.cuboids().isEmpty()) {
                player.closeContainer();
                return;
            }
            if (!"confirm".equals(packet.action)) {
                player.closeContainer();
                return;
            }

            EconomyState state = EconomyState.get(player.server);
            String validationError = LandSelectionValidator.validate(player.serverLevel(), selection);
            if (validationError != null) {
                player.displayClientMessage(Component.translatable(validationError), false);
                return;
            }

            if (selection.type() == LandType.ADMIN && state.hasAdminLand()) {
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.spawn_already_exists"), false);
                return;
            }

            if (state.selectionOverlaps(selection)) {
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.overlap"), false);
                return;
            }

            PlayerProfile profile = state.profile(player.getUUID());
            long price = selection.type() == LandType.ADMIN ? 0 : profile.socialClass().discountedLandPrice(selection.price());
            if (selection.type() != LandType.ADMIN && !profile.spendCredits(price)) {
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.shop.no_money"), false);
                return;
            }

            long purchasePricePerBlock = selection.type() == LandType.ADMIN ? 0 : profile.socialClass().discountedLandPrice(selection.type().pricePerBlock());
            Item deed = deedItem(selection.type());
            consumeOne(player, deed);
            state.addLandSelection(player.getUUID(), selection, purchasePricePerBlock, packet.memo);
            SyncCreditsPacket.send(player, profile.credits());
            LandSelectionManager.clear(player);
            SyncLandSelectionPacket.clear(player);
            player.closeContainer();
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.claimed"), false);
        });
        context.setPacketHandled(true);
    }

    private static Item deedItem(LandType type) {
        return switch (type) {
            case BASIC -> ModItems.BASIC_LAND_DEED.get();
            case NORMAL -> ModItems.NORMAL_LAND_DEED.get();
            case INDUSTRIAL -> ModItems.INDUSTRIAL_LAND_DEED.get();
            case ADMIN -> ModItems.ADMIN_LAND_DEED.get();
        };
    }

    private static LandType heldLandType(ServerPlayer player) {
        Item item = player.getMainHandItem().getItem();
        if (item == ModItems.BASIC_LAND_DEED.get()) {
            return LandType.BASIC;
        }
        if (item == ModItems.NORMAL_LAND_DEED.get()) {
            return LandType.NORMAL;
        }
        if (item == ModItems.INDUSTRIAL_LAND_DEED.get()) {
            return LandType.INDUSTRIAL;
        }
        if (item == ModItems.ADMIN_LAND_DEED.get()) {
            return LandType.ADMIN;
        }
        return null;
    }

    private static void consumeOne(ServerPlayer player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                return;
            }
        }
    }
}
