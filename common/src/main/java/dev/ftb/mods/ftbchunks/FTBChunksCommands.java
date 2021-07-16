package dev.ftb.mods.ftbchunks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.util.UUIDTypeAdapter;
import dev.ftb.mods.ftbchunks.data.ClaimResult;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import dev.ftb.mods.ftbteams.data.TeamArgument;
import dev.ftb.mods.ftbteams.data.TeamArgumentProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntBiFunction;

/**
 * @author LatvianModder
 */
public class FTBChunksCommands {
	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection selection) {
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
								.executes(context -> bypassProtection(context.getSource().getPlayerOrException()))
						)
						.then(Commands.literal("extra_claim_chunks")
								.then(Commands.argument("player", EntityArgument.player())
										.then(Commands.literal("get")
												.executes(context -> getExtraClaimChunks(context.getSource(), EntityArgument.getPlayer(context, "player")))
										)
										.then(Commands.literal("set")
												.then(Commands.argument("number", IntegerArgumentType.integer(0))
														.executes(context -> setExtraClaimChunks(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "number")))
												)
										)
										.then(Commands.literal("add")
												.then(Commands.argument("number", IntegerArgumentType.integer())
														.executes(context -> addExtraClaimChunks(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "number")))
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
														.executes(context -> setExtraForceLoadChunks(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "number")))
												)
										)
										.then(Commands.literal("add")
												.then(Commands.argument("number", IntegerArgumentType.integer())
														.executes(context -> addExtraForceLoadChunks(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "number")))
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
				)
				.then(Commands.literal("block_color")
						.requires(source -> source.getServer().isSingleplayer())
						.executes(context -> FTBChunks.PROXY.blockColor())
				)
		);

		dispatcher.register(Commands.literal("chunks").redirect(command));
	}

	private static int bypassProtection(ServerPlayer player) {
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(player);
		data.bypassProtection = !data.bypassProtection;
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
		int ox = Mth.floor(anchor.x) >> 4;
		int oz = Mth.floor(anchor.z) >> 4;
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
		long now = System.currentTimeMillis();

		forEachChunk(team, level, anchor, r, (data, pos) -> {
			ClaimResult result = data.claim(source, pos, false);

			if (result.isSuccess()) {
				result.setClaimedTime(now);
				success[0]++;
			}
		});

		source.sendSuccess(new TextComponent("Claimed " + success[0] + " chunks!"), false);

		ChunkDimPos origin = new ChunkDimPos(level.dimension(), anchor.x >> 4, anchor.z >> 4);
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

		source.sendSuccess(new TextComponent("Unclaimed " + success[0] + " chunks!"), false);

		ChunkDimPos origin = new ChunkDimPos(level.dimension(), anchor.x >> 4, anchor.z >> 4);
		FTBChunks.LOGGER.info(source.getTextName() + " unclaimed " + success[0] + " chunks around " + origin + "for team " + team.getDisplayName());

		return success[0];
	}

	private static int load(CommandSourceStack source, int r) throws CommandSyntaxException {
		int[] success = new int[1];
		long now = System.currentTimeMillis();

		forEachChunk(source, r, (data, pos) -> {
			ClaimResult result = data.load(source, pos, false);

			if (result.isSuccess()) {
				result.setForceLoadedTime(now);
				success[0]++;
			}
		});

		source.sendSuccess(new TextComponent("Loaded " + success[0] + " chunks!"), false);
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

		source.sendSuccess(new TextComponent("Unloaded " + success[0] + " chunks!"), false);
		FTBChunks.LOGGER.info(source.getTextName() + " unloaded " + success[0] + " chunks at " + new ChunkDimPos(source.getPlayerOrException()));
		return success[0];
	}

	private static int unclaimAll(CommandSourceStack source, Team team) {
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(team);

		for (ClaimedChunk c : data.getClaimedChunks()) {
			data.unclaim(source, c.getPos(), false);
		}
		data.save();

		return 1;
	}

	private static int unloadAll(CommandSourceStack source, Team team) {
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(team);

		for (ClaimedChunk c : data.getClaimedChunks()) {
			data.unload(source, c.getPos(), false);
		}
		data.save();

		return 1;
	}

	private static int info(CommandSourceStack source, ChunkDimPos pos) {
		source.sendSuccess(new TextComponent("Location: " + pos), true);

		ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(pos);

		if (chunk == null) {
			source.sendSuccess(new TextComponent("Chunk not claimed!"), true);
			return 0;
		}

		source.sendSuccess(new TextComponent("Owner: ").append(chunk.getTeamData().getTeam().getColoredName()).append(" / " + UUIDTypeAdapter.fromUUID(chunk.getTeamData().getTeamId())), true);

		if (source.hasPermission(2)) {
			source.sendSuccess(new TextComponent("Force Loaded: " + chunk.isForceLoaded()), true);
		}

		return 1;
	}

	private static int getExtraClaimChunks(CommandSourceStack source, ServerPlayer player) {
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraClaimChunks), false);
		return 1;
	}

	private static int setExtraClaimChunks(CommandSourceStack source, ServerPlayer player, int i) {
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(player);
		data.extraClaimChunks = Math.max(0, i);
		data.save();
		SendGeneralDataPacket.send(data, player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraClaimChunks), false);
		return 1;
	}

	private static int addExtraClaimChunks(CommandSourceStack source, ServerPlayer player, int i) {
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(player);
		data.extraClaimChunks = Math.max(0, data.extraClaimChunks + i);
		data.save();
		SendGeneralDataPacket.send(data, player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraClaimChunks), false);
		return 1;
	}

	private static int getExtraForceLoadChunks(CommandSourceStack source, ServerPlayer player) {
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraForceLoadChunks), false);
		return 1;
	}

	private static int setExtraForceLoadChunks(CommandSourceStack source, ServerPlayer player, int i) {
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(player);
		data.extraForceLoadChunks = Math.max(0, i);
		data.save();
		SendGeneralDataPacket.send(data, player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraForceLoadChunks), false);
		return 1;
	}

	private static int addExtraForceLoadChunks(CommandSourceStack source, ServerPlayer player, int i) {
		FTBChunksTeamData data = FTBChunksAPI.getManager().getData(player);
		data.extraForceLoadChunks = Math.max(0, data.extraForceLoadChunks + i);
		data.save();
		SendGeneralDataPacket.send(data, player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraForceLoadChunks), false);
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
		return new ColumnPos(new BlockPos(pos));
	}

	private static Team selfTeam(CommandSourceStack source) throws CommandSyntaxException {
		return FTBTeamsAPI.getPlayerTeam(source.getPlayerOrException());
	}
}