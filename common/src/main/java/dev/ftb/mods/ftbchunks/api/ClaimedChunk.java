package dev.ftb.mods.ftbchunks.api;

import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.commands.CommandSourceStack;

/**
 * Represents a chunk which has been claimed. See {@link ClaimedChunkManager} for methods to retrieve instances of
 * this class.
 */
public interface ClaimedChunk extends ClaimResult {
    /**
     * Get the team data for the team which owns this claim.
     *
     * @return the owning team data
     */
    ChunkTeamData getTeamData();

    /**
     * Get the dimension and chunk pos for this chunk.
     *
     * @return the dimension and chunk pos
     */
    ChunkDimPos getPos();

    /**
     * Get the time (milliseconds since epoch) at which this chunk was claimed.
     *
     * @return the claim time
     */
    long getTimeClaimed();

    /**
     * Get the time (milliseconds since epoch) at which this chunk was force-loaded.
     *
     * @return the claim time; 0 if the chunk is not currently force-loaded
     */
    long getForceLoadedTime();

    /**
     * Convenience method to check if the chunk is force-loaded; equivalent to: {@code if (getForceLoadedTime() > 0)}
     *
     * @return true if the chunk is force-loaded
     */
    boolean isForceLoaded();

    /**
     * Unclaim this chunk; the owning team will lose the claim.
     *
     * @param source the command source (player or console) doing the unclaiming
     */
    void unclaim(CommandSourceStack source, boolean sync);

    /**
     * Un-force-load this chunk. This is a no-op if the chunk isn't currently force-loaded.
     *
     * @param source the command source (player or console) doing the un-force-loading
     */
    void unload(CommandSourceStack source);

    /**
     * Get the time (milliseconds since epoch) at which any force-loading on this chunk will automatically expire.
     * Note that actual auto-expiry may happen slightly after this time, since the server runs an auto-expiry task
     * every few minutes.
     *
     * @return the expiry time, or 0 if there is no auto-expiry in effect
     */
    long getForceLoadExpiryTime();

    /**
     * Get the time (milliseconds since epoch) at which any force-loading on this chunk will automatically expire.
     * This is normally done via the client chunk claiming GUI (using the mouse wheel to adjust expiry times on a
     * claimed chunk).
     *
     * @param forceLoadExpiryTime new auto-expiry time; pass 0 to disable auto-expiry
     */
    void setForceLoadExpiryTime(long forceLoadExpiryTime);

    /**
     * Convenience method to check if force-loading has already expired. Note that the current time is passed in
     * rather than calculated within the method for performance reasons; this could be called for multiple chunks at
     * a time.
     *
     * @param now the current time, as returned by {@code System.getCurrentTimeMillis()}
     * @return true if force-loading has expired
     */
    boolean hasForceLoadExpired(long now);
}
