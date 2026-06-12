package com.nogeon.economyland.client;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.player.TemperatureAccessoryEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NoGeonEconomyLand.MOD_ID, value = Dist.CLIENT)
public final class TemperatureAccessoryTooltipEvents {
    private TemperatureAccessoryTooltipEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (TemperatureAccessoryEvents.FROSTWARD_RING.equals(itemId)) {
            event.getToolTip().add(Component.translatable("tooltip.nogeon_economy_land.frostward_ring.temperature").withStyle(ChatFormatting.AQUA));
        } else if (TemperatureAccessoryEvents.FIREWARD_RING.equals(itemId)) {
            event.getToolTip().add(Component.translatable("tooltip.nogeon_economy_land.fireward_ring.temperature").withStyle(ChatFormatting.GOLD));
        }
    }
}
