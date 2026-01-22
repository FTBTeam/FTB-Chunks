package dev.ftb.mods.ftbchunks.client.minimap;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.jspecify.annotations.Nullable;

public record MinimapRenderState(float offX, float offZ, float zws, float alpha, int x0, int x1, int y0, int y1,
                                 @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds)
        implements PictureInPictureRenderState {

    public MinimapRenderState(float offX, float offZ, float zws, float alpha, int x0, int x1, int y0, int y1, @Nullable ScreenRectangle scissorArea) {
        this(offX, offZ, zws, alpha, x0, x1, y0, y1, scissorArea, PictureInPictureRenderState.getBounds(x0, x1, y0, y1, scissorArea));
    }

    @Override
    public float scale() {
        return 1f;
    }
}
