package dev.ftb.mods.ftbchunks.api.client.minimap;

import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftblibrary.math.XZ;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Minimal context for Minimap Info Components
 *
 * @param minecraft The Minecraft instance (Helper)
 * @param player The client player
 * @param mapDimension The dimension of the players location
 * @param mapChunksPos The chunk for the players location
 * @param playerPos the players pos
 * @param infoSettings raw settings for this component
 */
public record MinimapContext(
        Minecraft minecraft,
        LocalPlayer player,
        MapDimension mapDimension,
        XZ mapChunksPos,
        Vec3 playerPos,
        Map<String, String> infoSettings
) {

    public String getSetting(MinimapInfoComponent infoComponent) {
        return infoSettings.getOrDefault(infoComponent.id().toString(), "");
    }
}