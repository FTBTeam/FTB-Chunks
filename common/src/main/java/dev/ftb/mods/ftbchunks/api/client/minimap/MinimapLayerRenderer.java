package dev.ftb.mods.ftbchunks.api.client.minimap;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;

@FunctionalInterface
public interface MinimapLayerRenderer {
    void renderLayer(GuiGraphics graphics, Matrix3x2fStack poseStack, MinimapRenderContext ctx);

    default boolean shouldRender(MinimapRenderContext ctx) {
        return true;
    }
}
