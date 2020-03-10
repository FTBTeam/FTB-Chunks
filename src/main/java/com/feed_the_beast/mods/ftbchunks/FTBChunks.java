package com.feed_the_beast.mods.ftbchunks;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunk;
import com.feed_the_beast.mods.ftbchunks.api.FTBChunksAPI;
import com.feed_the_beast.mods.ftbchunks.client.FTBChunksClient;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkManagerImpl;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkPlayerDataImpl;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
import com.feed_the_beast.mods.ftbchunks.net.SendColorMapPacket;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.resources.ReloadListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LatvianModder
 */
@Mod("ftbchunks")
public class FTBChunks
{
	public static final Logger LOGGER = LogManager.getLogger("FTB Chunks");
	public static FTBChunksCommon PROXY;

	public static final int TILES = 15;
	public static final int TILE_SIZE = 16;
	public static final int TILE_OFFSET = TILES / 2;
	public static final int GUI_SIZE = TILE_SIZE * TILES;
	public static final float UV = (float) TILES / (float) TILE_SIZE;

	public FTBChunks()
	{
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
		MinecraftForge.EVENT_BUS.addListener(FTBChunksCommands::new);
		MinecraftForge.EVENT_BUS.addListener(this::serverAboutToStart);
		MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
		MinecraftForge.EVENT_BUS.addListener(this::serverStopped);
		MinecraftForge.EVENT_BUS.addListener(this::worldSaved);
		MinecraftForge.EVENT_BUS.addListener(this::loggedIn);

		MinecraftForge.EVENT_BUS.addListener(this::blockLeftClick);
		MinecraftForge.EVENT_BUS.addListener(this::blockRightClick);
		MinecraftForge.EVENT_BUS.addListener(this::itemRightClick);
		MinecraftForge.EVENT_BUS.addListener(this::blockBreak);
		MinecraftForge.EVENT_BUS.addListener(this::blockPlace);
		MinecraftForge.EVENT_BUS.addListener(this::fillBucket);

		MinecraftForge.EVENT_BUS.addListener(this::chunkChange);
		MinecraftForge.EVENT_BUS.addListener(this::mobSpawned);
		MinecraftForge.EVENT_BUS.addListener(this::explosionDetonate);

		//noinspection Convert2MethodRef
		PROXY = DistExecutor.runForDist(() -> () -> new FTBChunksClient(), () -> () -> new FTBChunksCommon());
		PROXY.init();
		FTBChunksAPI.INSTANCE = new FTBChunksAPIImpl();
	}

	private void init(FMLCommonSetupEvent event)
	{
		FTBChunksNet.init();
	}

