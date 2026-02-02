package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.BiFunction;

public class ModRenderPipelines {
    public static final RenderPipeline MINIMAP_MASKED = RenderPipeline.builder()
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withVertexShader(FTBChunksAPI.id("core/minimap_mask"))
            .withFragmentShader(FTBChunksAPI.id("core/minimap_mask"))
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withLocation(FTBChunksAPI.id("pipeline/minimap_mask"))
            .build();

    private static final BiFunction<Identifier, Identifier, RenderType> MINIMAP_MASKED_RENDER = Util.memoize(
            (minimapTexture, maskTexture) -> RenderType.create(
                    "ftbchunks_minimap_masked",
                    RenderSetup.builder(MINIMAP_MASKED)
                            .withTexture("Sampler0", minimapTexture)
                            .withTexture("Sampler1", maskTexture)
                            .setLayeringTransform(LayeringTransform.NO_LAYERING)
                            .createRenderSetup()
            )
    );

    public static RenderType getMinimapMaskedRender(Identifier minimapTexture, Identifier maskTexture) {
        return MINIMAP_MASKED_RENDER.apply(minimapTexture, maskTexture);
    }
}
