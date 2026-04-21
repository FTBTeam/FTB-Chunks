package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapComponentContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.api.client.minimap.TranslatedOption;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.NameMap;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ZoneInfoComponent implements MinimapInfoComponent {
    public static final Identifier ID = FTBChunksAPI.id("zone");
    public static final Component WILDERNESS = Component.translatable("wilderness").withStyle(s -> s.withColor(ChatFormatting.DARK_GREEN).withItalic(true));

    @Nullable
    private Team team;
    private boolean shouldRender;
    private long lastCheck = 0L;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void render(MinimapComponentContext context, GuiGraphicsExtractor graphics, Font font) {
        String setting = context.getSetting(this);

        if (team != null) {
            drawCenteredText(context.minecraft().font, graphics, team.getColoredName(), 0);
        } else if (setting.equals(ShowWilderness.SHOW_WILDERNESS.name())) {
            drawCenteredText(context.minecraft().font, graphics, WILDERNESS, 0);
        }
    }

    @Override
    public boolean shouldRender(MinimapComponentContext context) {
        // we don't want to be checking the chunk ownership every frame... every 500ms should be good enough
        long now = Util.getEpochMillis();
        if (now - lastCheck > 500L) {
            lastCheck = now;
            team = null;

            var data = context.mapDimension().getRegion(XZ.regionFromChunk(context.mapChunksPos().x(), context.mapChunksPos().z())).getData();
            if (data != null) {
                Optional<Team> foundTeam = data.getChunk(XZ.of(context.mapChunksPos().x(), context.mapChunksPos().z())).getTeam();
                if (foundTeam.isPresent()) {
                    team = foundTeam.get();
                    shouldRender = true;
                }
            }
            if (team == null) {
                String setting = context.getSetting(this);
                shouldRender = !setting.isEmpty() && !setting.equals(ShowWilderness.JUST_CLAIMED.name());
            }
        }

        return shouldRender;
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
