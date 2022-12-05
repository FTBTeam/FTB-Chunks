package dev.ftb.mods.ftbchunks.integration.waystones;

import dev.ftb.mods.ftbchunks.integration.MapIconEvent;
import dev.ftb.mods.ftbchunks.integration.RefreshMinimapIconsEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.*;

public class WaystonesCommon {
    private static final Map<ResourceKey<Level>, List<WaystoneMapIcon>> WAYSTONES = new HashMap<>();

    public static void init() {
        MapIconEvent.MINIMAP.register(WaystonesCommon::mapWidgets);
        MapIconEvent.LARGE_MAP.register(WaystonesCommon::mapWidgets);
    }

    public static void updateWaystones(List<WaystoneData> waystoneData) {
        WAYSTONES.clear();

        waystoneData.forEach(w -> WAYSTONES.computeIfAbsent(w.dimension(), k -> new ArrayList<>()).add(w.icon()));

        RefreshMinimapIconsEvent.trigger();
    }

    public static void mapWidgets(MapIconEvent event) {
        List<WaystoneMapIcon> list = WAYSTONES.getOrDefault(event.getDimension(), Collections.emptyList());

        if (!list.isEmpty()) {
            for (WaystoneMapIcon icon : list) {
                event.add(icon);
            }
        }
    }
}
