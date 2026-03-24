package dev.ftb.mods.ftbchunks.data;

import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Object;
import dev.ftb.mods.ftbchunks.config.FTBChunksWorldConfig;
import dev.ftb.mods.ftblibrary.json5.Json5Ops;
import dev.ftb.mods.ftblibrary.json5.Json5Util;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TeamMemberData {
    private int maxClaims;
    private int maxForceLoads;
    private boolean offlineForceLoader;
    private final Set<ChunkDimPos> originalClaims;

    private TeamMemberData(int maxClaims, int maxForceLoads, boolean offlineForceLoader, Set<ChunkDimPos> originalClaims) {
        this.maxClaims = maxClaims;
        this.maxForceLoads = maxForceLoads;
        this.offlineForceLoader = offlineForceLoader;
        this.originalClaims = originalClaims;
    }

    public static TeamMemberData defaultData() {
        return new TeamMemberData(FTBChunksWorldConfig.MAX_CLAIMED_CHUNKS.get(), FTBChunksWorldConfig.MAX_FORCE_LOADED_CHUNKS.get(), false, new HashSet<>());
    }

    public static TeamMemberData fromJson(Json5Object json) {
        int maxClaims = Json5Util.getInt(json, "max_claimed_chunks").orElse(FTBChunksWorldConfig.MAX_CLAIMED_CHUNKS.get());
        int maxForced = Json5Util.getInt(json,"max_force_loaded_chunks").orElse(FTBChunksWorldConfig.MAX_FORCE_LOADED_CHUNKS.get());
        boolean offline = Json5Util.getBoolean(json,"offline_force_loader").orElse(false);
        Set<ChunkDimPos> orig = Json5Util.getJson5Object(json, "original_claims").map(TeamMemberData::readOriginalClaims).orElse(Set.of());

        return new TeamMemberData(maxClaims, maxForced, offline, orig);
    }

    public static TeamMemberData fromPlayerData(ServerPlayer player, ChunkTeamDataImpl otherTeam) {
        return new TeamMemberData(
                otherTeam.getMaxClaimChunks() + otherTeam.getExtraClaimChunks(),
                otherTeam.getMaxForceLoadChunks() + otherTeam.getExtraForceLoadChunks(),
                FTBChunksWorldConfig.canPlayerOfflineForceload(player),
                new HashSet<>(otherTeam.getClaimedChunks().stream().map(ClaimedChunkImpl::getPos).toList())
        );
    }

    public Json5Object toJson() {
        Json5Object tag = new Json5Object();

        tag.addProperty("max_claimed_chunks", maxClaims);
        tag.addProperty("max_force_loaded_chunks", maxForceLoads);
        tag.addProperty("offline_force_loader", offlineForceLoader);
        Json5Object origTag = writeOriginalClaims();
        if (!origTag.isEmpty()) tag.add("original_claims", origTag);

        return tag;
    }

    private Json5Object writeOriginalClaims() {
        Json5Object origTag = new Json5Object();

        Map<String, Json5Array> perDimensionTags = new HashMap<>();
        originalClaims.forEach(cdp -> {
            Json5Array l = perDimensionTags.computeIfAbsent(cdp.dimension().identifier().toString(), ignored -> new Json5Array());
            l.add(ChunkPos.CODEC.encodeStart(Json5Ops.INSTANCE, cdp.chunkPos()).getOrThrow());
        });
        perDimensionTags.forEach(origTag::add);

        return origTag;
    }

    private static Set<ChunkDimPos> readOriginalClaims(Json5Object tag) {
        Set<ChunkDimPos> res = new HashSet<>();
        tag.asMap().forEach((dimStr, el) -> {
            try {
                ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimStr));
                Set<ChunkDimPos> cdpSet = new HashSet<>();
                el.getAsJson5Array().forEach(member -> {
                    ChunkPos cp = ChunkPos.CODEC.parse(Json5Ops.INSTANCE, member).getOrThrow();
                    cdpSet.add(new ChunkDimPos(dimKey, cp));
                });
                res.addAll(cdpSet);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        });
        return res;
    }

    public int getMaxClaims() {
        return maxClaims;
    }

    public void setMaxClaims(int maxClaims) {
        this.maxClaims = maxClaims;
    }

    public int getMaxForceLoads() {
        return maxForceLoads;
    }

    public void setMaxForceLoads(int maxForceLoads) {
        this.maxForceLoads = maxForceLoads;
    }

    public boolean isOfflineForceLoader() {
        return offlineForceLoader;
    }

    public void setOfflineForceLoader(boolean offlineForceLoader) {
        this.offlineForceLoader = offlineForceLoader;
    }

    public Set<ChunkDimPos> getOriginalClaims() {
        return originalClaims;
    }
}
