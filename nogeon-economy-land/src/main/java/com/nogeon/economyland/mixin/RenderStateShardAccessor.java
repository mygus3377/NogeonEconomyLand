package com.nogeon.economyland.mixin;

import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderStateShard.class)
public interface RenderStateShardAccessor {
    @Accessor("RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER")
    static RenderStateShard.ShaderStateShard getArmorEntityGlintShader() {
        throw new UnsupportedOperationException();
    }

    @Accessor("RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER")
    static RenderStateShard.ShaderStateShard getEntityGlintDirectShader() {
        throw new UnsupportedOperationException();
    }

    @Accessor("RENDERTYPE_ENTITY_GLINT_SHADER")
    static RenderStateShard.ShaderStateShard getEntityGlintShader() {
        throw new UnsupportedOperationException();
    }

    @Accessor("RENDERTYPE_GLINT_DIRECT_SHADER")
    static RenderStateShard.ShaderStateShard getGlintDirectShader() {
        throw new UnsupportedOperationException();
    }

    @Accessor("RENDERTYPE_GLINT_SHADER")
    static RenderStateShard.ShaderStateShard getGlintShader() {
        throw new UnsupportedOperationException();
    }

    @Accessor("COLOR_WRITE")
    static RenderStateShard.WriteMaskStateShard getColorWrite() {
        throw new UnsupportedOperationException();
    }

    @Accessor("NO_CULL")
    static RenderStateShard.CullStateShard getNoCull() {
        throw new UnsupportedOperationException();
    }

    @Accessor("GLINT_TEXTURING")
    static RenderStateShard.TexturingStateShard getGlintTexturing() {
        throw new UnsupportedOperationException();
    }

    @Accessor("EQUAL")
    static RenderStateShard.DepthTestStateShard getEqual() {
        throw new UnsupportedOperationException();
    }
}
