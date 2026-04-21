package dev.ftb.mods.ftbchunks.client.minimap.layers;

import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapLayerRenderer;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapRenderContext;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.UnknownNullability;
import org.joml.Matrix3x2fStack;

public enum IconLayerRenderer implements MinimapLayerRenderer {
    INSTANCE;

    @Override
    public void extractLayer(@UnknownNullability GuiGraphicsExtractor graphics, Matrix3x2fStack poseStack, MinimapRenderContext ctx) {
        for (MapIcon icon : ctx.mapIcons()) {
            // map icons (waypoints, entities...)
            Vec3 pos = icon.getPos(ctx.deltaTracker().getGameTimeDeltaPartialTick(false));
            double distance = MathUtils.dist(ctx.playerPos().x, ctx.playerPos().z, pos.x, pos.z);
            float scaledDist = (float) (distance * ctx.scale() * ctx.zoom());

            float halfSize = ctx.size() / 2F;

            if (!icon.isVisible(MapType.MINIMAP, distance, scaledDist > halfSize)) {
                continue;
            }

            if (scaledDist > halfSize) {
                scaledDist = halfSize;
            }

            float angle = (float) (Mth.atan2(ctx.playerPos().z - pos.z, ctx.playerPos().x - pos.x) + ctx.rotation());

            float iconHalfSize = (float) (halfSize / (16F / icon.getIconScale(MapType.MINIMAP)));
            float wx = Mth.cos(angle) * scaledDist;
            float wy = Mth.sin(angle) * scaledDist;
            boolean onMapEdge = icon.isIconOnEdge(MapType.MINIMAP, scaledDist >= halfSize);

            poseStack.pushMatrix();
            poseStack.translate(wx - iconHalfSize, wy - iconHalfSize - (onMapEdge ? iconHalfSize / 2F : 0F));
            poseStack.scale(iconHalfSize * 2F, iconHalfSize * 2F);
            icon.draw(MapType.MINIMAP, graphics, 0, 0, 1, 1, scaledDist >= halfSize, 255);
            poseStack.popMatrix();
        }
    }
}
