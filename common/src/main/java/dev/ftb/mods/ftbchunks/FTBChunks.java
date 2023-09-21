package dev.ftb.mods.ftbchunks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.*;
import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.architectury.utils.value.IntValue;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.data.*;
import dev.ftb.mods.ftbchunks.net.*;
import dev.ftb.mods.ftblibrary.integration.stages.StageHelper;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.config.ConfigUtil;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.event.*;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FTBChunks {
	public static final String MOD_ID = "ftbchunks";
	public static final Logger LOGGER = LogManager.getLogger("FTB Chunks");
	public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setLenient().create();
	public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

	public static FTBChunks instance;

	public static final int TILES = 15;
	public static final int TILE_SIZE = 16;
	public static final int TILE_OFFSET = TILES / 2;
	public static final int MINIMAP_SIZE = TILE_SIZE * TILES;
	public static final XZ[] RELATIVE_SPIRAL_POSITIONS = new XZ[TILES * TILES];

	public static final Registrar<Block> BLOCK_REGISTRY = RegistrarManager.get(MOD_ID).get(Registries.BLOCK);

	public FTBChunks() {
		FTBChunksAPI._init(FTBChunksAPIImpl.INSTANCE);

		FTBChunksNet.init();

		for (int i = 0; i < RELATIVE_SPIRAL_POSITIONS.length; i++) {
			RELATIVE_SPIRAL_POSITIONS[i] = MathUtils.getSpiralPoint(i + 1);
		}

		LifecycleEvent.SERVER_BEFORE_START.register(this::serverBeforeStart);
		LifecycleEvent.SERVER_LEVEL_LOAD.register(this::serverLevelLoad);

		TeamManagerEvent.CREATED.register(this::teamManagerCreated);
		TeamManagerEvent.DESTROYED.register(this::teamManagerDestroyed);

		TeamEvent.PLAYER_LOGGED_IN.register(this::loggedIn);
		TeamEvent.CREATED.register(this::teamCreated);
		TeamEvent.LOADED.register(this::teamLoaded);
		TeamEvent.SAVED.register(this::teamSaved);
		TeamEvent.COLLECT_PROPERTIES.register(this::teamConfig);
		TeamEvent.PLAYER_JOINED_PARTY.register(this::playerJoinedParty);
		TeamEvent.PLAYER_LEFT_PARTY.register(this::playerLeftParty);
		TeamEvent.OWNERSHIP_TRANSFERRED.register(this::teamOwnershipTransferred);
		TeamEvent.PROPERTIES_CHANGED.register(this::teamPropertiesChanged);
		TeamEvent.ADD_ALLY.register(this::playerAllianceChange);
		TeamEvent.REMOVE_ALLY.register(this::playerAllianceChange);

		InteractionEvent.LEFT_CLICK_BLOCK.register(this::blockLeftClick);
		InteractionEvent.RIGHT_CLICK_BLOCK.register(this::blockRightClick);
		InteractionEvent.RIGHT_CLICK_ITEM.register(this::itemRightClick);
		InteractionEvent.INTERACT_ENTITY.register(this::interactEntity);
		InteractionEvent.FARMLAND_TRAMPLE.register(this::farmlandTrample);

		BlockEvent.BREAK.register(this::blockBreak);
		BlockEvent.PLACE.register(this::blockPlace);

		PlayerEvent.PLAYER_QUIT.register(this::loggedOut);
		PlayerEvent.FILL_BUCKET.register(this::fillBucket);
		PlayerEvent.PLAYER_CLONE.register(this::playerCloned);
		PlayerEvent.CHANGE_DIMENSION.register(this::playerChangedDimension);
		PlayerEvent.ATTACK_ENTITY.register(this::playerAttackEntity);

		EntityEvent.ENTER_SECTION.register(this::enterSection);
		EntityEvent.LIVING_CHECK_SPAWN.register(this::checkSpawn);
		EntityEvent.LIVING_HURT.register(this::onLivingHurt);

		ExplosionEvent.DETONATE.register(this::explosionDetonate);

		CommandRegistrationEvent.EVENT.register(FTBChunksCommands::registerCommands);

		TickEvent.SERVER_POST.register(this::serverTickPost);
		TickEvent.PLAYER_POST.register(this::playerTickPost);

		EnvExecutor.runInEnv(Env.CLIENT, () -> FTBChunksClient.INSTANCE::init);
	}

	private EventResult playerAttackEntity(Player player, Level level, Entity entity, InteractionHand interactionHand, @Nullable EntityHitResult entityHitResult) {
		// note: intentionally does not prevent attacking living entities;
		// this is for preventing griefing of entities like paintings & item frames
		if (player instanceof ServerPlayer) {
			if (!(entity instanceof LivingEntity) && ClaimedChunkManagerImpl.getInstance().shouldPreventInteraction(player, interactionHand, entity.blockPosition(), Protection.ATTACK_NONLIVING_ENTITY, entity)) {
				return EventResult.interruptFalse();
			}
		}

		return EventResult.pass();
	}

	private EventResult onLivingHurt(LivingEntity living, DamageSource damageSource, float dmg) {
		if (!living.level().isClientSide() && living instanceof Player target && damageSource.getEntity() instanceof Player attacker) {
			PvPMode mode = FTBChunksWorldConfig.PVP_MODE.get();
			if (mode == PvPMode.ALWAYS) {
				return EventResult.pass();
			}
			if (isPvPProtectedChunk(mode, attacker) || isPvPProtectedChunk(mode, target)) {
				PlayerNotifier.notifyWithCooldown(attacker, Component.translatable("ftbchunks.message.no_pvp").withStyle(ChatFormatting.GOLD), 3000L);
				return EventResult.interruptFalse();
			}
		}
		return EventResult.pass();
	}

	private boolean isPvPProtectedChunk(PvPMode mode, Player player) {
		ClaimedChunk cc = ClaimedChunkManagerImpl.getInstance().getChunk(new ChunkDimPos(player.level(), player.blockPosition()));
		return cc != null && (mode == PvPMode.NEVER || !cc.getTeamData().getTeam().getProperty(FTBChunksProperties.ALLOW_PVP));
	}

	private void playerTickPost(Player player) {
		if (player.level().isClientSide && player.level().getGameTime() % 20 == 0) {
			FTBChunksClient.INSTANCE.maybeClearDeathpoint(player);
		}
	}

	private void serverBeforeStart(MinecraftServer server) {
		var configPath = server.getWorldPath(ConfigUtil.SERVER_CONFIG_DIR);
		ConfigUtil.loadDefaulted(FTBChunksWorldConfig.CONFIG, configPath, FTBChunks.MOD_ID);
	}

	private void serverLevelLoad(ServerLevel level) {
		if (ClaimedChunkManagerImpl.getInstance() != null) {
			ClaimedChunkManagerImpl.getInstance().initForceLoadedChunks(level);
		} else {
			FTBChunks.LOGGER.warn("Level " + level.dimension().location() + " loaded before FTB Chunks manager was initialized! Unable to force-load chunks");
		}
	}

	private void teamManagerCreated(TeamManagerEvent event) {
		ClaimedChunkManagerImpl.init(event.getManager());
	}

	private void teamManagerDestroyed(TeamManagerEvent event) {
		ClaimedChunkManagerImpl.shutdown();
	}

	private void loggedIn(PlayerLoggedInAfterTeamEvent event) {
		ServerPlayer player = event.getPlayer();
		ChunkTeamDataImpl data = ClaimedChunkManagerImpl.getInstance().getOrCreateData(player);
		data.updateLimits();

		String playerId = event.getPlayer().getUUID().toString();
		FTBChunks.LOGGER.debug("handling player team login: player = {}, team = {}",
				playerId, data.getTeamId());

		SNBTCompoundTag config = new SNBTCompoundTag();
		FTBChunksWorldConfig.CONFIG.write(config);
		UUID managerId = FTBTeamsAPI.api().getManager().getId();
		new LoginDataPacket(managerId, config).sendTo(player);
		SendGeneralDataPacket.send(data, player);
		FTBChunks.LOGGER.debug("server config and team data sent to {}", playerId);

		long now = System.currentTimeMillis();
		Map<Pair<ResourceKey<Level>, UUID>, List<SendChunkPacket.SingleChunk>> chunksToSend = new HashMap<>();

		for (ClaimedChunkImpl chunk : ClaimedChunkManagerImpl.getInstance().getAllClaimedChunks()) {
			chunksToSend.computeIfAbsent(Pair.of(chunk.getPos().dimension(), chunk.getTeamData().getTeamId()), s -> new ArrayList<>())
					.add(new SendChunkPacket.SingleChunk(now, chunk.getPos().x(), chunk.getPos().z(), chunk));
		}

		chunksToSend.forEach((dimensionAndId, chunkPackets) -> {
			FTBTeamsAPI.api().getManager().getTeamByID(dimensionAndId.getRight()).ifPresent(team -> {
				ChunkTeamDataImpl teamData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(team);
				if (teamData.canPlayerUse(player, FTBChunksProperties.CLAIM_VISIBILITY)) {
					SendManyChunksPacket packet = new SendManyChunksPacket(dimensionAndId.getLeft(), dimensionAndId.getRight(), chunkPackets);
					packet.sendTo(player);
				}
			});
		});
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

	public void loggedOut(ServerPlayer player) {
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
			FTBChunks.LOGGER.warn("on player disconnect: player '{}' has no team?", player.getGameProfile().getName());
		}
	}

	private void teamCreated(TeamCreatedEvent teamEvent) {
		ClaimedChunkManagerImpl.getInstance().getOrCreateData(teamEvent.getTeam());
	}

	private void teamLoaded(TeamEvent teamEvent) {
		ClaimedChunkManagerImpl.getInstance().getOrCreateData(teamEvent.getTeam());
	}

	private void teamSaved(TeamEvent teamEvent) {
		ClaimedChunkManagerImpl.getInstance().getOrCreateData(teamEvent.getTeam()).saveNow();
	}

	public EventResult blockLeftClick(Player player, InteractionHand hand, BlockPos pos, Direction face) {
		// calling architectury stub method
		//noinspection ConstantConditions
		if (player instanceof ServerPlayer && ClaimedChunkManagerImpl.getInstance().shouldPreventInteraction(player, hand, pos, FTBChunksExpected.getBlockBreakProtection(), null)) {
			return EventResult.interruptFalse();
		}

		return EventResult.pass();
	}

	public EventResult blockRightClick(Player player, InteractionHand hand, BlockPos pos, Direction face) {
		// calling architectury stub method
		//noinspection ConstantConditions
		if (player instanceof ServerPlayer sp && ClaimedChunkManagerImpl.getInstance().shouldPreventInteraction(player, hand, pos, FTBChunksExpected.getBlockInteractProtection(), null)) {
			FTBCUtils.forceHeldItemSync(sp, hand);
			return EventResult.interruptFalse();
		}

		return EventResult.pass();
	}

	public CompoundEventResult<ItemStack> itemRightClick(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer sp && ClaimedChunkManagerImpl.getInstance().shouldPreventInteraction(player, hand, BlockPos.containing(player.getEyePosition(1F)), Protection.RIGHT_CLICK_ITEM, null)) {
			FTBCUtils.forceHeldItemSync(sp, hand);
			return CompoundEventResult.interruptFalse(player.getItemInHand(hand));
		}

		return CompoundEventResult.pass();
	}


	private EventResult interactEntity(Player player, Entity entity, InteractionHand hand) {
		if (player instanceof ServerPlayer && ClaimedChunkManagerImpl.getInstance().shouldPreventInteraction(player, hand, entity.blockPosition(), Protection.INTERACT_ENTITY, entity)) {
			return EventResult.interruptFalse();
		}

		return EventResult.pass();
	}

	public EventResult blockBreak(Level level, BlockPos pos, BlockState blockState, ServerPlayer player, @Nullable IntValue intValue) {
		if (ClaimedChunkManagerImpl.getInstance().shouldPreventInteraction(player, InteractionHand.MAIN_HAND, pos, FTBChunksExpected.getBlockBreakProtection(), null)) {
			return EventResult.interruptFalse();
		}

		return EventResult.pass();
	}

	public EventResult blockPlace(Level level, BlockPos pos, BlockState blockState, @Nullable Entity entity) {
		// calling architectury stub method
		//noinspection ConstantConditions
		if (entity instanceof ServerPlayer sp && ClaimedChunkManagerImpl.getInstance().shouldPreventInteraction(entity, InteractionHand.MAIN_HAND, pos, FTBChunksExpected.getBlockPlaceProtection(), null)) {
			FTBCUtils.forceHeldItemSync(sp, InteractionHand.MAIN_HAND);
			return EventResult.interruptFalse();
		}

		return EventResult.pass();
	}

	public CompoundEventResult<ItemStack> fillBucket(Player player, Level level, ItemStack emptyBucket, @Nullable HitResult target) {
		if (player instanceof ServerPlayer && target instanceof BlockHitResult hitResult) {
			InteractionHand hand = player.getUsedItemHand();
			if (ClaimedChunkManagerImpl.getInstance().shouldPreventInteraction(player, hand, hitResult.getBlockPos(), Protection.EDIT_FLUID, null)) {
				return CompoundEventResult.interrupt(false, player.getItemInHand(hand));
			}
		}

		return CompoundEventResult.pass();
	}

	public EventResult farmlandTrample(Level world, BlockPos pos, BlockState blockState, float distance, Entity entity) {
		if (entity instanceof ServerPlayer && ClaimedChunkManagerImpl.getInstance().shouldPreventInteraction(entity, InteractionHand.MAIN_HAND, pos, Protection.EDIT_BLOCK, null)) {
			return EventResult.interrupt(false);
		}

		return EventResult.pass();
	}

	// This event is a nightmare, gets fired before login
	public void enterSection(Entity entity, int chunkX, int chunkY, int chunkZ, int prevX, int prevY, int prevZ) {
		if (chunkX == prevX && chunkZ == prevZ && chunkY != prevY
				|| !(entity instanceof ServerPlayer player) || PlayerHooks.isFake(player)
				|| !FTBTeamsAPI.api().isManagerLoaded() || !FTBChunksAPI.api().isManagerLoaded()) {
			return;
		}

		FTBTeamsAPI.api().getManager().getTeamForPlayerID(player.getUUID()).ifPresent(team -> {
			ChunkTeamDataImpl data = ClaimedChunkManagerImpl.getInstance().getOrCreateData(team);
			data.checkForChunkChange(player, chunkX, chunkZ);
		});
	}

	public EventResult checkSpawn(LivingEntity entity, LevelAccessor levelAccessor, double x, double y, double z, MobSpawnType type, @Nullable BaseSpawner spawner) {
		if (!levelAccessor.isClientSide() && !(entity instanceof Player) && levelAccessor instanceof Level level) {
			switch (type) {
				case NATURAL, CHUNK_GENERATION, SPAWNER, STRUCTURE, JOCKEY, PATROL -> {
					ClaimedChunkImpl chunk = ClaimedChunkManagerImpl.getInstance().getChunk(new ChunkDimPos(level, BlockPos.containing(x, y, z)));
					if (chunk != null && !chunk.canEntitySpawn(entity)) {
						return EventResult.interrupt(false);
					}
				}
			}
		}

		return EventResult.pass();
	}

	private boolean ignoreExplosion(Level level, Explosion explosion) {
		if (level.isClientSide() || explosion.getToBlow().isEmpty()) {
			return true;
		}

		return explosion.source == null && !FTBChunksWorldConfig.PROTECT_UNKNOWN_EXPLOSIONS.get();
	}

	public void explosionDetonate(Level level, Explosion explosion, List<Entity> affectedEntities) {
		if (ignoreExplosion(level, explosion)) {
			return;
		}

		List<BlockPos> list = new ArrayList<>(explosion.getToBlow());
		explosion.clearToBlow();
		Map<ChunkDimPos, Boolean> map = new HashMap<>();

		for (BlockPos pos : list) {
			if (map.computeIfAbsent(new ChunkDimPos(level, pos), cpos -> {
				ClaimedChunkImpl chunk = ClaimedChunkManagerImpl.getInstance().getChunk(cpos);
				return chunk == null || chunk.allowExplosions();
			})) {
				explosion.getToBlow().add(pos);
			}
		}
	}

	private void playerCloned(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wonGame) {
		// this is better than checking for living death event, because player cloning isn't cancellable
		// the player death event is cancellable, and we can't detect cancelled events with Architectury
		if (!wonGame) {
			newPlayer.getLastDeathLocation().ifPresent(loc -> {
				int num = newPlayer.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
				new PlayerDeathPacket(loc, num).sendTo(newPlayer);
			});
		}
	}

	private void playerChangedDimension(ServerPlayer serverPlayer, ResourceKey<Level> oldLevel, ResourceKey<Level> newLevel) {
		LongRangePlayerTracker.INSTANCE.stopTracking(serverPlayer);

		StageHelper.INSTANCE.getProvider().sync(serverPlayer);
	}

	private void teamConfig(TeamCollectPropertiesEvent event) {
		event.add(FTBChunksProperties.ALLOW_EXPLOSIONS);
		event.add(FTBChunksProperties.ALLOW_MOB_GRIEFING);
		event.add(FTBChunksProperties.ALLOW_ALL_FAKE_PLAYERS);
		event.add(FTBChunksProperties.ALLOW_NAMED_FAKE_PLAYERS);
		event.add(FTBChunksProperties.ALLOW_FAKE_PLAYERS_BY_ID);
		event.add(FTBChunksProperties.ALLOW_PVP);

		// block edit/interact properties vary on forge & fabric
		FTBChunksExpected.getPlatformSpecificProperties(event);

		event.add(FTBChunksProperties.ENTITY_INTERACT_MODE);
		event.add(FTBChunksProperties.NONLIVING_ENTITY_ATTACK_MODE);
		event.add(FTBChunksProperties.CLAIM_VISIBILITY);
		event.add(FTBChunksProperties.LOCATION_MODE);

		// event.add(FTBChunksTeamData.MINIMAP_MODE);
	}

	private void playerJoinedParty(PlayerJoinedPartyTeamEvent event) {
		ChunkTeamDataImpl playerData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.getPreviousTeam());
		ChunkTeamDataImpl partyData  = ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.getTeam());

		partyData.addMemberData(event.getPlayer(), playerData);
		partyData.updateLimits();

		// keep a note of the claims the player had - if/when they leave the party, they get those claims back
		// prevents malicious party stealing a player's claims by inviting and then kicking the player
		transferClaims(playerData, partyData, playerData.getClaimedChunks());

		partyData.setForceLoadMember(event.getPlayer().getUUID(), FTBChunksWorldConfig.canPlayerOfflineForceload(event.getPlayer()));

		PlayerVisibilityPacket.syncToLevel(event.getPlayer().level());
		partyData.syncChunksToPlayer(event.getPlayer());
	}

	private void playerLeftParty(PlayerLeftPartyTeamEvent event) {
		ChunkTeamDataImpl partyData  = ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.getTeam());

		FTBTeamsAPI.api().getManager().getPlayerTeamForPlayerID(event.getPlayerId()).ifPresent(personalTeam -> {
			ChunkTeamDataImpl playerData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(personalTeam);
			if (event.getTeamDeleted()) {
				// last player leaving the party; transfer any remaining claims the party had back to that player, if possible
				transferClaims(partyData, playerData, partyData.getClaimedChunks());
				// and purge party team data from manager & disk
				ClaimedChunkManagerImpl.getInstance().deleteTeam(event.getTeam());
			} else {
				// return the departing player's original claims to them, if possible
				transferClaims(partyData, playerData, partyData.getOriginalClaims(event.getPlayerId()));
			}
		});

		partyData.deleteMemberData(event.getPlayerId());

		partyData.updateLimits();

		if (event.getPlayer() != null) {
			PlayerVisibilityPacket.syncToLevel(event.getPlayer().level());
			partyData.syncChunksToPlayer(event.getPlayer());
		}
	}

	private void transferClaims(ChunkTeamDataImpl transferFrom, ChunkTeamDataImpl transferTo, Collection<ClaimedChunkImpl> chunksToTransfer) {
		CommandSourceStack sourceStack = ClaimedChunkManagerImpl.getInstance().getMinecraftServer().createCommandSourceStack();

		Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> chunksToSend = new HashMap<>();
		Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> chunksToUnclaim = new HashMap<>();
		int chunks = 0;
		long now = System.currentTimeMillis();
		int total = transferTo.getClaimedChunks().size();

		for (ClaimedChunkImpl chunk : chunksToTransfer) {
			ChunkDimPos cdp = chunk.getPos();
			if (total >= transferTo.getMaxClaimChunks()) {
				chunk.unclaim(sourceStack, false);
				chunksToUnclaim.computeIfAbsent(cdp.dimension(), s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, cdp.x(), cdp.z(), null));
			} else {
				chunk.setTeamData(transferTo);
				chunksToSend.computeIfAbsent(cdp.dimension(), s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, cdp.x(), cdp.z(), chunk));
				chunks++;
			}

			if (chunk.isForceLoaded()) {
				// also transfer any claim tickets for the old team's ID, since it's no longer valid
				ServerLevel level = ClaimedChunkManagerImpl.getInstance().getMinecraftServer().getLevel(cdp.dimension());
				if (level != null) {
					FTBChunksExpected.addChunkToForceLoaded(level, FTBChunks.MOD_ID, transferFrom.getTeamId(), cdp.x(), cdp.z(), false);
					if (chunk.isActuallyForceLoaded()) {
						FTBChunksExpected.addChunkToForceLoaded(level, FTBChunks.MOD_ID, transferTo.getTeamId(), cdp.x(), cdp.z(), true);
					}
				}
			}

			total++;
		}

		transferFrom.markDirty();
		transferTo.markDirty();

		if (chunks > 0) {
			chunksToSend.forEach((dimension, chunkPackets) -> {
				if (!chunkPackets.isEmpty()) {
					ChunkSendingUtils.sendManyChunksToAll(sourceStack.getServer(), transferTo, new SendManyChunksPacket(dimension, transferTo.getTeamId(), chunkPackets));
				}
			});

			chunksToUnclaim.forEach((dimension, chunkPackets) -> {
				if (!chunkPackets.isEmpty()) {
					new SendManyChunksPacket(dimension, Util.NIL_UUID, chunkPackets).sendToAll(sourceStack.getServer());
				}
			});

			FTBChunks.LOGGER.info("Transferred " + chunks + "/" + total + " chunks from " + transferFrom + " to " + transferTo);
		}
	}

	private void teamOwnershipTransferred(PlayerTransferredTeamOwnershipEvent event) {
		ChunkTeamDataImpl data = ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.getTeam());
		data.updateLimits();
	}

	private void teamPropertiesChanged(TeamPropertiesChangedEvent event) {
		MinecraftServer server = ClaimedChunkManagerImpl.getInstance().getMinecraftServer();
		if (server == null) return;

		if (event.getPreviousProperties().get(FTBChunksProperties.LOCATION_MODE) != event.getTeam().getProperty(FTBChunksProperties.LOCATION_MODE)) {
			// team is showing or hiding player locations; sync visible player UUIDs to all players
			PlayerVisibilityPacket.syncToAll();
		}

		if (event.getPreviousProperties().get(FTBChunksProperties.CLAIM_VISIBILITY) != event.getTeam().getProperty(FTBChunksProperties.CLAIM_VISIBILITY)) {
			// team is showing or hiding claims; sync all their claims to all players
			ChunkTeamDataImpl teamData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.getTeam());
			teamData.syncChunksToAll(server);
		}

		ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.getTeam()).clearFakePlayerNameCache();
	}

	private void playerAllianceChange(TeamAllyEvent event) {
		// player(s) have become allied or unallied with a team; sync team's chunks to them (either to show or hide claims)
		ChunkTeamDataImpl teamData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(event.getTeam());
		List<ServerPlayer> players = new ArrayList<>();
		event.getPlayers().forEach(profile -> {
			ServerPlayer p = ClaimedChunkManagerImpl.getInstance().getMinecraftServer().getPlayerList().getPlayer(profile.getId());
			if (p != null) {
				teamData.syncChunksToPlayer(p);
				players.add(p);
			}
		});

		// also sync player head visibility; that might have changed too
		PlayerVisibilityPacket.syncToPlayers(players);
	}

	private void serverTickPost(MinecraftServer minecraftServer) {
		ClaimExpirationManager.INSTANCE.tick(minecraftServer);
		LongRangePlayerTracker.INSTANCE.tick(minecraftServer);
	}
}
