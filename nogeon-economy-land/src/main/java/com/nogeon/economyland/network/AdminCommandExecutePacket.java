package com.nogeon.economyland.network;

import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class AdminCommandExecutePacket {
    private final String command;

    public AdminCommandExecutePacket(String command) {
        this.command = command == null ? "" : command;
    }

    public static void encode(AdminCommandExecutePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.command, 1024);
    }

    public static AdminCommandExecutePacket decode(FriendlyByteBuf buffer) {
        return new AdminCommandExecutePacket(buffer.readUtf(1024));
    }

    public static void handle(AdminCommandExecutePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || !sender.hasPermissions(2)) {
                return;
            }
            String command = packet.command.trim();
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            if (command.isBlank()) {
                sender.displayClientMessage(Component.literal("실행할 명령어가 비어 있습니다."), false);
                return;
            }
            CommandSourceStack source = sender.createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();
            sender.server.getCommands().performPrefixedCommand(source, "/" + command);
            sender.displayClientMessage(Component.literal("실행됨: /" + command), false);
        });
        context.setPacketHandled(true);
    }
}
