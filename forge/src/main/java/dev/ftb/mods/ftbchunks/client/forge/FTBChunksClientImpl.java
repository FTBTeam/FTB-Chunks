package dev.ftb.mods.ftbchunks.client.forge;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.RegionMapPanel;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.WaypointType;
import dev.ftb.mods.ftbchunks.compat.waystones.WaystoneWidget;
import dev.ftb.mods.ftbchunks.compat.waystones.WaystonesCompat;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import net.blay09.mods.waystones.api.IWaystone;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class FTBChunksClientImpl {
    public static void registerPlatform() {
        FTBChunksClient.openMapKey = new KeyMapping("key.ftbchunks.map", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.ui");
        ClientRegistry.registerKeyBinding(FTBChunksClient.openMapKey);
        MinecraftForge.EVENT_BUS.addListener(FTBChunksClientImpl::renderWorldLastForge);
    }

    private static void renderWorldLastForge(RenderWorldLastEvent event) {
        ((FTBChunksClient) FTBChunks.PROXY).renderWorldLast(event.getMatrixStack());
    }

    public static void addWidgets(RegionMapPanel panel) {
        WaystonesCompat.getWaystones(panel.largeMap.dimension.dimension).forEach(waystone ->
                panel.add(new WaystoneWidget(panel, waystone))
        );
    }

    public static void renderMinimap(Minecraft mc, int x, int y, MapDimension dim, float scale, float minimapRotation, PoseStack matrixStack, BufferBuilder buffer, Tesselator tesselator) {
        double magicNumber = 3.2D;
        int s = (int) (64D * scale);

        WaystonesCompat.getWaystones(dim.dimension).forEach(waystone -> {

            BlockPos pos = waystone.getPos();
            double distance = MathUtils.dist(mc.player.getX(), mc.player.getZ(), pos.getX() + 0.5D, pos.getZ() + 0.5D);

            double d = distance / magicNumber * scale;

            if (d > s / 2D) {
                d = s / 2D;
            }

            double angle = Math.atan2(mc.player.getZ() - pos.getZ() - 0.5D, mc.player.getX() - pos.getX() - 0.5D) + minimapRotation * Math.PI / 180D;

            float wx = (float) (x + s / 2D + Math.cos(angle) * d);
            float wy = (float) (y + s / 2D + Math.sin(angle) * d);
            float ws = s / 32F;

            int color = WaystonesCompat.colorFor(waystone);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = (color >> 0) & 0xFF;

            Matrix4f m = matrixStack.last().pose();

            mc.getTextureManager().bind(WaypointType.WAYSTONE.texture);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            buffer.vertex(m, wx - ws, wy - ws, 0).color(r, g, b, 255).uv(0F, 0F).endVertex();
            buffer.vertex(m, wx - ws, wy + ws, 0).color(r, g, b, 255).uv(0F, 1F).endVertex();
            buffer.vertex(m, wx + ws, wy + ws, 0).color(r, g, b, 255).uv(1F, 1F).endVertex();
            buffer.vertex(m, wx + ws, wy - ws, 0).color(r, g, b, 255).uv(1F, 0F).endVertex();
            tesselator.end();

        });
    }

}
