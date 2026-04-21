package dev.ftb.mods.ftbchunks.api.client.event;

import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.Team;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/// Fired when the client receives an update from the server about the claim or force-loading status of a chunk. One
/// event may contain multiple chunk/dimension positions; they are aggregated by owning team and claim/forceload/expiry
/// dates.
///
/// Note that this event is fired on the client potentially several ticks after the server has made the updates,
/// depending on the value of the "task_queue_ticks" client config setting.
///
/// Corresponding platform-native events to listen to:
/// * `FTBChunksClientEvent.ChunkUpdatedFromServer` (NeoForge)
/// * `FTBChunksClientEvents.CHUNKS_UPDATED_FROM_SERVER` (Fabric)
public interface ChunksUpdatedFromServerEvent extends Consumer<ChunksUpdatedFromServerEvent.Data> {
    record Data(
            Set<ChunkDimPos> chunkDimPos,
            @Nullable Team team,
            @Nullable Date claimed,
            @Nullable Date forceLoaded,
            @Nullable Date expiry
    ) {
        /// Get the team to which this chunk belongs.
        ///
        /// @return the owning team, or `Optional.empty()` if the chunk is unclaimed
        public Optional<Team> getTeam() {
            return Optional.ofNullable(team);
        }

        /// {@return the chunk positions affected by this event}
        public Collection<ChunkDimPos> getChunks() {
            return Collections.unmodifiableCollection(chunkDimPos);
        }

        /// Get the time at which this chunk was claimed.
        ///
        /// @return the claim time, or `Optional.empty()` if the chunk is unclaimed
        public Optional<Date> getClaimTime() {
            return Optional.ofNullable(claimed);
        }

        /// Get the time at which this chunk was force-loaded.
        ///
        /// @return the force-loading time, or `Optional.empty()` if the chunk is not force-loaded
        public Optional<Date> getForceloadedTime() {
            return Optional.ofNullable(forceLoaded);
        }

        /// Get the time at which any force-loading for this chunk will expire.
        ///
        /// @return the expiry time, or `Optional.empty()` if the chunk is not force-loaded or force-loading does not expire
        public Optional<Date> getForceLoadExpiryTime() {
            return Optional.ofNullable(expiry);
        }

        /// Convenience method to check if the chunk is claimed by any team.
        ///
        /// @return true if the chunk is claimed, false otherwise
        public boolean isClaimed() {
            return team != null;
        }
    }
}
