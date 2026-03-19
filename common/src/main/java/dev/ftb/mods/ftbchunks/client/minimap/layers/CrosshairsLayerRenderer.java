package dev.ftb.mods.ftbchunks.client.minimap.layers;

import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapLayerRenderer;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapRenderContext;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

public enum CrosshairsLayerRenderer implements MinimapLayerRenderer {
    INSTANCE;

    @Override
    public void extractLayer(GuiGraphicsExtractor graphics, Matrix3x2fStack poseStack, MinimapRenderContext ctx) {
        // scaling here to get a better linewidth, default is too fat
        poseStack.pushMatrix();
        poseStack.scale(0.5f, 0.5f);
        int crosshairLen = ctx.size() - 1;
        graphics.horizontalLine(-crosshairLen, crosshairLen, 0, (40 << 24));
        graphics.verticalLine(0, -crosshairLen, crosshairLen, (40 << 24));
        poseStack.popMatrix();
    }

    @Override
    public boolean shouldExtract(MinimapRenderContext ctx) {
        return FTBChunksClientConfig.MINIMAP_RETICLE.get();
    }
}
