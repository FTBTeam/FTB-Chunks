package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

public class ModRenderPipelines {
    public static final RenderPipeline MINIMAP_MASKED = RenderPipeline.builder()
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withVertexShader(FTBChunksAPI.id("core/minimap_mask"))
            .withFragmentShader(FTBChunksAPI.id("core/minimap_mask"))
            .withSampler("Sampler0")
            .withSampler("Sampler1")
//            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
//            .withBlend(BlendFunction.TRANSLUCENT)
            // TODO: Fix me.
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withLocation(FTBChunksAPI.id("pipeline/minimap_mask"))
            .build();
}
