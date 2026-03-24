package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapComponentContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

public class BiomeComponent implements MinimapInfoComponent {
    public static final Identifier ID = FTBChunksAPI.id("biome");

    @Nullable
    private ResourceKey<Biome> biomeKey;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void render(MinimapComponentContext context, GuiGraphicsExtractor graphics, Font font) {
        if (biomeKey != null) {
            drawCenteredText(context.minecraft().font, graphics, Component.translatable("ftbchunks.minimap.biome").append(": ")
                    .append(Component.translatable("biome." + biomeKey.identifier().getNamespace() + "." + biomeKey.identifier().getPath())), 0);
        }
    }

    @Override
    public boolean shouldRender(MinimapComponentContext context) {
        Holder<Biome> biome = context.clientLevel().getBiome(context.clientPlayer().blockPosition());
        if (biome.unwrapKey().isPresent()) {
            biomeKey = biome.unwrapKey().get();
            return true;
        }

        return false;
    }
}
