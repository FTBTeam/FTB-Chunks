package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapContext;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class GameTimeComponent extends AbstractTimeComponent{

    public static final ResourceLocation ID = FTBChunksAPI.rl("game_time");
    private static final Icon CLOCK_ICON = ItemIcon.getItemIcon(Items.CLOCK);

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void render(MinimapContext context, GuiGraphics graphics, Font font) {
        FTBChunksClientConfig.ClockedTimeMode clockedTimeMode = FTBChunksClientConfig.MINIMAP_SHOW_GAME_TIME.get();
        if(clockedTimeMode == FTBChunksClientConfig.ClockedTimeMode.CLOCK) {
            CLOCK_ICON.draw(graphics, -8, 0, 16, 16);
        }else {
            long time = context.minecraft().level.getDayTime() % 24000L;
            int hours = (int) (time / 1000L);
            int minutes = (int) ((time % 1000L) * 60L / 1000L);
            drawCenteredText(context.minecraft().font, graphics, Component.literal(createTimeString(hours, minutes, clockedTimeMode == FTBChunksClientConfig.ClockedTimeMode.TWENTY_FOUR)), 0);
        }
    }

    @Override
    public boolean enabled() {
        return FTBChunksClientConfig.MINIMAP_SHOW_GAME_TIME.get() != FTBChunksClientConfig.ClockedTimeMode.OFF;
    }

    @Override
    public int height(MinimapContext context) {
        return FTBChunksClientConfig.MINIMAP_SHOW_GAME_TIME.get() != FTBChunksClientConfig.ClockedTimeMode.CLOCK ? super.height(context) : 10;
    }
}
