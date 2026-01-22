package dev.ftb.mods.ftbchunks.client.minimap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.ModRenderPipelines;
import net.minecraft.client.Minecraft;
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
        VertexConsumer maskBuffer = bufferSource.getBuffer(ModRenderPipelines.getMaskedRender(FTBChunksClient.CIRCLE_MASK));

//        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(RenderSystem.outputDepthTextureOverride.texture(), 0.0);
//
        var pose = poseStack.last().pose();
        float alpha = state.alpha();
//
//        maskBuffer.addVertex(pose, state.x0(), state.y0(), 0F).setColor(255, 255, 255, alpha).setUv(0f, 0f);
//        maskBuffer.addVertex(pose, state.x0(), state.y1(), 0F).setColor(255, 255, 255, alpha).setUv(0f, 1f);
//        maskBuffer.addVertex(pose, state.x1(), state.y1(), 0F).setColor(255, 255, 255, alpha).setUv(1f, 1f);
//        maskBuffer.addVertex(pose, state.x1(), state.y0(), 0F).setColor(255, 255, 255, alpha).setUv(1f, 0f);

        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.text(MinimapRegionCutoutTexture.ID));

        float u0 = state.offX() - state.zws();
        float u1 = state.offX() + state.zws();
        float v0 = state.offZ() - state.zws();
        float v1 = state.offZ() + state.zws();

        buffer.addVertex(pose, state.x0(), state.y0(), 0F).setColor(255, 255, 255, alpha).setUv(u0, v0).setLight(0x00F000F0);
        buffer.addVertex(pose, state.x0(), state.y1(), 0F).setColor(255, 255, 255, alpha).setUv(u0, v1).setLight(0x00F000F0);
        buffer.addVertex(pose, state.x1(), state.y1(), 0F).setColor(255, 255, 255, alpha).setUv(u1, v1).setLight(0x00F000F0);
        buffer.addVertex(pose, state.x1(), state.y0(), 0F).setColor(255, 255, 255, alpha).setUv(u1, v0).setLight(0x00F000F0);

        bufferSource.endBatch();
//        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(Minecraft.getInstance().getMainRenderTarget().getDepthTexture(), 1.0);
    }

    @Override
    protected String getTextureLabel() {
        return "ftbchunks:minimap";
    }

}
