package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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

    public static TeamMemberData deserializeNBT(CompoundTag tag) {
        int maxClaims = tag.getInt("max_claimed_chunks");
        int maxForced = tag.getInt("max_force_loaded_chunks");
        boolean offline = tag.getBoolean("offline_force_loader");
        Set<ChunkDimPos> orig = readOriginalClaims(tag.getCompound("original_claims"));

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

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putInt("max_claimed_chunks", maxClaims);
        tag.putInt("max_force_loaded_chunks", maxForceLoads);
        tag.putBoolean("offline_force_loader", offlineForceLoader);
        CompoundTag origTag = writeOriginalClaims();
        if (!origTag.isEmpty()) tag.put("original_claims", origTag);

        return tag;
    }

    private CompoundTag writeOriginalClaims() {
        CompoundTag origTag = new CompoundTag();

        Map<String, ListTag> perDimensionTags = new HashMap<>();
        originalClaims.forEach(cdp -> {
            ListTag l = perDimensionTags.computeIfAbsent(cdp.dimension().location().toString(), k -> new ListTag());
            SNBTCompoundTag cdpTag = new SNBTCompoundTag();
            cdpTag.singleLine();
            cdpTag.putInt("x", cdp.x());
            cdpTag.putInt("z", cdp.z());
            l.add(cdpTag);
        });
        perDimensionTags.forEach(origTag::put);

        return origTag;
    }

    private static Set<ChunkDimPos> readOriginalClaims(CompoundTag tag) {
        Set<ChunkDimPos> res = new HashSet<>();
        for (String dimStr : tag.getAllKeys()) {
            try {
                ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimStr));
                Set<ChunkDimPos> cdpSet = new HashSet<>();
                tag.getList(dimStr, Tag.TAG_COMPOUND).forEach(el -> {
                    if (el instanceof CompoundTag c) {
                        cdpSet.add(new ChunkDimPos(dimKey, c.getInt("x"), c.getInt("z")));
                    }
                });
                res.addAll(cdpSet);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
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
