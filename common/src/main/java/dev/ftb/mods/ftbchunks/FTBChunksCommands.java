package dev.ftb.mods.ftbchunks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.util.UUIDTypeAdapter;
import dev.ftb.mods.ftbchunks.data.*;
import dev.ftb.mods.ftbchunks.net.AddWaypointPacket;
import dev.ftb.mods.ftbchunks.net.LoadedChunkViewPacket;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamArgument;
import dev.ftb.mods.ftbteams.data.TeamArgumentProvider;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntBiFunction;

/**
 * @author LatvianModder
 */
public class FTBChunksCommands {
	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registry, Commands.CommandSelection selection) {
		LiteralCommandNode<CommandSourceStack> command = dispatcher.register(Commands.literal("ftbchunks")
				.then(Commands.literal("claim")
						.executes(context -> claim(context.getSource(), selfTeam(context.getSource()), 0))
						.then(radiusArg().executes(context -> claim(context.getSource(), selfTeam(context.getSource()), getRadiusArg(context))))
				)
				.then(Commands.literal("unclaim")
						.executes(context -> unclaim(context.getSource(), selfTeam(context.getSource()), 0))
						.then(radiusArg().executes(context -> unclaim(context.getSource(), selfTeam(context.getSource()), getRadiusArg(context))))
				)
				.then(Commands.literal("load")
						.executes(context -> load(context.getSource(), 0))
						.then(radiusArg().executes(context -> load(context.getSource(), getRadiusArg(context))))
				)
				.then(Commands.literal("unload")
						.executes(context -> unload(context.getSource(), 0))
						.then(radiusArg().executes(context -> unload(context.getSource(), getRadiusArg(context))))
				)
				.then(Commands.literal("unclaim_all")
						.executes(context -> unclaimAll(context.getSource(), selfTeam(context.getSource())))
						.then(forTeam(FTBChunksCommands::unclaimAll))
				)
				.then(Commands.literal("unload_all")
						.executes(context -> unloadAll(context.getSource(), selfTeam(context.getSource())))
						.then(forTeam(FTBChunksCommands::unloadAll))
				)
				.then(Commands.literal("info")
						.executes(context -> info(context.getSource(), new ChunkDimPos(context.getSource().getLevel(), new BlockPos(context.getSource().getPosition()))))
						.then(Commands.argument("x", IntegerArgumentType.integer())
								.then(Commands.argument("z", IntegerArgumentType.integer())
										.executes(context -> info(context.getSource(), new ChunkDimPos(context.getSource().getLevel(), new BlockPos(IntegerArgumentType.getInteger(context, "x"), 0, IntegerArgumentType.getInteger(context, "z")))))
										.then(dimArg()
												.executes(context -> info(context.getSource(), new ChunkDimPos(getDimArg(context).dimension(), IntegerArgumentType.getInteger(context, "x") >> 4, IntegerArgumentType.getInteger(context, "z") >> 4)))
										)
								)
						)
				)
				.then(Commands.literal("admin")
						.requires(source -> source.hasPermission(2))
						.then(Commands.literal("bypass_protection")
								.executes(context -> bypassProtection(context.getSource()))
						)
						.then(Commands.literal("extra_claim_chunks")
								.then(Commands.argument("player", EntityArgument.player())
										.then(Commands.literal("get")
												.executes(context -> getExtraClaimChunks(context.getSource(), EntityArgument.getPlayer(context, "player")))
										)
										.then(Commands.literal("set")
												.then(Commands.argument("number", IntegerArgumentType.integer(0))
														.executes(context -> setExtraClaimChunks(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "number"), false))
												)
										)
										.then(Commands.literal("add")
												.then(Commands.argument("number", IntegerArgumentType.integer())
														.executes(context -> setExtraClaimChunks(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "number"), true))
												)
										)
								)
						)
						.then(Commands.literal("extra_force_load_chunks")
								.then(Commands.argument("player", EntityArgument.player())
										.then(Commands.literal("get")
												.executes(context -> getExtraForceLoadChunks(context.getSource(), EntityArgument.getPlayer(context, "player")))
										)
										.then(Commands.literal("set")
												.then(Commands.argument("number", IntegerArgumentType.integer(0))
														.executes(context -> setExtraForceLoadChunks(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "number"), false))
												)
										)
										.then(Commands.literal("add")
												.then(Commands.argument("number", IntegerArgumentType.integer())
														.executes(context -> setExtraForceLoadChunks(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "number"), true))
												)
										)
								)
						)
						.then(Commands.literal("claim_as")
								.then(Commands.argument("team", TeamArgument.create())
										.executes(context -> claim(context.getSource(), TeamArgument.get(context, "team"), 0))
										.then(radiusArg()
												.executes(context -> claim(context.getSource(), TeamArgument.get(context, "team"), getRadiusArg(context)))
												.then(anchorArg()
														.executes(context -> claim(context.getSource(), TeamArgument.get(context, "team"), getRadiusArg(context), getAnchorArg(context), context.getSource().getLevel()))
														.then(dimArg()
																.executes(context -> claim(context.getSource(), TeamArgument.get(context, "team"), getRadiusArg(context), getAnchorArg(context), getDimArg(context)))
														)
												)
										)
								)
						)
						.then(Commands.literal("unclaim_as")
								.then(Commands.argument("team", TeamArgument.create())
										.executes(context -> unclaim(context.getSource(), TeamArgument.get(context, "team"), 0))
										.then(radiusArg()
												.executes(context -> unclaim(context.getSource(), TeamArgument.get(context, "team"), getRadiusArg(context)))
												.then(anchorArg()
														.executes(context -> unclaim(context.getSource(), TeamArgument.get(context, "team"), getRadiusArg(context), getAnchorArg(context), context.getSource().getLevel()))
														.then(dimArg()
																.executes(context -> unclaim(context.getSource(), TeamArgument.get(context, "team"), getRadiusArg(context), getAnchorArg(context), getDimArg(context)))
														)
												)
										)
								)
						)
						.then(Commands.literal("unclaim_everything")
								.executes(context -> unclaimEverything(context.getSource()))
						)
						.then(Commands.literal("unload_everything")
								.executes(context -> unloadEverything(context.getSource()))
						)
						.then(Commands.literal("view_loaded_chunks")
								.executes(context -> viewLoadedChunks(context.getSource(), context.getSource().getLevel()))
								.then(Commands.literal("reset")
										.executes(context -> resetLoadedChunks(context.getSource(), context.getSource().getLevel()))
										.then(Commands.argument("dimension", DimensionArgument.dimension())
												.executes(context -> resetLoadedChunks(context.getSource(), DimensionArgument.getDimension(context, "dimension")))
										)
								)
								.then(Commands.argument("dimension", DimensionArgument.dimension())
										.executes(context -> viewLoadedChunks(context.getSource(), DimensionArgument.getDimension(context, "dimension")))
								)
						)
						.then(Commands.literal("reset_block_break_counter")
								.then(Commands.literal("all")
										.executes(context -> resetBlockBreakCounter(context.getSource(), null))
								)
								.then(Commands.argument("team", TeamArgument.create())
										.executes(context -> resetBlockBreakCounter(context.getSource(), TeamArgument.get(context, "team")))
								)
						)
				)
				.then(Commands.literal("block_color")
						.requires(source -> source.getServer().isSingleplayer())
						.executes(context -> FTBChunks.PROXY.blockColor())
				)
				.then(Commands.literal("waypoint")
						.then(Commands.literal("add")
								.then(Commands.argument("name", StringArgumentType.string())
										.then(Commands.argument("position", BlockPosArgument.blockPos())
												.executes(context -> addWaypoint(context.getSource(), StringArgumentType.getString(context, "name"), BlockPosArgument.getLoadedBlockPos(context, "position")))
												.then(Commands.argument("color", ColorArgument.color())
														.executes(context -> addWaypoint(context.getSource(), StringArgumentType.getString(context, "name"), BlockPosArgument.getLoadedBlockPos(context, "position"), ColorArgument.getColor(context, "color")))
												)
										)
								)
						)
				)
		);

		dispatcher.register(Commands.literal("chunks").redirect(command));
	}

	private static int addWaypoint(CommandSourceStack source, String name, BlockPos position, ChatFormatting color) throws CommandSyntaxException {
		if (color.getColor() != null) {
			ServerPlayer player = source.getPlayerOrException();
			new AddWaypointPacket(name, position, color.getColor()).sendTo(player);
		}
		return 1;
	}

	private static int addWaypoint(CommandSourceStack source, String name, BlockPos position) throws CommandSyntaxException {
		int idx = source.getPlayerOrException().getRandom().nextInt(ChatFormatting.values().length);
		return addWaypoint(source, name, position, ChatFormatting.values()[idx]);
	}

	private static int bypassProtection(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ClaimedChunkManager manager = FTBChunksAPI.getManager();
		manager.setBypassProtection(player.getUUID(), !manager.getBypassProtection(player.getUUID()));
		source.sendSuccess(Component.literal("bypass_protection = " + manager.getBypassProtection(player.getUUID())), true);
		return 1;
	}

	private interface ChunkCallback {
		void accept(FTBChunksTeamData data, ChunkDimPos pos) throws CommandSyntaxException;
	}

	private static void forEachChunk(CommandSourceStack source, int r, ChunkCallback callback) throws CommandSyntaxException {
		Team team = FTBTeamsAPI.getPlayerTeam(source.getPlayerOrException());
		forEachChunk(team, source.getLevel(), toColumn(source.getPosition()), r, callback);
	}

	private static void forEachChunk(Team team, Level level, ColumnPos anchor, int r, ChunkCallback callback) throws CommandSyntaxException {
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(team);
		ResourceKey<Level> dimId = level.dimension();
		int ox = Mth.floor(anchor.x()) >> 4;
		int oz = Mth.floor(anchor.z()) >> 4;
		List<ChunkDimPos> list = new ArrayList<>();

		r = r >> 4;

		for (int z = -r; z <= r; z++) {
			for (int x = -r; x <= r; x++) {
				list.add(new ChunkDimPos(dimId, ox + x, oz + z));
			}
		}

		list.sort(Comparator.comparingDouble(o -> MathUtils.distSq(ox, oz, o.x, o.z)));

		for (ChunkDimPos pos : list) {
			callback.accept(data, pos);
		}
	}

	private static int claim(CommandSourceStack source, Team team, int r) throws CommandSyntaxException {
		return claim(source, team, r, toColumn(source.getPosition()), source.getLevel());
	}

	private static int claim(CommandSourceStack source, Team team, int r, ColumnPos anchor, Level level) throws CommandSyntaxException {
		int[] success = new int[1];

		forEachChunk(team, level, anchor, r, (data, pos) -> {
			if (data.claim(source, pos, false).isSuccess()) {
				success[0]++;
			}
		});

		source.sendSuccess(Component.literal("Claimed " + success[0] + " chunks!"), false);

		ChunkDimPos origin = new ChunkDimPos(level.dimension(), anchor.x() >> 4, anchor.z() >> 4);
		FTBChunks.LOGGER.info(source.getTextName() + " claimed " + success[0] + " chunks around " + origin + "for team " + team.getDisplayName());

		return success[0];
	}

	private static int unclaim(CommandSourceStack source, Team team, int r) throws CommandSyntaxException {
		return unclaim(source, team, r, toColumn(source.getPosition()), source.getLevel());
	}

	private static int unclaim(CommandSourceStack source, Team team, int r, ColumnPos anchor, Level level) throws CommandSyntaxException {
		int[] success = new int[1];

		forEachChunk(team, level, anchor, r, (data, pos) -> {
			if (data.unclaim(source, pos, false).isSuccess()) {
				success[0]++;
			}
		});

		source.sendSuccess(Component.literal("Unclaimed " + success[0] + " chunks!"), false);

		ChunkDimPos origin = new ChunkDimPos(level.dimension(), anchor.x() >> 4, anchor.z() >> 4);
		FTBChunks.LOGGER.info(source.getTextName() + " unclaimed " + success[0] + " chunks around " + origin + "for team " + team.getDisplayName());

		return success[0];
	}

	private static int load(CommandSourceStack source, int r) throws CommandSyntaxException {
		int[] success = new int[1];

		forEachChunk(source, r, (data, pos) -> {
			ClaimResult result = data.load(source, pos, false);
			if (result.isSuccess()) {
				success[0]++;
			}
		});

		source.sendSuccess(Component.literal("Loaded " + success[0] + " chunks!"), false);
		FTBChunks.LOGGER.info(source.getTextName() + " loaded " + success[0] + " chunks at " + new ChunkDimPos(source.getPlayerOrException()));
		return success[0];
	}

	private static int unload(CommandSourceStack source, int r) throws CommandSyntaxException {
		int[] success = new int[1];

		forEachChunk(source, r, (data, pos) -> {
			if (data.unload(source, pos, false).isSuccess()) {
				success[0]++;
			}
		});

		source.sendSuccess(Component.literal("Unloaded " + success[0] + " chunks!"), false);
		FTBChunks.LOGGER.info(source.getTextName() + " unloaded " + success[0] + " chunks at " + new ChunkDimPos(source.getPlayerOrException()));
		return success[0];
	}

	private static int unclaimAll(CommandSourceStack source, Team team) {
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(team);

		for (ClaimedChunk c : new ArrayList<>(data.getClaimedChunks())) {
			data.unclaim(source, c.getPos(), false);
		}
		data.save();

		return 1;
	}

	private static int unloadAll(CommandSourceStack source, Team team) {
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(team);

		for (ClaimedChunk c : new ArrayList<>(data.getClaimedChunks())) {
			data.unload(source, c.getPos(), false);
		}
		data.save();

		return 1;
	}

	private static int info(CommandSourceStack source, ChunkDimPos pos) {
		source.sendSuccess(Component.literal("Location: " + pos), true);

		ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(pos);

		if (chunk == null) {
			source.sendSuccess(Component.literal("Chunk not claimed!"), true);
			return 0;
		}

		source.sendSuccess(Component.literal("Owner: ").append(chunk.getTeamData().getTeam().getColoredName()).append(" / " + UUIDTypeAdapter.fromUUID(chunk.getTeamData().getTeamId())), true);

		if (source.hasPermission(2)) {
			source.sendSuccess(Component.literal("Force Loaded: " + chunk.isForceLoaded()), true);
		}

		return 1;
	}

	private static int getExtraClaimChunks(CommandSourceStack source, ServerPlayer player) {
		FTBChunksTeamData personalData = FTBChunksAPI.getManager().getPersonalData(player);
		if (personalData == null) {
			source.sendFailure(Component.literal("can't get personal team data for ").append(player.getDisplayName()));
			return 1;
		}

		source.sendSuccess(player.getDisplayName().copy().append("/extra_claim_chunks = " + personalData.extraClaimChunks), false);
		return 1;
	}

	private static int setExtraClaimChunks(CommandSourceStack source, ServerPlayer player, int extra, boolean adding) {
		// limit is added to player's *personal* data, even if they're in a party
		FTBChunksTeamData personalData = FTBChunksAPI.getManager().getPersonalData(player);
		if (personalData == null) {
			source.sendFailure(Component.literal("can't get personal team data for ").append(player.getDisplayName()));
			return 1;
		}
		personalData.extraClaimChunks = Math.max(0, extra + (adding ? personalData.extraClaimChunks : 0));
		personalData.save();

		// player's new personal limit will affect the team limit
		FTBChunksTeamData teamData = FTBChunksAPI.getManager().getData(player);
		teamData.updateLimits();
		SendGeneralDataPacket.send(teamData, player);

		source.sendSuccess(player.getDisplayName().copy().append("/extra_claim_chunks = " + personalData.extraClaimChunks), false);
		return 1;
	}

	private static int getExtraForceLoadChunks(CommandSourceStack source, ServerPlayer player) {
		FTBChunksTeamData personalData = FTBChunksAPI.getManager().getPersonalData(player);
		if (personalData == null) {
			source.sendFailure(Component.literal("can't get personal team data for ").append(player.getDisplayName()));
			return 1;
		}

		source.sendSuccess(player.getDisplayName().copy().append("/extra_force_load_chunks = " + personalData.extraForceLoadChunks), false);
		return 1;
	}

	private static int setExtraForceLoadChunks(CommandSourceStack source, ServerPlayer player, int extra, boolean adding) {
		// limit is added to player's *personal* data, even if they're in a party
		FTBChunksTeamData personalData = FTBChunksAPI.getManager().getPersonalData(player);
		if (personalData == null) {
			source.sendFailure(Component.literal("can't get personal team data for ").append(player.getDisplayName()));
			return 1;
		}
		personalData.extraForceLoadChunks = Math.max(0, extra + (adding ? personalData.extraForceLoadChunks : 0));
		personalData.save();

		// player's new personal limit will affect the team limit
		FTBChunksTeamData teamData = FTBChunksAPI.getManager().getData(player);
		teamData.updateLimits();
		SendGeneralDataPacket.send(teamData, player);

		source.sendSuccess(player.getDisplayName().copy().append("/extra_force_load_chunks = " + personalData.extraForceLoadChunks), false);
		return 1;
	}

	private static int unclaimEverything(CommandSourceStack source) {
		for (ClaimedChunk c : new ArrayList<>(FTBChunksAPI.getManager().getAllClaimedChunks())) {
			c.teamData.unclaim(source, c.getPos(), false);
			c.teamData.save();
		}

		return 1;
	}

	private static int unloadEverything(CommandSourceStack source) {
		for (ClaimedChunk c : new ArrayList<>(FTBChunksAPI.getManager().getAllClaimedChunks())) {
			c.teamData.unload(source, c.getPos(), false);
			c.teamData.save();
		}

		return 1;
	}

	private static int viewLoadedChunks(CommandSourceStack source, ServerLevel level) throws CommandSyntaxException {
		var chunks = new Long2IntOpenHashMap();

		for (ChunkHolder holder : level.getChunkSource().chunkMap.updatingChunkMap.values()) {
			chunks.put(holder.getPos().toLong(), 1);
		}

		var map = FTBChunksAPI.getManager().getForceLoadedChunks(level.dimension());
		for (long pos : map.keySet()) {
			if (chunks.get(pos) == 1) {
				chunks.put(pos, 2);
			}
		}

		for (long pos : level.getForcedChunks()) {
			if (chunks.get(pos) == 1) {
				chunks.put(pos, 2);
			}
		}

		source.sendSuccess(Component.literal(String.format("Chunks Loaded: %d. Check the map to see loaded chunks", chunks.size())), false);
		new LoadedChunkViewPacket(level.dimension(), chunks).sendTo(source.getPlayerOrException());
		return 1;
	}

	private static int resetLoadedChunks(CommandSourceStack source, ServerLevel level) throws CommandSyntaxException {
		new LoadedChunkViewPacket(level.dimension(), Long2IntMaps.EMPTY_MAP).sendTo(source.getPlayerOrException());
		return 1;
	}

	private static RequiredArgumentBuilder<CommandSourceStack, Integer> radiusArg() {
		return Commands.argument("radius_in_blocks", IntegerArgumentType.integer(0, 512));
	}

	private static int getRadiusArg(CommandContext<CommandSourceStack> context) {
		return IntegerArgumentType.getInteger(context, "radius_in_blocks");
	}

	private static RequiredArgumentBuilder<CommandSourceStack, Coordinates> anchorArg() {
		return Commands.argument("anchor", new ColumnPosArgument());
	}

	private static ColumnPos getAnchorArg(CommandContext<CommandSourceStack> context) {
		return ColumnPosArgument.getColumnPos(context, "anchor");
	}

	private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> dimArg() {
		return Commands.argument("dimension", DimensionArgument.dimension());
	}

	private static ServerLevel getDimArg(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		return DimensionArgument.getDimension(context, "dimension");
	}

	private static RequiredArgumentBuilder<CommandSourceStack, TeamArgumentProvider> forTeam(ToIntBiFunction<CommandSourceStack, Team> callback) {
		return Commands.argument("team", TeamArgument.create())
				.requires(source -> source.hasPermission(2))
				.executes(context -> callback.applyAsInt(context.getSource(), TeamArgument.get(context, "team")));
	}

	private static ColumnPos toColumn(Vec3 pos) {
		BlockPos blockPos = new BlockPos(pos);
		return new ColumnPos(blockPos.getX(), blockPos.getZ());
	}

	private static Team selfTeam(CommandSourceStack source) throws CommandSyntaxException {
		return FTBTeamsAPI.getPlayerTeam(source.getPlayerOrException());
	}

	private static int resetBlockBreakCounter(CommandSourceStack source, @Nullable Team team) {
		if (team == null) {
			FTBChunksAPI.getManager().getAllTeamData().forEach(FTBChunksTeamData::resetBrokenBlocksCounter);
		} else {
			FTBChunksTeamData data = FTBChunksAPI.getManager().getData(team);
			data.resetBrokenBlocksCounter();
		}
		return 1;
	}
}
