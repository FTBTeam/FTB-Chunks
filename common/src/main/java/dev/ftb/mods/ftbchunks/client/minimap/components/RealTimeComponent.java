package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapContext;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftblibrary.snbt.config.EnumValue;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.time.LocalDateTime;

public class RealTimeComponent extends AbstractTimeComponent{

    public static final ResourceLocation ID = FTBChunksAPI.rl("real_time");

    @Override
    public boolean enabled() {
        return FTBChunksClientConfig.MINIMAP_SHOW_REAL_TIME.get() != FTBChunksClientConfig.TimeMode.OFF;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void render(MinimapContext context, GuiGraphics graphics, Font font) {
        LocalDateTime now = LocalDateTime.now();
        int hours = now.getHour();
        int minutes = now.getMinute();
        drawCenteredText(context.minecraft().font, graphics, Component.literal(createTimeString(hours, minutes, FTBChunksClientConfig.MINIMAP_SHOW_REAL_TIME.get() == FTBChunksClientConfig.TimeMode.TWENTY_FOUR)), 0);
    }
}
