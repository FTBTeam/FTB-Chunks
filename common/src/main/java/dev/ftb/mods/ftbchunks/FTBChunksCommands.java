package dev.ftb.mods.ftbchunks;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.util.UndashedUuid;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.client.gui.EntityIconSettingsScreen;
import dev.ftb.mods.ftbchunks.client.mapicon.EntityIcons;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftbchunks.net.AddWaypointPacket;
import dev.ftb.mods.ftbchunks.net.LoadedChunkViewPacket;
import dev.ftb.mods.ftbchunks.net.OpenClaimGUIPacket;
import dev.ftb.mods.ftbchunks.net.RequestBlockColorPacket;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.data.TeamArgument;
import dev.ftb.mods.ftbteams.data.TeamArgumentProvider;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
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
import net.minecraft.core.GlobalPos;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntBiFunction;

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
                                .executes(context -> info(context.getSource(), new ChunkDimPos(context.getSource().getLevel(), BlockPos.containing(context.getSource().getPosition()))))
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
                                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
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
                                .then(Commands.literal("unclaim_dimension")
                                        .executes(context -> unclaimDimension(context.getSource()))
                                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                .executes(context -> unclaimDimension(context.getSource(), DimensionArgument.getDimension(context, "dimension").dimension()))
                                        )
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
                                .then(Commands.literal("open_claim_gui_as")
                                        .then(Commands.argument("team", TeamArgument.create())
                                                .executes(FTBChunksCommands::openClaimGuiAs)
                                        )
                                )
                        )
                        .then(Commands.literal("block_color")
//						.requires(source -> source.getServer().isSingleplayer())
                                        .executes(context -> {
                                            NetworkManager.sendToPlayer(context.getSource().getPlayerOrException(), new RequestBlockColorPacket());
                                            return Command.SINGLE_SUCCESS;
                                        })
                        )
                        .then(Commands.literal("waypoint")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .then(Commands.argument("position", BlockPosArgument.blockPos())
                                                        .executes(context -> addWaypoint(context.getSource(), StringArgumentType.getString(context, "name"), BlockPosArgument.getBlockPos(context, "position")))
                                                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                                .executes(context -> addWaypoint(context.getSource(), StringArgumentType.getString(context, "name"), DimensionArgument.getDimension(context, "dimension"), BlockPosArgument.getBlockPos(context, "position")))
                                                                .then(Commands.argument("color", ColorArgument.color())
                                                                        .executes(context -> addWaypoint(context.getSource(), StringArgumentType.getString(context, "name"), DimensionArgument.getDimension(context, "dimension"), BlockPosArgument.getBlockPos(context, "position"), ColorArgument.getColor(context, "color")))
                                                                        .then(Commands.argument("gui", BoolArgumentType.bool())
                                                                                .executes(context -> addWaypoint(context.getSource(), StringArgumentType.getString(context, "name"), DimensionArgument.getDimension(context, "dimension"), BlockPosArgument.getBlockPos(context, "position"), ColorArgument.getColor(context, "color"), BoolArgumentType.getBool(context, "gui")))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
        );

        if (FTBChunks.isDevMode()) {
            dispatcher.register(Commands.literal("ftbchunks_dev")
                    .then(Commands.literal("create_gui")
                            .executes(context -> {
                                Minecraft.getInstance().submit(() -> new EntityIconSettingsScreen(true).openGui());
                                return Command.SINGLE_SUCCESS;
                            })
                    )
            );
        }

        dispatcher.register(Commands.literal("chunks").redirect(command));
    }

    private static int addWaypoint(CommandSourceStack source, String name, ServerLevel level, BlockPos position, ChatFormatting color, boolean useGui) throws CommandSyntaxException {
        if (color.getColor() != null) {
            NetworkManager.sendToPlayer(source.getPlayerOrException(), new AddWaypointPacket(name, new GlobalPos(level.dimension(), position), color.getColor(), useGui));
            source.sendSuccess(() -> Component.translatable("ftbchunks.command.waypoint_added", name), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int addWaypoint(CommandSourceStack source, String name, ServerLevel level, BlockPos position, ChatFormatting color) throws CommandSyntaxException {
        return addWaypoint(source, name, level, position, color, false);
    }

    private static int addWaypoint(CommandSourceStack source, String name, BlockPos position) throws CommandSyntaxException {
        int idx = source.getPlayerOrException().getRandom().nextInt(ChatFormatting.values().length);
        return addWaypoint(source, name, source.getLevel(), position, ChatFormatting.values()[idx], false);
    }

    private static int addWaypoint(CommandSourceStack source, String name, ServerLevel level, BlockPos position) throws CommandSyntaxException {
        int idx = source.getPlayerOrException().getRandom().nextInt(ChatFormatting.values().length);
        return addWaypoint(source, name, level, position, ChatFormatting.values()[idx], false);
    }

    private static int bypassProtection(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ClaimedChunkManagerImpl manager = claimManager();
        manager.setBypassProtection(player.getUUID(), !manager.getBypassProtection(player.getUUID()));
        source.sendSuccess(() -> {
            boolean bypassProtection = manager.getBypassProtection(player.getUUID());
            return Component.translatable("ftbchunks.command.bypass_protection_" + (bypassProtection ? "enabled" : "disabled"));
        }, true);
        return Command.SINGLE_SUCCESS;
    }

    private static int openClaimGuiAs(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Team team = TeamArgument.get(context, "team");
        NetworkManager.sendToPlayer(context.getSource().getPlayerOrException(), new OpenClaimGUIPacket(team.getTeamId()));
        return Command.SINGLE_SUCCESS;
    }

    private interface ChunkCallback {
        void accept(ChunkTeamDataImpl data, ChunkDimPos pos) throws CommandSyntaxException;
    }

    private static void forEachChunk(CommandSourceStack source, int r, ChunkCallback callback) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Team team = FTBTeamsAPI.api().getManager().getTeamForPlayer(player).orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(player.getId()));
        forEachChunk(team, source.getLevel(), toColumn(source.getPosition()), r, callback);
    }

    private static void forEachChunk(Team team, Level level, ColumnPos anchor, int r, ChunkCallback callback) throws CommandSyntaxException {
        ChunkTeamDataImpl data = claimManager().getOrCreateData(team);
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

        list.sort(Comparator.comparingDouble(cdp -> MathUtils.distSq(ox, oz, cdp.x(), cdp.z())));

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

        source.sendSuccess(() -> Component.translatable("ftbchunks.commands.claimed", success[0]), false);

        ChunkDimPos origin = new ChunkDimPos(level.dimension(), anchor.x() >> 4, anchor.z() >> 4);
        FTBChunks.LOGGER.info(source.getTextName() + " claimed " + success[0] + " chunks around " + origin + "for team " + team.getShortName());

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

        source.sendSuccess(() -> Component.translatable("ftbchunks.commands.unclaimed", success[0]), false);

        ChunkDimPos origin = new ChunkDimPos(level.dimension(), anchor.x() >> 4, anchor.z() >> 4);
        FTBChunks.LOGGER.info(source.getTextName() + " unclaimed " + success[0] + " chunks around " + origin + "for team " + team.getShortName());

        return success[0];
    }

    private static int load(CommandSourceStack source, int r) throws CommandSyntaxException {
        int[] success = new int[1];

        forEachChunk(source, r, (data, pos) -> {
            ClaimResult result = data.forceLoad(source, pos, false);
            if (result.isSuccess()) {
                success[0]++;
            }
        });

        source.sendSuccess(() -> Component.translatable("ftbchunks.commands.force_loaded", success[0]), false);
        FTBChunks.LOGGER.info(source.getTextName() + " loaded " + success[0] + " chunks at " + new ChunkDimPos(source.getPlayerOrException()));
        return success[0];
    }

    private static int unload(CommandSourceStack source, int r) throws CommandSyntaxException {
        int[] success = new int[1];

        forEachChunk(source, r, (data, pos) -> {
            if (data.unForceLoad(source, pos, false).isSuccess()) {
                success[0]++;
            }
        });

        source.sendSuccess(() -> Component.translatable("ftbchunks.command.unloaded", success[0]), false);
        FTBChunks.LOGGER.info(source.getTextName() + " unloaded " + success[0] + " chunks at " + new ChunkDimPos(source.getPlayerOrException()));
        return success[0];
    }

    private static int unclaimAll(CommandSourceStack source, Team team) {
        ChunkTeamDataImpl data = claimManager().getOrCreateData(team);

        int unclaimed = 0;
        for (ClaimedChunkImpl c : new ArrayList<>(data.getClaimedChunks())) {
            ClaimResult unclaim = data.unclaim(source, c.getPos(), false);
            if (unclaim.isSuccess()) {
                unclaimed++;
            }
        }
        data.markDirty();
        int finalUnclaimed = unclaimed;
        source.sendSuccess(() -> Component.translatable("ftbchunks.commands.unclaimed", finalUnclaimed), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int unloadAll(CommandSourceStack source, Team team) {
        ChunkTeamDataImpl data = claimManager().getOrCreateData(team);

        int unloaded = 0;
        for (ClaimedChunk c : new ArrayList<>(data.getClaimedChunks())) {
            ClaimResult claimResult = data.unForceLoad(source, c.getPos(), false);
            if (claimResult.isSuccess()) {
                unloaded++;
            }
        }
        data.markDirty();
        int finalUnloaded = unloaded;
        source.sendSuccess(() -> Component.translatable("ftbchunks.command.unloaded", finalUnloaded), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int info(CommandSourceStack source, ChunkDimPos pos) throws CommandSyntaxException {

        ClaimedChunk chunk = claimManager().getChunk(pos);

        boolean canKnowChunkStatus = source.hasPermission(Commands.LEVEL_GAMEMASTERS) || (source.isPlayer() || (chunk != null && chunk.getTeamData().canPlayerUse(source.getPlayerOrException(), FTBChunksProperties.CLAIM_VISIBILITY)));

        if (canKnowChunkStatus) {
            if (chunk == null) {
                source.sendSuccess(() -> Component.translatable("ftbchunks.commands.not_claimed"), false);
                return Command.SINGLE_SUCCESS;
            }

            source.sendSuccess(() -> {
                Component owner = chunk.getTeamData().getTeam().getColoredName().copy().append(" / " + UndashedUuid.toString(chunk.getTeamData().getTeam().getId()));
                String location = pos.dimension().location().toString() + " [" + pos.x() + ", " + pos.z() + "]";
                return Component.translatable("ftbchunks.commands.location", location)
                        .append(Component.literal("\n"))
                        .append(Component.translatable("ftbchunks.commands.owner").append(owner.getString()))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("ftbchunks.commands.is_force_loaded", chunk.isForceLoaded()));
            }, true);

            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.translatable("ftbchunks.commands.cant_determined"), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int getExtraClaimChunks(CommandSourceStack source, ServerPlayer player) {
        ChunkTeamData personalData = claimManager().getPersonalData(player.getUUID());
        if (personalData == null) {
            source.sendFailure(Component.translatable("ftbchunks.commands.no_personal_info", player.getDisplayName()));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("ftbchunks.command.extra_chunks_claimed", player.getDisplayName(), personalData.getExtraClaimChunks()), true);
        return personalData.getExtraClaimChunks();
    }

    private static int setExtraClaimChunks(CommandSourceStack source, ServerPlayer player, int extra, boolean adding) {
        // limit is added to player's *personal* data, even if they're in a party
        ChunkTeamDataImpl personalData = claimManager().getPersonalData(player.getUUID());
        if (personalData == null) {
            source.sendFailure(Component.translatable("ftbchunks.commands.no_personal_info", player.getDisplayName()));
            return 0;
        }
        personalData.setExtraClaimChunks(Math.max(0, extra + (adding ? personalData.getExtraClaimChunks() : 0)));
        personalData.markDirty();

        // player's new personal limit will affect the team limit
        ChunkTeamDataImpl teamData = claimManager().getOrCreateData(player);
        teamData.updateLimits();
        SendGeneralDataPacket.send(teamData, player);

        source.sendSuccess(() -> Component.translatable("ftbchunks.command.set_extra_chunks_claimed", personalData.getExtraClaimChunks(), player.getDisplayName()), true);
        return personalData.getExtraClaimChunks();
    }

    private static int getExtraForceLoadChunks(CommandSourceStack source, ServerPlayer player) {
        ChunkTeamData personalData = claimManager().getPersonalData(player.getUUID());
        if (personalData == null) {
            source.sendFailure(Component.translatable("ftbchunks.commands.no_personal_info", player.getDisplayName()));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("ftbchunks.command.extra_forceload_chunks", player.getDisplayName(), personalData.getExtraForceLoadChunks()), true);
        return personalData.getExtraForceLoadChunks();
    }

    private static int setExtraForceLoadChunks(CommandSourceStack source, ServerPlayer player, int extra, boolean adding) {
        // limit is added to player's *personal* data, even if they're in a party
        ChunkTeamDataImpl personalData = claimManager().getPersonalData(player.getUUID());
        if (personalData == null) {
            source.sendFailure(Component.translatable("ftbchunks.commands.no_personal_info", player.getDisplayName()));
            return 0;
        }
        personalData.setExtraForceLoadChunks(Math.max(0, extra + (adding ? personalData.getExtraForceLoadChunks() : 0)));
        personalData.markDirty();

        // player's new personal limit will affect the team limit
        ChunkTeamDataImpl teamData = claimManager().getOrCreateData(player);
        teamData.updateLimits();
        SendGeneralDataPacket.send(teamData, player);

        source.sendSuccess(() -> Component.translatable("ftbchunks.command.set_extra_forceload_chunks", personalData.getExtraForceLoadChunks(), player.getDisplayName()), true);
        return personalData.getExtraForceLoadChunks();
    }

    private static int unclaimEverything(CommandSourceStack source) {
        int claimedChunks = 0;
        for (ClaimedChunk c : new ArrayList<>(claimManager().getAllClaimedChunks())) {
            c.getTeamData().unclaim(source, c.getPos(), false);
            claimedChunks++;
        }
        int finalClaimedChunks = claimedChunks;
        source.sendSuccess(() -> Component.translatable("ftbchunks.commands.unclaimed", finalClaimedChunks), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int unclaimDimension(CommandSourceStack source, ResourceKey<Level> dim) {
        int claimedChunks = 0;
        for (ClaimedChunk c : new ArrayList<>(claimManager().getAllClaimedChunks())) {
            if (source.getLevel().dimension().equals(dim)) {
                c.getTeamData().unclaim(source, c.getPos(), false);
                claimedChunks++;
            }
        }
        int finalClaimedChunks = claimedChunks;
        source.sendSuccess(() -> Component.translatable("ftbchunks.commands.unclaimed", finalClaimedChunks), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int unclaimDimension(CommandSourceStack source) {
        return unclaimDimension(source, source.getLevel().dimension());
    }

    private static int unloadEverything(CommandSourceStack source) {
        int unloadedChunks = 0;
        int totalChunks = 0;
        for (ClaimedChunk c : claimManager().getAllClaimedChunks()) {
            if (c.isForceLoaded()) {
                c.getTeamData().unForceLoad(source, c.getPos(), false);
                unloadedChunks++;
            }
            totalChunks++;
        }
        final int finalUnloadedChunks = unloadedChunks;
        final int finalTotalChunks = totalChunks;
        source.sendSuccess(() -> Component.translatable("ftbchunks.commands.unloaded", finalUnloadedChunks, finalTotalChunks), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int viewLoadedChunks(CommandSourceStack source, ServerLevel level) throws CommandSyntaxException {
        var chunks = new Long2IntOpenHashMap();

        for (ChunkHolder holder : level.getChunkSource().chunkMap.updatingChunkMap.values()) {
            chunks.put(holder.getPos().toLong(), LoadedChunkViewPacket.LOADED);
        }

        var map = claimManager().getForceLoadedChunks(level.dimension());
        for (long pos : map.keySet()) {
            if (chunks.get(pos) == LoadedChunkViewPacket.LOADED) {
                chunks.put(pos, LoadedChunkViewPacket.FORCE_LOADED);
            }
        }

        for (long pos : level.getForcedChunks()) {
            if (chunks.get(pos) == LoadedChunkViewPacket.LOADED) {
                chunks.put(pos, LoadedChunkViewPacket.FORCE_LOADED);
            }
        }

        source.sendSuccess(() -> Component.translatable("ftbchunks.command.view_loaded", chunks.size()), false);
        NetworkManager.sendToPlayer(source.getPlayerOrException(), new LoadedChunkViewPacket(level.dimension(), chunks));

        return Command.SINGLE_SUCCESS;
    }

    private static int resetLoadedChunks(CommandSourceStack source, ServerLevel level) throws CommandSyntaxException {
        NetworkManager.sendToPlayer(source.getPlayerOrException(), new LoadedChunkViewPacket(level.dimension(), Long2IntMaps.EMPTY_MAP));
        return Command.SINGLE_SUCCESS;
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
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> callback.applyAsInt(context.getSource(), TeamArgument.get(context, "team")));
    }

    private static ColumnPos toColumn(Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        return new ColumnPos(blockPos.getX(), blockPos.getZ());
    }

    private static Team selfTeam(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return FTBTeamsAPI.api().getManager().getTeamForPlayer(player).orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(player.getId()));
    }

    private static ClaimedChunkManagerImpl claimManager() {
        return ClaimedChunkManagerImpl.getInstance();
    }
}
