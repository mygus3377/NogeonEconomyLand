package com.nogeon.economyland.mixin;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderType.class)
public interface RenderTypeAccessor {
    @Invoker("create")
    static RenderType callCreate(String name, com.mojang.blaze3d.vertex.VertexFormat format, com.mojang.blaze3d.vertex.VertexFormat.Mode mode, int bufferSize, boolean affectsOutline, boolean sortsOnUpload, RenderType.CompositeState state) {
        throw new UnsupportedOperationException();
    }

    @Accessor("name")
    String getName();
}
