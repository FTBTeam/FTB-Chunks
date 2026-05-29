package dev.ftb.mods.ftbchunks;

import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftbchunks.net.LoadedChunkViewPacket;
import dev.ftb.mods.ftbchunks.util.platform.ForceLoadHandler;
import dev.ftb.mods.ftbchunks.util.platform.PlatformProtections;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

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

    @Override
    public boolean isForceLoadingRequested(Level level, ChunkPos pos) {
        if (level.isClientSide()) {
            if (level.dimension().equals(ClientUtils.getClientLevel().dimension())) {
                return MapDimension.getCurrent().map(dim -> {
                    MapRegion mapRegion = dim.getRegions().get(XZ.regionFromChunk(pos));
                    int l = dim.getLoadedView(mapRegion, pos.x() & 0x1f, pos.z() & 0x1f);
                    return l == LoadedChunkViewPacket.FORCE_LOADED;
                }).orElse(false);
            }
            return false;
        } else {
            return isManagerLoaded() && getManager().isChunkForceLoaded(new ChunkDimPos(level.dimension(), pos));
        }
    }

    @Override
    public Optional<Team> getOwningTeam(Level level, @UnknownNullability ChunkPos pos) {
        if (level.isClientSide()) {
            if (level.dimension().equals(ClientUtils.getClientLevel().dimension())) {
                return MapDimension.getCurrent().map(dim -> dim.getOwningTeam(pos));
            }
            return Optional.empty();
        } else {
            var cc = FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level.dimension(), pos));
            return cc == null ? Optional.empty() : Optional.of(cc.getTeamData().getTeam());
        }
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
