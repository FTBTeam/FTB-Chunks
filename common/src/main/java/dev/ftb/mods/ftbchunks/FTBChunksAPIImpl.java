package dev.ftb.mods.ftbchunks;

import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftbchunks.util.platform.ForceLoadHandler;
import dev.ftb.mods.ftbchunks.util.platform.PlatformProtections;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public enum FTBChunksAPIImpl implements FTBChunksAPI.API {
    INSTANCE;

    private ForceLoadHandler forceLoadHandler = (_, _, _, _, _)
            -> FTBChunks.LOGGER.warn("no force loaded handler has been set!");
    @Nullable
    private PlatformProtections platformProtections;

    @Override
    public ClaimedChunkManagerImpl getManager() {
        return Objects.requireNonNull(ClaimedChunkManagerImpl.getInstance());
    }

    @Override
    public boolean isManagerLoaded() {
        return ClaimedChunkManagerImpl.exists();
    }

    @Override
    public ClaimResult claimAsPlayer(ServerPlayer player, ResourceKey<Level> dimension, ChunkPos pos, boolean checkOnly) {
        return getManager().getOrCreateData(player).claim(player.createCommandSourceStack(), new ChunkDimPos(dimension, pos), checkOnly);
    }

    @Override
    public boolean isChunkForceLoaded(ChunkDimPos chunkPos) {
        return isManagerLoaded() && getManager().isChunkForceLoaded(chunkPos);
    }

    public void setForceLoadHandler(ForceLoadHandler handler) {
        this.forceLoadHandler = handler;
    }

    public ForceLoadHandler getForceLoadHandler() {
        return forceLoadHandler;
    }

    public PlatformProtections getProtectionImplementations() {
        return Objects.requireNonNull(platformProtections);
    }

    public void setPlatformProtections(PlatformProtections platformProtections) {
        this.platformProtections = platformProtections;
    }
}
