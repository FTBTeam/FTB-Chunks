package dev.ftb.mods.ftbchunks.client.minimap;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public record MaskedMinimapRenderState(
        RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
        int x0, int y0, int x1, int y1,
        float u0, float u1, float v0, float v1,
        int alpha,
        @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public MaskedMinimapRenderState(
            RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
            int x0, int y0, int x1, int y1,
            float u0, float u1, float v0, float v1,
            int alpha,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(
                pipeline, textureSetup, pose,
                x0, y0, x1, y1,
                u0, u1, v0, v1,
                alpha,
                scissorArea, getBounds(x0, y0, x1, y1, pose, scissorArea)
        );
    }

    @Override
    public void buildVertices(VertexConsumer vertexconsumer) {
        // Encode mask UVs into vertex color (r = u, g = v, a = alpha).
        vertexconsumer.addVertexWith2DPose(this.pose(), this.x0(), this.y0()).setUv(this.u0(), this.v0()).setColor(0, 0, 255, this.alpha());
        vertexconsumer.addVertexWith2DPose(this.pose(), this.x0(), this.y1()).setUv(this.u0(), this.v1()).setColor(0, 255, 255, this.alpha());
        vertexconsumer.addVertexWith2DPose(this.pose(), this.x1(), this.y1()).setUv(this.u1(), this.v1()).setColor(255, 255, 255, this.alpha());
        vertexconsumer.addVertexWith2DPose(this.pose(), this.x1(), this.y0()).setUv(this.u1(), this.v0()).setColor(255, 0, 255, this.alpha());
    }

    private static @Nullable ScreenRectangle getBounds(int x0, int y0, int x1, int y1, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }
}
