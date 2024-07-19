package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.TranslatedOption;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class GameTimeComponent implements MinimapInfoComponent {

    public static final ResourceLocation ID = FTBChunksAPI.rl("game_time");
    private static final Icon CLOCK_ICON = ItemIcon.getItemIcon(Items.CLOCK);

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void render(MinimapContext context, GuiGraphics graphics, Font font) {
        String setting = context.getSetting(this);
        if (setting.equals(ClockedTimeMode.CLOCK.name())) {
            CLOCK_ICON.draw(graphics, -8, 0, 16, 16);
            return;
        }

        Minecraft minecraft = context.minecraft();

        long time = minecraft.level.getDayTime() % 24000L;
        int hours = (int) (time / 1000L);
        int minutes = (int) ((time % 1000L) * 60L / 1000L);
        drawCenteredText(minecraft.font, graphics, Component.literal(RealTimeComponent.createTimeString(hours, minutes, setting.equals(ClockedTimeMode.TWENTY_FOUR.name()))), 0);
    }


    @Override
    public int height(MinimapContext context) {
        String setting = context.getSetting(this);
        return !setting.equals(ClockedTimeMode.CLOCK.name()) ? MinimapInfoComponent.super.height(context) : 10;
    }

    @Override
    public Set<TranslatedOption> getConfigComponents() {
        return Arrays.stream(ClockedTimeMode.values())
                .map(value -> new TranslatedOption(value.name(), "ftbchunks.time_mode." + ClockedTimeMode.NAME_MAP.getName(value)))
                .collect(Collectors.toSet());
    }

    public enum ClockedTimeMode {
        TWENTY_FOUR,
        TWELVE,
        CLOCK;

        public static final NameMap<ClockedTimeMode> NAME_MAP = NameMap.of(TWENTY_FOUR, values()).baseNameKey("ftbchunks.time_mode").create();
    }

}
