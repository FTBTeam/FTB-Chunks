package dev.ftb.mods.ftbchunks.client.mapicon;

import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;

public record InWorldMapIcon(MapIcon icon, float x, float y, double distanceToPlayer, double distanceToMouse) {
}
