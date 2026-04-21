package dev.ftb.mods.ftbchunks.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbchunks.net.RequestBlockColorPacket;
import dev.ftb.mods.ftblibrary.platform.network.Server2PlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class BlockColorCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("block_color")
                .executes(context -> {
                    Server2PlayNetworking.send(context.getSource().getPlayerOrException(), RequestBlockColorPacket.INSTANCE);
                    return Command.SINGLE_SUCCESS;
                });
    }
}
