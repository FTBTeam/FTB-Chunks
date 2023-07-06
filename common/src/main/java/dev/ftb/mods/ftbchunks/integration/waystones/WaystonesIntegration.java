package dev.ftb.mods.ftbchunks.integration.waystones;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.event.MapIconEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.*;

public class WaystonesIntegration {
    private static final Map<ResourceKey<Level>, List<WaystoneMapIcon>> WAYSTONES = new HashMap<>();

    public static void initClient() {
        MapIconEvent.MINIMAP.register(WaystonesIntegration::mapWidgets);
        MapIconEvent.LARGE_MAP.register(WaystonesIntegration::mapWidgets);
    }

    public static void updateWaystones(List<WaystoneData> waystoneData) {
        WAYSTONES.clear();

        waystoneData.forEach(w -> WAYSTONES.computeIfAbsent(w.dimension(), k -> new ArrayList<>()).add(w.icon()));

        FTBChunksAPI.clientApi().requestMinimapIconRefresh();
    }

    public static void mapWidgets(MapIconEvent event) {
        WAYSTONES.getOrDefault(event.getDimension(), Collections.emptyList())
                .forEach(event::add);
    }
}
