package com.nogeon.economyland.client;

import com.nogeon.economyland.NoGeonEconomyLand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = NoGeonEconomyLand.MOD_ID, value = Dist.CLIENT)
public final class TooltipWrapEvents {
    private static final int MAX_WIDTH = 200; // 최대 가로 폭 픽셀

    private TooltipWrapEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        Font font = mc.font;
        if (font == null) return;

        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty()) return;

        List<Component> newTooltip = new ArrayList<>();
        // 첫 번째 줄(아이템 이름)은 줄바꿈 대상에서 제외하여 본래의 타이틀 디자인을 유지합니다.
        newTooltip.add(tooltip.get(0));

        for (int i = 1; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            if (font.width(line) > MAX_WIDTH) {
                // 바닐라 StringSplitter를 사용해 최대 가로 폭을 넘지 않는 선에서 스타일을 유지하며 쪼갭니다.
                List<FormattedText> splitLines = font.getSplitter().splitLines(line, MAX_WIDTH, Style.EMPTY);
                for (FormattedText splitLine : splitLines) {
                    newTooltip.add(fromFormattedText(splitLine));
                }
            } else {
                newTooltip.add(line);
            }
        }

        tooltip.clear();
        tooltip.addAll(newTooltip);
    }

    private static Component fromFormattedText(FormattedText formattedText) {
        if (formattedText instanceof Component component) {
            return component;
        }
        MutableComponent result = Component.empty();
        formattedText.visit((style, text) -> {
            result.append(Component.literal(text).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }
}
