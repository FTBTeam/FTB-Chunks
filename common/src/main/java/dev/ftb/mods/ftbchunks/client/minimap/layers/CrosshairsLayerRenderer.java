package dev.ftb.mods.ftbchunks.client.minimap.layers;

import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapLayerRenderer;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapRenderContext;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;

public enum CrosshairsLayerRenderer implements MinimapLayerRenderer {
    INSTANCE;

    @Override
    public void renderLayer(GuiGraphics graphics, Matrix3x2fStack poseStack, MinimapRenderContext ctx) {
        // scaling here to get a better linewidth, default is too fat
        poseStack.pushMatrix();
        poseStack.scale(0.5f, 0.5f);
        int crosshairLen = ctx.size() - 1;
        graphics.hLine(-crosshairLen, crosshairLen, 0, (40 << 24));
        graphics.vLine(0, -crosshairLen, crosshairLen, (40 << 24));
        poseStack.popMatrix();
    }

    @Override
    public boolean shouldRender(MinimapRenderContext ctx) {
        return FTBChunksClientConfig.MINIMAP_RETICLE.get();
    }
}
