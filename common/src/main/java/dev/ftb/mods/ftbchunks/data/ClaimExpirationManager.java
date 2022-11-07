package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftbchunks.net.SendManyChunksPacket;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum ClaimExpirationManager {
    INSTANCE;

    private static final int RUN_INTERVAL = 600_000; // in milliseconds - every 10 minutes
    private long lastRun = 0L;

    public void tick() {
        long now = System.currentTimeMillis();
        if (now - lastRun > RUN_INTERVAL) {
            checkForIdleTeams(now);
            checkForTemporaryClaims(now);
            lastRun = now;
        }
    }

    private void checkForIdleTeams(final long now) {
        final long max = FTBChunksWorldConfig.MAX_IDLE_DAYS_BEFORE_UNCLAIM.get() * 86_400_000L; // days -> milliseconds

        if (max == 0L) return;

        Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> toSync = new HashMap<>();
        ClaimedChunkManager manager = FTBChunksAPI.getManager();
        CommandSourceStack sourceStack = FTBTeamsAPI.getManager().server.createCommandSourceStack();

        FTBTeamsAPI.getManager().getTeams().forEach(team -> {
            FTBChunksTeamData data = manager.getData(team);
            if (now - data.getLastLoginTime() > max) {
                // unclaim this team's chunks!
                List<ClaimedChunk> chunks = new ArrayList<>(data.getClaimedChunks());
                FTBChunks.LOGGER.info("all chunk claims for team {} have expired due to team inactivity; unclaiming {} chunks", data, chunks.size());
                chunks.forEach(c -> {
                    unclaimChunk(now, c, toSync, sourceStack);
                });
                data.save();

            }
        });

        syncUnclaimed(toSync, sourceStack);
    }

    private void checkForTemporaryClaims(final long now) {
        Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> toSync = new HashMap<>();
        ClaimedChunkManager manager = FTBChunksAPI.getManager();
        CommandSourceStack sourceStack = FTBTeamsAPI.getManager().server.createCommandSourceStack();

        manager.getAllClaimedChunks().forEach(c -> {
            if (c.hasExpired(now)) {
                FTBChunks.LOGGER.info("unclaiming chunk {} - expiry time {} passed", c, c.getExpiryTime());
                unclaimChunk(now, c, toSync, sourceStack);
            }
        });

        syncUnclaimed(toSync, sourceStack);
    }

    private static void unclaimChunk(long now, ClaimedChunk c, Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> toSync, CommandSourceStack sourceStack) {
        c.unclaim(sourceStack, false);
        toSync.computeIfAbsent(c.pos.dimension, s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, c.pos.x, c.pos.z, null));
    }

    private static void syncUnclaimed(Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> toSync, CommandSourceStack sourceStack) {
        toSync.forEach((dimension, chunkPackets) -> {
            if (!chunkPackets.isEmpty()) {
                new SendManyChunksPacket(dimension, Util.NIL_UUID, chunkPackets).sendToAll(sourceStack.getServer());
            }
        });
    }
}
