package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftbchunks.net.SendManyChunksPacket;
import dev.ftb.mods.ftbteams.data.ServerTeam;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.*;

public enum ClaimExpirationManager {
    INSTANCE;

    private static final long RUN_INTERVAL = 600_000L;       // milliseconds in 10 minutes
    private static final long DAYS_TO_MILLIS = 86_400_000L;  // milliseconds in a day

    private long lastRun = 0L;

    public void tick(MinecraftServer server) {
        if ((server.getTickCount() & 0x3f) == 0) {
            // System.currentTimeMillis() can be slow-ish on some JVMs so don't check every single tick
            long now = System.currentTimeMillis();
            if (now - lastRun > RUN_INTERVAL) {
                var chunkMap = FTBChunksAPI.getManager().getClaimedChunksByTeam(cc -> !(cc.teamData.getTeam() instanceof ServerTeam));
                checkForIdleTeams(server, now, chunkMap);
                checkForTemporaryClaims(server, now, chunkMap);
                lastRun = now;
            }
        }
    }

    private void checkForIdleTeams(MinecraftServer server, final long now, Map<UUID, List<ClaimedChunk>> chunkMap) {
        final long maxClaim = (long) (FTBChunksWorldConfig.MAX_IDLE_DAYS_BEFORE_UNCLAIM.get() * DAYS_TO_MILLIS);
        final long maxForce = (long) (FTBChunksWorldConfig.MAX_IDLE_DAYS_BEFORE_UNFORCE.get() * DAYS_TO_MILLIS);

        if (maxClaim == 0L && maxForce == 0L) return;

        List<ClaimedChunk> expiredClaims = new ArrayList<>();
        List<ClaimedChunk> expiredForceloads = new ArrayList<>();

        chunkMap.forEach((id, chunks) -> {
            List<ClaimedChunk> toExpireClaims = new ArrayList<>();
            List<ClaimedChunk> toExpireForce = new ArrayList<>();
            chunks.forEach(cc -> {
                if (maxClaim > 0 && now - cc.teamData.getLastLoginTime() > maxClaim && cc.teamData.getTeam().getOnlineMembers().isEmpty()) {
                    toExpireClaims.add(cc);
                }
                if (maxForce > 0 && cc.isForceLoaded() && now - cc.teamData.getLastLoginTime() > maxForce && cc.teamData.getTeam().getOnlineMembers().isEmpty()) {
                    toExpireForce.add(cc);
                }
            });
            if (!toExpireClaims.isEmpty()) {
                FTBChunks.LOGGER.info("all chunk claims for team {} have expired due to team inactivity; unclaiming {} chunks", id.toString(), toExpireClaims.size());
                expiredClaims.addAll(toExpireClaims);
            }
            if (!toExpireForce.isEmpty()) {
                FTBChunks.LOGGER.info("all forceloads for team {} have expired due to team inactivity; unforcing {} chunks", id.toString(), toExpireForce.size());
                expiredForceloads.addAll(toExpireForce);
            }
        });

        CommandSourceStack sourceStack = server.createCommandSourceStack();
        Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> toSync = new HashMap<>();

        if (!expiredForceloads.isEmpty()) {
            expiredForceloads.forEach(cc -> unloadChunk(now, cc, toSync, sourceStack));
        }
        if (!expiredClaims.isEmpty()) {
            expiredClaims.forEach(cc -> unclaimChunk(now, cc, toSync, sourceStack));
        }
        if (!toSync.isEmpty()) {
            syncChunks(toSync, server, Util.NIL_UUID);
        }
    }

    private void checkForTemporaryClaims(MinecraftServer server, final long now, Map<UUID, List<ClaimedChunk>> chunkMap) {
        chunkMap.forEach((teamId, chunks) -> {
            List<ClaimedChunk> expired = chunks.stream()
                    .filter(cc -> cc.isForceLoaded() && cc.hasExpired(now))
                    .toList();
            if (!expired.isEmpty()) {
                FTBChunksTeamData teamData = expired.get(0).teamData;
                CommandSourceStack sourceStack = server.createCommandSourceStack();
                Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> toSync = new HashMap<>();
                expired.forEach(cc -> {
                    FTBChunks.LOGGER.info("un-forceloading chunk {} - expiry time {} passed", cc, cc.getForceLoadExpiryTime());
                    unloadChunk(now, cc, toSync, sourceStack);
                });
                syncChunks(toSync, server, teamData.getTeamId());
            }
        });
    }

    private static void unclaimChunk(long now, ClaimedChunk c, Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> toSync, CommandSourceStack sourceStack) {
        c.unclaim(sourceStack, false);
        toSync.computeIfAbsent(c.pos.dimension, s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, c.pos.x, c.pos.z, null));
    }

    private static void unloadChunk(long now, ClaimedChunk c, Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> toSync, CommandSourceStack sourceStack) {
        c.unload(sourceStack);
        toSync.computeIfAbsent(c.pos.dimension, s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, c.pos.x, c.pos.z, c));
    }

    private static void syncChunks(Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> toSync, MinecraftServer server, UUID teamId) {
        toSync.forEach((dimension, chunkPackets) -> {
            if (!chunkPackets.isEmpty()) {
                new SendManyChunksPacket(dimension, teamId, chunkPackets).sendToAll(server);
            }
        });
    }
}
