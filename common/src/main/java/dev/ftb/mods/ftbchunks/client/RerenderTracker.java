package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftbchunks.client.map.ChunkUpdateTask;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.config.FTBChunksClientConfig;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/// Tracks blocks which need updating from the world into MapRegionData structures
public class RerenderTracker {
    // map of chunk pos -> set of offset within that chunk
    private final Map<ChunkPos, IntOpenHashSet> rerenderCache = new HashMap<>();
    // for debug reporting
    private int rerenderCount = 0;

    public Set<ChunkPos> getPendingRerender() {
        return rerenderCache.keySet();
    }

    public void requestRerender(BlockPos pos) {
        ChunkPos chunkPos = ChunkPos.containing(pos);
        IntOpenHashSet set = rerenderCache.computeIfAbsent(chunkPos, _ -> new IntOpenHashSet());

        if (set.add((pos.getX() & 15) + ((pos.getZ() & 15) * 16))) {
            if (FTBChunksClientConfig.DEBUG_INFO.get()) {
                rerenderCount++;
            }
        }
    }

    public void requestRerender(ChunkPos pos) {
        MapManager manager = MapManager.getInstance().orElse(null);
        Level level = Minecraft.getInstance().level;

        if (level != null) {
            ChunkAccess chunkAccess = level.getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false);
            if (chunkAccess != null) {
                FTBChunksClient.MAP_EXECUTOR.execute(new ChunkUpdateTask(manager, level, chunkAccess, pos));
            }
        }
    }

    public void run(Level level, MapManager manager) {
        if (!rerenderCache.isEmpty()) {
            rerenderCache.forEach((chunkPos, blocks) -> {
                    ChunkAccess chunkAccess = level.getChunk(chunkPos.x(), chunkPos.z(), ChunkStatus.FULL, false);
                    if (chunkAccess != null) {
                        FTBChunksClient.MAP_EXECUTOR.execute(new ChunkUpdateTask(manager, level, chunkAccess, chunkPos, blocks.toIntArray()));
                    }
            });
            rerenderCache.clear();
        }
    }

    public void clear() {
        rerenderCache.clear();
        rerenderCount = 0;
    }

    public int getRerenderCount() {
        return rerenderCount;
    }
}
