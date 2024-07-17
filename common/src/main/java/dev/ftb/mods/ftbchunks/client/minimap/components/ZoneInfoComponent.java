package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class ZoneInfoComponent implements MinimapInfoComponent {
    public static final ResourceLocation ID = FTBChunksAPI.rl("zone");

    private Team team;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void render(MinimapContext context, GuiGraphics graphics, Font font) {
        drawCenteredText(context.minecraft().font, graphics, team.getColoredName(), 0);
    }

    @Override
    public boolean shouldRender(MinimapContext context) {
        var data = context.mapDimension().getRegion(context.region()).getData();
        if (data == null) {
            return false;
        }

        Optional<Team> foundTeam = data.getChunk(context.region()).getTeam();
        if (foundTeam.isEmpty()) {
            return false;
        }

        team = foundTeam.get();
        return true;
    }

    @Override
    public boolean enabled() {
        return FTBChunksClientConfig.MINIMAP_ZONE.get();
    }
}
