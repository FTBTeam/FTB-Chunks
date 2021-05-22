package dev.ftb.mods.ftbchunks.client.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.MinimapRenderer;
import dev.ftb.mods.ftbchunks.client.RegionMapPanel;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class FTBChunksClientImpl {
    public static void registerPlatform() {
        FTBChunksClient.openMapKey = new KeyMapping("key.ftbchunks.map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.ui");
        KeyBindingHelper.registerKeyBinding(FTBChunksClient.openMapKey);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(FTBChunksClientImpl::renderWorldLastFabric);
    }

    private static void renderWorldLastFabric(WorldRenderContext context) {
        ((FTBChunksClient) FTBChunks.PROXY).renderWorldLast(context.matrixStack());
    }

    public static void renderMinimap(MapDimension dimension, MinimapRenderer renderer) {
    }

    public static void addWidgets(RegionMapPanel panel) {
    }

}
