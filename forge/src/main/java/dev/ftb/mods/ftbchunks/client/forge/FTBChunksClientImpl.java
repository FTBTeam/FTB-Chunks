package dev.ftb.mods.ftbchunks.client.forge;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.MinimapRenderer;
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

    public static void renderMinimap(MapDimension dimension, MinimapRenderer renderer) {
        WaystonesCompat.getWaystones(dimension.dimension).forEach(waystone ->
                renderer.render(waystone.getPos().getX(), waystone.getPos().getZ(), WaystonesCompat.colorFor(waystone), 0)
        );
    }

}
