package dev.ftb.mods.ftbchunks.api;

import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.Team;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public interface ClaimedChunkManager {
    /**
     * Get the FTB Chunks team data for the given team, creating a new instance if necessary.
     *
     * @param team the team
     * @return the FTB Chunks data for the team
     */
    ChunkTeamData getOrCreateData(@NotNull Team team);

    /**
     * Get the FTB Chunks team data for the given player ID, creating a new instance if necessary. This will always
     * for the player's personal team, even if they are currently in a party team.
     *
     * @param id a player UUID
     * @return the FTB Chunks data for the player
     */
    ChunkTeamData getPersonalData(UUID id);

    /**
     * Get the FTB Chunks team data for the given player, creating a new instance if necessary. This will get the
     * data for the party team the player is in, if applicable.
     *
     * @param player the player
     * @return the FTB Chunks data for the team
     */
    ChunkTeamData getOrCreateData(ServerPlayer player);

    boolean hasData(ServerPlayer player);

    /**
     * Get the claimed chunk data for the chunk at the given position.
     *
     * @param pos the dimension and chunk pos
     * @return the claim chunk data, or null if the chunk is not currently claimed
     */
    @Nullable
    ClaimedChunk getChunk(ChunkDimPos pos);

    /**
     * Get an unmodifiable view of all claimed chunks on this server.
     *
     * @return all claimed chunks
     */
    Collection<? extends ClaimedChunk> getAllClaimedChunks();

    /**
     * Get an unmodifiable view of all claimed chunks, grouped by team ID.
     *
     * @param predicate a predicate to filter chunks from the returned data
     * @return a map of team ID to collection of claimed chunks
     */
    Map<UUID, Collection<ClaimedChunk>> getClaimedChunksByTeam(Predicate<ClaimedChunk> predicate);

    /**
     * Return true if the given player has special permission to bypass all chunk protections. By default, no
     * player has this permission, even server ops (but server ops may give themselves this permission via the
     * {@code /ftbchunks admin bypass_protection} command).
     *
     * @param player the player being checked
     * @return true if the player can bypass protection checking
     */
    boolean getBypassProtection(UUID player);

    /**
     * Set the protection bypass flag for the given player. See {@link #getBypassProtection(UUID)} for more
     * information.
     *
     * @param player the player to adjust
     * @param bypass true if the player should be able to bypass all protection, false otherwise
     */
    void setBypassProtection(UUID player, boolean bypass);

    /**
     * Check if the intended interaction should be prevented from occurring.
     *
     * @implNote null may be passed as the acting entity, but this method will always return false if the actor does
     * not extend {@code ServerPlayer} (fake players are OK as long as this the case)
     *
     * @param actor the entity performing the interaction, should be a player
     * @param hand the actor's hand
     * @param pos the block position at which the action will be performed
     * @param protection the type of protection being checked for
     * @param targetEntity the entity being acted upon, if any (e.g. a painting, armor stand etc.)
     * @return true to prevent the interaction, false to permit it
     */
    boolean shouldPreventInteraction(@Nullable Entity actor, InteractionHand hand, BlockPos pos, Protection protection, @Nullable Entity targetEntity);

    /**
     * Get an unmodifiable view of all currently force-loaded chunks, grouped by dimension. The key of the returned
     * map is a long encoding of the chunk position; you can obtain the chunk pos via {@code new ChunkPos(long pos)}.
     * The value of the map items is the UUID of the owning {@link Team}.
     *
     * @return a map of dimension to map of all force-loaded chunks, mapping long-encoded chunk pos to team ID
     */
    Map<ResourceKey<Level>, Long2ObjectMap<UUID>> getForceLoadedChunks();

    /**
     * Get an unmodifiable view of the force loaded chunks for the given dimension. The keys and values of the returned
     * map as the same as for {@link #getForceLoadedChunks()}.
     *
     * @param dimension the dimension to check
     * @return a map of all force-loaded chunks, mapping long-encoded chunk pos to team ID
     */
    @NotNull Long2ObjectMap<UUID> getForceLoadedChunks(ResourceKey<Level> dimension);

    /**
     * Check if the given chunk (dimension and chunk pos) is currently force-loaded.
     *
     * @param chunkDimPos the chunk dimension and position
     * @return true if the chunk is force-loaded
     */
    boolean isChunkForceLoaded(ChunkDimPos chunkDimPos);
}
