package dev.ftb.mods.ftbchunks.api;

import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.property.PrivacyProperty;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.UUID;

/**
 * Represents the FTB Chunks information regarding the {@link Team} (either player or party) which may own zero or
 * more chunks on the server. Instances of this can be retrieved via {@link ClaimedChunkManager#getOrCreateData(Team)}
 * or {@link ClaimedChunkManager#getOrCreateData(ServerPlayer)}.
 */
public interface ChunkTeamData {
    /**
     * Convenience method to get the manager instance.
     *
     * @return the claimed chunk manager
     */
    ClaimedChunkManager getManager();

    /**
     * Convenience method to get the FTB Teams manager instances.
     * @return the team manager
     */
    TeamManager getTeamManager();

    /**
     * Get the team that this data pertains to.
     *
     * @return the team
     */
    Team getTeam();

    /**
     * Get a collection of all the chunks this team currently has claimed.
     *
     * @return all the claimed chunks for this team
     */
    Collection<? extends ClaimedChunk> getClaimedChunks();

    /**
     * Get a collection of all the chunks this team currently has force-loading enabled for. Note that the chunks
     * are not necessarily force-loaded right now, depending on player online status and server offline force-loading
     * settings; use {@link ClaimedChunk#isActuallyForceLoaded()} to verify this.
     *
     * @return all the force-loaded chunks for this team
     */
    Collection<? extends ClaimedChunk> getForceLoadedChunks();

    /**
     * Try to claim the given chunk for this team.
     *
     * @param source the command source (player or console) claiming the chunk
     * @param pos the combined dimension and chunk pos
     * @param checkOnly true if just simulating the claim
     *
     * @return the result of the attempt
     */
    ClaimResult claim(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly);

    /**
     * Try to release any claim on the given chunk for this team.
     *
     * @param source the command source (player or console) unclaiming the chunk
     * @param pos the combined dimension and chunk pos
     * @param checkOnly true if just simulating the unclaim
     *
     * @return the result of the attempt
     */
    ClaimResult unclaim(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly);

    /**
     * Try to force-load the given chunk for this team.
     *
     * @param source the command source (player or console) force-loading the chunk
     * @param pos the combined dimension and chunk pos
     * @param checkOnly true if just simulating the force-load
     *
     * @return the result of the attempt
     */
    ClaimResult forceLoad(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly);

    /**
     * Try to cancel any force-load this team has for the given chunk.
     *
     * @param source the command source (player or console) un-force-loading the chunk
     * @param pos the combined dimension and chunk pos
     * @param checkOnly true if just simulating the un-force-load
     *
     * @return the un-force-load result
     */
    ClaimResult unForceLoad(CommandSourceStack source, ChunkDimPos pos, boolean checkOnly);

    /**
     * Convenience method to check if the given player ID is a member of this team
     */
    boolean isTeamMember(UUID playerId);

    /**
     * Convenience method to check if the given player ID is an ally of this team
     */
    boolean isAlly(UUID playerId);

    /**
     * Check if the given player is allowed to use the given privacy property of this team. In general this will be
     * true if
     * <ul>
     *     <li>the player is a member of the team</li>
     *     <li>the player is an ally of the team, and the property has a value of {@link dev.ftb.mods.ftbteams.api.property.PrivacyMode#ALLIES} or {@link dev.ftb.mods.ftbteams.api.property.PrivacyMode#PUBLIC}</li>
     *     <li>the property has a value of {@link dev.ftb.mods.ftbteams.api.property.PrivacyMode#PUBLIC}</li>
     * </ul>
     *
     * @param player the player being checked
     * @param property the team property to check (see {@link FTBChunksProperties} for a list of team properties added by FTB Chunks)
     * @return true if the player has permission to use the property
     */
    boolean canPlayerUse(ServerPlayer player, PrivacyProperty property);

    /**
     * Set the number of extra chunks this team may have claimed. This is 0 by default, but can be increased via
     * the {@code /ftbchunks admin extra_claim_chunks set ...} command.
     *
     * @return the number of extra chunks over the default which this team may claim
     */
    int getExtraClaimChunks();

    /**
     * Set the number of extra chunks this team may have claimed. See {@link #getExtraClaimChunks()}.
     *
     * @param extraClaimChunks the number of extra chunks over the default which this team may claim
     */
    void setExtraClaimChunks(int extraClaimChunks);

    /**
     * Convenience method to check if this team's claims should be hidden from players not on the team. Equivalent
     * to checking the {@link FTBChunksProperties#CLAIM_VISIBILITY} team property being not public.
     *
     * @return true if this team's chunk claims are not publicly visible
     */
    boolean shouldHideClaims();

    /**
     * Get the maximum number of chunks that may be claimed by this team.
     *
     * @return the chunk claim limit
     */
    int getMaxClaimChunks();

    /**
     * Can this team force-load chunks when none of its players are logged in?
     *
     * @return true if this team can do offline force-loading
     */
    boolean canDoOfflineForceLoading();

    /**
     * Set the number of extra chunks this team may force-load. This is 0 by default, but can be increased via
     * the {@code /ftbchunks admin extra_force_load_chunks set ...} command.
     *
     * @return the number of extra chunks over the default which this team may force-load
     */
    int getExtraForceLoadChunks();

    /**
     * Set the number of extra chunks this team may have force-loaded. See {@link #getExtraForceLoadChunks()}.
     *
     * @param extraForceLoadChunks the number of extra chunks over the default which this team may force-load
     */
    void setExtraForceLoadChunks(int extraForceLoadChunks);

    /**
     * Get the maximum number of chunks that may be force-loaded by this team.
     *
     * @return the chunk force-load limit
     */
    int getMaxForceLoadChunks();

    /**
     * Are explosions able to damage terrain in chunks claimed by this team? This is a convenience method to check
     * the value of the {@link FTBChunksProperties#ALLOW_EXPLOSIONS} team property.
     *
     * @return true if explosions can damage terrain, false if not
     */
    boolean canExplosionsDamageTerrain();

    /**
     * Are mobs able to grief terrain in chunks claimed by this team? This is a convenience method to check
     * the value of the {@link FTBChunksProperties#ALLOW_MOB_GRIEFING} team property.
     * <p>
     * Currently, this only extends to Endermen being allowed to pick up and place blocks.
     *
     * @return true if mobs can grief terrain, false if not
     */
    boolean allowMobGriefing();

    /**
     * Is player-vs-player damage allowed in chunks claimed by this team? This is a convenience method to check
     * the value of the {@link FTBChunksProperties#ALLOW_PVP} team property.
     * <p>
     * If PvP is not allowed, then if either the attacker or the victim are in a claimed chunk, PvP damage will
     * be cancelled. Note this is not 100% foolproof, e.g. it won't prevent a player pouring a bucket of lava
     * over another player, for example.
     * <p>
     * This is also dependent on the server's "Allow PvP Combat" setting being set to "per_team".
     *
     * @return true if PvP os permitted, false if not
     */
    boolean allowPVP();

    /**
     * Get the time (milliseconds since the epoch) that any member of this team logged in to the server. This time is
     * used when chunk claiming and force-load expiry is in effect on this server.
     *
     * @return the last login time for this team.
     */
    long getLastLoginTime();

    /**
     * If the given player is an online player, update the team data based on their offline forceloading permissions.
     * Intended for use by external permission mods such as FTB Ranks; call this when permissions change.
     * <p>
     * If the given player is not currently online or not a member of this team, this is a no-op.
     *
     * @param playerId ID of the player to check
     */
    void checkMemberForceLoading(UUID playerId);
}
