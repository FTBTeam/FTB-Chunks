package dev.ftb.mods.ftbchunks.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

import static dev.ftb.mods.ftbchunks.command.FTBChunksCommands.executeForTeam;
import static dev.ftb.mods.ftbchunks.command.FTBChunksCommands.selfTeam;

public class UnloadAllCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("unload_all")
                .executes(context -> unloadAll(
                        context.getSource(), selfTeam(context.getSource()))
                )
                .then(
                        executeForTeam(UnloadAllCommand::unloadAll)
                );
    }

    static int unloadAll(CommandSourceStack source, Team team) {
        ChunkTeamDataImpl data = FTBChunksCommands.claimManager().getOrCreateData(team);

        int unloaded = 0;
        for (ClaimedChunk c : new ArrayList<>(data.getClaimedChunks())) {
            ClaimResult claimResult = data.unForceLoad(source, c.getPos(), false);
            if (claimResult.isSuccess()) {
                unloaded++;
            }
        }
        data.markDirty();
        int finalUnloaded = unloaded;
        source.sendSuccess(() -> Component.translatable("ftbchunks.commands.unloaded", finalUnloaded, data.getClaimedChunks().size()), true);
        return Command.SINGLE_SUCCESS;
    }
}
