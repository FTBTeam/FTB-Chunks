package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapComponentContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class FPSComponent implements MinimapInfoComponent {

    public static final Identifier ID = FTBChunksAPI.id("fps");

    public FPSComponent() {
        super();
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void render(MinimapComponentContext context, GuiGraphicsExtractor graphics, Font font) {
        drawCenteredText(context.minecraft().font, graphics, Component.translatable("ftbchunks.fps", Minecraft.getInstance().getFps()), 0);
    }

}
