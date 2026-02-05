package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftbchunks.api.client.FTBChunksClientAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class FTBChunksClientAPIImpl implements FTBChunksClientAPI {
    // thread-safe list here
    private static final List<MinimapInfoComponent> minimapComponents = new CopyOnWriteArrayList<>();

    @Override
    public Optional<WaypointManager> getWaypointManager() {
        return MapDimension.getCurrent().flatMap(d -> Optional.of(d.getWaypointManager()));
    }

    @Override
    public Optional<WaypointManager> getWaypointManager(ResourceKey<Level> dimension) {
        return MapManager.getInstance().flatMap(manager -> Optional.of(manager.getDimension(dimension).getWaypointManager()));
    }

    @Override
    public void requestMinimapIconRefresh() {
        FTBChunksClient.INSTANCE.getMinimapRenderer().refreshIcons();
    }

    @Override
    public void registerMinimapComponent(MinimapInfoComponent component) {
        if (minimapComponents.contains(component)) {
            throw new IllegalStateException("Minimap component %s already registered".formatted(component.id()));
        }

        minimapComponents.add(component);
    }

    @Override
    public boolean isMinimapComponentEnabled(MinimapInfoComponent component) {
        return !FTBChunksClientConfig.MINIMAP_INFO_HIDDEN.get().contains(component.id().toString());
    }

    @Override
    public void setMinimapComponentEnabled(MinimapInfoComponent component, boolean enabled) {
        if (enabled) {
            FTBChunksClientConfig.MINIMAP_INFO_HIDDEN.get().remove(component.id().toString());
        } else {
            FTBChunksClientConfig.MINIMAP_INFO_HIDDEN.get().add(component.id().toString());
        }
        FTBChunksClientConfig.saveConfig();
        FTBChunksClient.INSTANCE.getMinimapRenderer().setupComponents();
    }

    @Override
    public List<MinimapInfoComponent> getMinimapComponents() {
        return Collections.unmodifiableList(minimapComponents);
    }
}
