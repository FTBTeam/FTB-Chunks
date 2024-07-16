package dev.ftb.mods.ftbchunks.client;

import com.google.common.collect.ImmutableList;
import dev.ftb.mods.ftbchunks.api.client.FTBChunksClientAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FTBChunksClientAPIImpl implements FTBChunksClientAPI {
    private static final List<MinimapInfoComponent> minimapComponents = new ArrayList<>();

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

    @Override
    public void registerMinimapComponent(MinimapInfoComponent component) {
        if (minimapComponents.contains(component)) {
            throw new IllegalStateException("Minimap component %s already registered".formatted(component.id()));
        }

        minimapComponents.add(component);
    }

    @Override
    public void recomputeMinimapComponents() {
        FTBChunksClient.INSTANCE.setupComponents();
    }

    @Override
    public ImmutableList<MinimapInfoComponent> getMinimapComponents() {
        return ImmutableList.copyOf(minimapComponents);
    }
}
