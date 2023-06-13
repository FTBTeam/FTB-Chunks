package dev.ftb.mods.ftbchunks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.*;
import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.Registries;
import dev.architectury.utils.EnvExecutor;
import dev.architectury.utils.value.IntValue;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.data.*;
import dev.ftb.mods.ftbchunks.integration.ftbranks.FTBRanksIntegration;
import dev.ftb.mods.ftbchunks.integration.stages.StageHelper;
import dev.ftb.mods.ftbchunks.integration.waystones.WaystonesCommon;
import dev.ftb.mods.ftbchunks.net.*;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.config.ConfigUtil;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.event.*;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
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

/**
 * @author LatvianModder
 */
public class FTBChunks {
	public static final String MOD_ID = "ftbchunks";
	public static final Logger LOGGER = LogManager.getLogger("FTB Chunks");
	public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setLenient().create();
	public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

	public static FTBChunks instance;
	public static FTBChunksCommon PROXY;

	public static final int TILES = 15;
	public static final int TILE_SIZE = 16;
	public static final int TILE_OFFSET = TILES / 2;
	public static final int MINIMAP_SIZE = TILE_SIZE * TILES;
	public static final XZ[] RELATIVE_SPIRAL_POSITIONS = new XZ[TILES * TILES];

	public static final Registrar<Block> BLOCK_REGISTRY = Registries.get(MOD_ID).get(Registry.BLOCK_REGISTRY);

	public FTBChunks() {
		PROXY = EnvExecutor.getEnvSpecific(() -> FTBChunksClient::new, () -> FTBChunksCommon::new);
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

		ExplosionEvent.DETONATE.register(this::explosionDetonate);

		CommandRegistrationEvent.EVENT.register(FTBChunksCommands::registerCommands);

		TickEvent.SERVER_POST.register(this::serverTickPost);
		TickEvent.PLAYER_POST.register(this::playerTickPost);

		if (Platform.isModLoaded("ftbranks")) {
			FTBRanksIntegration.registerEvents();
		}
		if (Platform.isModLoaded("waystones")) {
			WaystonesCommon.init();
		}

		PROXY.init();
	}

	private EventResult playerAttackEntity(Player player, Level level, Entity entity, InteractionHand interactionHand, @Nullable EntityHitResult entityHitResult) {
		// note: intentionally does not prevent attacking living entities;
		// this is for preventing griefing of entities like paintings & item frames
		if (player instanceof ServerPlayer && !(entity instanceof LivingEntity) && FTBChunksAPI.getManager().protect(player, interactionHand, entity.blockPosition(), Protection.ATTACK_NONLIVING_ENTITY, entity)) {
			return EventResult.interruptFalse();
		}

		return EventResult.pass();
	}

	private void playerTickPost(Player player) {
		if (player.level.isClientSide && player.level.getGameTime() % 20 == 0) {
			FTBChunks.PROXY.maybeClearDeathpoint(player);
		}
	}

	private void serverBeforeStart(MinecraftServer server) {
		var configPath = server.getWorldPath(ConfigUtil.SERVER_CONFIG_DIR);
		ConfigUtil.loadDefaulted(FTBChunksWorldConfig.CONFIG, configPath, FTBChunks.MOD_ID);
	}

	private void serverLevelLoad(ServerLevel level) {
		if (FTBChunksAPI.manager != null) {
			FTBChunksAPI.manager.initForceLoadedChunks(level);
		} else {
			FTBChunks.LOGGER.warn("Level " + level.dimension().location() + " loaded before FTB Chunks manager was initialized! Unable to force-load chunks");
		}
	}

	private void teamManagerCreated(TeamManagerEvent event) {
		FTBChunksAPI.manager = new ClaimedChunkManager(event.getManager());
	}

	private void teamManagerDestroyed(TeamManagerEvent event) {
		FTBChunksAPI.manager = null;
	}

