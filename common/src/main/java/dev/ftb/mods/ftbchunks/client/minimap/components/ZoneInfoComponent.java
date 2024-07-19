package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.api.client.minimap.TranslatedOption;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ZoneInfoComponent implements MinimapInfoComponent {
    public static final ResourceLocation ID = FTBChunksAPI.rl("zone");
    public static final Component WILDNESS = Component.translatable("wilderness").withStyle(s -> s.withColor(ChatFormatting.DARK_GREEN).withItalic(true));

    private Team team;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void render(MinimapContext context, GuiGraphics graphics, Font font) {
        String setting = context.getSetting(this);

        if (team != null) {
            drawCenteredText(context.minecraft().font, graphics, team.getColoredName(), 0);
        } else if (setting.equals(ShowWilderness.SHOW_WILDERNESS.name())) {
            drawCenteredText(context.minecraft().font, graphics, WILDNESS, 0);
        }
    }

    @Override
    public boolean shouldRender(MinimapContext context) {
        var data = context.mapDimension().getRegion(XZ.regionFromChunk(context.mapChunksPosX(), context.mapChunksPosZ())).getData();

        team = null;
        if (data != null) {
            Optional<Team> foundTeam = data.getChunk(XZ.of(context.mapChunksPosX(), context.mapChunksPosZ())).getTeam();
            if (foundTeam.isPresent()) {
                team = foundTeam.get();
                return true;
            }
        }

        String setting = context.getSetting(this);
        return !setting.isEmpty() && !setting.equals(ShowWilderness.JUST_CLAIMED.name());
    }


    @Override
    public Set<TranslatedOption> getConfigComponents() {
        return Arrays.stream(ShowWilderness.values())
                .map(value -> new TranslatedOption(value.name(), "ftbchunks.show_wilderness." + ShowWilderness.NAME_MAP.getName(value)))
                .collect(Collectors.toSet());
    }

    public enum ShowWilderness {
        JUST_CLAIMED,
        SHOW_WILDERNESS;

        public static final NameMap<ShowWilderness> NAME_MAP = NameMap.of(SHOW_WILDERNESS, values()).baseNameKey("ftbchunks.show_wilderness").create();
    }

}
