package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.icon.WaypointIcon;
import dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.mapicon.MapIconComparator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class WaypointBeaconRenderer {
    public static final Identifier WAYPOINT_BEAM = FTBChunksAPI.id("textures/waypoint_beam.png");

    static void renderBeacons(Minecraft mc, DeltaTracker tickDelta, Vec3 cameraPos) {
        if (FTBChunksClientConfig.IN_WORLD_WAYPOINTS.get() && mc.player != null) {
            List<WaypointIcon> visibleWaypoints = findVisibleBeacons(mc.player, tickDelta);
            if (!visibleWaypoints.isEmpty()) {
                drawWaypointBeacons(mc, visibleWaypoints, cameraPos);
            }
        }
    }

    static void drawWaypointBeacons(Minecraft mc, List<WaypointIcon> waypoints, Vec3 cameraPos) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        float y1 = (float) (cameraPos.y + 30D);
        float y2 = y1 + 70F;
        assert mc.level != null;
        int yMin = mc.level.getMinY();

        RenderType renderType = RenderTypes.beaconBeam(WAYPOINT_BEAM, true);
        VertexConsumer depthBuffer = mc.renderBuffers().bufferSource().getBuffer(renderType);

        for (WaypointIcon waypoint : waypoints) {
            drawWaypointBeacon(poseStack, cameraPos, depthBuffer, y1, y2, yMin, waypoint);
        }

        mc.renderBuffers().bufferSource().endBatch(renderType);
    }

    private static void drawWaypointBeacon(PoseStack poseStack, Vec3 cameraPos, VertexConsumer depthBuffer, float y1, float y2, int yMin, WaypointIcon waypoint) {
        Vec3 pos = waypoint.getPos(1f);
        int alpha = waypoint.getAlpha();
        double angle = Math.atan2(cameraPos.z - pos.z, cameraPos.x - pos.x) * 180D / Math.PI;

        int r = waypoint.getColor().redi();
        int g = waypoint.getColor().greeni();
        int b = waypoint.getColor().bluei();

        poseStack.pushPose();
        poseStack.translate(pos.x, 0, pos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (-angle - 135D)));

        float s = 0.6F;

        Matrix4f m = poseStack.last().pose();

        depthBuffer.addVertex(m, -s, yMin, s).setColor(r, g, b, alpha).setUv(0F, 1F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0f, 1f, 0f);
        depthBuffer.addVertex(m, -s, y1, s).setColor(r, g, b, alpha).setUv(0F, 0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0f, 1f, 0f);
        depthBuffer.addVertex(m, s, y1, -s).setColor(r, g, b, alpha).setUv(1F, 0F).
                setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0f, 1f, 0f);
        depthBuffer.addVertex(m, s, yMin, -s).setColor(r, g, b, alpha).setUv(1F, 1F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0f, 1f, 0f);

        depthBuffer.addVertex(m, -s, y1, s).setColor(r, g, b, alpha).setUv(0F, 1F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0f, 1f, 0f);
        depthBuffer.addVertex(m, -s, y2, s).setColor(r, g, b, 0).setUv(0F, 0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0f, 1f, 0f);
        depthBuffer.addVertex(m, s, y2, -s).setColor(r, g, b, 0).setUv(1F, 0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0f, 1f, 0f);
        depthBuffer.addVertex(m, s, y1, -s).setColor(r, g, b, alpha).setUv(1F, 1F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(poseStack.last(), 0f, 1f, 0f);

        poseStack.popPose();
    }

    static List<WaypointIcon> findVisibleBeacons(Player player, DeltaTracker tickDelta) {
        return MapManager.getInstance().map(manager -> {
            List<WaypointIcon> visibleWaypoints = new ArrayList<>();

            double fadeOutDistance = FTBChunksClientConfig.WAYPOINT_BEACON_FADE_DISTANCE.get();
            double fadeOutDistanceP = fadeOutDistance * 2D / 3D;

            MapDimension dim = manager.getDimension(player.level().dimension());
            for (Waypoint waypoint : dim.getWaypointManager()) {
                if (!waypoint.isHidden()) {
                    double distance = Math.sqrt(waypoint.getDistanceSq(player));

                    if (distance > fadeOutDistanceP && distance <= FTBChunksClientConfig.WAYPOINT_MAX_DISTANCE.get()) {
                        int alpha = distance < fadeOutDistance ?
                                (int) (150 * ((distance - fadeOutDistanceP) / (fadeOutDistance - fadeOutDistanceP))) :
                                150;
                        if (alpha > 0) {
                            waypoint.getMapIcon().ifPresent(icon -> {
                                icon.setAlpha(alpha);
                                visibleWaypoints.add(icon);
                            });
                        }
                    }
                }
            }

            visibleWaypoints.sort(new MapIconComparator(player.position(), tickDelta.getGameTimeDeltaPartialTick(false)));

            return visibleWaypoints;
        }).orElse(List.of());
    }
}