	private void loggedIn(PlayerLoggedInAfterTeamEvent event) {
		ServerPlayer player = event.getPlayer();
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(player);
		data.updateLimits();

		String playerId = event.getPlayer().getUUID().toString();
		FTBChunks.LOGGER.debug("handling player team login: player = {}, team = {}",
				playerId, data.getTeamId());

		SNBTCompoundTag config = new SNBTCompoundTag();
		FTBChunksWorldConfig.CONFIG.write(config);
		new LoginDataPacket(event.getTeam().manager.getId(), config).sendTo(player);
		SendGeneralDataPacket.send(data, player);
		FTBChunks.LOGGER.debug("server config and team data sent to {}", playerId);

		long now = System.currentTimeMillis();
		Map<Pair<ResourceKey<Level>, UUID>, List<SendChunkPacket.SingleChunk>> chunksToSend = new HashMap<>();

		for (ClaimedChunk chunk : FTBChunksAPI.getManager().getAllClaimedChunks()) {
			chunksToSend.computeIfAbsent(Pair.of(chunk.pos.dimension, chunk.teamData.getTeamId()), s -> new ArrayList<>())
					.add(new SendChunkPacket.SingleChunk(now, chunk.pos.x, chunk.pos.z, chunk));
		}

		chunksToSend.forEach((dimensionAndId, chunkPackets) -> {
			Team team = FTBTeamsAPI.getManager().getTeamByID(dimensionAndId.getRight());
			FTBChunksTeamData teamData = FTBChunksAPI.getManager().getData(team);
			if (teamData.canUse(player, FTBChunksTeamData.CLAIM_VISIBILITY)) {
				SendManyChunksPacket packet = new SendManyChunksPacket(dimensionAndId.getLeft(), dimensionAndId.getRight(), chunkPackets);
				packet.sendTo(player);
			}
		});
		FTBChunks.LOGGER.debug("claimed chunk data sent to {}", playerId);

		data.setLastLoginTime(now);

		data.setForceLoadMember(player.getUUID(), FTBChunksWorldConfig.canPlayerOfflineForceload(player));

		if (data.getTeam().getOnlineMembers().size() == 1 && !data.canForceLoadChunks()) {
			// first player on the team to log in; force chunks if the team can't do offline chunk-loading
			data.updateChunkTickets(true);
		}

		SendVisiblePlayerListPacket.syncToLevel(player.level);
		FTBChunks.LOGGER.debug("visible player list sent to {}", playerId);
	}

	public void loggedOut(ServerPlayer player) {
		if (!FTBTeamsAPI.isManagerLoaded() || !FTBChunksAPI.isManagerLoaded() || !FTBChunksAPI.getManager().hasData(player)) {
			return;
		}

		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(player);
		data.setForceLoadMember(player.getUUID(), FTBChunksWorldConfig.canPlayerOfflineForceload(player));
		FTBChunksAPI.getManager().clearForceLoadedCache();
		LongRangePlayerTracker.INSTANCE.stopTracking(player);

		if (data.getTeam().getOnlineMembers().size() == 1 && !data.canForceLoadChunks()) {
			// last player on the team to log out; unforce chunks if the team can't do offline chunk-loading
			data.updateChunkTickets(false);
		}
	}

	private void teamCreated(TeamCreatedEvent teamEvent) {
		FTBChunksAPI.manager.getData(teamEvent.getTeam());
	}

	private void teamLoaded(TeamEvent teamEvent) {
		FTBChunksAPI.manager.getData(teamEvent.getTeam());
	}

	private void teamSaved(TeamEvent teamEvent) {
		FTBChunksAPI.manager.getData(teamEvent.getTeam()).saveNow();
	}

	public EventResult blockLeftClick(Player player, InteractionHand hand, BlockPos pos, Direction face) {
		// calling architectury stub method
		//noinspection ConstantConditions
		if (player instanceof ServerPlayer && FTBChunksAPI.getManager().protect(player, hand, pos, FTBChunksExpected.getBlockBreakProtection(), null)) {
			return EventResult.interruptFalse();
		}

		return EventResult.pass();
	}

	public EventResult blockRightClick(Player player, InteractionHand hand, BlockPos pos, Direction face) {
		// calling architectury stub method
		//noinspection ConstantConditions
		if (player instanceof ServerPlayer sp && FTBChunksAPI.getManager().protect(player, hand, pos, FTBChunksExpected.getBlockInteractProtection(), null)) {
			FTBCUtils.forceHeldItemSync(sp, hand);
			return EventResult.interruptFalse();
		}

		return EventResult.pass();
	}

