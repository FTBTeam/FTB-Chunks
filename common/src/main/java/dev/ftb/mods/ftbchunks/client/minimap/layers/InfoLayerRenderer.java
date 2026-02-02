package dev.ftb.mods.ftbchunks.client.minimap.layers;

import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapLayerRenderer;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapRenderContext;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;

public enum InfoLayerRenderer implements MinimapLayerRenderer {
    INSTANCE;

    @Override
    public void renderLayer(GuiGraphics graphics, Matrix3x2fStack poseStack, MinimapRenderContext ctx) {
        var fontScale = FTBChunksClientConfig.MINIMAP_FONT_SCALE.get().floatValue();
        int yPos = FTBChunksClientConfig.TEXT_ABOVE_MINIMAP.get() ? -ctx.size() / 2 - ctx.componentsHeight() : ctx.size() / 2 + 1;

        for (MinimapInfoComponent component : ctx.components()) {
            if (component.shouldRender(ctx.componentContext())) {
                poseStack.pushMatrix();
                poseStack.translate(0, yPos);
                poseStack.scale(fontScale, fontScale);
                component.render(ctx.componentContext(), graphics, ctx.componentContext().minecraft().font);
                poseStack.popMatrix();
                yPos += component.height(ctx.componentContext());
            }
        }
    }
}
