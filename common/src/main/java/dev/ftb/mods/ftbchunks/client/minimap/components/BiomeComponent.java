package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

public class BiomeComponent implements MinimapInfoComponent {

    public static final Identifier ID = FTBChunksAPI.id("biome");

    private ResourceKey<Biome> biomeKey;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void render(MinimapContext context, GuiGraphics graphics, Font font) {
        drawCenteredText(context.minecraft().font, graphics, Component.translatable("biome." + biomeKey.identifier().getNamespace() + "." + biomeKey.identifier().getPath()), 0);
    }

    @Override
    public boolean shouldRender(MinimapContext context) {
        Holder<Biome> biome = context.minecraft().level.getBiome(context.minecraft().player.blockPosition());
        if (biome.unwrapKey().isPresent()) {
            biomeKey = biome.unwrapKey().get();
            return true;
        }

        return false;
    }
}
