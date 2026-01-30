package dev.ftb.mods.ftbchunks.client.minimap.layers;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.ModRenderPipelines;
import dev.ftb.mods.ftbchunks.client.minimap.MaskedMinimapRenderState;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapLayerRenderer;
import dev.ftb.mods.ftbchunks.client.minimap.MinimapRegionCutoutTexture;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapRenderContext;
import dev.ftb.mods.ftbchunks.core.mixin.GuiGraphicsMixin;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
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
    public void renderLayer(GuiGraphics graphics, Matrix3x2fStack poseStack, MinimapRenderContext ctx) {
        poseStack.pushMatrix();
        poseStack.rotate(ctx.rotation() + Mth.PI);

        float offX = 0.5F + (float) ((MathUtils.mod(ctx.playerPos().x, 16D) / 16D - 0.5D) / (double) FTBChunks.TILES);
        float offZ = 0.5F + (float) ((MathUtils.mod(ctx.playerPos().z, 16D) / 16D - 0.5D) / (double) FTBChunks.TILES);
        float zws = 2F / (FTBChunks.TILES * ctx.zoom());

        float halfSize = ctx.size() / 2F;
        int x0 = (int) -halfSize;
        int y0 = (int) -halfSize;
        int x1 = (int) halfSize;
        int y1 = (int) halfSize;

        float u0 = offX - zws;
        float u1 = offX + zws;
        float v0 = offZ - zws;
        float v1 = offZ + zws;

        int alpha = FTBChunksClientConfig.MINIMAP_ALPHA.get();

        if (FTBChunksClientConfig.SQUARE_MINIMAP.get()) {
            graphics.blit(MinimapRegionCutoutTexture.ID, x0, y0, x1, y1, u0, u1, v0, v1);
        } else {
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(MinimapRegionCutoutTexture.ID);
            GpuTextureView textureView = texture.getTextureView();
            GpuSampler sampler = texture.getSampler();
            AbstractTexture maskTexture = Minecraft.getInstance().getTextureManager().getTexture(CIRCLE_MASK);
            TextureSetup textureSetup = TextureSetup.doubleTexture(textureView, sampler, maskTexture.getTextureView(), maskTexture.getSampler());
            ((GuiGraphicsMixin) graphics).getGuiRenderState().submitGuiElement(
                    new MaskedMinimapRenderState(
                            ModRenderPipelines.MINIMAP_MASKED,
                            textureSetup,
                            new Matrix3x2f(poseStack),
                            x0, y0, x1, y1,
                            u0, u1, v0, v1,
                            alpha,
                            null
                    )
            );
        }
        poseStack.popMatrix();

        var borderId = FTBChunksClientConfig.SQUARE_MINIMAP.get() ? SQUARE_BORDER : CIRCLE_BORDER;
        var borderTexture = Minecraft.getInstance().getTextureManager().getTexture(borderId);

        // draw the map border
        graphics.submitBlit(
                RenderPipelines.GUI_TEXTURED,
                borderTexture.getTextureView(),
                borderTexture.getSampler(),
                x0, y0, x1, y1,
                0F, 1F, 0F, 1F,
                (alpha << 24) | (255 << 16) | (255 << 8) | 255
        );
    }
}
