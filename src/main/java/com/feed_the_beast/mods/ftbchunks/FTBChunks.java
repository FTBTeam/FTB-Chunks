package com.feed_the_beast.mods.ftbchunks;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunk;
import com.feed_the_beast.mods.ftbchunks.api.FTBChunksAPI;
import com.feed_the_beast.mods.ftbchunks.client.FTBChunksClient;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkImpl;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkManagerImpl;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkPlayerDataImpl;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import com.feed_the_beast.mods.ftbchunks.impl.KnownFakePlayer;
import com.feed_the_beast.mods.ftbchunks.impl.XZ;
import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
import com.feed_the_beast.mods.ftbchunks.net.LoginDataPacket;
import com.feed_the_beast.mods.ftbchunks.net.PlayerDeathPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendAllChunksPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendChunkPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendGeneralDataPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendVisiblePlayerListPacket;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.stats.Stats;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.PacketDistributor;
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
@Mod("ftbchunks")
public class FTBChunks
{
	public static final Logger LOGGER = LogManager.getLogger("FTB Chunks");
	public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setLenient().create();

	public static FTBChunks instance;
	public FTBChunksCommon proxy;

	public static final int TILES = 15;
	public static final int TILE_SIZE = 16;
	public static final int TILE_OFFSET = TILES / 2;
	public static final int MINIMAP_SIZE = TILE_SIZE * TILES;
	public static final XZ[] RELATIVE_SPIRAL_POSITIONS = new XZ[TILES * TILES];

	public static boolean teamsMod = false;
	public static boolean ranksMod = false;

	public FTBChunks()
	{
		instance = this;
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
		MinecraftForge.EVENT_BUS.register(this);
		proxy = DistExecutor.safeRunForDist(() -> FTBChunksClient::new, () -> FTBChunksCommon::new);
		proxy.init();
		FTBChunksAPI.INSTANCE = new FTBChunksAPIImpl();
		FTBChunksConfig.init();
	}

	private void init(FMLCommonSetupEvent event)
	{
		teamsMod = ModList.get().isLoaded("ftbteams");
		ranksMod = ModList.get().isLoaded("ftbranks");
		FTBChunksNet.init();

		for (int i = 0; i < RELATIVE_SPIRAL_POSITIONS.length; i++)
		{
			RELATIVE_SPIRAL_POSITIONS[i] = XZ.of(MathUtils.getSpiralPoint(i + 1));
		}
	}

	@SubscribeEvent
	public void serverAboutToStart(FMLServerAboutToStartEvent event)
	{
		FTBChunksAPIImpl.manager = new ClaimedChunkManagerImpl(event.getServer());
	}

	@SubscribeEvent
	public void serverStarting(FMLServerStartingEvent event)
	{
		FTBChunksAPIImpl.manager.init();
	}

	@SubscribeEvent
	public void serverStopped(FMLServerStoppedEvent event)
	{
		FTBChunksAPIImpl.manager = null;
	}

	@SubscribeEvent
	public void worldSaved(WorldEvent.Save event)
	{
		if (FTBChunksAPIImpl.manager != null && !event.getWorld().isClientSide() && event.getWorld() instanceof World)
		{
			FTBChunksAPIImpl.manager.serverSaved();
		}
	}

	@SubscribeEvent
	public void loggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		final ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
		ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(player);

		if (!data.getName().equals(event.getPlayer().getGameProfile().getName()))
		{
			data.profile = new GameProfile(data.getUuid(), event.getPlayer().getGameProfile().getName());
			data.save();
		}

		FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> player), new LoginDataPacket(FTBChunksAPIImpl.manager.serverId));
		SendGeneralDataPacket.send(data, player);
		SendVisiblePlayerListPacket.sendAll();

		Date now = new Date();
		Map<Pair<RegistryKey<World>, UUID>, List<SendChunkPacket.SingleChunk>> chunksToSend = new HashMap<>();

		for (ClaimedChunkImpl chunk : FTBChunksAPIImpl.manager.claimedChunks.values())
		{
			chunksToSend.computeIfAbsent(Pair.of(chunk.pos.dimension, chunk.playerData.getUuid()), s -> new ArrayList<>()).add(new SendChunkPacket.SingleChunk(now, chunk.pos.x, chunk.pos.z, chunk));
		}

		for (Map.Entry<Pair<RegistryKey<World>, UUID>, List<SendChunkPacket.SingleChunk>> entry : chunksToSend.entrySet())
		{
			SendAllChunksPacket packet = new SendAllChunksPacket();
			packet.dimension = entry.getKey().getLeft();
			packet.owner = entry.getKey().getRight();
			packet.chunks = entry.getValue();
			FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> player), packet);
		}

		for (ClaimedChunk c : data.getClaimedChunks())
		{
			if (c.isForceLoaded())
			{
				ClaimedChunkImpl chunk = FTBChunksAPIImpl.manager.claimedChunks.get(c.getPos());

				if (chunk != null)
				{
					chunk.postSetForceLoaded(true);
				}
			}
		}

		boolean canChunkLoadOffline = FTBChunksConfig.getChunkLoadOffline(data, player);
		data.setChunkLoadOffline(canChunkLoadOffline);
	}

	@SubscribeEvent
	public void loggedOut(PlayerEvent.PlayerLoggedOutEvent event)
	{
		final ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
		ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(player);
		boolean canChunkLoadOffline = FTBChunksConfig.getChunkLoadOffline(data, player);
		data.setChunkLoadOffline(canChunkLoadOffline);

		if (!canChunkLoadOffline)
		{
			for (ClaimedChunk chunk : data.getClaimedChunks())
			{
				ClaimedChunkImpl c = FTBChunksAPIImpl.manager.claimedChunks.get(chunk.getPos());

				if (c == null)
				{
					return;
				}
				c.postSetForceLoaded(false);
			}
		}
	}

	private boolean isValidPlayer(@Nullable Entity entity)
	{
		if (FTBChunksConfig.disableProtection)
		{
			return false;
		}

		if (entity instanceof ServerPlayerEntity)
		{
			if (entity instanceof FakePlayer)
			{
				if (FTBChunksConfig.disableAllFakePlayers)
				{
					return false;
				}

				KnownFakePlayer player = FTBChunksAPIImpl.manager.knownFakePlayers.get(entity.getUUID());

				if (player == null)
				{
					player = new KnownFakePlayer(entity.getUUID(), ((FakePlayer) entity).getGameProfile().getName(), false);
					FTBChunksAPIImpl.manager.knownFakePlayers.put(player.uuid, player);
					FTBChunksAPIImpl.manager.saveFakePlayers = true;
				}

				return !player.banned;
			}

			return true;
		}

		return false;
	}

	@SubscribeEvent
	public void blockLeftClick(PlayerInteractEvent.LeftClickBlock event)
	{
		if (isValidPlayer(event.getPlayer()))
		{
			ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos(event.getWorld(), event.getPos()));

			if (chunk != null)
			{
				if (!chunk.canEdit((ServerPlayerEntity) event.getPlayer(), event.getWorld().getBlockState(event.getPos())))
				{
					event.setCanceled(true);
				}
			}
		}
	}

	@SubscribeEvent
	public void blockRightClick(PlayerInteractEvent.RightClickBlock event)
	{
		if (isValidPlayer(event.getPlayer()))
		{
			ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos(event.getWorld(), event.getPos()));

			if (chunk != null)
			{
				if (!chunk.canInteract((ServerPlayerEntity) event.getPlayer(), event.getWorld().getBlockState(event.getPos())))
				{
					event.setCanceled(true);
				}
			}
		}
	}

	@SubscribeEvent
	public void itemRightClick(PlayerInteractEvent.RightClickItem event)
	{
		if (isValidPlayer(event.getPlayer())
				&& !event.getItemStack().isEdible()
				&& !isPlayerTryingToFly(event)
		)
		{
			ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos(event.getWorld(), event.getPos()));

			if (chunk != null)
			{
				if (!chunk.canInteract((ServerPlayerEntity) event.getPlayer(), event.getWorld().getBlockState(event.getPos())))
				{
					event.setCanceled(true);
				}
			}
		}
	}

	private boolean isPlayerTryingToFly(PlayerInteractEvent.RightClickItem event) {
		boolean isItemFirework = event.getItemStack().getItem() instanceof FireworkRocketItem;
		boolean isPlayerWearingElytra = false
		for (ItemStack armorSlot : event.getPlayer().getArmorSlots()) {
			if (armorSlot.getItem() instanceof ElytraItem)
				isPlayerWearingElytra = true;
		}
		return isItemFirework && isPlayerWearingElytra;
	}

	@SubscribeEvent
	public void blockBreak(BlockEvent.BreakEvent event)
	{
		if (event.getWorld() instanceof World && isValidPlayer(event.getPlayer()))
		{
			ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos((World) event.getWorld(), event.getPos()));

			if (chunk != null)
			{
				if (!chunk.canEdit((ServerPlayerEntity) event.getPlayer(), event.getState()))
				{
					event.setCanceled(true);
				}
			}
		}
	}

	@SubscribeEvent
	public void blockPlace(BlockEvent.EntityPlaceEvent event)
	{
		if (isValidPlayer(event.getEntity()))
		{
			if (event instanceof BlockEvent.EntityMultiPlaceEvent)
			{
				for (BlockSnapshot snapshot : ((BlockEvent.EntityMultiPlaceEvent) event).getReplacedBlockSnapshots())
				{
					if (snapshot.getWorld() instanceof World)
					{
						ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos((World) snapshot.getWorld(), snapshot.getPos()));

						if (chunk != null && !chunk.canEdit((ServerPlayerEntity) event.getEntity(), snapshot.getCurrentBlock()))
						{
							event.setCanceled(true);
							return;
						}
					}
				}
			}
			else if (event.getWorld() instanceof World)
			{
				ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos((World) event.getWorld(), event.getPos()));

				if (chunk != null && !chunk.canEdit((ServerPlayerEntity) event.getEntity(), event.getPlacedBlock()))
				{
					event.setCanceled(true);
				}
			}
		}
	}

	@SubscribeEvent
	public void fillBucket(FillBucketEvent event)
	{
		if (isValidPlayer(event.getPlayer()) && event.getTarget() != null && event.getTarget() instanceof BlockRayTraceResult)
		{
			ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos(event.getWorld(), ((BlockRayTraceResult) event.getTarget()).getBlockPos()));

			Fluid fluid = Fluids.EMPTY;

			if (event.getEmptyBucket().getItem() instanceof BucketItem)
			{
				fluid = ((BucketItem) event.getEmptyBucket().getItem()).getFluid();
			}

			if (chunk != null && !chunk.canEdit((ServerPlayerEntity) event.getEntity(), fluid.defaultFluidState().createLegacyBlock()))
			{
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void farmlandTrample(BlockEvent.FarmlandTrampleEvent event)
	{
		if (event.getWorld() instanceof ServerWorld && isValidPlayer(event.getEntity()))
		{
			ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos((ServerWorld) event.getWorld(), event.getPos()));

			if (chunk != null && !chunk.canEdit((ServerPlayerEntity) event.getEntity(), event.getState()))
			{
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void chunkChange(EntityEvent.EnteringChunk event)
	{
		if (event.getEntity() instanceof FakePlayer || !(event.getEntity() instanceof ServerPlayerEntity))
		{
			return;
		}

		ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
		ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(player);

		int newX = event.getNewChunkX();
		int newZ = event.getNewChunkZ();

		if (data.prevChunkX != newX || data.prevChunkZ != newZ)
		{
			ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos(player));
			String s = chunk == null ? "-" : (chunk.getGroupID() + ":" + chunk.getPlayerData().getName());

			if (!data.lastChunkID.equals(s))
			{
				data.lastChunkID = s;

				if (chunk != null)
				{
					player.displayClientMessage(chunk.getDisplayName(), true);
				}
				else
				{
					player.displayClientMessage(new TranslationTextComponent("wilderness").withStyle(TextFormatting.DARK_GREEN), true);
				}
			}

			data.prevChunkX = newX;
			data.prevChunkZ = newZ;
		}
	}

	@SubscribeEvent
	public void mobSpawned(LivingSpawnEvent.CheckSpawn event)
	{
		if (!event.getWorld().isClientSide() && !(event.getEntity() instanceof PlayerEntity) && event.getWorld() instanceof World)
		{
			switch (event.getSpawnReason())
			{
				case NATURAL:
				case CHUNK_GENERATION:
				case SPAWNER:
				case STRUCTURE:
				case JOCKEY:
				case PATROL:
				{
					ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos((World) event.getWorld(), new BlockPos(event.getX(), event.getY(), event.getZ())));

					if (chunk != null && !chunk.canEntitySpawn(event.getEntity()))
					{
						event.setResult(Event.Result.DENY);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void explosionDetonate(ExplosionEvent.Detonate event)
	{
		// check config if explosion blocking is disabled

		if (event.getWorld().isClientSide() || event.getExplosion().getToBlow().isEmpty())
		{
			return;
		}

		List<BlockPos> list = new ArrayList<>(event.getExplosion().getToBlow());
		event.getExplosion().clearToBlow();
		Map<ChunkDimPos, Boolean> map = new HashMap<>();

		for (BlockPos pos : list)
		{
			if (map.computeIfAbsent(new ChunkDimPos(event.getWorld(), pos), cpos ->
			{
				ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(cpos);
				return chunk == null || chunk.allowExplosions();
			}))
			{
				event.getExplosion().getToBlow().add(pos);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void playerDeath(LivingDeathEvent event)
	{
		if (event.getEntity() instanceof ServerPlayerEntity)
		{
			ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
			RegistryKey<World> dim = player.level.dimension();
			int x = MathHelper.floor(player.getX());
			int z = MathHelper.floor(player.getZ());
			int num = player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS)) + 1;
			FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getEntity()), new PlayerDeathPacket(dim, x, z, num));
		}
	}
}