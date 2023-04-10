package dev.ftb.mods.ftbchunks.compat.commonprot;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksExpected;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.Protection;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.common.protection.api.ProtectionProvider;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public class FTBChunksProtectionProvider implements ProtectionProvider {
    public static ResourceLocation ID = new ResourceLocation(FTBChunks.MOD_ID, "provider");
    private static LoadingCache<GameProfile, ServerPlayer> FAKE_PLAYERS = null;

    public static void init() {
        CommonProtection.register(ID, new FTBChunksProtectionProvider());

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            FAKE_PLAYERS = CacheBuilder.newBuilder()
                .initialCapacity(64)
                .expireAfterWrite(
                    30, TimeUnit.SECONDS)
                .softValues()
                .build(CacheLoader.from((profile) -> new ServerPlayer(server, server.overworld(), profile, null)));
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            FAKE_PLAYERS = null;
        });
    }

    @Override
    public boolean isProtected(Level world, BlockPos pos) {
        return FTBChunksAPI.getManager().getChunk(new ChunkDimPos(world, pos)) != null;
    }

    @Override
    public boolean isAreaProtected(Level world, AABB area) {
        int minCX = (int) Math.floor(area.minX / 16);
        int minCZ = (int) Math.floor(area.minX / 16);
        int maxCX = (int) Math.ceil(area.maxX / 16);
        int maxCZ = (int) Math.ceil(area.maxZ / 16);

        for (var chunk : FTBChunksAPI.getManager().getAllClaimedChunks()) {
            if (chunk.pos.x >= minCX && chunk.pos.x < maxCX
                && chunk.pos.z >= minCZ && chunk.pos.z < maxCZ) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canBreakBlock(Level world, BlockPos pos, GameProfile profile, Player player) {
        player = tryResolvePlayer(world, profile);

        if (player == null) return true;

        return !FTBChunksAPI.getManager().protect(player, InteractionHand.MAIN_HAND, pos, FTBChunksExpected.getBlockBreakProtection(), null);
    }

    @Override
    public boolean canExplodeBlock(Level world, BlockPos pos, Explosion explosion, GameProfile profile, Player player) {
        if (explosion.source == null && !FTBChunksWorldConfig.PROTECT_UNKNOWN_EXPLOSIONS.get()) {
            return true;
        }

        ChunkDimPos chunkPos = new ChunkDimPos(world, pos);
        ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(chunkPos);

        return chunk == null || chunk.allowExplosions();
    }

    @Override
    public boolean canPlaceBlock(Level world, BlockPos pos, GameProfile profile, Player player) {
        player = tryResolvePlayer(world, profile);

        if (player == null) return true;

        return !FTBChunksAPI.getManager().protect(player, InteractionHand.MAIN_HAND, pos, FTBChunksExpected.getBlockPlaceProtection(), null);
    }

    @Override
    public boolean canInteractBlock(Level world, BlockPos pos, GameProfile profile, Player player) {
        player = tryResolvePlayer(world, profile);

        if (player == null) return true;

        return !FTBChunksAPI.getManager().protect(player, InteractionHand.MAIN_HAND, pos, FTBChunksExpected.getBlockInteractProtection(), null);

    }

    @Override
    public boolean canInteractEntity(Level world, Entity entity, GameProfile profile, Player player) {
        player = tryResolvePlayer(world, profile);

        if (player == null) return true;

        return !FTBChunksAPI.getManager().protect(player, InteractionHand.MAIN_HAND, entity.blockPosition(), Protection.INTERACT_ENTITY, entity);
    }

    @Override
    public boolean canDamageEntity(Level world, Entity entity, GameProfile profile, Player player) {
        if (entity instanceof LivingEntity) return true;

        player = tryResolvePlayer(world, profile);

        if (player == null) return true;

        return !FTBChunksAPI.getManager().protect(player, InteractionHand.MAIN_HAND, entity.blockPosition(), Protection.ATTACK_NONLIVING_ENTITY, entity);
    }

    public static @Nullable ServerPlayer tryResolvePlayer(Level l, GameProfile profile) {
        if (!(l instanceof ServerLevel sl)) {
            return null;
        }

        ServerPlayer online = sl.getServer().getPlayerList().getPlayer(profile.getId());

        if (online != null) return online;

        return FAKE_PLAYERS.getUnchecked(profile);
    }
}
