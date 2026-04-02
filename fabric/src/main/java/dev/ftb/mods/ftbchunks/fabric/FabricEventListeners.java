package dev.ftb.mods.ftbchunks.fabric;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.command.FTBChunksCommands;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftbchunks.util.FTBCUtils;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.event.CollectTeamPropertiesEvent;
import dev.ftb.mods.ftbteams.api.fabric.FTBTeamsEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;

public enum FabricEventListeners {
    INSTANCE;

    private FTBChunks mod;

    public static FabricEventListeners get() {
        return INSTANCE;
    }

    public void init(FTBChunks mod) {
        this.mod = mod;

        ServerLevelEvents.LOAD.register((_, level) -> mod.onServerLevelLoad(level));

        FTBTeamsEvents.TEAM_MANAGER.register(mod::onTeamManagerEvent);
        FTBTeamsEvents.TEAM_PLAYER_LOGGED_IN.register(mod::onPlayerLogin);
        FTBTeamsEvents.TEAM_CREATED.register(mod::onTeamCreated);
        FTBTeamsEvents.TEAM_LOADED.register(mod::onTeamLoaded);
        FTBTeamsEvents.TEAM_SAVED.register(mod::onTeamSaved);
        FTBTeamsEvents.COLLECT_TEAM_PROPERTIES.register(this::collectProperties);
        FTBTeamsEvents.PLAYER_JOINED_PARTY_TEAM.register(mod::onPlayerJoinedParty);
        FTBTeamsEvents.PLAYER_LEFT_PARTY_TEAM.register(mod::onPlayerLeftParty);
        FTBTeamsEvents.PLAYER_TRANSFERRED_OWNERSHIP.register(mod::onTeamOwnershipTransferred);
        FTBTeamsEvents.TEAM_PROPERTIES_CHANGED.register(mod::onTeamPropertiesChanged);
        FTBTeamsEvents.TEAM_ALLY.register(mod::onPlayerAllianceChange);

        AttackBlockCallback.EVENT.register(mod::blockLeftClick);
        AttackEntityCallback.EVENT.register(mod::playerAttackEntity);
        UseBlockCallback.EVENT.register((player, _, hand, hitResult)
                -> mod.blockRightClick(player, hand, hitResult.getBlockPos(), hitResult.getDirection()));
        UseItemCallback.EVENT.register((player, _, hand)
                -> mod.itemRightClick(player, hand));
        UseEntityCallback.EVENT.register((player, _, hand, entity, _)
                -> mod.interactEntity(player, entity, hand) ? InteractionResult.PASS : InteractionResult.FAIL);
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, _)
                -> mod.blockBreak(level, pos, state, player));

        ServerPlayerEvents.LEAVE.register(mod::onPlayerLogout);
        ServerPlayerEvents.COPY_FROM.register(mod::playerCloned);

        // TODO bucket filling mixin (do we need this?)
//        PlayerEvent.FILL_BUCKET.register(this::fillBucket);
        // TODO: spawn check mixins (multiple)
//        EntityEvent.LIVING_CHECK_SPAWN.register(this::checkSpawn);

        CommandRegistrationCallback.EVENT.register(FTBChunksCommands::registerCommands);

        ServerTickEvents.END_SERVER_TICK.register(mod::serverTickPost);
        ServerLifecycleEvents.SERVER_STARTING.register(_ -> FTBCUtils.postMinYEvent(false));
    }

    public void playerChangedDimension(ServerPlayer serverPlayer, ResourceKey<Level> oldDimension, ResourceKey<Level> newDimension) {
        mod.playerChangedDimension(serverPlayer, oldDimension, newDimension);
    }

    private void collectProperties(CollectTeamPropertiesEvent.Data data) {
        // common properties
        mod.addCommonTeamProperties(data);

        // Fabric-specific properties
        data.addProperty(FTBChunksProperties.BLOCK_EDIT_AND_INTERACT_MODE);
    }

    public void handleServerExplosion(ServerExplosion explosion, Set<BlockPos> posSet) {
        // called from ServerExplosionMixin, not a Fabric API listener
        var byChunk = posSet.stream().collect(Collectors.groupingBy(ChunkPos::containing));

        byChunk.forEach((chunkPos, posList) -> {
            ClaimedChunkImpl cc = ClaimedChunkManagerImpl.getInstance().getChunk(new ChunkDimPos(explosion.level().dimension(), chunkPos));
            if (cc != null && !cc.allowExplosions()) {
                posList.forEach(posSet::remove);
            }
        });
    }

    public boolean canFarmlandTrample(Entity entity, BlockPos pos) {
        return mod.canFarmlandTrample(entity, pos);
    }

    public boolean canPlaceBlock(@Nullable Player player, BlockPos pos) {
        return !(player instanceof ServerPlayer)
                || !ClaimedChunkManagerImpl.getInstance().shouldPreventInteraction(player, InteractionHand.MAIN_HAND, pos, Protection.EDIT_BLOCK, null);
    }

    public void onSectionChange(Player player, int chunkX, int chunkZ) {
        FTBTeamsAPI.api().getManager().getTeamForPlayerID(player.getUUID()).ifPresent(team -> {
            ChunkTeamDataImpl data = ClaimedChunkManagerImpl.getInstance().getOrCreateData(team);
            data.checkForChunkChange(player, chunkX, chunkZ);
        });
    }
}
