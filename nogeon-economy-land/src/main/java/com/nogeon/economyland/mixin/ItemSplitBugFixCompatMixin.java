package com.nogeon.economyland.mixin;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.iwaliner.item_split_bug_fix.ModCoreItemSplitBugFix", remap = false)
public abstract class ItemSplitBugFixCompatMixin {
    @Shadow
    private static List<Pattern> blacklistPattern;

    @Shadow
    private static Set<Item> blacklistCache;

    @Shadow
    private static Set<Item> checkedItemsCache;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void nogeon$makeCachesThreadSafe(CallbackInfo ci) {
        blacklistPattern = new CopyOnWriteArrayList<>(blacklistPattern);
        blacklistCache = ConcurrentHashMap.newKeySet();
        checkedItemsCache = ConcurrentHashMap.newKeySet();
    }
}
