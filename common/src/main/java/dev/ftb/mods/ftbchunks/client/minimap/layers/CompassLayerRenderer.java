package dev.ftb.mods.ftbchunks.client.minimap.layers;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapLayerRenderer;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapRenderContext;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftblibrary.client.icon.Color4IRenderer;
import dev.ftb.mods.ftblibrary.client.icon.IconHelper;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;

import java.util.List;

public enum CompassLayerRenderer implements MinimapLayerRenderer {
    INSTANCE;

    private static final List<Icon<?>> COMPASS = List.of(
            Icon.getIcon(FTBChunksAPI.id("textures/compass_e.png")),
            Icon.getIcon(FTBChunksAPI.id("textures/compass_n.png")),
            Icon.getIcon(FTBChunksAPI.id("textures/compass_w.png")),
            Icon.getIcon(FTBChunksAPI.id("textures/compass_s.png"))
    );

    @Override
    public void extractLayer(GuiGraphicsExtractor graphics, Matrix3x2fStack poseStack, MinimapRenderContext ctx) {
        // compass letters at the 4 cardinal points
        float dist = ctx.size() / 2.2F;
        float ws = ctx.size() / 32F;
        for (int face = 0; face < COMPASS.size(); face++) {
            float angle = ctx.rotation() + Mth.PI - face * Mth.HALF_PI;
            float wx = Mth.cos(angle) * dist;
            float wy = Mth.sin(angle) * dist;
            Color4IRenderer.INSTANCE.render(Color4I.BLACK.withAlpha(60), graphics, (int) (wx - ws), (int) (wy - ws), (int) (ws * 2), (int) (ws * 2));
            IconHelper.renderIcon(COMPASS.get(face), graphics, (int) (wx - ws), (int) (wy - ws), (int) ws * 2, (int) ws * 2);
        }
    }

    @Override
    public boolean shouldExtract(MinimapRenderContext ctx) {
        return FTBChunksClientConfig.MINIMAP_COMPASS.get();
    }
}
