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
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    public void render(MinimapComponentContext context, GuiGraphicsExtractor graphics, Font font) {
        List<Component> components = new ArrayList<>();

        long memory = MapManager.getInstance().map(MapManager::estimateMemoryUsage).orElse(0L);
        XZ chunkXZ = XZ.of(context.mapChunksPos().x(), context.mapChunksPos().z());

        addLine(components, "TaskQ",  ClientTaskQueue.queueSize());
        addLine(components, "Chunk",  "[" + chunkXZ.x() + ", " + chunkXZ.z() + "]");
        addLine(components, "Mem",  "~" + StringUtils.formatDouble00(memory / 1024D / 1024D) + " MB");
        addLine(components, "Updates", FTBChunksClient.INSTANCE.getRerenderTracker().getRerenderCount());
        if (ChunkUpdateTask.getDebugLastTime() > 0L) {
            addLine(components, "Last", String.format("%,d ns", ChunkUpdateTask.getDebugLastTime()));
        }

        int y = 0;
        for (Component component : components) {
            drawCenteredText(context.minecraft().font, graphics, component, y);
            y += font.lineHeight + 1;
        }
    }

    private void addLine(List<Component> components, String title, Object text) {
        components.add(Component.literal(title).append(": ").append(text.toString()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public int height(MinimapComponentContext context) {
        return computeLineHeight(context.minecraft(), ChunkUpdateTask.getDebugLastTime() > 0L ? 5 : 4);
    }
}
