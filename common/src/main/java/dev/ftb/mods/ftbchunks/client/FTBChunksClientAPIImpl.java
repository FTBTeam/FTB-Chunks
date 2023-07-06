package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftbchunks.api.client.FTBChunksClientAPI;
import dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class FTBChunksClientAPIImpl implements FTBChunksClientAPI {
    @Override
    public Optional<WaypointManager> getWaypointManager() {
        return MapDimension.getCurrent().flatMap(d -> Optional.ofNullable(d.getWaypointManager()));
    }

    @Override
    public Optional<WaypointManager> getWaypointManager(ResourceKey<Level> dimension) {
        return MapManager.getInstance().flatMap(manager -> Optional.ofNullable(manager.getDimension(dimension).getWaypointManager()));
    }

    @Override
    public void requestMinimapIconRefresh() {
        FTBChunksClient.INSTANCE.refreshMinimapIcons();
    }
}
