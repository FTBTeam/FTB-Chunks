package dev.ftb.mods.ftbchunks.api.client.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.Team;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class ChunksUpdatedFromServerEvent {
    /**
     * Fired when the client receives an update from the server about the claim or force-loading status of a chunk. One
     * event may contain multiple chunk/dimension positions; they are aggregated by owning team and claim/forceload/expiry
     * dates.
     * <p>
     * Note that this event is fired on the client potentially several ticks after the server has made the updates,
     * depending on the value of the "task_queue_ticks" client config setting.
     */
    public static final Event<Consumer<ChunksUpdatedFromServerEvent>> UPDATED = EventFactory.createLoop();

    private final Set<ChunkDimPos> chunkDimPos;
    @Nullable
    private final Team team;
    @Nullable
    private final Date claimed;
    @Nullable
    private final Date forceLoaded;
    @Nullable
    private final Date expiry;

    public ChunksUpdatedFromServerEvent(Set<ChunkDimPos> chunkDimPos, @Nullable Team team, @Nullable Date claimed, @Nullable Date forceLoaded, @Nullable Date expiry) {
        this.team = team;
        this.chunkDimPos = chunkDimPos;
        this.claimed = claimed;
        this.forceLoaded = forceLoaded;
        this.expiry = expiry;
    }

    /**
     * Get the team to which this chunk belongs.
     *
     * @return the owning team, or {@code Optional.empty()} if the chunk is unclaimed
     */
    public Optional<Team> getTeam() {
        return Optional.ofNullable(team);
    }

    /**
     * {@return the chunk positions affected by this event}
     */
    public Collection<ChunkDimPos> getChunks() {
        return chunkDimPos;
    }

    /**
     * Get the time at which this chunk was claimed.
     *
     * @return the claim time, or {@code Optional.empty()} if the chunk is unclaimed
     */
    public Optional<Date> getClaimTime() {
        return Optional.ofNullable(claimed);
    }

    /**
     * Get the time at which this chunk was force-loaded.
     *
     * @return the force-loading time, or {@code Optional.empty()} if the chunk is not force-loaded
     */
    public Optional<Date> getForceloadedTime() {
        return Optional.ofNullable(forceLoaded);
    }

    /**
     * Get the time at which any force-loading for this chunk will expire.
     *
     * @return the expiry time, or {@code Optional.empty()} if the chunk is not force-loaded or force-loading does not expire
     */
    public Optional<Date> getForceLoadExpiryTime() {
        return Optional.ofNullable(expiry);
    }

    /**
     * Convenience method to check if the chunk is claimed by any team.
     *
     * @return true if the chunk is claimed, false otherwise
     */
    public boolean isClaimed() {
        return team != null;
    }
}
