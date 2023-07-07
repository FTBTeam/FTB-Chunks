package dev.ftb.mods.ftbchunks;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftbchunks.net.SendPlayerPositionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks which players can see which players on the long-range display (i.e. outside of standard vanilla entity
 * tracking range). Updated once a second, if a player has moved by >4 blocks, to keep network chatter down.
 * <p>
 * By default, players must be in the same party, or allied, to be able to see each other long-range. Non-party players
 * won't be long-range visible to others unless they make their "Location Visibility" team property PUBLIC.
 */
public enum LongRangePlayerTracker {
    INSTANCE;

    private long lastTick = 0L;

    private final Table<UUID,UUID,BlockPos> trackingMap = HashBasedTable.create();

    public void tick(MinecraftServer server) {
        int interval = FTBChunksWorldConfig.LONG_RANGE_TRACKER_INTERVAL.get();

        if (interval != 0 && server.getTickCount() - lastTick > interval) {
            lastTick = server.getTickCount();

            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            // 16 blocks in chunk
            int maxDistSq = server.getPlayerList().getViewDistance() * server.getPlayerList().getViewDistance() * 256;

            players.forEach(p1 -> p1.level().players().forEach(p2a -> {
                if (p2a instanceof ServerPlayer p2) {
                    if (shouldTrack(p1, p2, maxDistSq)) {
                        // send a tracking update to p1 IF p2's pos has changed by more than 4 blocks
                        BlockPos lastPos = trackingMap.get(p1.getUUID(), p2.getUUID());
                        if (lastPos == null || p2.blockPosition().distSqr(lastPos) > 16) {
                            new SendPlayerPositionPacket(p2, p2.blockPosition()).sendTo(p1);
                            trackingMap.put(p1.getUUID(), p2.getUUID(), p2.blockPosition());
                        }
                    } else if (trackingMap.contains(p1.getUUID(), p2.getUUID())) {
                        // send an invalid pos to tell p1's client to stop tracking p2
                        new SendPlayerPositionPacket(p2, null).sendTo(p1);
                        trackingMap.remove(p1.getUUID(), p2.getUUID());
                    }
                }
            }));
        }
    }

    public void stopTracking(ServerPlayer player) {
        // called when a player logs out or changes dimension

        if (player.getServer() == null) return;

        Map<UUID,UUID> toRemove = new HashMap<>();
        for (UUID trackingId : trackingMap.rowKeySet()) {
            if (trackingMap.contains(trackingId, player.getUUID())) {
                toRemove.put(trackingId, player.getUUID());
            }
        }

        toRemove.forEach((trackingId, disconnectedId) -> {
            ServerPlayer trackingPlayer = player.getServer().getPlayerList().getPlayer(trackingId);
            if (trackingPlayer != null) {
                new SendPlayerPositionPacket(player, null).sendTo(trackingPlayer);
            }
            trackingMap.remove(trackingId, disconnectedId);
        });
    }

    private boolean shouldTrack(ServerPlayer p1, ServerPlayer p2, int maxDistSq) {
        // must be a different player, in the same dimension, and outside the standard vanilla player tracking distance
        if (p1 == p2 || p1.distanceToSqr(p2) < maxDistSq) return false;

        if (FTBChunksWorldConfig.LOCATION_MODE_OVERRIDE.get()) {
            return true;
        }

        // and player 1 must be able to see player 2 (i.e. player 2's team settings must allow it)
        ChunkTeamDataImpl p2Team = ClaimedChunkManagerImpl.getInstance().getOrCreateData(p2);
        return p2Team.canPlayerUse(p1, FTBChunksProperties.LOCATION_MODE);
    }
}
