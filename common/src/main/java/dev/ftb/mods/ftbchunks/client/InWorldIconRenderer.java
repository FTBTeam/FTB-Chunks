package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.mapicon.InWorldMapIcon;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InWorldIconRenderer {
    private final List<InWorldMapIcon> inWorldMapIcons = new ArrayList<>();
    private Vec3 cameraPos = Vec3.ZERO;
    @Nullable
    private Matrix4f worldMatrix;
    @Nullable
    private Matrix4f savedProjectionMatrix;

    public void renderInWorldIcons(GuiGraphics graphics, DeltaTracker tickDelta, Vec3 playerPos, Collection<MapIcon> miniMapIcons) {
        if (worldMatrix == null) {
            return;
        }

        float halfScreenW = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2F;
        float halfScreenH = Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2F;

        InWorldMapIcon focusedIcon = null;
        Player player = ClientUtils.getClientPlayer();

        for (MapIcon icon : miniMapIcons) {
            Vec3 pos = icon.getPos(tickDelta.getGameTimeDeltaPartialTick(false));
            double playerDist = pos.distanceTo(playerPos);

            if (icon.isVisible(MapType.WORLD_ICON, playerDist, false)) {
                Vector4f v = new Vector4f((float) (pos.x - cameraPos.x), (float) (pos.y - cameraPos.y), (float) (pos.z - cameraPos.z), 1F);
                double lookAngle = player.getLookAngle().dot(new Vec3(v.x(), v.y(), v.z()).normalize());
                if (lookAngle > 0) {  // icon in front of the player?
                    // transform the icon's camera-relative coords into clip space (-1.0 -> 1.0, -1.0 -> 1.0)
                    // (worldMatrix is a combination of the model-view and projection matrices)
                    worldMatrix.transform(v);
                    v.div(v.w());
                    // get the actual screen coordinates
                    float ix = halfScreenW + v.x() * halfScreenW;
                    float iy = halfScreenH - v.y() * halfScreenH;
                    double mouseDist = MathUtils.dist(ix, iy, halfScreenW, halfScreenH);
                    InWorldMapIcon inWorldMapIcon = new InWorldMapIcon(icon, ix, iy, playerDist, mouseDist);

                    if (mouseDist <= 5D * FTBChunksClientConfig.WAYPOINT_FOCUS_DISTANCE.get() && (focusedIcon == null || focusedIcon.distanceToMouse() > mouseDist)) {
                        focusedIcon = inWorldMapIcon;
                    }

                    inWorldMapIcons.add(inWorldMapIcon);
                }
            }
        }

        double fadeStart = FTBChunksClientConfig.WAYPOINT_DOT_FADE_DISTANCE.get();
        double fadeMin = fadeStart * 2D / 3D;

        for (InWorldMapIcon icon : inWorldMapIcons) {
            if (icon.distanceToPlayer() > fadeMin) {
                int iconAlpha = icon.distanceToPlayer() < fadeStart ?
                        (int) (255 * ((icon.distanceToPlayer() - fadeMin) / (fadeStart - fadeMin))) :
                        255;

                float minSize = 0.25f;
                float maxSize = (float) (minSize * FTBChunksClientConfig.WAYPOINT_FOCUS_SCALE.get());
                if (iconAlpha > 0) {
                    float iconScale = Mth.lerp((50f - Math.min((float) icon.distanceToMouse(), 50f)) / 50f, minSize, maxSize);
                    Matrix3x2fStack poseStack = graphics.pose();
                    poseStack.pushMatrix();
                    poseStack.translate(icon.x(), icon.y());
                    poseStack.scale(iconScale, iconScale);
                    icon.icon().draw(MapType.WORLD_ICON, graphics, -8, -8, 16, 16, icon != focusedIcon, iconAlpha);
                    poseStack.popMatrix();
                }
            }
        }

        inWorldMapIcons.clear();
    }

    public void copyProjectionMatrix(Matrix4f projectionMatrix) {
        savedProjectionMatrix = new Matrix4f(projectionMatrix);
    }

    public void renderLevelStage(Matrix4fc modelViewMatrix, Vec3 cameraPosIn, DeltaTracker tickDelta) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.options.hideGui || MapManager.getInstance().isEmpty() || mc.level == null || mc.player == null
                || MapDimension.getCurrent().isEmpty() || !FTBChunksWorldConfig.playerHasMapStage(mc.player)) {
            return;
        }

        // save these for use by renderInWorldIcons()
        worldMatrix = new Matrix4f(savedProjectionMatrix).mul(modelViewMatrix);
        cameraPos = new Vec3(cameraPosIn.x, cameraPosIn.y, cameraPosIn.z);

        WaypointBeaconRenderer.renderBeacons(mc, tickDelta, cameraPos);
    }
}
