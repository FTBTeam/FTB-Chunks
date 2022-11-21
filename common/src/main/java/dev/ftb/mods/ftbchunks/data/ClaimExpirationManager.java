package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftbchunks.net.SendManyChunksPacket;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.stream.Collectors;

public enum ClaimExpirationManager {
    INSTANCE;

    private static final int RUN_INTERVAL = 600_000; // in milliseconds - every 10 minutes
    private long lastRun = 0L;

    public void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        if (now - lastRun > RUN_INTERVAL) {
            checkForIdleTeams(server, now);
            checkForTemporaryClaims(server, now);
            lastRun = now;
        }
    }

    private void checkForIdleTeams(MinecraftServer server, final long now) {
        final long max = FTBChunksWorldConfig.MAX_IDLE_DAYS_BEFORE_UNCLAIM.get() * 86_400_000L; // days -> milliseconds

        if (max == 0L) return;

        ClaimedChunkManager manager = FTBChunksAPI.getManager();
        CommandSourceStack sourceStack = server.createCommandSourceStack();

        List<ClaimedChunk> expired = new ArrayList<>();

        FTBTeamsAPI.getManager().getTeams().stream()
                .map(manager::getData)
                .filter(data -> now - data.getLastLoginTime() > max)
                .forEach(data -> {
                    Collection<ClaimedChunk> chunks = data.getClaimedChunks();
                    expired.addAll(chunks);
                    FTBChunks.LOGGER.info("all chunk claims for team {} have expired due to team inactivity; unclaiming {} chunks", data, chunks.size());
                });

        if (!expired.isEmpty()) {
            Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> toSync = new HashMap<>();
            expired.forEach(c -> unclaimChunk(now, c, toSync, sourceStack));
            syncChunks(toSync, sourceStack, Util.NIL_UUID);
        }
    }

    private void checkForTemporaryClaims(MinecraftServer server, final long now) {
        ClaimedChunkManager manager = FTBChunksAPI.getManager();

        Map<UUID,List<ClaimedChunk>> chunkMap = manager.getAllClaimedChunks().stream()
                .collect(Collectors.groupingBy(cc -> cc.teamData.getTeamId()));

        chunkMap.forEach((teamId, chunks) -> {
            Team team = FTBTeamsAPI.getManager().getTeamByID(teamId);
            if (team != null) {
                FTBChunksTeamData data = manager.getData(team);
                List<ClaimedChunk> expired = chunks.stream()
                        .filter(cc -> cc.hasExpired(now))
                        .toList();
                if (!expired.isEmpty()) {
                    CommandSourceStack sourceStack = server.createCommandSourceStack();
                    Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> toSync = new HashMap<>();
                    expired.forEach(cc -> {
                        FTBChunks.LOGGER.info("un-forceloading chunk {} - expiry time {} passed", cc, cc.getForceLoadExpiryTime());
                        unloadChunk(now, cc, toSync, sourceStack);
                    });
                    syncChunks(toSync, sourceStack, data.getTeamId());
                }
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

    private static void syncChunks(Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> toSync, CommandSourceStack sourceStack, UUID teamId) {
        toSync.forEach((dimension, chunkPackets) -> {
            if (!chunkPackets.isEmpty()) {
                new SendManyChunksPacket(dimension, teamId, chunkPackets).sendToAll(sourceStack.getServer());
            }
        });
    }
}