	public CompoundEventResult<ItemStack> itemRightClick(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer sp && FTBChunksAPI.getManager().protect(player, hand, new BlockPos(player.getEyePosition(1F)), Protection.RIGHT_CLICK_ITEM, null)) {
			FTBCUtils.forceHeldItemSync(sp, hand);
			return CompoundEventResult.interruptFalse(player.getItemInHand(hand));
		}

		return CompoundEventResult.pass();
	}


	private EventResult interactEntity(Player player, Entity entity, InteractionHand hand) {
		if (player instanceof ServerPlayer && FTBChunksAPI.getManager().protect(player, hand, entity.blockPosition(), Protection.INTERACT_ENTITY, entity)) {
			return EventResult.interruptFalse();
		}

		return EventResult.pass();
	}

	public EventResult blockBreak(Level level, BlockPos pos, BlockState blockState, ServerPlayer player, @Nullable IntValue intValue) {
		if (FTBChunksAPI.getManager().protect(player, InteractionHand.MAIN_HAND, pos, FTBChunksExpected.getBlockBreakProtection(), null)) {
			return EventResult.interruptFalse();
		}

		return EventResult.pass();
	}

	public EventResult blockPlace(Level level, BlockPos pos, BlockState blockState, @Nullable Entity entity) {
		// calling architectury stub method
		//noinspection ConstantConditions
		if (entity instanceof ServerPlayer sp && FTBChunksAPI.getManager().protect(entity, InteractionHand.MAIN_HAND, pos, FTBChunksExpected.getBlockPlaceProtection(), null)) {
			FTBCUtils.forceHeldItemSync(sp, InteractionHand.MAIN_HAND);
			return EventResult.interruptFalse();
		}

		return EventResult.pass();
	}

	public CompoundEventResult<ItemStack> fillBucket(Player player, Level level, ItemStack emptyBucket, @Nullable HitResult target) {
		if (player instanceof ServerPlayer && target instanceof BlockHitResult hitResult) {
			InteractionHand hand = player.getUsedItemHand();
			if (FTBChunksAPI.getManager().protect(player, hand, hitResult.getBlockPos(), Protection.EDIT_FLUID, null)) {
				return CompoundEventResult.interrupt(false, player.getItemInHand(hand));
			}
		}

		return CompoundEventResult.pass();
	}

	public EventResult farmlandTrample(Level world, BlockPos pos, BlockState blockState, float distance, Entity entity) {
		if (entity instanceof ServerPlayer && FTBChunksAPI.getManager().protect(entity, InteractionHand.MAIN_HAND, pos, Protection.EDIT_BLOCK, null)) {
			return EventResult.interrupt(false);
		}

		return EventResult.pass();
	}

	// This event is a nightmare, gets fired before login
	public void enterSection(Entity entity, int chunkX, int chunkY, int chunkZ, int prevX, int prevY, int prevZ) {
		if (chunkX == prevX && chunkZ == prevZ && chunkY != prevY) {
			return;
		}

		if (!(entity instanceof ServerPlayer player) || PlayerHooks.isFake((ServerPlayer) entity)) {
			return;
		}

		if (!FTBTeamsAPI.isManagerLoaded() || !FTBChunksAPI.isManagerLoaded()) {
			return;
		}

		Team team = FTBTeamsAPI.getPlayerTeam(player.getUUID());

		if (team == null) {
			return;
		}

		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(team);

		if (data.prevChunkX != chunkX || data.prevChunkZ != chunkZ) {
			ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(new ChunkDimPos(player));
			String s = chunk == null ? "-" : chunk.getTeamData().getTeamId().toString();

			if (!data.lastChunkID.equals(s)) {
				data.lastChunkID = s;

				if (chunk != null) {
					player.displayClientMessage(chunk.getTeamData().getTeam().getColoredName(), true);
				} else {
					player.displayClientMessage(Component.translatable("wilderness").withStyle(ChatFormatting.DARK_GREEN), true);
				}
			}

			data.prevChunkX = chunkX;
			data.prevChunkZ = chunkZ;
		}
	}

	public EventResult checkSpawn(LivingEntity entity, LevelAccessor level, double x, double y, double z, MobSpawnType type, @Nullable BaseSpawner spawner) {
		if (!level.isClientSide() && !(entity instanceof Player) && level instanceof Level) {
			switch (type) {
				case NATURAL, CHUNK_GENERATION, SPAWNER, STRUCTURE, JOCKEY, PATROL -> {
					ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(new ChunkDimPos((Level) level, new BlockPos(x, y, z)));

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
				ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(cpos);
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
				ResourceKey<Level> dim = loc.dimension();
				int x = loc.pos().getX();
				int y = loc.pos().getY();
				int z = loc.pos().getZ();
				int num = newPlayer.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
				new PlayerDeathPacket(dim, x, y, z, num).sendTo(newPlayer);
			});
		}
	}

	private void playerChangedDimension(ServerPlayer serverPlayer, ResourceKey<Level> oldLevel, ResourceKey<Level> newLevel) {
		LongRangePlayerTracker.INSTANCE.stopTracking(serverPlayer);

		StageHelper.INSTANCE.get().sync(serverPlayer);
	}

	private void teamConfig(TeamCollectPropertiesEvent event) {
		event.add(FTBChunksTeamData.ALLOW_EXPLOSIONS);
		event.add(FTBChunksTeamData.ALLOW_MOB_GRIEFING);
		event.add(FTBChunksTeamData.ALLOW_ALL_FAKE_PLAYERS);
		event.add(FTBChunksTeamData.ALLOW_NAMED_FAKE_PLAYERS);
		event.add(FTBChunksTeamData.ALLOW_FAKE_PLAYERS_BY_ID);

		// block edit/interact properties vary on forge & fabric
		FTBChunksExpected.getPlatformSpecificProperties(event);

		event.add(FTBChunksTeamData.ENTITY_INTERACT_MODE);
		event.add(FTBChunksTeamData.NONLIVING_ENTITY_ATTACK_MODE);
		event.add(FTBChunksTeamData.CLAIM_VISIBILITY);
		event.add(FTBChunksTeamData.LOCATION_MODE);

		// event.add(FTBChunksTeamData.MINIMAP_MODE);
	}

	private void playerJoinedParty(PlayerJoinedPartyTeamEvent event) {
		FTBChunksTeamData playerData = FTBChunksAPI.getManager().getData(event.getPreviousTeam());
		FTBChunksTeamData partyData  = FTBChunksAPI.getManager().getData(event.getTeam());

		partyData.addMemberData(event.getPlayer(), playerData);
		partyData.updateLimits();

		// keep a note of the claims the player had - if/when they leave the party, they get those claims back
		// prevents malicious party stealing a player's claims by inviting and then kicking the player
		transferClaims(playerData, partyData, playerData.getClaimedChunks());

		partyData.setForceLoadMember(event.getPlayer().getUUID(), FTBChunksWorldConfig.canPlayerOfflineForceload(event.getPlayer()));

		SendVisiblePlayerListPacket.syncToLevel(event.getPlayer().level);
		partyData.syncChunksToPlayer(event.getPlayer());
	}

	private void playerLeftParty(PlayerLeftPartyTeamEvent event) {
		FTBChunksTeamData partyData  = FTBChunksAPI.getManager().getData(event.getTeam());
		Team personalTeam = FTBTeamsAPI.getManager().getInternalPlayerTeam(event.getPlayerId());

		if (personalTeam != null) {
			FTBChunksTeamData playerData = FTBChunksAPI.getManager().getData(personalTeam);
			if (event.getTeamDeleted()) {
				// last player leaving the party; transfer any remaining claims the party had back to that player, if possible
				transferClaims(partyData, playerData, partyData.getClaimedChunks());
				// and purge party team data from manager & disk
				FTBChunksAPI.getManager().deleteTeam(event.getTeam());
			} else {
				// return the departing player's original claims to them, if possible
				transferClaims(partyData, playerData, partyData.getOriginalClaims(event.getPlayerId()));
			}
		}

		partyData.deleteMemberData(event.getPlayerId());

		partyData.updateLimits();

		if (event.getPlayer() != null) {
			SendVisiblePlayerListPacket.syncToLevel(event.getPlayer().level);
			partyData.syncChunksToPlayer(event.getPlayer());
		}
	}

	private void transferClaims(FTBChunksTeamData transferFrom, FTBChunksTeamData transferTo, Collection<ClaimedChunk> chunksToTransfer) {
		CommandSourceStack sourceStack = FTBTeamsAPI.getManager().server.createCommandSourceStack();

		Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> chunksToSend = new HashMap<>();
		Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> chunksToUnclaim = new HashMap<>();
		int chunks = 0;
		long now = System.currentTimeMillis();
		int total = transferTo.getClaimedChunks().size();

		for (ClaimedChunk chunk : chunksToTransfer) {
			if (total >= transferTo.getMaxClaimChunks()) {
				chunk.unclaim(sourceStack, false);
				chunksToUnclaim.computeIfAbsent(chunk.pos.dimension, s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, chunk.pos.x, chunk.pos.z, null));
			} else {
				chunk.teamData = transferTo;
				chunksToSend.computeIfAbsent(chunk.pos.dimension, s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, chunk.pos.x, chunk.pos.z, chunk));
				chunks++;
			}

			if (chunk.isForceLoaded()) {
				// also transfer any claim tickets for the old team's ID, since it's no longer valid
				ServerLevel level = FTBChunksAPI.getManager().getMinecraftServer().getLevel(chunk.pos.dimension);
				if (level != null) {
					FTBChunksExpected.addChunkToForceLoaded(level, FTBChunks.MOD_ID, transferFrom.getTeamId(), chunk.pos.x, chunk.pos.z, false);
					if (chunk.isActuallyForceLoaded()) {
						FTBChunksExpected.addChunkToForceLoaded(level, FTBChunks.MOD_ID, transferTo.getTeamId(), chunk.pos.x, chunk.pos.z, true);
					}
				}
			}

			total++;
		}

		transferFrom.save();
		transferTo.save();

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
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(event.getTeam());
		data.updateLimits();
	}

	private void teamPropertiesChanged(TeamPropertiesChangedEvent event) {
		MinecraftServer server = FTBChunksAPI.getManager().getMinecraftServer();
		if (server == null) return;

		if (event.getOldProperties().get(FTBChunksTeamData.LOCATION_MODE) != event.getTeam().getProperty(FTBChunksTeamData.LOCATION_MODE)) {
			// team is showing or hiding player locations; sync visible player UUIDs to all players
			SendVisiblePlayerListPacket.syncToAll();
		}

		if (event.getOldProperties().get(FTBChunksTeamData.CLAIM_VISIBILITY) != event.getTeam().getProperty(FTBChunksTeamData.CLAIM_VISIBILITY)) {
			// team is showing or hiding claims; sync all their claims to all players
			FTBChunksTeamData teamData = FTBChunksAPI.getManager().getData(event.getTeam());
			teamData.syncChunksToAll(server);
		}

		FTBChunksAPI.getManager().getData(event.getTeam()).clearFakePlayerNameCache();
	}

	private void playerAllianceChange(TeamAllyEvent event) {
		// player(s) have become allied or unallied with a team; sync team's chunks to them (either to show or hide claims)
		FTBChunksTeamData teamData = FTBChunksAPI.getManager().getData(event.getTeam());
		List<ServerPlayer> players = new ArrayList<>();
		event.getPlayers().forEach(profile -> {
			ServerPlayer p = FTBChunksAPI.getManager().getMinecraftServer().getPlayerList().getPlayer(profile.getId());
			if (p != null) {
				teamData.syncChunksToPlayer(p);
				players.add(p);
			}
		});

		// also sync player head visibility; that might have changed too
		SendVisiblePlayerListPacket.syncToPlayers(players);
	}

	private void serverTickPost(MinecraftServer minecraftServer) {
		ClaimExpirationManager.INSTANCE.tick(minecraftServer);
		LongRangePlayerTracker.INSTANCE.tick(minecraftServer);
	}
}
