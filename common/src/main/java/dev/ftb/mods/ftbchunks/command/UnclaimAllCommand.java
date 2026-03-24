package dev.ftb.mods.ftbchunks.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkImpl;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

import static dev.ftb.mods.ftbchunks.command.FTBChunksCommands.executeForTeam;
import static dev.ftb.mods.ftbchunks.command.FTBChunksCommands.selfTeam;

public class UnclaimAllCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("unclaim_all")
                .executes(context -> unclaimAll(
                        context.getSource(),
                        selfTeam(context.getSource()))
                )
                .then(
                        executeForTeam(UnclaimAllCommand::unclaimAll)
                );
    }

    static int unclaimAll(CommandSourceStack source, Team team) {
        ChunkTeamDataImpl data = FTBChunksCommands.claimManager().getOrCreateData(team);

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
}
