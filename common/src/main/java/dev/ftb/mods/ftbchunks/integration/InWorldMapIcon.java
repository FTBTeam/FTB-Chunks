package dev.ftb.mods.ftbchunks.integration;

import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;

public record InWorldMapIcon(MapIcon icon, float x, float y, double distanceToPlayer, double distanceToMouse) {
}
