package dev.ftb.mods.ftbchunks;

import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Objects;

public enum FTBChunksAPIImpl implements FTBChunksAPI.API {
    INSTANCE;

    @Override
    public ClaimedChunkManagerImpl getManager() {
        return Objects.requireNonNull(ClaimedChunkManagerImpl.getInstance());
    }

    @Override
    public boolean isManagerLoaded() {
        return ClaimedChunkManagerImpl.getInstance() != null;
    }

    @Override
    public ClaimResult claimAsPlayer(ServerPlayer player, ResourceKey<Level> dimension, ChunkPos pos, boolean checkOnly) {
        return getManager().getOrCreateData(player).claim(player.createCommandSourceStack(), new ChunkDimPos(dimension, pos), checkOnly);
    }

    @Override
    public boolean isChunkForceLoaded(ChunkDimPos chunkPos) {
        return isManagerLoaded() && getManager().isChunkForceLoaded(chunkPos);
    }
}
