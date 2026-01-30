package dev.ftb.mods.ftbchunks.client.minimap.layers;

import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.PointerIconMode;
import dev.ftb.mods.ftbchunks.client.gui.PointerIcon;
import dev.ftb.mods.ftbchunks.client.mapicon.EntityMapIcon;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapLayerRenderer;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapRenderContext;
import dev.ftb.mods.ftblibrary.client.icon.IconHelper;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3x2fStack;

public enum PlayerIconLayerRenderer implements MinimapLayerRenderer {
    INSTANCE;

    @Override
    public void renderLayer(GuiGraphics graphics, Matrix3x2fStack poseStack, MinimapRenderContext ctx) {
        Player player = ClientUtils.getClientPlayer();

        // pointer icon at the map centre (player position)
        poseStack.pushMatrix();
        if (ctx.rotationLocked()) {
            poseStack.rotate((player.getVisualRotationYInDegrees() + 180F) * Mth.DEG_TO_RAD);
        }
        poseStack.scale(ctx.size() / 16F, ctx.size() / 16F);

        PointerIconMode mode = FTBChunksClientConfig.POINTER_ICON_MODE_MINIMAP.get();
        if (mode.showPointer()) {
            IconHelper.renderIcon(PointerIcon.POINTER, graphics, -1, -1, 2, 2);
        }
        if (mode.showFace()) {
            if (mode.showPointer()) {
                // scale & shift face size a little to better align with the pointer
                poseStack.scale(0.75f, 0.75f);
                poseStack.translate(0f, 0.32f);
            }
            poseStack.translate(-0.5f, -0.5f);
            new EntityMapIcon(player, FaceIcon.getFace(player.getGameProfile(), true))
                    .draw(MapType.MINIMAP, graphics, 0, 0, 1, 1, false, 255);
        }

        poseStack.popMatrix();
    }

    @Override
    public boolean shouldRender(MinimapRenderContext ctx) {
        return ctx.rotationLocked() || FTBChunksClientConfig.SHOW_PLAYER_WHEN_UNLOCKED.get();
    }
}
