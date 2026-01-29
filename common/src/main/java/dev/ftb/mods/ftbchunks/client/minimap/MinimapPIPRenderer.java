package dev.ftb.mods.ftbchunks.client.minimap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.ModRenderPipelines;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class MinimapPIPRenderer extends PictureInPictureRenderer<MinimapRenderState> {
    public MinimapPIPRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<MinimapRenderState> getRenderStateClass() {
        return MinimapRenderState.class;
    }

    @Override
    protected void renderToTexture(MinimapRenderState state, PoseStack poseStack) {
        var pose = poseStack.last().pose();
        float alpha = state.alpha();
        boolean square = FTBChunksClientConfig.SQUARE_MINIMAP.get();
        VertexConsumer buffer = square
                ? bufferSource.getBuffer(RenderTypes.text(MinimapRegionCutoutTexture.ID))
                : bufferSource.getBuffer(ModRenderPipelines.getMinimapMaskedRender(MinimapRegionCutoutTexture.ID, FTBChunksClient.CIRCLE_MASK));

        float u0 = state.offX() - state.zws();
        float u1 = state.offX() + state.zws();
        float v0 = state.offZ() - state.zws();
        float v1 = state.offZ() + state.zws();

        if (square) {
            buffer.addVertex(pose, state.x0(), state.y0(), 0F).setColor(255, 255, 255, alpha).setUv(u0, v0).setLight(0x00F000F0);
            buffer.addVertex(pose, state.x0(), state.y1(), 0F).setColor(255, 255, 255, alpha).setUv(u0, v1).setLight(0x00F000F0);
            buffer.addVertex(pose, state.x1(), state.y1(), 0F).setColor(255, 255, 255, alpha).setUv(u1, v1).setLight(0x00F000F0);
            buffer.addVertex(pose, state.x1(), state.y0(), 0F).setColor(255, 255, 255, alpha).setUv(u1, v0).setLight(0x00F000F0);
        } else {
            buffer.addVertex(pose, state.x0(), state.y0(), 0F).setColor(0, 0, 255, alpha).setUv(u0, v0).setLight(0x00F000F0);
            buffer.addVertex(pose, state.x0(), state.y1(), 0F).setColor(0, 255, 255, alpha).setUv(u0, v1).setLight(0x00F000F0);
            buffer.addVertex(pose, state.x1(), state.y1(), 0F).setColor(255, 255, 255, alpha).setUv(u1, v1).setLight(0x00F000F0);
            buffer.addVertex(pose, state.x1(), state.y0(), 0F).setColor(255, 0, 255, alpha).setUv(u1, v0).setLight(0x00F000F0);
        }

        bufferSource.endBatch();
    }

    @Override
    protected String getTextureLabel() {
        return "ftbchunks:minimap";
    }

}
