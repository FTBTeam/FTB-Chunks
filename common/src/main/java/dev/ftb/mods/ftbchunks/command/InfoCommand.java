package dev.ftb.mods.ftbchunks.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.util.UndashedUuid;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

import static dev.ftb.mods.ftbchunks.command.FTBChunksCommands.dimArg;
import static dev.ftb.mods.ftbchunks.command.FTBChunksCommands.getDimArg;

public class InfoCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("info")
                .executes(context -> info(
                        context.getSource(),
                        new ChunkDimPos(context.getSource().getLevel(), BlockPos.containing(context.getSource().getPosition())))
                )
                .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                .executes(context -> info(
                                        context.getSource(),
                                        new ChunkDimPos(context.getSource().getLevel(), new BlockPos(IntegerArgumentType.getInteger(context, "x"), 0, IntegerArgumentType.getInteger(context, "z"))))
                                )
                                .then(dimArg()
                                        .executes(context -> info(
                                                        context.getSource(),
                                                        new ChunkDimPos(getDimArg(context).dimension(), IntegerArgumentType.getInteger(context, "x") >> 4, IntegerArgumentType.getInteger(context, "z") >> 4)
                                                )
                                        )
                                )
                        )
                );
    }

    @SuppressWarnings("SameReturnValue")
    static int info(CommandSourceStack source, ChunkDimPos pos) throws CommandSyntaxException {
        ClaimedChunk chunk = FTBChunksCommands.claimManager().getChunk(pos);

        boolean canKnowChunkStatus =
                source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
                || (source.isPlayer() || (chunk != null && chunk.getTeamData().canPlayerUse(source.getPlayerOrException(), FTBChunksProperties.CLAIM_VISIBILITY)));

        if (canKnowChunkStatus) {
            if (chunk == null) {
                source.sendSuccess(() -> Component.translatable("ftbchunks.commands.not_claimed"), false);
                return Command.SINGLE_SUCCESS;
            }

            source.sendSuccess(() -> {
                Component owner = chunk.getTeamData().getTeam().getColoredName().copy().append(" / " + UndashedUuid.toString(chunk.getTeamData().getTeam().getId()));
                String location = pos.dimension().identifier().toString() + " [" + pos.x() + ", " + pos.z() + "]";
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
}
