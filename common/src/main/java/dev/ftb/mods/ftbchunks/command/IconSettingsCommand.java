package dev.ftb.mods.ftbchunks.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dedicated.DedicatedServer;

public class IconSettingsCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("icon_settings")
                .executes(context -> {
                    if (context.getSource().getServer() instanceof DedicatedServer) {
                        context.getSource().sendFailure(Component.literal("Not on a dedicated server!"));
                        return 0;
                    } else {
                        FTBChunksClient.openIconSettingsScreen();
                        return Command.SINGLE_SUCCESS;
                    }
                });
    }
}
