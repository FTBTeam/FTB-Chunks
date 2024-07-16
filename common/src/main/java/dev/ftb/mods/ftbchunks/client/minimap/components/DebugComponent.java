package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.client.ClientTaskQueue;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.map.ChunkUpdateTask;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.StringUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class DebugComponent implements MinimapInfoComponent {

    public static final ResourceLocation ID = FTBChunksAPI.rl("debug");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void render(MinimapContext context, GuiGraphics graphics, Font font) {
        List<Component> components = new ArrayList<>();
        XZ playerXZ = XZ.regionFromChunk(context.mapChunkPosX(), context.mapChunkPosZ());
        long memory = MapManager.getInstance().map(MapManager::estimateMemoryUsage).orElse(0L);
        components.add(Component.literal("TQ: " + ClientTaskQueue.queueSize()).withStyle(ChatFormatting.GRAY));
        components.add(Component.literal("Rgn: " + playerXZ).withStyle(ChatFormatting.GRAY));
        components.add(Component.literal("Mem: ~" + StringUtils.formatDouble00(memory / 1024D / 1024D) + " MB").withStyle(ChatFormatting.GRAY));
        components.add(Component.literal("Updates: " + FTBChunksClient.INSTANCE.getRenderedDebugCount()).withStyle(ChatFormatting.GRAY));
        if(ChunkUpdateTask.getDebugLastTime() > 0L) {
            components.add(Component.literal(String.format("Last: %,d ns", ChunkUpdateTask.getDebugLastTime())).withStyle(ChatFormatting.GRAY));
        }

        int y = 0;
        int lineHeight = computeLineHeight(context.minecraft(), 1) * 2 + 1;
        for (Component component : components) {
            drawCenteredText(context.minecraft().font, graphics, component, y);
            y += lineHeight;
        }
    }

    @Override
    public int height(MinimapContext context) {
        return computeLineHeight(context.minecraft(), 5) + 1;
    }
}
