package dev.ftb.mods.ftbchunks.client.minimap.layers;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapLayerRenderer;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapRenderContext;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.ModRenderPipelines;
import dev.ftb.mods.ftbchunks.client.minimap.MaskedMinimapRenderState;
import dev.ftb.mods.ftbchunks.client.minimap.MinimapRegionCutoutTexture;
import dev.ftb.mods.ftbchunks.core.mixin.GuiGraphicsMixin;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

public enum TerrainLayerRenderer implements MinimapLayerRenderer {
    INSTANCE;

    public static final Identifier CIRCLE_MASK = FTBChunksAPI.id("textures/circle_mask.png");
    public static final Identifier CIRCLE_BORDER = FTBChunksAPI.id("textures/circle_border.png");
    public static final Identifier SQUARE_BORDER = FTBChunksAPI.id("textures/square_border.png");

    @Override
    public void extractLayer(GuiGraphicsExtractor graphics, Matrix3x2fStack poseStack, MinimapRenderContext ctx) {
        poseStack.pushMatrix();

        poseStack.rotate(ctx.rotation() + Mth.PI);
        poseStack.translate(-ctx.size() / 2f, -ctx.size() / 2f);

        float offX = 0.5F + (float) ((MathUtils.mod(ctx.playerPos().x, 16D) / 16D - 0.5D) / (double) FTBChunks.TILES);
        float offZ = 0.5F + (float) ((MathUtils.mod(ctx.playerPos().z, 16D) / 16D - 0.5D) / (double) FTBChunks.TILES);
        float zws = 2F / (FTBChunks.TILES * ctx.zoom());

        float u0 = offX - zws;
        float u1 = offX + zws;
        float v0 = offZ - zws;
        float v1 = offZ + zws;

        int alpha = FTBChunksClientConfig.MINIMAP_ALPHA.get();

        if (FTBChunksClientConfig.SQUARE_MINIMAP.get()) {
            graphics.blit(MinimapRegionCutoutTexture.ID, 0, 0, ctx.size(), ctx.size(), u0, u1, v0, v1);
        } else {
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(MinimapRegionCutoutTexture.ID);
            AbstractTexture maskTexture = Minecraft.getInstance().getTextureManager().getTexture(CIRCLE_MASK);
            TextureSetup textureSetup = TextureSetup.doubleTexture(
                    texture.getTextureView(), texture.getSampler(),
                    maskTexture.getTextureView(), maskTexture.getSampler()
            );
            ((GuiGraphicsMixin) graphics).getGuiRenderState().addGuiElement(
                    new MaskedMinimapRenderState(
                            ModRenderPipelines.MINIMAP_MASKED,
                            textureSetup,
                            new Matrix3x2f(poseStack),
                            0, 0, ctx.size(), ctx.size(),
                            u0, u1, v0, v1,
                            alpha
                    )
            );
        }

        // draw the map border
        var borderId = FTBChunksClientConfig.SQUARE_MINIMAP.get() ? SQUARE_BORDER : CIRCLE_BORDER;
        graphics.blit(borderId, 0, 0, ctx.size(), ctx.size(), 0F, 1F, 0F, 1F);

        poseStack.popMatrix();
    }
}
