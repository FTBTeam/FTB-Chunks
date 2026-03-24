package dev.ftb.mods.ftbchunks.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.Level;

import static dev.ftb.mods.ftbchunks.command.FTBChunksCommands.*;

public class ClaimCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("claim")
                .executes(context -> claim(
                        context.getSource(),
                        selfTeam(context.getSource()),
                        0)
                )
                .then(radiusArg()
                        .executes(context -> claim(
                                context.getSource(),
                                selfTeam(context.getSource()),
                                getRadiusArg(context))
                        )
                );

    }

    static int claim(CommandSourceStack source, Team team, int r) throws CommandSyntaxException {
        return claim(source, team, r, FTBChunksCommands.toColumnPos(source.getPosition()), source.getLevel());
    }

    static int claim(CommandSourceStack source, Team team, int r, ColumnPos anchor, Level level) throws CommandSyntaxException {
        int[] success = new int[1];

        FTBChunksCommands.forEachChunk(team, level, anchor, r, (data, pos) -> {
            if (data.claim(source, pos, false).isSuccess()) {
                success[0]++;
            }
        });

        source.sendSuccess(() -> Component.translatable("ftbchunks.commands.claimed", success[0]), false);

        ChunkDimPos origin = new ChunkDimPos(level.dimension(), anchor.x() >> 4, anchor.z() >> 4);
        FTBChunks.LOGGER.info("{} claimed {} chunks around {} for team {}", source.getTextName(), success[0], origin, team.getShortName());

        return success[0];
    }
}
