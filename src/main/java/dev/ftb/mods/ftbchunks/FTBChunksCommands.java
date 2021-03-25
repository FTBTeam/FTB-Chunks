package dev.ftb.mods.ftbchunks;

import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.util.UUIDTypeAdapter;
import dev.ftb.mods.ftbchunks.api.ChunkDimPos;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkPlayerData;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.impl.ClaimedChunkPlayerDataImpl;
import dev.ftb.mods.ftbchunks.impl.FTBChunksAPIImpl;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author LatvianModder
 */
@Mod.EventBusSubscriber
public class FTBChunksCommands {
	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		LiteralCommandNode<CommandSourceStack> command = event.getDispatcher().register(Commands.literal("ftbchunks")
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
				)
				.then(Commands.literal("block_color")
						.requires(source -> source.getServer().isSingleplayer())
						.executes(context -> FTBChunks.instance.proxy.blockColor())
				)
		);

		event.getDispatcher().register(Commands.literal("chunks").redirect(command));
	}

	private static int bypassProtection(ServerPlayer player) {
		ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(player);
		data.bypassProtection = !data.bypassProtection;
		return 1;
	}

	private interface ChunkCallback {
		void accept(ClaimedChunkPlayerData data, ChunkDimPos pos) throws CommandSyntaxException;
	}

	private static void forEachChunk(CommandSourceStack source, int r, ChunkCallback callback) throws CommandSyntaxException {
		ClaimedChunkPlayerData data = FTBChunksAPI.INSTANCE.getManager().getData(source.getPlayerOrException());
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
			ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.playerData.get(profile.getId());

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
			ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.playerData.get(profile.getId());

			if (data != null) {
				for (ClaimedChunk c : data.getClaimedChunks()) {
					data.unload(source, c.getPos(), false);
				}
			}
		}

		return 1;
	}

	private static int info(CommandSourceStack source, ChunkDimPos pos) {
		source.sendSuccess(new TextComponent("Location: " + pos), true);

		ClaimedChunk chunk = FTBChunksAPIImpl.manager.getChunk(pos);

		if (chunk == null) {
			source.sendSuccess(new TextComponent("Chunk not claimed!"), true);
			return 0;
		}

		source.sendSuccess(new TextComponent("Owner: " + chunk.getPlayerData().getName() + " / " + UUIDTypeAdapter.fromUUID(chunk.getPlayerData().getUuid())), true);

		if (source.hasPermission(2)) {
			source.sendSuccess(new TextComponent("Force Loaded: " + chunk.isForceLoaded()), true);
		}

		return 1;
	}

	private static int getExtraClaimChunks(CommandSourceStack source, ServerPlayer player) {
		ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraClaimChunks), false);
		return 1;
	}

	private static int setExtraClaimChunks(CommandSourceStack source, ServerPlayer player, int i) {
		ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(player);
		data.extraClaimChunks = Math.max(0, i);
		data.save();
		SendGeneralDataPacket.send(data, player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraClaimChunks), false);
		return 1;
	}

	private static int addExtraClaimChunks(CommandSourceStack source, ServerPlayer player, int i) {
		ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(player);
		data.extraClaimChunks = Math.max(0, data.extraClaimChunks + i);
		data.save();
		SendGeneralDataPacket.send(data, player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraClaimChunks), false);
		return 1;
	}

	private static int getExtraForceLoadChunks(CommandSourceStack source, ServerPlayer player) {
		ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraForceLoadChunks), false);
		return 1;
	}

	private static int setExtraForceLoadChunks(CommandSourceStack source, ServerPlayer player, int i) {
		ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(player);
		data.extraForceLoadChunks = Math.max(0, i);
		data.save();
		SendGeneralDataPacket.send(data, player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraForceLoadChunks), false);
		return 1;
	}

	private static int addExtraForceLoadChunks(CommandSourceStack source, ServerPlayer player, int i) {
		ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(player);
		data.extraForceLoadChunks = Math.max(0, data.extraForceLoadChunks + i);
		data.save();
		SendGeneralDataPacket.send(data, player);
		source.sendSuccess(new TextComponent("").append(player.getDisplayName()).append(" == " + data.extraForceLoadChunks), false);
		return 1;
	}
}