package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.api.client.minimap.TranslatedOption;
import dev.ftb.mods.ftblibrary.config.NameMap;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class RealTimeComponent implements MinimapInfoComponent {

    public static final ResourceLocation ID = FTBChunksAPI.rl("real_time");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void render(MinimapContext context, GuiGraphics graphics, Font font) {
        String setting = context.getSetting(this);
        LocalDateTime now = LocalDateTime.now();
        int hours = now.getHour();
        int minutes = now.getMinute();
        drawCenteredText(font, graphics, Component.literal(createTimeString(hours, minutes, setting.equals(TimeMode.TWENTY_FOUR.name()))), 0);
    }

    @Override
    public Set<TranslatedOption> getConfigComponents() {
        return Arrays.stream(TimeMode.values())
                .map(value -> new TranslatedOption(value.name(),"ftbchunks.time_mode." + TimeMode.NAME_MAP.getName(value)))
                .collect(Collectors.toSet());
    }

    static String createTimeString(int hours, int minutes, boolean twentyFourHourClock) {
        if (twentyFourHourClock) {
            return String.format("%02d:%02d", hours, minutes);
        }

        String ampm = hours >= 12 ? "PM" : "AM";
        if (hours == 0) {
            hours = 12;
        } else if (hours > 12) {
            hours -= 12;
        }

        return String.format("%2d:%02d %s", hours, minutes, ampm);
    }

    public enum TimeMode {
        TWENTY_FOUR,
        TWELVE;

        public static final NameMap<TimeMode> NAME_MAP = NameMap.of(TWENTY_FOUR, values()).baseNameKey("ftbchunks.time_mode").create();
    }
}
