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

public class UnclaimCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("unclaim")
                .executes(context -> unclaim(
                        context.getSource(),
                        selfTeam(context.getSource()),
                        0)
                )
                .then(radiusArg()
                        .executes(context -> unclaim(
                                context.getSource(),
                                selfTeam(context.getSource()),
                                getRadiusArg(context))
                        )
                );
    }

    static int unclaim(CommandSourceStack source, Team team, int r) throws CommandSyntaxException {
        return unclaim(source, team, r, toColumnPos(source.getPosition()), source.getLevel());
    }

    static int unclaim(CommandSourceStack source, Team team, int r, ColumnPos anchor, Level level) throws CommandSyntaxException {
        int[] success = new int[1];

        forEachChunk(team, level, anchor, r, (data, pos) -> {
            if (data.unclaim(source, pos, false).isSuccess()) {
                success[0]++;
            }
        });

        source.sendSuccess(() -> Component.translatable("ftbchunks.commands.unclaimed", success[0]), false);

        ChunkDimPos origin = new ChunkDimPos(level.dimension(), anchor.x() >> 4, anchor.z() >> 4);
        FTBChunks.LOGGER.info("{} unclaimed {} chunks around {} for team {}", source.getTextName(), success[0], origin, team.getShortName());

        return success[0];
    }
}
