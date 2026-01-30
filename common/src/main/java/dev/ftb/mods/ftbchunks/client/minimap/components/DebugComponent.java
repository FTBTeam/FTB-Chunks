package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapComponentContext;
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
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class DebugComponent implements MinimapInfoComponent {

    public static final Identifier ID = FTBChunksAPI.id("debug");

    public DebugComponent() {
        super();
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void render(MinimapComponentContext context, GuiGraphics graphics, Font font) {
        List<Component> components = new ArrayList<>();
        long memory = MapManager.getInstance().map(MapManager::estimateMemoryUsage).orElse(0L);

        components.add(Component.literal("TQ: " + ClientTaskQueue.queueSize()).withStyle(ChatFormatting.GRAY));
        components.add(Component.literal("Rgn: " + XZ.of(context.mapChunksPos().x(), context.mapChunksPos().z())).withStyle(ChatFormatting.GRAY));
        components.add(Component.literal("Mem: ~" + StringUtils.formatDouble00(memory / 1024D / 1024D) + " MB").withStyle(ChatFormatting.GRAY));
        components.add(Component.literal("Updates: " + FTBChunksClient.INSTANCE.getRenderedDebugCount()).withStyle(ChatFormatting.GRAY));
        if (ChunkUpdateTask.getDebugLastTime() > 0L) {
            components.add(Component.literal(String.format("Last: %,d ns", ChunkUpdateTask.getDebugLastTime())).withStyle(ChatFormatting.GRAY));
        }

        int y = 0;
        int lineHeight = /*computeLineHeight(context.minecraft(), 1) +*/ font.lineHeight + 1;
        for (Component component : components) {
            drawCenteredText(context.minecraft().font, graphics, component, y);
            y += lineHeight;
        }
    }

    @Override
    public int height(MinimapComponentContext context) {
        return computeLineHeight(context.minecraft(), ChunkUpdateTask.getDebugLastTime() > 0L ? 5 : 4);// + context.minecraft().font.lineHeight;
    }
}
