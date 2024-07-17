package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.InfoConfigComponent;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GameTimeComponent implements MinimapInfoComponent {

    public static final ResourceLocation ID = FTBChunksAPI.rl("game_time");
    private static final Icon CLOCK_ICON = ItemIcon.getItemIcon(Items.CLOCK);

    //Todo make this better - unreal
    private final Map<InfoConfigComponent, FTBChunksClientConfig.ClockedTimeMode> configComponents;
    private final Map<FTBChunksClientConfig.ClockedTimeMode, InfoConfigComponent> configComponentsReverse;

    public GameTimeComponent() {
        this.configComponents = new HashMap<>();
        this.configComponentsReverse = new HashMap<>();
        for (FTBChunksClientConfig.ClockedTimeMode value : FTBChunksClientConfig.ClockedTimeMode.values()) {
            configComponents.put(() -> FTBChunksClientConfig.ClockedTimeMode.NAME_MAP.getDisplayName(value), value);
            configComponentsReverse.put(value, () -> FTBChunksClientConfig.ClockedTimeMode.NAME_MAP.getDisplayName(value));
        }
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void render(MinimapContext context, GuiGraphics graphics, Font font) {
        FTBChunksClientConfig.ClockedTimeMode clockedTimeMode = FTBChunksClientConfig.MINIMAP_SHOW_GAME_TIME.get();
        if (clockedTimeMode == FTBChunksClientConfig.ClockedTimeMode.CLOCK) {
            CLOCK_ICON.draw(graphics, -8, 0, 16, 16);
            return;
        }

        Minecraft minecraft = context.minecraft();

        long time = minecraft.level.getDayTime() % 24000L;
        int hours = (int) (time / 1000L);
        int minutes = (int) ((time % 1000L) * 60L / 1000L);
        drawCenteredText(minecraft.font, graphics, Component.literal(RealTimeComponent.createTimeString(hours, minutes, clockedTimeMode == FTBChunksClientConfig.ClockedTimeMode.TWENTY_FOUR)), 0);
    }


    @Override
    public int height(MinimapContext context) {
        return FTBChunksClientConfig.MINIMAP_SHOW_GAME_TIME.get() != FTBChunksClientConfig.ClockedTimeMode.CLOCK ? MinimapInfoComponent.super.height(context) : 10;
    }

    @Override
    public Set<InfoConfigComponent> getConfigComponents() {
        return configComponents.keySet();
    }

    @Override
    @Nullable
    public InfoConfigComponent getActiveConfigComponent() {
        return configComponentsReverse.get(FTBChunksClientConfig.MINIMAP_SHOW_GAME_TIME.get());
    }

    @Override
    public void setActiveConfigComponent(InfoConfigComponent component) {
        FTBChunksClientConfig.MINIMAP_SHOW_GAME_TIME.set(configComponents.get(component));
    }
}
