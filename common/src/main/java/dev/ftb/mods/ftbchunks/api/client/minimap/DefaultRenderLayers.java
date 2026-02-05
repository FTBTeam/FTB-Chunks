package dev.ftb.mods.ftbchunks.api.client.minimap;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.event.MinimapLayerEvent;
import net.minecraft.resources.Identifier;

/**
 * All the built-in minimap layer render IDs. You can use these to order against when adding custom layers via the
 * {@link MinimapLayerEvent}.
 * <p>
 * Note that adding a layer renderer before the {@code TERRAIN} layer (the first built-in layer) will probably result
 * in your layer being hidden by that layer!
 */
public class DefaultRenderLayers {
    public static final Identifier TERRAIN = FTBChunksAPI.id("terrain");
    public static final Identifier CROSSHAIRS = FTBChunksAPI.id("crosshairs");
    public static final Identifier COMPASS = FTBChunksAPI.id("compass");
    public static final Identifier ICONS = FTBChunksAPI.id("icons");
    public static final Identifier PLAYER = FTBChunksAPI.id("player");
    public static final Identifier INFO = FTBChunksAPI.id("info");
}
