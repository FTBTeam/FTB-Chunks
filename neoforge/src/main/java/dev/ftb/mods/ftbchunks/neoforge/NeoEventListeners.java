package dev.ftb.mods.ftbchunks.neoforge;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.api.FTBChunksTags;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.command.FTBChunksCommands;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftbchunks.util.FTBCUtils;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.neoforge.FTBTeamsEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NeoEventListeners {
    private final FTBChunks mod;

    public NeoEventListeners(FTBChunks mod) {
        this.mod = mod;

        IEventBus bus = NeoForge.EVENT_BUS;

        bus.addListener(FTBTeamsEvent.TeamPlayerLoggedIn.class, event -> mod.onPlayerLogin(event.getEventData()));
        bus.addListener(FTBTeamsEvent.TeamManager.class, event -> mod.onTeamManagerEvent(event.getEventData()));
        bus.addListener(FTBTeamsEvent.TeamCreated.class, event -> mod.onTeamCreated(event.getEventData()));
        bus.addListener(FTBTeamsEvent.TeamLoaded.class, event -> mod.onTeamLoaded(event.getEventData()));
        bus.addListener(FTBTeamsEvent.TeamSaved.class, event -> mod.onTeamSaved(event.getEventData()));
        bus.addListener(FTBTeamsEvent.PlayerJoinedPartyTeam.class, event -> mod.onPlayerJoinedParty(event.getEventData()));
        bus.addListener(FTBTeamsEvent.PlayerLeftPartyTeam.class, event -> mod.onPlayerLeftParty(event.getEventData()));
        bus.addListener(FTBTeamsEvent.PlayerTransferredOwnership.class, event -> mod.onTeamOwnershipTransferred(event.getEventData()));
        bus.addListener(FTBTeamsEvent.TeamPropertiesChanged.class, event -> mod.onTeamPropertiesChanged(event.getEventData()));
        bus.addListener(FTBTeamsEvent.TeamAlly.class, event -> mod.onPlayerAllianceChange(event.getEventData()));
        bus.addListener(this::collectTeamProperties);

        bus.addListener(EventPriority.HIGHEST, this::blockLeftClick);
        bus.addListener(EventPriority.HIGHEST, this::blockRightClick);
        bus.addListener(EventPriority.HIGHEST, this::itemRightClick);
        bus.addListener(EventPriority.HIGHEST, this::interactEntity);
        bus.addListener(EventPriority.HIGHEST, this::farmlandTrample);
        bus.addListener(EventPriority.HIGHEST, this::blockBreak);
        bus.addListener(EventPriority.HIGHEST, this::blockPlace);
        bus.addListener(EventPriority.HIGHEST, this::playerAttackEntity);

        bus.addListener(this::entityInteractSpecific);
        bus.addListener(this::mobGriefing);
        bus.addListener(this::enteringSection);
        bus.addListener(this::onLevelLoad);
        bus.addListener(this::onPlayerLogout);
        bus.addListener(this::playerCloned);
        bus.addListener(this::playerChangeDimension);
        bus.addListener(this::checkSpawn);
        bus.addListener(this::onLivingHurt);
        bus.addListener(this::registerCommands);
        bus.addListener(this::explosionDetonate);
        bus.addListener(ServerTickEvent.Post.class, event -> mod.serverTickPost(event.getServer()));
        bus.addListener(ServerStartingEvent.class, _ -> FTBCUtils.postMinYEvent(false));

        // TODO bucket filling mixin (do we need this?)
//        bus.addListener(this::fillBucket);
    }

    private void explosionDetonate(ExplosionEvent.Detonate event) {
        List<BlockPos> posList = event.getAffectedBlocks();
        var byChunk = posList.stream().collect(Collectors.groupingBy(ChunkPos::containing));

        List<BlockPos> newList = new ArrayList<>();
        byChunk.forEach((chunkPos, l) -> {
            ClaimedChunkImpl cc = ClaimedChunkManagerImpl.getInstance().getChunk(new ChunkDimPos(event.getExplosion().level().dimension(), chunkPos));
            if (cc == null || cc.allowExplosions()) {
                newList.addAll(l);
            }
        });
        if (newList.size() != posList.size()) {
            posList.clear();
            posList.addAll(newList);
        }
    }

    private void registerCommands(RegisterCommandsEvent event) {
        FTBChunksCommands.registerCommands(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    private void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!FTBChunks.onLivingHurt(event.getEntity(), event.getSource(), event.getAmount())) {
            event.setCanceled(true);
        }
    }

    private void checkSpawn(MobSpawnEvent.SpawnPlacementCheck event) {
        Vec3 vec = Vec3.atBottomCenterOf(event.getPos());
        if (!mod.checkSpawn(event.getEntityType(), event.getLevel(), vec.x(), vec.y(), vec.z(), event.getSpawnType(), null)) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    private void playerAttackEntity(AttackEntityEvent event) {
        if (mod.playerAttackEntity(event.getEntity(), event.getEntity().level(), event.getEntity().getUsedItemHand(), event.getTarget(), null) == InteractionResult.FAIL) {
            event.setCanceled(true);
        }
    }

    private void playerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            mod.playerChangedDimension(sp, event.getFrom(), event.getTo());
        }
    }

    private void playerCloned(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer prev && event.getEntity() instanceof ServerPlayer current) {
            mod.playerCloned(prev, current, !event.isWasDeath());
        }
    }

    private void blockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!mod.blockPlace(event.getLevel(), event.getPos(), event.getState(), event.getEntity(), Protection.EDIT_BLOCK)) {
            event.setCanceled(true);
        }
    }

    private void blockBreak(BreakBlockEvent event) {
        if (!mod.blockBreak(event.getLevel(), event.getPos(), event.getState(), event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    private void blockLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        assert event.getFace() != null;
        if (mod.blockLeftClick(event.getEntity(), event.getLevel(), event.getHand(), event.getPos(), event.getFace()) == InteractionResult.FAIL) {
            event.setCanceled(true);
        }
    }

    private void blockRightClick(PlayerInteractEvent.RightClickBlock event) {
        assert event.getFace() != null;
        if (mod.blockRightClick(event.getEntity(), event.getHand(), event.getPos(), event.getFace()) == InteractionResult.FAIL) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    private void itemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (mod.itemRightClick(event.getEntity(), event.getHand()) == InteractionResult.FAIL) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    private void interactEntity(PlayerInteractEvent.EntityInteract event) {
        if (!mod.interactEntity(event.getEntity(), event.getTarget(), event.getHand())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    private void farmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!mod.canFarmlandTrample(event.getEntity(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    private void collectTeamProperties(FTBTeamsEvent.CollectTeamProperties event) {
        // common properties
        mod.addCommonTeamProperties(event.getEventData());

        // NeoForge-specific properties
        event.addProperty(FTBChunksProperties.BLOCK_EDIT_MODE);
        event.addProperty(FTBChunksProperties.BLOCK_INTERACT_MODE);
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            mod.onPlayerLogout(sp);
        }
    }

    private void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            mod.onServerLevelLoad(serverLevel);
        }
    }

    /// Temporary hack until Architectury hopefully adds direct support for this. Needed to prevent interaction
    /// with Armor Stands (and probably anything where the specific interaction position is important). NOTE:
    /// currently broken in 1.18.2 due to a Forge bug:
    /// <a href="https://github.com/MinecraftForge/MinecraftForge/issues/8143">...</a>
    ///
    /// @param event the event
    private void entityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!event.getEntity().level().isClientSide() && ClaimedChunkManagerImpl.getInstance().shouldPreventInteraction(event.getEntity(), event.getHand(), event.getEntity().blockPosition(), Protection.INTERACT_ENTITY, event.getTarget())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    private void mobGriefing(EntityMobGriefingEvent event) {
        // we could do this for all mob griefing but that's arguably OP (could trivialize wither fights, for example)
        // expose this as a tag so pack makers can change to what they want (default tag with only enderman)
        // due to current limitations on fabric this tag will only work on NeoForge, while fabric only supports endman
        // Note: explicit check for server-side needed due to Optifine brain-damage
        if (event.getEntity().is(FTBChunksTags.Entities.ENTITY_MOB_GRIEFING_BLACKLIST_TAG) && !event.getEntity().level().isClientSide()) {
            ClaimedChunkImpl cc = ClaimedChunkManagerImpl.getInstance().getChunk(new ChunkDimPos(event.getEntity()));

            if (cc != null && !cc.allowMobGriefing()) {
                event.setCanGrief(false);
            }
        }
    }

    private void enteringSection(EntityEvent.EnteringSection event) {
        if (event.getEntity() instanceof ServerPlayer sp && !(event.getEntity() instanceof FakePlayer)
                && event.didChunkChange() && FTBTeamsAPI.api().isManagerLoaded() && FTBChunksAPI.api().isManagerLoaded()) {
            FTBTeamsAPI.api().getManager().getTeamForPlayerID(event.getEntity().getUUID()).ifPresent(team -> {
                ChunkTeamDataImpl data = ClaimedChunkManagerImpl.getInstance().getOrCreateData(team);
                data.checkForChunkChange(sp, event.getNewPos().x(), event.getNewPos().z());
            });
        }
    }
}
