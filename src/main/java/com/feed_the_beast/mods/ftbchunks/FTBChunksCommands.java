package com.feed_the_beast.mods.ftbchunks;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.ClaimResult;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunk;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunkPlayerData;
import com.feed_the_beast.mods.ftbchunks.api.FTBChunksAPI;
import com.feed_the_beast.mods.ftbchunks.api.PrivacyMode;
import com.feed_the_beast.mods.ftbchunks.api.Waypoint;
import com.feed_the_beast.mods.ftbchunks.api.WaypointType;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkPlayerDataImpl;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import com.feed_the_beast.mods.ftbchunks.impl.map.MapDimension;
import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;
import com.feed_the_beast.mods.ftbchunks.net.SendWaypointsPacket;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class FTBChunksCommands
{
	public FTBChunksCommands(FMLServerStartingEvent event)
	{
		LiteralCommandNode<CommandSource> command = event.getCommandDispatcher().register(Commands.literal("ftbchunks")
				.then(Commands.literal("claim")
						.executes(context -> claim(context.getSource(), 0))
						.then(Commands.argument("radius", IntegerArgumentType.integer(0, 30))
								.executes(context -> claim(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))
						)
				)
				.then(Commands.literal("unclaim")
						.executes(context -> unclaim(context.getSource(), 0))
						.then(Commands.argument("radius", IntegerArgumentType.integer(0, 30))
								.executes(context -> unclaim(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))
						)
				)
				.then(Commands.literal("load")
						.executes(context -> load(context.getSource(), 0))
						.then(Commands.argument("radius", IntegerArgumentType.integer(0, 30))
								.executes(context -> load(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))
						)
				)
				.then(Commands.literal("unload")
						.executes(context -> unload(context.getSource(), 0))
						.then(Commands.argument("radius", IntegerArgumentType.integer(0, 30))
								.executes(context -> unload(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))
						)
				)
				.then(Commands.literal("unclaim_all")
						.then(Commands.argument("players", GameProfileArgument.gameProfile())
								.requires(source -> source.hasPermissionLevel(2))
								.executes(context -> unclaimAll(context.getSource(), GameProfileArgument.getGameProfiles(context, "players")))
						)
						.executes(context -> unclaimAll(context.getSource(), Collections.singleton(context.getSource().asPlayer().getGameProfile())))
				)
				.then(Commands.literal("unload_all")
						.then(Commands.argument("players", GameProfileArgument.gameProfile())
								.requires(source -> source.hasPermissionLevel(2))
								.executes(context -> unloadAll(context.getSource(), GameProfileArgument.getGameProfiles(context, "players")))
						)
						.executes(context -> unloadAll(context.getSource(), Collections.singleton(context.getSource().asPlayer().getGameProfile())))
				)
				.then(Commands.literal("info")
						.executes(context -> info(context.getSource(), new ChunkDimPos(context.getSource().getWorld(), new BlockPos(context.getSource().getPos()))))
						.then(Commands.argument("x", IntegerArgumentType.integer())
								.then(Commands.argument("z", IntegerArgumentType.integer())
										.executes(context -> info(context.getSource(), new ChunkDimPos(context.getSource().getWorld(), new BlockPos(IntegerArgumentType.getInteger(context, "x"), 0, IntegerArgumentType.getInteger(context, "z")))))
										.then(Commands.argument("dimension", DimensionArgument.getDimension())
												.executes(context -> info(context.getSource(), new ChunkDimPos(ChunkDimPos.getID(DimensionArgument.getDimensionArgument(context, "dimension")), IntegerArgumentType.getInteger(context, "x") >> 4, IntegerArgumentType.getInteger(context, "z") >> 4)))
										)
								)
						)
				)
				.then(Commands.literal("waypoints")
						.then(Commands.literal("add")
								.executes(context -> addWaypoint(context.getSource().asPlayer(), "Waypoint"))
								.then(Commands.argument("name", StringArgumentType.greedyString())
										.executes(context -> addWaypoint(context.getSource().asPlayer(), StringArgumentType.getString(context, "name")))
								)
						)
						.then(Commands.literal("delete_death_points")
								.executes(context -> deleteDeathPoints(context.getSource().asPlayer()))
						)
				)
				.then(Commands.literal("admin")
						.requires(source -> source.hasPermissionLevel(2))
						.then(Commands.literal("export")
								.requires(source -> source.hasPermissionLevel(2))
								.then(Commands.literal("json")
										.requires(source -> source.hasPermissionLevel(2))
										.executes(context -> exportJson(context.getSource()))
								)
								.then(Commands.literal("svg")
										.requires(source -> source.hasPermissionLevel(2))
										.executes(context -> exportSvg(context.getSource()))
								)
						)
						.then(Commands.literal("clear_task_queue")
								.requires(source -> source.hasPermissionLevel(4))
								.executes(context -> clearTaskQueue(context.getSource()))
						)
						.then(Commands.literal("send_entire_map")
								.requires(source -> source.hasPermissionLevel(4))
								.then(Commands.argument("players", EntityArgument.players())
										.requires(source -> source.hasPermissionLevel(4))
										.executes(context -> sendMap(context.getSource(), EntityArgument.getPlayers(context, "players")))
								)
						)
				)
		);

		event.getCommandDispatcher().register(Commands.literal("chunks").redirect(command));
	}

	private interface ChunkCallback
	{
		void accept(ClaimedChunkPlayerData data, ChunkDimPos pos) throws CommandSyntaxException;
	}

	private void forEachChunk(CommandSource source, int r, ChunkCallback callback) throws CommandSyntaxException
	{
		ClaimedChunkPlayerData data = FTBChunksAPI.INSTANCE.getManager().getData(source.asPlayer());
		String dimId = ChunkDimPos.getID(source.getWorld());
		int ox = MathHelper.floor(source.getPos().x) >> 4;
		int oz = MathHelper.floor(source.getPos().z) >> 4;
		List<ChunkDimPos> list = new ArrayList<>();

		for (int z = -r; z <= r; z++)
		{
			for (int x = -r; x <= r; x++)
			{
				list.add(new ChunkDimPos(dimId, ox + x, oz + z));
			}
		}

		list.sort(Comparator.comparingDouble(o -> MathUtils.distSq(ox, oz, o.x, o.z)));

		for (ChunkDimPos pos : list)
		{
			callback.accept(data, pos);
		}
	}

	private int claim(CommandSource source, int r) throws CommandSyntaxException
	{
		int[] success = new int[1];
		Instant time = Instant.now();

		forEachChunk(source, r, (data, pos) -> {
			ClaimResult result = data.claim(source, pos, false);

			if (result.isSuccess())
			{
				result.setClaimedTime(time);
				success[0]++;
			}
		});

		source.sendFeedback(new StringTextComponent("Claimed " + success[0] + " chunks!"), true);
		FTBChunks.LOGGER.info(source.getName() + " claimed " + success[0] + " chunks at " + new ChunkDimPos(source.asPlayer()));
		return success[0];
	}

	private int unclaim(CommandSource source, int r) throws CommandSyntaxException
	{
		int[] success = new int[1];

		forEachChunk(source, r, (data, pos) -> {
			if (data.unclaim(source, pos, false).isSuccess())
			{
				success[0]++;
			}
		});

		source.sendFeedback(new StringTextComponent("Unclaimed " + success[0] + " chunks!"), true);
		FTBChunks.LOGGER.info(source.getName() + " unclaimed " + success[0] + " chunks at " + new ChunkDimPos(source.asPlayer()));
		return success[0];
	}

	private int load(CommandSource source, int r) throws CommandSyntaxException
	{
		int[] success = new int[1];
		Instant time = Instant.now();

		forEachChunk(source, r, (data, pos) -> {
			ClaimResult result = data.load(source, pos, false);

			if (result.isSuccess())
			{
				result.setForceLoadedTime(time);
				success[0]++;
			}
		});

		source.sendFeedback(new StringTextComponent("Loaded " + success[0] + " chunks!"), true);
		FTBChunks.LOGGER.info(source.getName() + " loaded " + success[0] + " chunks at " + new ChunkDimPos(source.asPlayer()));
		return success[0];
	}

	private int unload(CommandSource source, int r) throws CommandSyntaxException
	{
		int[] success = new int[1];

		forEachChunk(source, r, (data, pos) -> {
			if (data.unload(source, pos, false).isSuccess())
			{
				success[0]++;
			}
		});

		source.sendFeedback(new StringTextComponent("Unloaded " + success[0] + " chunks!"), true);
		FTBChunks.LOGGER.info(source.getName() + " unloaded " + success[0] + " chunks at " + new ChunkDimPos(source.asPlayer()));
		return success[0];
	}

	private int unclaimAll(CommandSource source, Collection<GameProfile> players)
	{
		for (GameProfile profile : players)
		{
			ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.playerData.get(profile.getId());

			if (data != null)
			{
				for (ClaimedChunk c : data.getClaimedChunks())
				{
					data.unclaim(source, c.getPos(), false);
				}

				data.save();
			}
		}

		return 1;
	}

	private int unloadAll(CommandSource source, Collection<GameProfile> players)
	{
		for (GameProfile profile : players)
		{
			ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.playerData.get(profile.getId());

			if (data != null)
			{
				for (ClaimedChunk c : data.getClaimedChunks())
				{
					data.unload(source, c.getPos(), false);
				}
			}
		}

		return 1;
	}

	private int info(CommandSource source, ChunkDimPos pos)
	{
		source.sendFeedback(new StringTextComponent("Location: " + pos), true);

		ClaimedChunk chunk = FTBChunksAPIImpl.manager.getChunk(pos);

		if (chunk == null)
		{
			source.sendFeedback(new StringTextComponent("Chunk not claimed!"), true);
			return 0;
		}

		source.sendFeedback(new StringTextComponent("Owner: " + chunk.getPlayerData().getName() + " / " + UUIDTypeAdapter.fromUUID(chunk.getPlayerData().getUuid())), true);

		if (source.hasPermissionLevel(2))
		{
			source.sendFeedback(new StringTextComponent("Force Loaded: " + chunk.isForceLoaded()), true);
		}

		return 1;
	}

	private int exportJson(CommandSource source)
	{
		FTBChunksAPIImpl.manager.exportJson();
		source.sendFeedback(new StringTextComponent("Exported FTB Chunks data to <world directory>/data/ftbchunks/export/all.json!"), true);
		return 1;
	}

	private int exportSvg(CommandSource source)
	{
		FTBChunksAPIImpl.manager.exportSvg();
		source.sendFeedback(new StringTextComponent("Exported FTB Chunks data to <world directory>/data/ftbchunks/export/<dimension>.svg!"), true);
		return 1;
	}

	private int addWaypoint(ServerPlayerEntity player, String name)
	{
		ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(player);

		Waypoint w = new Waypoint(data, UUID.randomUUID());
		w.name = name;
		w.dimension = ChunkDimPos.getID(player.world);
		w.privacy = PrivacyMode.PRIVATE;
		w.x = MathHelper.floor(player.getPosX());
		w.y = MathHelper.floor(player.getPosY() + 2);
		w.z = MathHelper.floor(player.getPosZ());

		if (w.name.toLowerCase().startsWith("home") || w.name.toLowerCase().startsWith("house") || w.name.toLowerCase().startsWith("base"))
		{
			w.type = WaypointType.HOME;
		}
		else
		{
			w.color = Color4I.hsb(player.world.rand.nextFloat(), 1F, 1F).rgb();
			w.type = WaypointType.DEFAULT;
		}

		data.waypoints.put(w.id, w);
		data.save();

		SendWaypointsPacket.send(player);
		return 1;
	}

	private int deleteDeathPoints(ServerPlayerEntity player)
	{
		ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(player);

		if (data.waypoints.values().removeIf(waypoint -> waypoint.type == WaypointType.DEATH))
		{
			data.save();
			SendWaypointsPacket.send(player);
		}

		return 1;
	}

	private int clearTaskQueue(CommandSource source)
	{
		source.sendFeedback(new StringTextComponent("Cleared " + FTBChunksAPIImpl.manager.map.taskQueue.size() + " tasks!"), true);
		FTBChunksAPIImpl.manager.map.taskQueue.clear();
		return 1;
	}

	private int pregenMap(CommandSource source)
	{
		source.sendFeedback(new StringTextComponent("WIP!"), false);
		/*
		if (!source.getServer().isSinglePlayer())
		{
			source.getServer().getPlayerList().getPlayers().forEach(p -> p.connection.disconnect(new StringTextComponent(source.getName() + " started world map pre-generation!")));
		}

		ServerWorld world = source.getWorld();
		source.getServer().getPlayerList().sendMessage(new StringTextComponent("Pregen started for '" + world.getDimension().getType().getRegistryName() + "'").applyTextStyle(TextFormatting.LIGHT_PURPLE));
		FTBChunksAPIImpl.manager.map.queue(new PregenMapRegionTask(world));
		 */
		return 1;
	}

	private int sendMap(CommandSource source, Collection<ServerPlayerEntity> players)
	{
		ServerWorld world = source.getWorld();

		MapDimension dimension = FTBChunksAPIImpl.manager.map.getDimension(world);

		if (Files.exists(dimension.directory))
		{
			try
			{
				int[] chunks = new int[1];
				Files.list(dimension.directory)
						.map(path -> path.getFileName().toString())
						.filter(name -> name.endsWith(".png"))
						.map(name -> name.split(","))
						.filter(name -> name.length == 3)
						.map(name -> dimension.getRegion(XZ.of(Integer.parseInt(name[0]), Integer.parseInt(name[1]))))
						.forEach(region -> {
							region.chunks.values().forEach(chunk -> {
								chunks[0]++;
								FTBChunksAPIImpl.manager.map.queueSend(world, chunk.getActualPos(), players::contains);
							});

							FTBChunksAPIImpl.manager.map.queue(() -> {
								for (ServerPlayerEntity p : players)
								{
									p.sendStatusMessage(new StringTextComponent(FTBChunksAPIImpl.manager.map.taskQueue.size() + " chunks left").applyTextStyle(TextFormatting.LIGHT_PURPLE), true);
								}
							});
						});

				for (ServerPlayerEntity p : players)
				{
					p.sendMessage(new StringTextComponent(source.getName() + " is sending entire map (" + chunks[0] + " chunks) to entire map to you, be prepared for lag!").applyTextStyle(TextFormatting.LIGHT_PURPLE));
				}

				FTBChunksAPIImpl.manager.map.queue(() -> {
					for (ServerPlayerEntity p : players)
					{
						p.sendMessage(new StringTextComponent("Map received!").applyTextStyle(TextFormatting.LIGHT_PURPLE));
					}
				});
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		return 1;
	}
}