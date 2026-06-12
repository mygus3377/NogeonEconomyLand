package com.nogeon.economyland.menu;

import com.nogeon.economyland.state.AuctionEntry;
import com.nogeon.economyland.state.AuctionState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkHooks;

public final class AuctionOpener {
    private AuctionOpener() {
    }

    public static void open(ServerPlayer player) {
        List<AuctionLine> lines = linesFor(player);
        AuctionMenu snapshot = new AuctionMenu(0, lines);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new AuctionMenu(containerId, lines),
            Component.translatable("screen.nogeon_economy_land.auction")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }

    private static List<AuctionLine> linesFor(ServerPlayer player) {
        List<AuctionLine> lines = new ArrayList<>();
        List<AuctionEntry> auctions = AuctionState.get(player.server).auctions();
        for (int index = auctions.size() - 1; index >= 0; index--) {
            AuctionEntry entry = auctions.get(index);
            if (!entry.stack().isEmpty()) {
                lines.add(new AuctionLine(
                    entry.id(),
                    entry.sellerName(),
                    entry.itemId(),
                    entry.stack().getDescriptionId(),
                    entry.count(),
                    entry.price(),
                    entry.sellerId().equals(player.getUUID()),
                    entry.stack()
                ));
                continue;
            }
            ResourceLocation itemId = ResourceLocation.tryParse(entry.itemId());
            if (itemId == null) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(itemId);
            lines.add(new AuctionLine(
                entry.id(),
                entry.sellerName(),
                entry.itemId(),
                item.getDescriptionId(),
                entry.count(),
                entry.price(),
                entry.sellerId().equals(player.getUUID()),
                new net.minecraft.world.item.ItemStack(item, entry.count())
            ));
        }
        return lines;
    }
}