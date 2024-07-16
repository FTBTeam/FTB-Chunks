package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public class BiomeComponent implements MinimapInfoComponent {

    public static final ResourceLocation ID = FTBChunksAPI.rl("biome");

    private ResourceKey<Biome> biomeKey;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void render(MinimapContext context, GuiGraphics graphics, Font font) {
        drawCenteredText(context.minecraft().font, graphics, Component.translatable("biome." + biomeKey.location().getNamespace() + "." + biomeKey.location().getPath()), 0);
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

    @Override
    public boolean enabled() {
        return FTBChunksClientConfig.MINIMAP_BIOME.get();
    }
}
