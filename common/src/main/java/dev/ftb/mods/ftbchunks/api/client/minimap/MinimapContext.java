package dev.ftb.mods.ftbchunks.api.client.minimap;

import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftblibrary.math.XZ;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.Map;

/**
 * Minimal context for Minimap Info Components
 *
 * @param minecraft The Minecraft instance (Helper)
 * @param player The client player
 * @param mapDimension The dimension of the players location
 * @param region The region of the players location
 * @param mapChunkPosX The chunk X position of the map
 * @param mapChunkPosZ The chunk Z position of the map
 * @param mapPlayerX The player X position on the map
 * @param mapPlayerY The player Y position on the map
 * @param mapPlayerZ The player Z position on the map
 */
public record MinimapContext(
   Minecraft minecraft,
   LocalPlayer player,
   MapDimension mapDimension,
   XZ region,
   int mapChunkPosX,
   int mapChunkPosZ,
   double mapPlayerX,
   double mapPlayerY,
   double mapPlayerZ,
   Map<String, String> infoSettings
) {

    public String getSetting(MinimapInfoComponent infoComponent) {
        return infoSettings.getOrDefault(infoComponent.id().toString(), "");
    }
}
