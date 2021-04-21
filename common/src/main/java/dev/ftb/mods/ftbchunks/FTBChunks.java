package dev.ftb.mods.ftbchunks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.core.FluidItemFTBC;
import dev.ftb.mods.ftbchunks.data.ChunkDimPos;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkPlayerData;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.XZ;
import dev.ftb.mods.ftbchunks.net.FTBChunksNet;
import dev.ftb.mods.ftbchunks.net.LoginDataPacket;
import dev.ftb.mods.ftbchunks.net.PlayerDeathPacket;
import dev.ftb.mods.ftbchunks.net.SendAllChunksPacket;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftbchunks.net.SendVisiblePlayerListPacket;
import dev.ftb.mods.ftbguilibrary.utils.MathUtils;
import dev.ftb.mods.ftbteams.event.TeamPropertiesChangedEvent;
import me.shedaniel.architectury.event.CompoundEventResult;
import me.shedaniel.architectury.event.EventResult;
import me.shedaniel.architectury.event.events.BlockEvent;
import me.shedaniel.architectury.event.events.CommandRegistrationEvent;
import me.shedaniel.architectury.event.events.EntityEvent;
import me.shedaniel.architectury.event.events.ExplosionEvent;
import me.shedaniel.architectury.event.events.InteractionEvent;
import me.shedaniel.architectury.event.events.LifecycleEvent;
import me.shedaniel.architectury.event.events.PlayerEvent;
import me.shedaniel.architectury.hooks.PlayerHooks;
import me.shedaniel.architectury.platform.Platform;
import me.shedaniel.architectury.registry.Registries;
import me.shedaniel.architectury.registry.Registry;
import me.shedaniel.architectury.utils.EnvExecutor;
import me.shedaniel.architectury.utils.IntValue;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class FTBChunks {
	public static final String MOD_ID = "ftbchunks";
	public static final Logger LOGGER = LogManager.getLogger("FTB Chunks");
	public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setLenient().create();

	public static FTBChunks instance;
	public static FTBChunksCommon PROXY;

	public static final int TILES = 15;
	public static final int TILE_SIZE = 16;
	public static final int TILE_OFFSET = TILES / 2;
	public static final int MINIMAP_SIZE = TILE_SIZE * TILES;
	public static final XZ[] RELATIVE_SPIRAL_POSITIONS = new XZ[TILES * TILES];

	public static final Registry<Block> BLOCK_REGISTRY = Registries.get(MOD_ID).get(net.minecraft.core.Registry.BLOCK_REGISTRY);

	public static boolean ranksMod = false;

	public FTBChunks() {
		PROXY = EnvExecutor.getEnvSpecific(() -> FTBChunksClient::new, () -> FTBChunksCommon::new);
		// FTBChunksWorldConfig.init();
		ranksMod = Platform.isModLoaded("ftbranks");
		FTBChunksNet.init();

		for (int i = 0; i < RELATIVE_SPIRAL_POSITIONS.length; i++) {
			RELATIVE_SPIRAL_POSITIONS[i] = XZ.of(MathUtils.getSpiralPoint(i + 1));
		}

		TeamPropertiesChangedEvent.EVENT.register(event -> {
			Date now = new Date();
			for (ServerPlayer player : event.getTeam().getOnlineMembers()) {
				Map<Pair<ResourceKey<Level>, UUID>, List<SendChunkPacket.SingleChunk>> chunksToSend = new HashMap<>();

				for (ClaimedChunk chunk : FTBChunksAPI.getManager().claimedChunks.values()) {
					chunksToSend.computeIfAbsent(Pair.of(chunk.pos.dimension, chunk.playerData.getUuid()), s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, chunk.pos.x, chunk.pos.z, chunk));
				}

				for (Map.Entry<Pair<ResourceKey<Level>, UUID>, List<SendChunkPacket.SingleChunk>> entry : chunksToSend.entrySet()) {
					SendAllChunksPacket packet = new SendAllChunksPacket();
					packet.dimension = entry.getKey().getLeft();
					packet.owner = entry.getKey().getRight();
					packet.chunks = entry.getValue();
					packet.sendTo(player);
				}
			}
		});

		LifecycleEvent.SERVER_BEFORE_START.register(this::serverAboutToStart);
		LifecycleEvent.SERVER_STARTING.register(this::serverStarting);
		LifecycleEvent.SERVER_STOPPED.register(this::serverStopped);
		LifecycleEvent.SERVER_WORLD_SAVE.register(this::worldSaved);
		PlayerEvent.PLAYER_JOIN.register(this::loggedIn);
		PlayerEvent.PLAYER_QUIT.register(this::loggedOut);
		InteractionEvent.LEFT_CLICK_BLOCK.register(this::blockLeftClick);
		InteractionEvent.RIGHT_CLICK_BLOCK.register(this::blockRightClick);
		InteractionEvent.RIGHT_CLICK_ITEM.register(this::itemRightClick);
		BlockEvent.BREAK.register(this::blockBreak);
		BlockEvent.PLACE.register(this::blockPlace);
		PlayerEvent.FILL_BUCKET.register(this::fillBucket);
		InteractionEvent.FARMLAND_TRAMPLE.register(this::farmlandTrample);
		EntityEvent.ENTER_CHUNK.register(this::chunkChange);
		EntityEvent.LIVING_CHECK_SPAWN.register(this::checkSpawn);
		ExplosionEvent.DETONATE.register(this::explosionDetonate);
		EntityEvent.LIVING_DEATH.register(this::playerDeath); // LOWEST
		CommandRegistrationEvent.EVENT.register(FTBChunksCommands::registerCommands);

		PROXY.init();
	}

	public void serverAboutToStart(MinecraftServer server) {
		FTBChunksAPI.manager = new ClaimedChunkManager(server);
	}

	public void serverStarting(MinecraftServer server) {
		FTBChunksAPI.manager.init();
	}

	public void serverStopped(MinecraftServer server) {
		FTBChunksAPI.manager = null;
	}

	public void worldSaved(ServerLevel level) {
		if (FTBChunksAPI.manager != null) {
			FTBChunksAPI.manager.serverSaved();
		}
	}

	public void loggedIn(ServerPlayer player) {
		ClaimedChunkPlayerData data = FTBChunksAPI.getManager().getData(player);

		if (!data.getName().equals(player.getGameProfile().getName())) {
			data.profile = new GameProfile(data.getUuid(), player.getGameProfile().getName());
			data.save();
		}


		new LoginDataPacket(FTBChunksAPI.getManager().serverId).sendTo(player);
		SendGeneralDataPacket.send(data, player);
		SendVisiblePlayerListPacket.sendAll();

		Date now = new Date();
		Map<Pair<ResourceKey<Level>, UUID>, List<SendChunkPacket.SingleChunk>> chunksToSend = new HashMap<>();

		for (ClaimedChunk chunk : FTBChunksAPI.getManager().claimedChunks.values()) {
			chunksToSend.computeIfAbsent(Pair.of(chunk.pos.dimension, chunk.playerData.getUuid()), s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, chunk.pos.x, chunk.pos.z, chunk));
		}

		for (Map.Entry<Pair<ResourceKey<Level>, UUID>, List<SendChunkPacket.SingleChunk>> entry : chunksToSend.entrySet()) {
			SendAllChunksPacket packet = new SendAllChunksPacket();
			packet.dimension = entry.getKey().getLeft();
			packet.owner = entry.getKey().getRight();
			packet.chunks = entry.getValue();
			packet.sendTo(player);
		}

		for (ClaimedChunk c : data.getClaimedChunks()) {
			if (c.isForceLoaded()) {
				ClaimedChunk chunk = FTBChunksAPI.getManager().claimedChunks.get(c.getPos());

				if (chunk != null) {
					chunk.postSetForceLoaded(true);
				}
			}
		}

		boolean canChunkLoadOffline = FTBChunksWorldConfig.get().getChunkLoadOffline(data, player);
		data.setChunkLoadOffline(canChunkLoadOffline);
	}

	public void loggedOut(ServerPlayer player) {
		ClaimedChunkPlayerData data = FTBChunksAPI.getManager().getData(player);
		boolean canChunkLoadOffline = FTBChunksWorldConfig.get().getChunkLoadOffline(data, player);
		data.setChunkLoadOffline(canChunkLoadOffline);

		if (!canChunkLoadOffline) {
			for (ClaimedChunk chunk : data.getClaimedChunks()) {
				ClaimedChunk c = FTBChunksAPI.getManager().claimedChunks.get(chunk.getPos());

				if (c == null) {
					return;
				}
				c.postSetForceLoaded(false);
			}
		}
	}

	private boolean checkPlayer(@Nullable Entity entity) {
		if (FTBChunksWorldConfig.get().disableProtection) {
			return false;
		}

		if (entity instanceof ServerPlayer) {
			if (PlayerHooks.isFake((ServerPlayer) entity)) {
				return !FTBChunksWorldConfig.get().disableAllFakePlayers;
			}

			return true;
		}

		return false;
	}

	private void printNoWildernessMessage(@Nullable Entity entity) {
		Component component = new TextComponent("You need to claim this chunk to interact with blocks here!");
		if (entity instanceof Player) {
			((Player) entity).displayClientMessage(component, true);
		} else if (entity != null) {
			entity.sendMessage(component, Util.NIL_UUID);
		}
	}

	public InteractionResult blockLeftClick(Player player, InteractionHand hand, BlockPos pos, Direction face) {
		if (checkPlayer(player)) {
			ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(new ChunkDimPos(player.level, pos));

			if (chunk != null) {
				if (!chunk.canEdit((ServerPlayer) player, player.level.getBlockState(pos))) {
					return InteractionResult.FAIL;
				}
			} else if (FTBChunksWorldConfig.get().noWilderness && !FTBChunksAPI.RIGHT_CLICK_BLACKLIST_TAG.contains(player.getItemInHand(hand).getItem())) {
				printNoWildernessMessage(player);
				return InteractionResult.FAIL;
			}
		}

		return InteractionResult.PASS;
	}

	public InteractionResult blockRightClick(Player player, InteractionHand hand, BlockPos pos, Direction face) {
		if (checkPlayer(player)) {
			ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(new ChunkDimPos(player.level, pos));

			if (chunk != null) {
				if (!chunk.canInteract((ServerPlayer) player, player.level.getBlockState(pos))) {
					return InteractionResult.FAIL;
				}
			} else if (FTBChunksWorldConfig.get().noWilderness && !FTBChunksAPI.RIGHT_CLICK_BLACKLIST_TAG.contains(player.getItemInHand(hand).getItem())) {
				printNoWildernessMessage(player);
				return InteractionResult.FAIL;
			}
		}

		return InteractionResult.PASS;
	}

	public InteractionResultHolder<ItemStack> itemRightClick(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (checkPlayer(player) && !stack.isEdible()) {
			ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(new ChunkDimPos(player.level, new BlockPos(player.getEyePosition(1F))));

			if (chunk != null) {
				if (!chunk.canRightClickItem((ServerPlayer) player, stack)) {
					return InteractionResultHolder.fail(stack);
				}
			} else if (FTBChunksWorldConfig.get().noWilderness && FTBChunksAPI.RIGHT_CLICK_BLACKLIST_TAG.contains(stack.getItem())) {
				printNoWildernessMessage(player);
				return InteractionResultHolder.fail(stack);
			}
		}

		return InteractionResultHolder.pass(stack);
	}

	public InteractionResult blockBreak(Level level, BlockPos pos, BlockState blockState, ServerPlayer player, @org.jetbrains.annotations.Nullable IntValue intValue) {
		if (checkPlayer(player)) {
			ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(new ChunkDimPos(level, pos));

			if (chunk != null) {
				if (!chunk.canEdit(player, blockState)) {
					return InteractionResult.FAIL;
				}
			} else if (FTBChunksWorldConfig.get().noWilderness) {
				printNoWildernessMessage(player);
				return InteractionResult.FAIL;
			}
		}

		return InteractionResult.PASS;
	}

	public InteractionResult blockPlace(Level level, BlockPos pos, BlockState blockState, @org.jetbrains.annotations.Nullable Entity entity) {
		if (checkPlayer(entity)) {
			/*if (event instanceof BlockEvent.EntityMultiPlaceEvent) {
				for (BlockSnapshot snapshot : ((BlockEvent.EntityMultiPlaceEvent) event).getReplacedBlockSnapshots()) {
					if (snapshot.getWorld() instanceof Level) {
						ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(new ChunkDimPos((Level) snapshot.getWorld(), snapshot.getPos()));

						if (chunk != null) {
							if (!chunk.canEdit((ServerPlayer) event.getEntity(), snapshot.getCurrentBlock())) {
								event.setCanceled(true);
								return;
							}
						} else if (FTBChunksConfig.noWilderness) {
							printNoWildernessMessage(event.getEntity());
							event.setCanceled(true);
							return;
						}
					}
				}
			} else if (event.getWorld() instanceof Level) {
			 */
			ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(new ChunkDimPos(level, pos));

			if (chunk != null) {
				if (!chunk.canEdit((ServerPlayer) entity, blockState)) {
					return InteractionResult.FAIL;
				}
			} else if (FTBChunksWorldConfig.get().noWilderness) {
				printNoWildernessMessage(entity);
				return InteractionResult.FAIL;
			}
			//}
		}

		return InteractionResult.PASS;
	}

	public CompoundEventResult<ItemStack> fillBucket(Player player, Level level, ItemStack emptyBucket, @org.jetbrains.annotations.Nullable HitResult target) {
		if (checkPlayer(player) && target instanceof BlockHitResult) {
			ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(new ChunkDimPos(player.level, ((BlockHitResult) target).getBlockPos()));

			Fluid fluid = Fluids.EMPTY;

			if (emptyBucket.getItem() instanceof FluidItemFTBC) {
				fluid = ((FluidItemFTBC) emptyBucket.getItem()).getFluidFTBC();
			}

			if (chunk != null) {
				if (!chunk.canEdit((ServerPlayer) player, fluid.defaultFluidState().createLegacyBlock())) {
					return CompoundEventResult.interrupt(false, null);
				}
			} else if (FTBChunksWorldConfig.get().noWilderness) {
				printNoWildernessMessage(player);
				return CompoundEventResult.interrupt(false, null);
			}
		}

		return CompoundEventResult.pass();
	}

	public EventResult farmlandTrample(Level world, BlockPos pos, BlockState blockState, float distance, Entity entity) {
		if (entity.level instanceof ServerLevel && checkPlayer(entity)) {
			ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(new ChunkDimPos(entity.level, pos));

			if (chunk != null) {
				if (!chunk.canEdit((ServerPlayer) entity, blockState)) {
					return EventResult.interrupt(false);
				}
			} else if (FTBChunksWorldConfig.get().noWilderness) {
				printNoWildernessMessage(entity);
				return EventResult.interrupt(false);
			}
		}

		return EventResult.pass();
	}

	public void chunkChange(Entity entity, int chunkX, int chunkZ, int prevX, int prevZ) {
		if (!(entity instanceof ServerPlayer) || PlayerHooks.isFake((ServerPlayer) entity)) {
			return;
		}

		ServerPlayer player = (ServerPlayer) entity;
		ClaimedChunkPlayerData data = FTBChunksAPI.getManager().getData(player);

		if (data.prevChunkX != chunkX || data.prevChunkZ != chunkZ) {
			ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(new ChunkDimPos(player));
			String s = chunk == null ? "-" : chunk.getPlayerData().getName();

			if (!data.lastChunkID.equals(s)) {
				data.lastChunkID = s;

				if (chunk != null) {
					player.displayClientMessage(chunk.getDisplayName(), true);
				} else {
					player.displayClientMessage(new TranslatableComponent("wilderness").withStyle(ChatFormatting.DARK_GREEN), true);
				}
			}

			data.prevChunkX = chunkX;
			data.prevChunkZ = chunkZ;
		}
	}

	public EventResult checkSpawn(LivingEntity entity, LevelAccessor level, double x, double y, double z, MobSpawnType type, @org.jetbrains.annotations.Nullable BaseSpawner spawner) {
		if (!level.isClientSide() && !(entity instanceof Player) && level instanceof Level) {
			switch (type) {
				case NATURAL:
				case CHUNK_GENERATION:
				case SPAWNER:
				case STRUCTURE:
				case JOCKEY:
				case PATROL: {
					ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(new ChunkDimPos((Level) level, new BlockPos(x, y, z)));

					if (chunk != null && !chunk.canEntitySpawn(entity)) {
						return EventResult.interrupt(false);
					}
				}
			}
		}

		return EventResult.pass();
	}

	public void explosionDetonate(Level level, Explosion explosion, List<Entity> affectedEntities) {
		// TODO: check config if explosion blocking is disabled

		if (level.isClientSide() || explosion.getToBlow().isEmpty()) {
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

	public InteractionResult playerDeath(LivingEntity entity, DamageSource source) {
		if (entity instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer) entity;
			ResourceKey<Level> dim = player.level.dimension();
			int x = Mth.floor(player.getX());
			int y = Mth.floor(player.getY());
			int z = Mth.floor(player.getZ());
			int num = player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS)) + 1;
			new PlayerDeathPacket(dim, x, y, z, num).sendTo((ServerPlayer) entity);
		}

		return InteractionResult.PASS;
	}
}