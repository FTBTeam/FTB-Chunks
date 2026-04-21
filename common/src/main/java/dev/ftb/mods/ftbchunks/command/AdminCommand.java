package dev.ftb.mods.ftbchunks.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftbchunks.net.LoadedChunkViewPacket;
import dev.ftb.mods.ftbchunks.net.OpenClaimGUIPacket;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftblibrary.platform.network.Server2PlayNetworking;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.command.TeamArgument;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

import static dev.ftb.mods.ftbchunks.command.FTBChunksCommands.*;

public class AdminCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("admin")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("bypass_protection")
                        .executes(context -> bypassProtection(context.getSource()))
                )
                .then(Commands.literal("extra_claim_chunks")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("get")
                                        .executes(context -> getExtraClaimChunks(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player"))
                                        )
                                )
                                .then(Commands.literal("set")
                                        .then(Commands.argument("number", IntegerArgumentType.integer(0))
                                                .executes(context -> setExtraClaimChunks(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "number"),
                                                        false)
                                                )
                                        )
                                )
                                .then(Commands.literal("add")
                                        .then(Commands.argument("number", IntegerArgumentType.integer())
                                                .executes(context -> setExtraClaimChunks(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "number"),
                                                        true)
                                                )
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
                                .executes(context -> ClaimCommand.claim(context.getSource(), TeamArgument.get(context, "team"), 0))
                                .then(radiusArg()
                                        .executes(context -> ClaimCommand.claim(context.getSource(), TeamArgument.get(context, "team"), getRadiusArg(context)))
                                        .then(anchorArg()
                                                .executes(context -> ClaimCommand.claim(context.getSource(), TeamArgument.get(context, "team"), getRadiusArg(context), getAnchorArg(context), context.getSource().getLevel()))
                                                .then(dimArg()
                                                        .executes(context -> ClaimCommand.claim(context.getSource(), TeamArgument.get(context, "team"), getRadiusArg(context), getAnchorArg(context), getDimArg(context)))
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("unclaim_as")
                        .then(Commands.argument("team", TeamArgument.create())
                                .executes(context -> UnclaimCommand.unclaim(context.getSource(), TeamArgument.get(context, "team"), 0))
                                .then(radiusArg()
                                        .executes(context -> UnclaimCommand.unclaim(context.getSource(), TeamArgument.get(context, "team"), getRadiusArg(context)))
                                        .then(anchorArg()
                                                .executes(context -> UnclaimCommand.unclaim(context.getSource(), TeamArgument.get(context, "team"), getRadiusArg(context), getAnchorArg(context), context.getSource().getLevel()))
                                                .then(dimArg()
                                                        .executes(context -> UnclaimCommand.unclaim(context.getSource(), TeamArgument.get(context, "team"), getRadiusArg(context), getAnchorArg(context), getDimArg(context)))
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
                                .executes(AdminCommand::openClaimGuiAs)
                        )
                );
    }

    private static int bypassProtection(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ClaimedChunkManagerImpl manager = FTBChunksCommands.claimManager();
        manager.setBypassProtection(player.getUUID(), !manager.getBypassProtection(player.getUUID()));
        source.sendSuccess(() -> {
            boolean bypassProtection = manager.getBypassProtection(player.getUUID());
            return Component.translatable("ftbchunks.command.bypass_protection_" + (bypassProtection ? "enabled" : "disabled"));
        }, true);
        return Command.SINGLE_SUCCESS;
    }

    private static int openClaimGuiAs(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Team team = TeamArgument.get(context, "team");
        Server2PlayNetworking.send(context.getSource().getPlayerOrException(), new OpenClaimGUIPacket(team.getTeamId()));
        return Command.SINGLE_SUCCESS;
    }

    private static int getExtraClaimChunks(CommandSourceStack source, ServerPlayer player) {
        ChunkTeamData personalData = FTBChunksCommands.claimManager().getPersonalData(player.getUUID());
        if (personalData == null) {
            source.sendFailure(Component.translatable("ftbchunks.commands.no_personal_info", player.getDisplayName()));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("ftbchunks.command.extra_chunks_claimed", player.getDisplayName(), personalData.getExtraClaimChunks()), true);
        return personalData.getExtraClaimChunks();
    }

    private static int setExtraClaimChunks(CommandSourceStack source, ServerPlayer player, int extra, boolean adding) {
        // limit is added to player's *personal* data, even if they're in a party
        ChunkTeamDataImpl personalData = FTBChunksCommands.claimManager().getPersonalData(player.getUUID());
        if (personalData == null) {
            source.sendFailure(Component.translatable("ftbchunks.commands.no_personal_info", player.getDisplayName()));
            return 0;
        }
        personalData.setExtraClaimChunks(Math.max(0, extra + (adding ? personalData.getExtraClaimChunks() : 0)));
        personalData.markDirty();

        // player's new personal limit will affect the team limit
        ChunkTeamDataImpl teamData = FTBChunksCommands.claimManager().getOrCreateData(player);
        if (teamData != null) {
            teamData.updateLimits();
            SendGeneralDataPacket.send(teamData, player);

            source.sendSuccess(() -> Component.translatable("ftbchunks.command.set_extra_chunks_claimed", personalData.getExtraClaimChunks(), player.getDisplayName()), true);
            return personalData.getExtraClaimChunks();
        }

        return 0;
    }

    private static int getExtraForceLoadChunks(CommandSourceStack source, ServerPlayer player) {
        ChunkTeamData personalData = FTBChunksCommands.claimManager().getPersonalData(player.getUUID());
        if (personalData == null) {
            source.sendFailure(Component.translatable("ftbchunks.commands.no_personal_info", player.getDisplayName()));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("ftbchunks.command.extra_forceload_chunks", player.getDisplayName(), personalData.getExtraForceLoadChunks()), true);
        return personalData.getExtraForceLoadChunks();
    }

    private static int setExtraForceLoadChunks(CommandSourceStack source, ServerPlayer player, int extra, boolean adding) {
        // limit is added to player's *personal* data, even if they're in a party
        ChunkTeamDataImpl personalData = FTBChunksCommands.claimManager().getPersonalData(player.getUUID());
        if (personalData == null) {
            source.sendFailure(Component.translatable("ftbchunks.commands.no_personal_info", player.getDisplayName()));
            return 0;
        }
        personalData.setExtraForceLoadChunks(Math.max(0, extra + (adding ? personalData.getExtraForceLoadChunks() : 0)));
        personalData.markDirty();

        // player's new personal limit will affect the team limit
        ChunkTeamDataImpl teamData = FTBChunksCommands.claimManager().getOrCreateData(player);
        teamData.updateLimits();
        SendGeneralDataPacket.send(teamData, player);

        source.sendSuccess(() -> Component.translatable("ftbchunks.command.set_extra_forceload_chunks", personalData.getExtraForceLoadChunks(), player.getDisplayName()), true);
        return personalData.getExtraForceLoadChunks();
    }

    private static int unclaimEverything(CommandSourceStack source) {
        int claimedChunks = 0;
        for (ClaimedChunk c : new ArrayList<>(FTBChunksCommands.claimManager().getAllClaimedChunks())) {
            c.getTeamData().unclaim(source, c.getPos(), false);
            claimedChunks++;
        }
        int finalClaimedChunks = claimedChunks;
        source.sendSuccess(() -> Component.translatable("ftbchunks.commands.unclaimed", finalClaimedChunks), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int unclaimDimension(CommandSourceStack source, ResourceKey<Level> dim) {
        int claimedChunks = 0;
        for (ClaimedChunk c : new ArrayList<>(FTBChunksCommands.claimManager().getAllClaimedChunks())) {
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
        for (ClaimedChunk c : FTBChunksCommands.claimManager().getAllClaimedChunks()) {
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
            chunks.put(holder.getPos().pack(), LoadedChunkViewPacket.LOADED);
        }

        var map = claimManager().getForceLoadedChunks(level.dimension());
        for (long pos : map.keySet()) {
            if (chunks.get(pos) == LoadedChunkViewPacket.LOADED) {
                chunks.put(pos, LoadedChunkViewPacket.FORCE_LOADED);
            }
        }

        for (long pos : level.getForceLoadedChunks()) {
            if (chunks.get(pos) == LoadedChunkViewPacket.LOADED) {
                chunks.put(pos, LoadedChunkViewPacket.FORCE_LOADED);
            }
        }

        source.sendSuccess(() -> Component.translatable("ftbchunks.command.view_loaded", chunks.size()), false);
        Server2PlayNetworking.send(source.getPlayerOrException(), new LoadedChunkViewPacket(level.dimension(), chunks));

        return Command.SINGLE_SUCCESS;
    }

    private static int resetLoadedChunks(CommandSourceStack source, ServerLevel level) throws CommandSyntaxException {
        Server2PlayNetworking.send(source.getPlayerOrException(), new LoadedChunkViewPacket(level.dimension(), Long2IntMaps.EMPTY_MAP));
        return Command.SINGLE_SUCCESS;
    }
}
