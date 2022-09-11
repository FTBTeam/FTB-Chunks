package dev.ftb.mods.ftbchunks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.ExplosionEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.hooks.level.entity.PlayerHooks;
import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.Registries;
import dev.architectury.utils.EnvExecutor;
import dev.architectury.utils.value.IntValue;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import dev.ftb.mods.ftbchunks.data.Protection;
import dev.ftb.mods.ftbchunks.net.FTBChunksNet;
import dev.ftb.mods.ftbchunks.net.LoginDataPacket;
import dev.ftb.mods.ftbchunks.net.PlayerDeathPacket;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftbchunks.net.SendManyChunksPacket;
import dev.ftb.mods.ftbchunks.net.SendVisiblePlayerListPacket;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.config.ConfigUtil;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.event.PlayerJoinedPartyTeamEvent;
import dev.ftb.mods.ftbteams.event.PlayerLoggedInAfterTeamEvent;
import dev.ftb.mods.ftbteams.event.PlayerTransferredTeamOwnershipEvent;
import dev.ftb.mods.ftbteams.event.TeamCollectPropertiesEvent;
import dev.ftb.mods.ftbteams.event.TeamCreatedEvent;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import dev.ftb.mods.ftbteams.event.TeamManagerEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
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
import net.minecraft.world.phys.HitResult;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

	public static boolean ranksMod = false;

	public FTBChunks() {
		PROXY = EnvExecutor.getEnvSpecific(() -> FTBChunksClient::new, () -> FTBChunksCommon::new);
		// FTBChunksWorldConfig.init();
		ranksMod = Platform.isModLoaded("ftbranks");
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
		TeamEvent.DELETED.register(this::teamDeleted);
		PlayerEvent.PLAYER_QUIT.register(this::loggedOut);
		InteractionEvent.LEFT_CLICK_BLOCK.register(this::blockLeftClick);
		InteractionEvent.RIGHT_CLICK_BLOCK.register(this::blockRightClick);
		InteractionEvent.RIGHT_CLICK_ITEM.register(this::itemRightClick);
		BlockEvent.BREAK.register(this::blockBreak);
		BlockEvent.PLACE.register(this::blockPlace);
		PlayerEvent.FILL_BUCKET.register(this::fillBucket);
		InteractionEvent.FARMLAND_TRAMPLE.register(this::farmlandTrample);
		EntityEvent.ENTER_SECTION.register(this::enterSection);
		EntityEvent.LIVING_CHECK_SPAWN.register(this::checkSpawn);
		ExplosionEvent.DETONATE.register(this::explosionDetonate);
		EntityEvent.LIVING_DEATH.register(this::playerDeath); // LOWEST
		CommandRegistrationEvent.EVENT.register(FTBChunksCommands::registerCommands);
		TeamEvent.COLLECT_PROPERTIES.register(this::teamConfig);
		TeamEvent.PLAYER_JOINED_PARTY.register(this::playerJoinedParty);
		TeamEvent.OWNERSHIP_TRANSFERRED.register(this::teamOwnershipTransferred);

		PROXY.init();
	}

	private void serverBeforeStart(MinecraftServer server) {
		var configPath = server.getWorldPath(ConfigUtil.SERVER_CONFIG_DIR);
		ConfigUtil.loadDefaulted(FTBChunksWorldConfig.CONFIG, configPath, FTBChunks.MOD_ID);

		FTBChunksWorldConfig.CLAIM_DIMENSION_BLACKLIST_SET.clear();

		for (String s : FTBChunksWorldConfig.CLAIM_DIMENSION_BLACKLIST.get()) {
			FTBChunksWorldConfig.CLAIM_DIMENSION_BLACKLIST_SET.add(ResourceKey.create(net.minecraft.core.Registry.DIMENSION_REGISTRY, new ResourceLocation(s)));
		}
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
		data.updateLimits(player);

		SNBTCompoundTag config = new SNBTCompoundTag();
		FTBChunksWorldConfig.CONFIG.write(config);
		new LoginDataPacket(event.getTeam().manager.getId(), config).sendTo(player);
		SendGeneralDataPacket.send(data, player);
		SendVisiblePlayerListPacket.sendAll();

		long now = System.currentTimeMillis();
		Map<Pair<ResourceKey<Level>, UUID>, List<SendChunkPacket.SingleChunk>> chunksToSend = new HashMap<>();

		for (ClaimedChunk chunk : FTBChunksAPI.getManager().claimedChunks.values()) {
			chunksToSend.computeIfAbsent(Pair.of(chunk.pos.dimension, chunk.teamData.getTeamId()), s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, chunk.pos.x, chunk.pos.z, chunk));
		}

		for (Map.Entry<Pair<ResourceKey<Level>, UUID>, List<SendChunkPacket.SingleChunk>> entry : chunksToSend.entrySet()) {
			SendManyChunksPacket packet = new SendManyChunksPacket();
			packet.dimension = entry.getKey().getLeft();
			packet.teamId = entry.getKey().getRight();
			packet.chunks = entry.getValue();
			packet.sendTo(player);
		}

		data.setForceLoadMember(player.getUUID(), true);
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

	private void teamDeleted(TeamEvent teamEvent) {
		if (!teamEvent.getTeam().getType().isPlayer()) {
			CommandSourceStack sourceStack = teamEvent.getTeam().manager.server.createCommandSourceStack();
			Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> chunksToUnclaim = new HashMap<>();
			long now = System.currentTimeMillis();

			for (ClaimedChunk chunk : FTBChunksAPI.manager.getData(teamEvent.getTeam()).getClaimedChunks()) {
				chunk.unclaim(sourceStack, false);
				chunksToUnclaim.computeIfAbsent(chunk.pos.dimension, s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, chunk.pos.x, chunk.pos.z, null));
			}

			for (Map.Entry<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> entry : chunksToUnclaim.entrySet()) {
				SendManyChunksPacket packet = new SendManyChunksPacket();
				packet.dimension = entry.getKey();
				packet.teamId = Util.NIL_UUID;
				packet.chunks = entry.getValue();
				packet.sendToAll(sourceStack.getServer());
			}
		}
	}

	public void loggedOut(ServerPlayer player) {
		if (!FTBTeamsAPI.isManagerLoaded() || !FTBChunksAPI.isManagerLoaded() || !FTBChunksAPI.getManager().hasData(player)) {
			return;
		}

		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(player);
		data.setForceLoadMember(player.getUUID(), FTBChunksWorldConfig.getChunkLoadOffline(player));
		FTBChunksAPI.getManager().updateForceLoadedChunks();
	}

	public EventResult blockLeftClick(Player player, InteractionHand hand, BlockPos pos, Direction face) {
		if (player instanceof ServerPlayer && FTBChunksAPI.getManager().protect(player, hand, pos, Protection.EDIT_BLOCK)) {
			return EventResult.interruptFalse();
		}

		return EventResult.pass();
	}

	public EventResult blockRightClick(Player player, InteractionHand hand, BlockPos pos, Direction face) {
		if (player instanceof ServerPlayer && FTBChunksAPI.getManager().protect(player, hand, pos, Protection.INTERACT_BLOCK)) {
			return EventResult.interruptFalse();
		}

		return EventResult.pass();
	}

	public CompoundEventResult<ItemStack> itemRightClick(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer && FTBChunksAPI.getManager().protect(player, hand, new BlockPos(player.getEyePosition(1F)), Protection.RIGHT_CLICK_ITEM)) {
			return CompoundEventResult.interruptFalse(player.getItemInHand(hand));
		}

		return CompoundEventResult.pass();
	}

	public EventResult blockBreak(Level level, BlockPos pos, BlockState blockState, ServerPlayer player, @Nullable IntValue intValue) {
		if (FTBChunksAPI.getManager().protect(player, InteractionHand.MAIN_HAND, pos, Protection.EDIT_BLOCK)) {
			return EventResult.interruptFalse();
		}

		return EventResult.pass();
	}

	public EventResult blockPlace(Level level, BlockPos pos, BlockState blockState, @Nullable Entity entity) {
		if (entity instanceof ServerPlayer && FTBChunksAPI.getManager().protect(entity, InteractionHand.MAIN_HAND, pos, Protection.EDIT_BLOCK)) {
			return EventResult.interruptFalse();
		}

		return EventResult.pass();
	}

	public CompoundEventResult<ItemStack> fillBucket(Player player, Level level, ItemStack emptyBucket, @Nullable HitResult target) {
		if (player instanceof ServerPlayer && target instanceof BlockHitResult && FTBChunksAPI.getManager().protect(player, InteractionHand.MAIN_HAND, ((BlockHitResult) target).getBlockPos(), Protection.EDIT_FLUID)) {
			return CompoundEventResult.interrupt(false, null);
		}

		return CompoundEventResult.pass();
	}

	public EventResult farmlandTrample(Level world, BlockPos pos, BlockState blockState, float distance, Entity entity) {
		if (entity instanceof ServerPlayer && FTBChunksAPI.getManager().protect(entity, InteractionHand.MAIN_HAND, pos, Protection.EDIT_BLOCK)) {
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
					player.displayClientMessage(new TranslatableComponent("wilderness").withStyle(ChatFormatting.DARK_GREEN), true);
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

		return explosion.source == null;
	}

	public void explosionDetonate(Level level, Explosion explosion, List<Entity> affectedEntities) {
		if (ignoreExplosion(level, explosion)) {
			return;
		}

		List<BlockPos> list = new ArrayList<>(explosion.getToBlow());
		explosion.clearToBlow();
		Map<ChunkDimPos, Boolean> map = new HashMap<>();

		for (BlockPos pos : list) {
			if (map.computeIfAbsent(new ChunkDimPos(level, pos), cpos ->
			{
				ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(cpos);
				return chunk == null || chunk.allowExplosions();
			})) {
				explosion.getToBlow().add(pos);
			}
		}
	}

	public EventResult playerDeath(LivingEntity entity, DamageSource source) {
		if (entity instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer) entity;
			ResourceKey<Level> dim = player.level.dimension();
			int x = Mth.floor(player.getX());
			int y = Mth.floor(player.getY());
			int z = Mth.floor(player.getZ());
			int num = player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS)) + 1;
			new PlayerDeathPacket(dim, x, y, z, num).sendTo((ServerPlayer) entity);
		}

		return EventResult.pass();
	}

	private void teamConfig(TeamCollectPropertiesEvent event) {
		event.add(FTBChunksTeamData.ALLOW_FAKE_PLAYERS);
		event.add(FTBChunksTeamData.BLOCK_EDIT_MODE);
		event.add(FTBChunksTeamData.BLOCK_INTERACT_MODE);
		event.add(FTBChunksTeamData.ALLOW_EXPLOSIONS);
		// event.add(FTBChunksTeamData.MINIMAP_MODE);
		// event.add(FTBChunksTeamData.LOCATION_MODE);
	}

	private void playerJoinedParty(PlayerJoinedPartyTeamEvent event) {
		CommandSourceStack sourceStack = event.getTeam().manager.server.createCommandSourceStack();
		FTBChunksTeamData oldData = FTBChunksAPI.getManager().getData(event.getPreviousTeam());
		FTBChunksTeamData newData = FTBChunksAPI.getManager().getData(event.getTeam());
		newData.updateLimits(event.getPlayer());

		Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> chunksToSend = new HashMap<>();
		Map<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> chunksToUnclaim = new HashMap<>();
		int chunks = 0;
		long now = System.currentTimeMillis();
		int total = 0;

		for (ClaimedChunk chunk : oldData.getClaimedChunks()) {
			if (total >= newData.maxClaimChunks) {
				chunk.unclaim(sourceStack, false);
				chunksToUnclaim.computeIfAbsent(chunk.pos.dimension, s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, chunk.pos.x, chunk.pos.z, null));
			} else {
				chunk.teamData = newData;
				chunksToSend.computeIfAbsent(chunk.pos.dimension, s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, chunk.pos.x, chunk.pos.z, chunk));
				chunks++;
			}

			total++;
		}

		if (chunks == 0) {
			return;
		}

		for (Map.Entry<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> entry : chunksToSend.entrySet()) {
			SendManyChunksPacket packet = new SendManyChunksPacket();
			packet.dimension = entry.getKey();
			packet.teamId = newData.getTeamId();
			packet.chunks = entry.getValue();
			packet.sendToAll(sourceStack.getServer());
		}

		for (Map.Entry<ResourceKey<Level>, List<SendChunkPacket.SingleChunk>> entry : chunksToUnclaim.entrySet()) {
			SendManyChunksPacket packet = new SendManyChunksPacket();
			packet.dimension = entry.getKey();
			packet.teamId = Util.NIL_UUID;
			packet.chunks = entry.getValue();
			packet.sendToAll(sourceStack.getServer());
		}

		FTBChunks.LOGGER.info("Transferred " + chunks + "/" + total + " chunks from " + oldData + " to " + newData);
	}

	private void teamOwnershipTransferred(PlayerTransferredTeamOwnershipEvent event) {
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(event.getTeam());
		data.updateLimits(event.getTo());
	}
}
