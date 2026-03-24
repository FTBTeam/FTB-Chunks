package dev.ftb.mods.ftbchunks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.command.TeamArgument;
import dev.ftb.mods.ftbteams.command.TeamArgumentProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
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
                        .then(ClaimCommand.register())
                        .then(UnclaimCommand.register())
                        .then(LoadCommand.register())
                        .then(UnloadCommand.register())
                        .then(UnclaimAllCommand.register())
                        .then(UnloadAllCommand.register())
                        .then(InfoCommand.register())
                        .then(AdminCommand.register())
                        .then(BlockColorCommand.register())
                        .then(WaypointCommand.register())
        );

        if (FTBChunks.isDevMode()) {
            dispatcher.register(Commands.literal("ftbchunks_dev")
                    .then(IconSettingsCommand.register())
            );
        }

        dispatcher.register(Commands.literal("chunks").redirect(command));
    }

    interface ChunkCallback {
        void accept(ChunkTeamDataImpl data, ChunkDimPos pos) throws CommandSyntaxException;
    }

    static void forEachChunk(CommandSourceStack source, int r, ChunkCallback callback) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Team team = FTBTeamsAPI.api().getManager().getTeamForPlayer(player).orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(player.getId()));
        forEachChunk(team, source.getLevel(), toColumnPos(source.getPosition()), r, callback);
    }

    static void forEachChunk(Team team, Level level, ColumnPos anchor, int r, ChunkCallback callback) throws CommandSyntaxException {
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

    static RequiredArgumentBuilder<CommandSourceStack, Integer> radiusArg() {
        return Commands.argument("radius_in_blocks", IntegerArgumentType.integer(0, 512));
    }

    static int getRadiusArg(CommandContext<CommandSourceStack> context) {
        return IntegerArgumentType.getInteger(context, "radius_in_blocks");
    }

    static RequiredArgumentBuilder<CommandSourceStack, Coordinates> anchorArg() {
        return Commands.argument("anchor", new ColumnPosArgument());
    }

    static ColumnPos getAnchorArg(CommandContext<CommandSourceStack> context) {
        return ColumnPosArgument.getColumnPos(context, "anchor");
    }

    static RequiredArgumentBuilder<CommandSourceStack, Identifier> dimArg() {
        return Commands.argument("dimension", DimensionArgument.dimension());
    }

    static ServerLevel getDimArg(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return DimensionArgument.getDimension(context, "dimension");
    }

    static RequiredArgumentBuilder<CommandSourceStack, TeamArgumentProvider> executeForTeam(ToIntBiFunction<CommandSourceStack, Team> callback) {
        return Commands.argument("team", TeamArgument.create())
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(context -> callback.applyAsInt(context.getSource(), TeamArgument.get(context, "team")));
    }

    static ColumnPos toColumnPos(Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        return new ColumnPos(blockPos.getX(), blockPos.getZ());
    }

    static Team selfTeam(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
                .orElseThrow(() -> TeamArgument.TEAM_NOT_FOUND.create(player.getId()));
    }

    static ClaimedChunkManagerImpl claimManager() {
        return ClaimedChunkManagerImpl.getInstance();
    }
}
