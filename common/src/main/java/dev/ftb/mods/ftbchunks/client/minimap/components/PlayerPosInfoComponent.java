package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PlayerPosInfoComponent implements MinimapInfoComponent {
    public static final ResourceLocation ID = FTBChunksAPI.rl("player_pos");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void render(MinimapContext context, GuiGraphics graphics, Font font) {
        var text = Component.literal(Mth.floor(context.mapPlayerX()) + " " + Mth.floor(context.mapPlayerY()) + " " + Mth.floor(context.mapPlayerZ()));
        drawCenteredText(font, graphics, text, 0);
    }

    @Override
    public boolean enabled() {
        return FTBChunksClientConfig.MINIMAP_XYZ.get();
    }
}
