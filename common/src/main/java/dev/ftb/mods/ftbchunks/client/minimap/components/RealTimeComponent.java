package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.time.LocalDateTime;

public class RealTimeComponent implements MinimapInfoComponent {

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

    static String createTimeString(int hours, int minutes, boolean twentyFourHourClock) {
        if (twentyFourHourClock) {
            return String.format("%02d:%02d", hours, minutes);
        }

        String ampm = hours >= 12 ? "PM" : "AM";
        if(hours == 0) {
            hours = 12;
        } else if(hours > 12) {
            hours -= 12;
        }

        return String.format("%02d:%02d %s", hours, minutes, ampm);
    }
}
