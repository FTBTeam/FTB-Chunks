package dev.ftb.mods.ftbchunks.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import static dev.ftb.mods.ftbchunks.command.FTBChunksCommands.*;

public class UnloadCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("unload")
                .executes(context -> unload(
                        context.getSource(),
                        0)
                )
                .then(radiusArg()
                        .executes(context -> unload(
                                context.getSource(),
                                getRadiusArg(context))
                        )
                );
    }

    static int unload(CommandSourceStack source, int r) throws CommandSyntaxException {
        int[] success = new int[2];

        forEachChunk(source, r, (data, pos) -> {
            if (data.unForceLoad(source, pos, false).isSuccess()) {
                success[0]++;
            }
            success[1]++;
        });

        source.sendSuccess(() -> Component.translatable("ftbchunks.commands.unloaded", success[0], success[1]), false);
        FTBChunks.LOGGER.info("{} unloaded {} chunks at {}", source.getTextName(), success[0], new ChunkDimPos(source.getPlayerOrException()));
        return success[0];
    }
}
