package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.InfoConfigComponent;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RealTimeComponent implements MinimapInfoComponent {

    public static final ResourceLocation ID = FTBChunksAPI.rl("real_time");

    //Todo make this better - unreal
    private final Map<InfoConfigComponent, FTBChunksClientConfig.TimeMode> configComponents;
    private final Map<FTBChunksClientConfig.TimeMode, InfoConfigComponent> configComponentsReverse;

    public RealTimeComponent() {
        this.configComponents = new HashMap<>();
        this.configComponentsReverse = new HashMap<>();
        for (FTBChunksClientConfig.TimeMode value : FTBChunksClientConfig.TimeMode.values()) {
            configComponents.put(() -> FTBChunksClientConfig.TimeMode.NAME_MAP.getDisplayName(value), value);
            configComponentsReverse.put(value, () -> FTBChunksClientConfig.TimeMode.NAME_MAP.getDisplayName(value));
        }
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

    @Override
    public Set<InfoConfigComponent> getConfigComponents() {
        return configComponents.keySet();
    }

    @Override
    @Nullable
    public InfoConfigComponent getActiveConfigComponent() {
        return configComponentsReverse.get(FTBChunksClientConfig.MINIMAP_SHOW_REAL_TIME.get());
    }

    @Override
    public void setActiveConfigComponent(InfoConfigComponent component) {
        FTBChunksClientConfig.MINIMAP_SHOW_REAL_TIME.set(configComponents.get(component));
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
