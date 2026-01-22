package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

public class ModRenderPipelines {
    public static final RenderPipeline.Snippet MASKED_SNIPPET = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withSampler("Sampler0")
//            .withBlend(BlendFunction.TRANSLUCENT)
//            .withColorWrite(true)
//            .withDepthWrite(true)
            .withCull(false)
            .buildSnippet();

    public static final RenderPipeline MASKED = RenderPipeline.builder(MASKED_SNIPPET)
            .withLocation("ftb/pipeline/masked")
            .withDepthTestFunction(DepthTestFunction.GREATER_DEPTH_TEST)
            .build();

    private static final Function<Identifier,RenderType> M = Util.memoize(texture ->
            RenderType.create("masked", RenderSetup.builder(MASKED)
                    .withTexture("Sampler0", texture, () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
                    .setLayeringTransform(LayeringTransform.NO_LAYERING)
                    .createRenderSetup())
    );

    public static RenderType getMaskedRender(Identifier maskTexture) {
        return M.apply(maskTexture);
    }
}
