package dev.ftb.mods.ftbchunks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.config.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.config.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.data.*;
import dev.ftb.mods.ftbchunks.net.*;
import dev.ftb.mods.ftbchunks.util.FTBCUtils;
import dev.ftb.mods.ftbchunks.util.PlayerNotifier;
import dev.ftb.mods.ftblibrary.config.manager.ConfigManager;
import dev.ftb.mods.ftblibrary.integration.stages.StageHelper;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.platform.Platform;
import dev.ftb.mods.ftblibrary.platform.network.Server2PlayNetworking;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.event.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class FTBChunks {
	public static final String MOD_ID = "ftbchunks";
	public static final Logger LOGGER = LogManager.getLogger("FTB Chunks");
	public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setStrictness(Strictness.LENIENT).create();

	public static final int TILES = 15;
	public static final int TILE_SIZE = 16;
	public static final int TILE_OFFSET = TILES / 2;
	public static final int MINIMAP_SIZE = TILE_SIZE * TILES;

	public static final XZ[] RELATIVE_SPIRAL_POSITIONS = new XZ[TILES * TILES];

	public FTBChunks() {
		FTBChunksAPI._init(FTBChunksAPIImpl.INSTANCE);

		FTBChunksNet.init();

		ConfigManager.getInstance().registerServerConfig(FTBChunksWorldConfig.CONFIG, MOD_ID + ".config.server", true, FTBChunksWorldConfig::onConfigChanged);
		ConfigManager.getInstance().registerClientConfig(FTBChunksClientConfig.CONFIG, MOD_ID + ".config.client");

		for (int i = 0; i < RELATIVE_SPIRAL_POSITIONS.length; i++) {
			RELATIVE_SPIRAL_POSITIONS[i] = MathUtils.getSpiralPoint(i + 1);
		}
	}

	private boolean preventInteraction(@Nullable Entity actor, InteractionHand hand, BlockPos pos, Protection protection, @Nullable Entity targetEntity) {
		return ClaimedChunkManagerImpl.getInstance().shouldPreventInteraction(actor, hand, pos, protection, targetEntity);
	}

	public InteractionResult playerAttackEntity(Player player, Level ignoredLevel, InteractionHand interactionHand, Entity entity, @Nullable EntityHitResult ignoredEntityHitResult) {
		// note: intentionally does not prevent attacking living entities;
		// this is for preventing griefing of entities like paintings & item frames
		if (player instanceof ServerPlayer) {
			if (protectedEntity(entity) && preventInteraction(player, interactionHand, entity.blockPosition(), Protection.ATTACK_NONLIVING_ENTITY, entity)) {
				return InteractionResult.FAIL;
			}
		}

		return InteractionResult.PASS;
	}

	private boolean protectedEntity(Entity e) {
		// Armor stands are a special case: not really living entities, but extend LivingEntity
		return e instanceof ArmorStand || !(e instanceof LivingEntity);
	}

	public static boolean onLivingHurt(LivingEntity living, DamageSource damageSource, float ignoredDamage) {
		if (!living.level().isClientSide() && living instanceof Player target && damageSource.getEntity() instanceof Player attacker) {
			PvPMode mode = FTBChunksWorldConfig.PVP_MODE.get();
			if (mode == PvPMode.ALWAYS) {
				return true;
			}
			if (isPvPProtectedChunk(mode, attacker) || isPvPProtectedChunk(mode, target)) {
				PlayerNotifier.notifyWithCooldown(attacker, Component.translatable("ftbchunks.message.no_pvp").withStyle(ChatFormatting.GOLD), 3000L);
				return false;
			}
		}
		return true;
	}

	private static boolean isPvPProtectedChunk(PvPMode mode, Player player) {
		ClaimedChunk cc = ClaimedChunkManagerImpl.getInstance().getChunk(new ChunkDimPos(player.level(), player.blockPosition()));
		return cc != null && (mode == PvPMode.NEVER || !cc.getTeamData().allowPVP());
	}

	public void onServerLevelLoad(ServerLevel level) {
		if (ClaimedChunkManagerImpl.exists()) {
			ClaimedChunkManagerImpl.getInstance().initForceLoadedChunks(level);
		} else {
			FTBChunks.LOGGER.warn("Level {} loaded before FTB Chunks manager was initialized! Unable to force-load chunks", level.dimension().identifier() );
		}
	}

	public void onTeamManagerEvent(TeamManagerEvent.Data event) {
		switch (event.action()) {
			case CREATED -> onTeamManagerCreated(event.manager());
			case DESTROYED -> onTeamManagerDestroyed();
		}
	}

	private void onTeamManagerCreated(TeamManager manager) {
		ClaimedChunkManagerImpl.init(manager);
	}

	private void onTeamManagerDestroyed() {
		ClaimedChunkManagerImpl.shutdown();
	}

	public void onPlayerLogin(TeamPlayerLoggedInEvent.Data eventData) {
		ServerPlayer player = eventData.player();

		ChunkTeamDataImpl data = ClaimedChunkManagerImpl.getInstance().getOrCreateData(player);
		if (data == null) {
			FTBChunks.LOGGER.error("couldn't get chunk team data for player {} on login?", player.getName().getString());
			return;
		}
		data.updateLimits();

		String playerId = player.getUUID().toString();
		FTBChunks.LOGGER.debug("handling player team login: player = {}, team = {}",
				playerId, data.getTeamId());

		UUID managerId = FTBTeamsAPI.api().getManager().getId();
		Server2PlayNetworking.send(player, new LoginDataPacket(managerId));
		SendGeneralDataPacket.send(data, player);
		FTBChunks.LOGGER.debug("team data sent to {}", playerId);

		long now = System.currentTimeMillis();
		Map<Pair<ResourceKey<Level>, UUID>, List<ChunkSyncInfo>> chunksToSend = new HashMap<>();

		for (ClaimedChunkImpl chunk : ClaimedChunkManagerImpl.getInstance().getAllClaimedChunks()) {
			chunksToSend.computeIfAbsent(Pair.of(chunk.getPos().dimension(), chunk.getTeamData().getTeamId()), s -> new ArrayList<>())
					.add(ChunkSyncInfo.create(now, chunk.getPos().x(), chunk.getPos().z(), chunk));
		}

		chunksToSend.forEach((dimensionAndId, chunkPackets) ->
				FTBTeamsAPI.api().getManager().getTeamByID(dimensionAndId.getRight()).ifPresent(team -> {
					ChunkTeamDataImpl teamData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(team);
					if (teamData.canPlayerUse(player, FTBChunksProperties.CLAIM_VISIBILITY)) {
						SendManyChunksPacket packet = new SendManyChunksPacket(dimensionAndId.getLeft(), dimensionAndId.getRight(), chunkPackets);
						Server2PlayNetworking.send(player, packet);
					}
				}));
		FTBChunks.LOGGER.debug("claimed chunk data sent to {}", playerId);

		data.setLastLoginTime(now);

		data.setForceLoadMember(player.getUUID(), FTBChunksWorldConfig.canPlayerOfflineForceload(player));

		if (data.getTeam().getOnlineMembers().size() == 1 && !data.canDoOfflineForceLoading()) {
			// first player on the team to log in; force chunks if the team can't do offline chunk-loading
			data.updateChunkTickets(true);
		}

		PlayerVisibilityPacket.syncToLevel(player.level());
		FTBChunks.LOGGER.debug("visible player list sent to {}", playerId);
	}

	public void onPlayerLogout(ServerPlayer player) {
		if (!FTBTeamsAPI.api().isManagerLoaded() || !FTBChunksAPI.api().isManagerLoaded()) {
			return;
		}

		ChunkTeamDataImpl data = ClaimedChunkManagerImpl.getInstance().getOrCreateData(player);
		// shouldn't normally be null here, but external problems could force a player logout before they have a team
		// https://github.com/FTBTeam/FTB-Mods-Issues/issues/932
		if (data != null) {
			data.setForceLoadMember(player.getUUID(), FTBChunksWorldConfig.canPlayerOfflineForceload(player));
			ClaimedChunkManagerImpl.getInstance().clearForceLoadedCache();
			LongRangePlayerTracker.INSTANCE.stopTracking(player);

			if (data.getTeam().getOnlineMembers().size() == 1 && !data.canDoOfflineForceLoading()) {
				// last player on the team to log out; unforce chunks if the team can't do offline chunk-loading
				data.updateChunkTickets(false);
			}
		} else {
			FTBChunks.LOGGER.warn("on player disconnect: player '{}' has no team?", player.getGameProfile().name());
		}
	}

	public void onTeamCreated(TeamCreatedEvent.Data event) {
		ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.team());
	}

	public void onTeamLoaded(TeamLoadedEvent.Data event) {
		ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.team());
	}

	public void onTeamSaved(TeamSavedEvent.Data event) {
		ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.team()).saveNow();
	}

	public InteractionResult blockLeftClick(Player player, Level level, InteractionHand hand, BlockPos pos, Direction face) {
		var prot = FTBChunksAPIImpl.INSTANCE.getProtectionImplementations().blockBreakProtection();
		if (player instanceof ServerPlayer && preventInteraction(player, hand, pos, prot, null)) {
			return InteractionResult.FAIL;
		}

		return InteractionResult.PASS;
	}

	public InteractionResult blockRightClick(Player player, InteractionHand hand, BlockPos pos, Direction face) {
		if (player instanceof ServerPlayer sp) {
			boolean blockItem = sp.getItemInHand(hand).getItem() instanceof BlockItem;
			ClaimedChunkManagerImpl mgr = ClaimedChunkManagerImpl.getInstance();
			// not ideal since it also prevents right-clicking *any* blocks if holding a block item when block placement is prevented
			//   but necessary - https://github.com/FTBTeam/FTB-Mods-Issues/issues/1752
			var prot = FTBChunksAPIImpl.INSTANCE.getProtectionImplementations();
			if (mgr.shouldPreventInteraction(player, hand, pos, prot.blockInteractProtection(), null)
					|| blockItem && mgr.shouldPreventInteraction(player, hand, pos, prot.blockPlaceProtection(), null))
			{
				FTBCUtils.forceHeldItemSync(sp, hand);
				return InteractionResult.FAIL;
			}
		}

		return InteractionResult.PASS;
	}

	public InteractionResult itemRightClick(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer sp && preventInteraction(player, hand, BlockPos.containing(player.getEyePosition(1F)), Protection.RIGHT_CLICK_ITEM, null)) {
			FTBCUtils.forceHeldItemSync(sp, hand);
			return InteractionResult.FAIL;
		}

		return InteractionResult.PASS;
	}


	public boolean interactEntity(Player player, Entity entity, InteractionHand hand) {
        return !(player instanceof ServerPlayer)
				|| !preventInteraction(player, hand, entity.blockPosition(), Protection.INTERACT_ENTITY, entity);
    }

	public boolean blockBreak(LevelAccessor ignoredLevel, BlockPos pos, BlockState ignoredBlockState, Player player) {
		var prot = FTBChunksAPIImpl.INSTANCE.getProtectionImplementations().blockBreakProtection();
        return !preventInteraction(player, InteractionHand.MAIN_HAND, pos, prot, null);
    }

	public boolean blockPlace(@UnknownNullability LevelAccessor level, BlockPos pos, BlockState blockState, @Nullable Entity entity, Protection protection) {
		if (entity instanceof ServerPlayer sp && preventInteraction(entity, InteractionHand.MAIN_HAND, pos, protection, null)) {
			FTBCUtils.forceHeldItemSync(sp, InteractionHand.MAIN_HAND);
			return false;
		}

		return true;
	}

	public InteractionResult fillBucket(Player player, Level level, ItemStack emptyBucket, @Nullable HitResult target) {
		if (player instanceof ServerPlayer && target instanceof BlockHitResult hitResult) {
			InteractionHand hand = player.getUsedItemHand();
			if (preventInteraction(player, hand, hitResult.getBlockPos(), Protection.EDIT_FLUID, null)) {
				return InteractionResult.FAIL;
			}
		}

		return InteractionResult.PASS;
	}

	public boolean canFarmlandTrample(Entity entity, BlockPos pos) {
        return !(entity instanceof ServerPlayer)
				|| !preventInteraction(entity, InteractionHand.MAIN_HAND, pos, Protection.EDIT_BLOCK, null);
    }

	public boolean checkSpawn(EntityType<?> entityType, LevelAccessor levelAccessor, double x, double y, double z, EntitySpawnReason type, @Nullable BaseSpawner spawner) {
		if (!levelAccessor.isClientSide() && entityType != EntityType.PLAYER && levelAccessor instanceof Level level) {
			switch (type) {
				case NATURAL, CHUNK_GENERATION, SPAWNER, STRUCTURE, JOCKEY, PATROL -> {
					ClaimedChunkImpl chunk = ClaimedChunkManagerImpl.getInstance().getChunk(new ChunkDimPos(level, BlockPos.containing(x, y, z)));
					if (chunk != null && !chunk.canEntitySpawn(entityType)) {
						return false;
					}
				}
			}
		}

		return true;
	}

	public void playerCloned(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wonGame) {
		// this is better than checking for living death event, because player cloning isn't cancellable
		if (!wonGame) {
			newPlayer.getLastDeathLocation().ifPresent(loc -> {
				int num = newPlayer.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
				Server2PlayNetworking.send(newPlayer, new PlayerDeathPacket(loc, num));
			});
		}
	}

	public void playerChangedDimension(ServerPlayer serverPlayer, ResourceKey<Level> oldLevel, ResourceKey<Level> newLevel) {
		LongRangePlayerTracker.INSTANCE.stopTracking(serverPlayer);

		StageHelper.INSTANCE.getProvider().sync(serverPlayer);
	}

	public void addCommonTeamProperties(CollectTeamPropertiesEvent.Data event) {
		// see Neo & Fabric event handlers for platform-specific properties
		event.addProperty(FTBChunksProperties.ALLOW_EXPLOSIONS);
		event.addProperty(FTBChunksProperties.ALLOW_MOB_GRIEFING);
		event.addProperty(FTBChunksProperties.ALLOW_ALL_FAKE_PLAYERS);
		event.addProperty(FTBChunksProperties.ALLOW_NAMED_FAKE_PLAYERS);
		event.addProperty(FTBChunksProperties.ALLOW_FAKE_PLAYERS_BY_ID);
		event.addProperty(FTBChunksProperties.ALLOW_PVP);
		event.addProperty(FTBChunksProperties.ENTITY_INTERACT_MODE);
		event.addProperty(FTBChunksProperties.NONLIVING_ENTITY_ATTACK_MODE);
		event.addProperty(FTBChunksProperties.CLAIM_VISIBILITY);
		event.addProperty(FTBChunksProperties.LOCATION_MODE);
		event.addProperty(FTBChunksProperties.BYPASS_PROTECTION);
	}

	public void onPlayerJoinedParty(PlayerJoinedPartyTeamEvent.Data event) {
		ChunkTeamDataImpl playerData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.previousTeam());
		ChunkTeamDataImpl partyData  = ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.team());

		partyData.addMemberData(event.player(), playerData);
		partyData.updateLimits();

		// keep a note of the claims the player had - if/when they leave the party, they get those claims back
		// prevents malicious party stealing a player's claims by inviting and then kicking the player
		transferClaims(playerData, partyData, playerData.getClaimedChunks());

		partyData.setForceLoadMember(event.player().getUUID(), FTBChunksWorldConfig.canPlayerOfflineForceload(event.player()));

		PlayerVisibilityPacket.syncToLevel(event.player().level());
		partyData.syncChunksToPlayer(event.player());
	}

	public void onPlayerLeftParty(PlayerLeftPartyTeamEvent.Data event) {
		ChunkTeamDataImpl partyData  = ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.team());

		FTBTeamsAPI.api().getManager().getPlayerTeamForPlayerID(event.playerId()).ifPresent(personalTeam -> {
			ChunkTeamDataImpl playerData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(personalTeam);
			if (event.teamDeleted()) {
				// last player leaving the party; transfer any remaining claims the party had back to that player, if possible
				transferClaims(partyData, playerData, partyData.getClaimedChunks());
				// and purge party team data from manager & disk
				ClaimedChunkManagerImpl.getInstance().deleteTeam(event.team());
			} else {
				// return the departing player's original claims to them, if possible
				transferClaims(partyData, playerData, partyData.getOriginalClaims(event.playerId()));
			}

			partyData.deleteMemberData(event.playerId());
			partyData.updateLimits();

			if (event.player() != null) {
				PlayerVisibilityPacket.syncToLevel(event.player().level());
				SendGeneralDataPacket.send(playerData, event.player());
			}
		});
	}

	private void transferClaims(ChunkTeamDataImpl transferFrom, ChunkTeamDataImpl transferTo, Collection<ClaimedChunkImpl> chunksToTransfer) {
		CommandSourceStack sourceStack = ClaimedChunkManagerImpl.getInstance().getMinecraftServer().createCommandSourceStack();

		String fromName = transferFrom.getTeam().getShortName();
		String toName = transferTo.getTeam().getShortName();

		transferFrom.clearClaimCaches();
		transferTo.clearClaimCaches();

		int nChunks = transferTo.getClaimedChunks().size();

		Map<ResourceKey<Level>, List<ChunkSyncInfo>> chunksToSend = new HashMap<>();
		Map<ResourceKey<Level>, List<ChunkSyncInfo>> chunksToUnclaim = new HashMap<>();
		int transferred = 0;
		int unclaimed = 0;
		long now = System.currentTimeMillis();
		int total = transferTo.getClaimedChunks().size();

		FTBChunks.LOGGER.info("attempting to transfer {} chunks from {} to {}", chunksToTransfer.size(), fromName, toName);

		for (ClaimedChunkImpl chunk : chunksToTransfer) {
			ChunkDimPos cdp = chunk.getPos();
			if (total >= transferTo.getMaxClaimChunks()) {
				chunk.unclaim(sourceStack, false);
				chunksToUnclaim.computeIfAbsent(cdp.dimension(), _ -> new ArrayList<>())
						.add(ChunkSyncInfo.create(now, cdp.x(), cdp.z(), null));
				unclaimed++;
			} else {
				chunk.setTeamData(transferTo);
				chunksToSend.computeIfAbsent(cdp.dimension(), _ -> new ArrayList<>())
						.add(ChunkSyncInfo.create(now, cdp.x(), cdp.z(), chunk));
				transferred++;
			}

			if (chunk.isForceLoaded()) {
				// also transfer any force-load tickets for the old team's ID, since it's no longer valid
				ServerLevel level = ClaimedChunkManagerImpl.getInstance().getMinecraftServer().getLevel(cdp.dimension());
				if (level != null) {
					var handler = FTBChunksAPIImpl.INSTANCE.getForceLoadHandler();
					handler.updateForceLoadingForChunk(level, transferFrom.getTeamId(), cdp.x(), cdp.z(), false);
					if (chunk.isActuallyForceLoaded()) {
						handler.updateForceLoadingForChunk(level, transferTo.getTeamId(), cdp.x(), cdp.z(), true);
					}
				}
			}

			total++;
		}

		transferFrom.updateLimits();
		transferTo.updateLimits();

		if (transferred > 0 || unclaimed > 0) {
			chunksToSend.forEach((dimension, chunkPackets) -> {
				if (!chunkPackets.isEmpty()) {
					new SendManyChunksPacket(dimension, transferTo.getTeamId(), chunkPackets)
							.sendToAll(sourceStack.getServer(), transferTo);
				}
			});

			chunksToUnclaim.forEach((dimension, chunkPackets) -> {
				if (!chunkPackets.isEmpty()) {
					Server2PlayNetworking.sendToAllPlayers(sourceStack.getServer(), new SendManyChunksPacket(dimension, Util.NIL_UUID, chunkPackets));
				}
			});
		}

		FTBChunks.LOGGER.info("Transferred {} chunks from {} ({}) to {} ({})", transferred, transferFrom, fromName, transferTo, toName);
		FTBChunks.LOGGER.info("Unclaimed {} chunks for {} ({}) due to claim limits", unclaimed, transferFrom, fromName);
		FTBChunks.LOGGER.info("Team {} had {} claimed chunks, now has {}", toName, nChunks, nChunks + transferred);
	}

	public void onTeamOwnershipTransferred(PlayerTransferredOwnershipEvent.Data event) {
		ChunkTeamDataImpl data = ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.team());
		data.updateLimits();
	}

	public void onTeamPropertiesChanged(TeamPropertiesChangedEvent.@UnknownNullability Data event) {
		MinecraftServer server = ClaimedChunkManagerImpl.getInstance().getMinecraftServer();

        if (event.previousProperties().get(FTBChunksProperties.LOCATION_MODE) != event.team().getProperty(FTBChunksProperties.LOCATION_MODE)) {
			// team is showing or hiding player locations; sync visible player UUIDs to all players
			PlayerVisibilityPacket.syncToAll();
		}

		if (event.previousProperties().get(FTBChunksProperties.CLAIM_VISIBILITY) != event.team().getProperty(FTBChunksProperties.CLAIM_VISIBILITY)) {
			// team is showing or hiding claims; sync all their claims to all players
			ChunkTeamDataImpl teamData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.team());
			teamData.syncChunksToAll(server);
		}

		ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.team()).clearFakePlayerNameCache();
	}

	public void onPlayerAllianceChange(TeamAllyEvent.Data event) {
		// player(s) have become allied or unallied with a team; sync team's chunks to them (either to show or hide claims)
		ChunkTeamDataImpl teamData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.team());
		List<ServerPlayer> players = new ArrayList<>();
		event.players().forEach(profile -> {
			ServerPlayer p = ClaimedChunkManagerImpl.getInstance().getMinecraftServer().getPlayerList().getPlayer(profile.id());
			if (p != null) {
				teamData.syncChunksToPlayer(p);
				players.add(p);
			}
		});

		// also sync player head visibility; that might have changed too
		PlayerVisibilityPacket.syncToPlayers(players);
	}

	public void serverTickPost(MinecraftServer minecraftServer) {
		ClaimExpirationManager.INSTANCE.tick(minecraftServer);
		LongRangePlayerTracker.INSTANCE.tick(minecraftServer);
	}

	public static boolean isDevMode() {
		return Platform.get().isDev() || FTBChunksWorldConfig.DEV_COMMANDS.get();
	}

}
