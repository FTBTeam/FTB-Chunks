package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.api.client.event.ChunksUpdatedFromServerEvent;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.Team;
import org.jetbrains.annotations.Nullable;

import java.util.*;


/**
 * Aggregates update-from-server events to avoid firing many events for individual chunk updates. Events are
 * aggregated by team ID (incl. none) and claim/load/expiry times.
 */
class PendingUpdateEvents {
    private final Map<MapKey, Set<ChunkDimPos>> pendingMap = new HashMap<>();

    void addPending(@Nullable Team team, ChunkDimPos pos, MapChunk.DateInfo dateInfo) {
        pendingMap.computeIfAbsent(MapKey.of(team, dateInfo), k -> new HashSet<>()).add(pos);
    }

    void fireEvents() {
        pendingMap.forEach((key, chunks) -> ChunksUpdatedFromServerEvent.UPDATED.invoker().accept(
                new ChunksUpdatedFromServerEvent(chunks, key.team, key.claimed, key.forceLoaded, key.expiry)
        ));
        pendingMap.clear();
    }

    private record MapKey(@Nullable Team team, Date claimed, Date forceLoaded, Date expiry) {
        static MapKey of(Team team, MapChunk.DateInfo dateInfo) {
            return new MapKey(team, dateInfo.claimed(), dateInfo.forceLoaded(), dateInfo.expiry());
        }
    }
}
