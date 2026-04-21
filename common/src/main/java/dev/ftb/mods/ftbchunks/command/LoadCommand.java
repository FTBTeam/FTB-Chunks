package dev.ftb.mods.ftbchunks.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import static dev.ftb.mods.ftbchunks.command.FTBChunksCommands.*;

public class LoadCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("load")
                .executes(context -> load(
                        context.getSource(),
                        0)
                )
                .then(radiusArg()
                        .executes(context -> load(
                                context.getSource(),
                                getRadiusArg(context))
                        )
                );
    }

    static int load(CommandSourceStack source, int r) throws CommandSyntaxException {
        int[] success = new int[1];

        forEachChunk(source, r, (data, pos) -> {
            ClaimResult result = data.forceLoad(source, pos, false);
            if (result.isSuccess()) {
                success[0]++;
            }
        });

        source.sendSuccess(() -> Component.translatable("ftbchunks.commands.force_loaded", success[0]), false);
        FTBChunks.LOGGER.info("{} loaded {} chunks at {}", source.getTextName(), success[0], new ChunkDimPos(source.getPlayerOrException()));
        return success[0];
    }
}