	private void serverAboutToStart(FMLServerAboutToStartEvent event)
	{
		FTBChunksAPIImpl.manager = new ClaimedChunkManagerImpl();

		event.getServer().getResourceManager().addReloadListener(new ReloadListener<JsonObject>()
		{
			@Override
			protected JsonObject prepare(IResourceManager resourceManager, IProfiler profiler)
			{
				Gson gson = new GsonBuilder().setLenient().create();
				JsonObject object = new JsonObject();

				try
				{
					for (IResource resource : resourceManager.getAllResources(new ResourceLocation("ftbchunks", "ftbchunks_colors.json")))
					{
						try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
						{
							for (Map.Entry<String, JsonElement> entry : gson.fromJson(reader, JsonObject.class).entrySet())
							{
								object.add(entry.getKey(), entry.getValue());
							}
						}
						catch (Exception ex)
						{
							ex.printStackTrace();
						}
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}

				return object;
			}

			@Override
			protected void apply(JsonObject object, IResourceManager resourceManager, IProfiler profiler)
			{
				FTBChunksAPIImpl.COLOR_MAP.clear();
				FTBChunksAPIImpl.COLOR_MAP_NET.clear();

				for (Map.Entry<String, JsonElement> entry : object.entrySet())
				{
					if (entry.getValue().isJsonPrimitive())
					{
						Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getKey()));
						Color4I color = Color4I.fromJson(entry.getValue());

						if (block != Blocks.AIR && !color.isEmpty())
						{
							FTBChunksAPIImpl.COLOR_MAP.put(block, color);
							FTBChunksAPIImpl.COLOR_MAP_NET.put(block.getRegistryName(), color.rgba());
						}
					}
				}
			}
		});
	}

	private void serverStarted(FMLServerStartedEvent event)
	{
		FTBChunksAPIImpl.manager.serverStarted(event.getServer());
	}

	private void serverStopped(FMLServerStoppedEvent event)
	{
		FTBChunksAPIImpl.manager = null;
	}

	private void worldSaved(WorldEvent.Save event)
	{
		if (!event.getWorld().isRemote())
		{
			FTBChunksAPIImpl.manager.serverSaved();
		}
	}

	private void loggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData((ServerPlayerEntity) event.getPlayer());

		if (!data.getName().equals(event.getPlayer().getGameProfile().getName()))
		{
			data.profile = new GameProfile(data.getUuid(), event.getPlayer().getGameProfile().getName());
			data.save();
		}

		FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()), new SendColorMapPacket(FTBChunksAPIImpl.COLOR_MAP_NET));
	}

	private boolean isValidPlayer(@Nullable Entity entity)
	{
		return entity instanceof ServerPlayerEntity && !(entity instanceof FakePlayer);
	}

	private void blockLeftClick(PlayerInteractEvent.LeftClickBlock event)
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

	private void blockRightClick(PlayerInteractEvent.RightClickBlock event)
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

	private void itemRightClick(PlayerInteractEvent.RightClickItem event)
	{
		if (isValidPlayer(event.getPlayer()) && !event.getItemStack().isFood())
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

	private void blockBreak(BlockEvent.BreakEvent event)
	{
		if (isValidPlayer(event.getPlayer()))
		{
			ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos(event.getWorld(), event.getPos()));

			if (chunk != null)
			{
				if (!chunk.canEdit((ServerPlayerEntity) event.getPlayer(), event.getState()))
				{
					event.setCanceled(true);
				}
			}
		}
	}

	private void blockPlace(BlockEvent.EntityPlaceEvent event)
	{
		if (isValidPlayer(event.getEntity()))
		{
			if (event instanceof BlockEvent.EntityMultiPlaceEvent)
			{
				for (BlockSnapshot snapshot : ((BlockEvent.EntityMultiPlaceEvent) event).getReplacedBlockSnapshots())
				{
					ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos(snapshot.getWorld(), snapshot.getPos()));

					if (chunk != null && !chunk.canEdit((ServerPlayerEntity) event.getEntity(), snapshot.getCurrentBlock()))
					{
						event.setCanceled(true);
						return;
					}
				}
			}
			else
			{
				ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos(event.getWorld(), event.getPos()));

				if (chunk != null && !chunk.canEdit((ServerPlayerEntity) event.getEntity(), event.getPlacedBlock()))
				{
					event.setCanceled(true);
				}
			}
		}
	}

	private void fillBucket(FillBucketEvent event)
	{
		if (isValidPlayer(event.getPlayer()) && event.getTarget() != null && event.getTarget() instanceof BlockRayTraceResult)
		{
			ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos(event.getWorld(), ((BlockRayTraceResult) event.getTarget()).getPos()));

			Fluid fluid = Fluids.EMPTY;

			if (event.getEmptyBucket().getItem() instanceof BucketItem)
			{
				fluid = ((BucketItem) event.getEmptyBucket().getItem()).getFluid();
			}

			if (chunk != null && !chunk.canEdit((ServerPlayerEntity) event.getEntity(), fluid.getDefaultState().getBlockState()))
			{
				event.setCanceled(true);
			}
		}
	}

	private void chunkChange(EntityEvent.EnteringChunk event)
	{
		if (isValidPlayer(event.getEntity()) && (event.getOldChunkX() != event.getNewChunkX() || event.getOldChunkZ() != event.getNewChunkZ()))
		{
			ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos(event.getEntity()));

			String s = chunk == null ? "-" : (chunk.getGroupID() + ":" + chunk.getPlayerData().getName());

			if (!event.getEntity().getPersistentData().getString("ftbchunks_lastchunk").equals(s))
			{
				event.getEntity().getPersistentData().putString("ftbchunks_lastchunk", s);

				if (chunk != null)
				{
					((ServerPlayerEntity) event.getEntity()).sendStatusMessage(chunk.getDisplayName().deepCopy().applyTextStyle(TextFormatting.AQUA), true);
				}
				else
				{
					((ServerPlayerEntity) event.getEntity()).sendStatusMessage(new TranslationTextComponent("wilderness").applyTextStyle(TextFormatting.DARK_GREEN), true);
				}
			}
		}
	}

	private void mobSpawned(LivingSpawnEvent.CheckSpawn event)
	{
		if (!event.getWorld().isRemote() && !(event.getEntity() instanceof PlayerEntity))
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
					ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(new ChunkDimPos(event.getWorld().getDimension().getType(), MathHelper.floor(event.getX()), MathHelper.floor(event.getZ())));

					if (chunk != null && !chunk.canEntitySpawn(event.getEntity()))
					{
						event.setResult(Event.Result.DENY);
					}
				}
			}
		}
	}

	private void explosionDetonate(ExplosionEvent.Detonate event)
	{
		// check config if explosion blocking is disabled

		if (event.getWorld().isRemote() || event.getExplosion().getAffectedBlockPositions().isEmpty())
		{
			return;
		}

		List<BlockPos> list = new ArrayList<>(event.getExplosion().getAffectedBlockPositions());
		event.getExplosion().clearAffectedBlockPositions();
		Map<ChunkDimPos, Boolean> map = new HashMap<>();

		for (BlockPos pos : list)
		{
			if (map.computeIfAbsent(new ChunkDimPos(event.getWorld(), pos), cpos ->
			{
				ClaimedChunk chunk = FTBChunksAPI.INSTANCE.getManager().getChunk(cpos);
				return chunk == null || chunk.allowExplosions();
			}))
			{
				event.getExplosion().getAffectedBlockPositions().add(pos);
			}
		}
	}
}