package dev.ftb.mods.ftbchunks;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.util.UUIDTypeAdapter;

import dev.ftb.mods.ftbchunks.data.ChunkDimPos;
import dev.ftb.mods.ftbchunks.data.ClaimResult;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkTeamData;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.LevelDataDirectory;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftbguilibrary.utils.MathUtils;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public class FTBChunksCommands {
	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection selection) {
		LiteralCommandNode<CommandSourceStack> command = dispatcher.register(Commands.literal("ftbchunks")
				.then(Commands.literal("claim")
						.executes(context -> claim(context.getSource(), 0))
						.then(Commands.argument("radius_in_blocks", IntegerArgumentType.integer(0, 200))
								.executes(context -> claim(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))
						)
				)
				.then(Commands.literal("unclaim")
						.executes(context -> unclaim(context.getSource(), 0))
						.then(Commands.argument("radius_in_blocks", IntegerArgumentType.integer(0, 200))
								.executes(context -> unclaim(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))
						)
				)
				.then(Commands.literal("load")
						.executes(context -> load(context.getSource(), 0))
						.then(Commands.argument("radius_in_blocks", IntegerArgumentType.integer(0, 200))
								.executes(context -> load(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))
						)
				)
				.then(Commands.literal("unload")
						.executes(context -> unload(context.getSource(), 0))
						.then(Commands.argument("radius_in_blocks", IntegerArgumentType.integer(0, 200))
								.executes(context -> unload(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))
						)
				)
				.then(Commands.literal("unclaim_all")
						.then(Commands.argument("players", GameProfileArgument.gameProfile())
								.requires(source -> source.hasPermission(2))
								.executes(context -> unclaimAll(context.getSource(), GameProfileArgument.getGameProfiles(context, "players")))
						)
						.executes(context -> unclaimAll(context.getSource(), Collections.singleton(context.getSource().getPlayerOrException().getGameProfile())))
				)
				.then(Commands.literal("unload_all")
						.then(Commands.argument("players", GameProfileArgument.gameProfile())
								.requires(source -> source.hasPermission(2))
								.executes(context -> unloadAll(context.getSource(), GameProfileArgument.getGameProfiles(context, "players")))
						)
						.executes(context -> unloadAll(context.getSource(), Collections.singleton(context.getSource().getPlayerOrException().getGameProfile())))
				)
				.then(Commands.literal("info")
						.executes(context -> info(context.getSource(), new ChunkDimPos(context.getSource().getLevel(), new BlockPos(context.getSource().getPosition()))))
						.then(Commands.argument("x", IntegerArgumentType.integer())
								.then(Commands.argument("z", IntegerArgumentType.integer())
										.executes(context -> info(context.getSource(), new ChunkDimPos(context.getSource().getLevel(), new BlockPos(IntegerArgumentType.getInteger(context, "x"), 0, IntegerArgumentType.getInteger(context, "z")))))
										.then(Commands.argument("dimension", DimensionArgument.dimension())
												.executes(context -> info(context.getSource(), new ChunkDimPos(DimensionArgument.getDimension(context, "dimension").dimension(), IntegerArgumentType.getInteger(context, "x") >> 4, IntegerArgumentType.getInteger(context, "z") >> 4)))
										)
								)
						)
				)
				.then(Commands.literal("allow_fake_players")
						.then(Commands.argument("allow", BoolArgumentType.bool())
								.executes(context -> allowFakePlayers(context.getSource().getPlayerOrException(), BoolArgumentType.getBool(context, "allow")))
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
						).then(Commands.literal("prune")
							.then(Commands.literal("region_files_with_no_claimed_chunks")
								.executes(context -> prune(context.getSource(), null, "region", false))
								.then(Commands.literal("dimension")
									.then(Commands.argument("dim", DimensionArgument.dimension())
										.executes(context -> prune(context.getSource(), DimensionArgument.getDimension(context, "dim"), "region", false))
										.then(Commands.literal("do_not_backup")
											.then(Commands.argument("doNotBackup", BoolArgumentType.bool())
												.executes(context -> prune(context.getSource(), DimensionArgument.getDimension(context, "dim"), "region", BoolArgumentType.getBool(context, "doNotBackup")))
											)
										)
									)
								)
							)
							.then(Commands.literal("poi_files_with_no_claimed_chunks")
								.executes(context -> prune(context.getSource(), null, "poi", false))
								.then(Commands.literal("dimension")
									.then(Commands.argument("dim", DimensionArgument.dimension())
										.executes(context -> prune(context.getSource(), DimensionArgument.getDimension(context, "dim"), "poi", false))
										.then(Commands.literal("do_not_backup")
											.then(Commands.argument("doNotBackup", BoolArgumentType.bool())
												.executes(context -> prune(context.getSource(), DimensionArgument.getDimension(context, "dim"), "poi", BoolArgumentType.getBool(context, "doNotBackup")))
											)
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
		ClaimedChunkTeamData data = FTBChunksAPI.getManager().getData(player);
		data.bypassProtection = !data.bypassProtection;
		return 1;
	}

	private interface ChunkCallback {

		void accept(ClaimedChunkTeamData data, ChunkDimPos pos) throws CommandSyntaxException;
	}

	private static void forEachChunk(CommandSourceStack source, int r, ChunkCallback callback) throws CommandSyntaxException {
		ClaimedChunkTeamData data = FTBChunksAPI.getManager().getData(source.getPlayerOrException());
		ResourceKey<Level> dimId = source.getLevel().dimension();
		int ox = Mth.floor(source.getPosition().x) >> 4;
		int oz = Mth.floor(source.getPosition().z) >> 4;
		List<ChunkDimPos> list = new ArrayList<>();

		r = Math.max(1, r >> 4);

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

	private static int claim(CommandSourceStack source, int r) throws CommandSyntaxException {
		int[] success = new int[1];
		Instant time = Instant.now();

		forEachChunk(source, r, (data, pos) -> {
			ClaimResult result = data.claim(source, pos, false);

			if (result.isSuccess()) {
				result.setClaimedTime(time);
				success[0]++;
			}
		});

		source.sendSuccess(new TextComponent("Claimed " + success[0] + " chunks!"), false);
		FTBChunks.LOGGER.info(source.getTextName() + " claimed " + success[0] + " chunks at " + new ChunkDimPos(source.getPlayerOrException()));
		return success[0];
	}

	private static int unclaim(CommandSourceStack source, int r) throws CommandSyntaxException {
		int[] success = new int[1];

		forEachChunk(source, r, (data, pos) -> {
			if (data.unclaim(source, pos, false).isSuccess()) {
				success[0]++;
			}
		});

		source.sendSuccess(new TextComponent("Unclaimed " + success[0] + " chunks!"), false);
		FTBChunks.LOGGER.info(source.getTextName() + " unclaimed " + success[0] + " chunks at " + new ChunkDimPos(source.getPlayerOrException()));
		return success[0];
	}

	private static int load(CommandSourceStack source, int r) throws CommandSyntaxException {
		int[] success = new int[1];
		Instant time = Instant.now();

		forEachChunk(source, r, (data, pos) -> {
			ClaimResult result = data.load(source, pos, false);

			if (result.isSuccess()) {
				result.setForceLoadedTime(time);
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

	private static int unclaimAll(CommandSourceStack source, Collection<GameProfile> players) {
		for (GameProfile profile : players) {
			ClaimedChunkTeamData data = FTBChunksAPI.getManager().getData(FTBTeamsAPI.getPlayerTeam(profile.getId()));

			if (data != null) {
				for (ClaimedChunk c : data.getClaimedChunks()) {
					data.unclaim(source, c.getPos(), false);
				}

				data.save();
			}
		}

		return 1;
	}

	private static int unloadAll(CommandSourceStack source, Collection<GameProfile> players) {
		for (GameProfile profile : players) {
			Team team = FTBTeamsAPI.getPlayerTeam(profile.getId());

			if (team != null) {
				ClaimedChunkTeamData data = FTBChunksAPI.getManager().getData(team);

				for (ClaimedChunk c : data.getClaimedChunks()) {
					data.unload(source, c.getPos(), false);
				}
			}
		}

		return 1;
	}

	private static int info(CommandSourceStack source, ChunkDimPos pos) {
		source.sendSuccess(new TextComponent("Location: " + pos), true);

		ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(pos);

		if (chunk == null) {
			source.sendSuccess(new TextComponent("Chunk not claimed!"), true);
			return 0;
		}

		source.sendSuccess(new TextComponent("Owner: ").append(chunk.getPlayerData().getDisplayName()).append(" / " + UUIDTypeAdapter.fromUUID(chunk.getPlayerData().getTeamId())), true);

		if (source.hasPermission(2)) {
			source.sendSuccess(new TextComponent("Force Loaded: " + chunk.isForceLoaded()), true);
		}

		return 1;
	}

	private static int allowFakePlayers(ServerPlayer player, boolean allow) {
		ClaimedChunkTeamData data = FTBChunksAPI.getManager().getData(player);
		data.allowFakePlayers = allow;
		data.save();
		player.sendMessage(new TextComponent("Set fake player interactions to " + allow), Util.NIL_UUID);
		return 1;
	}

	private static int getExtraClaimChunks(CommandSourceStack source, ServerPlayer player) {
		ClaimedChunkTeamData data = FTBChunksAPI.getManager().getData(player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraClaimChunks), false);
		return 1;
	}

	private static int setExtraClaimChunks(CommandSourceStack source, ServerPlayer player, int i) {
		ClaimedChunkTeamData data = FTBChunksAPI.getManager().getData(player);
		data.extraClaimChunks = Math.max(0, i);
		data.save();
		SendGeneralDataPacket.send(data, player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraClaimChunks), false);
		return 1;
	}

	private static int addExtraClaimChunks(CommandSourceStack source, ServerPlayer player, int i) {
		ClaimedChunkTeamData data = FTBChunksAPI.getManager().getData(player);
		data.extraClaimChunks = Math.max(0, data.extraClaimChunks + i);
		data.save();
		SendGeneralDataPacket.send(data, player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraClaimChunks), false);
		return 1;
	}

	private static int getExtraForceLoadChunks(CommandSourceStack source, ServerPlayer player) {
		ClaimedChunkTeamData data = FTBChunksAPI.getManager().getData(player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraForceLoadChunks), false);
		return 1;
	}

	private static int setExtraForceLoadChunks(CommandSourceStack source, ServerPlayer player, int i) {
		ClaimedChunkTeamData data = FTBChunksAPI.getManager().getData(player);
		data.extraForceLoadChunks = Math.max(0, i);
		data.save();
		SendGeneralDataPacket.send(data, player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraForceLoadChunks), false);
		return 1;
	}

	private static int addExtraForceLoadChunks(CommandSourceStack source, ServerPlayer player, int i) {
		ClaimedChunkTeamData data = FTBChunksAPI.getManager().getData(player);
		data.extraForceLoadChunks = Math.max(0, data.extraForceLoadChunks + i);
		data.save();
		SendGeneralDataPacket.send(data, player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraForceLoadChunks), false);
		return 1;
	}
	private static int prune(CommandSourceStack source, @Nullable ServerLevel level, String subDirectory, boolean doNotBackup){
		ResourceKey<Level> levelKey = level==null? Level.OVERWORLD: level.dimension();
		String levelDataPath = LevelDataDirectory.getDirectoryFromDimensionKey(levelKey, subDirectory);
		String backupPath = doNotBackup? null: levelDataPath + "pruned/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + "/";

		Collection<String> namesOfRegionFilesWithClaimedChunks = FTBChunksAPI.getManager().getNamesOfRegionFilesWithClaimedChunks(levelKey);
		if(FTBChunksAPI.getManager().pruneRegionFiles(levelDataPath, backupPath, namesOfRegionFilesWithClaimedChunks, !doNotBackup)){
			source.sendSuccess(new TextComponent(doNotBackup? "Pruned successfuly!": "Completed successfuly!, pruned files backed up to: " + backupPath), false);
			return 1;
		}
		source.sendFailure(new TextComponent("Failed to prune, check the log for details."));
		return 1;
	}
}