package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.SNBTNet;
import dev.ftb.mods.ftblibrary.snbt.config.ConfigUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public class ServerConfigRequestPacket extends BaseC2SMessage {
    private final SNBTCompoundTag config;

    public ServerConfigRequestPacket(SNBTCompoundTag config) {
        this.config = config;
    }

    public ServerConfigRequestPacket(FriendlyByteBuf buf) {
        config = SNBTNet.readCompound(buf);
    }

    @Override
    public MessageType getType() {
        return FTBChunksNet.SERVER_CONFIG_REQUEST;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        SNBTNet.write(buf, config);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof ServerPlayer sp && sp.hasPermissions(2)) {
            MinecraftServer server = sp.getServer();

            FTBChunks.LOGGER.info("FTB Chunks server config updated from client by player {}", sp.getName().getString());
            FTBChunksWorldConfig.CONFIG.read(config);

            Path file = server.getWorldPath(ConfigUtil.SERVER_CONFIG_DIR).resolve(FTBChunksWorldConfig.CONFIG.key + ".snbt");
            FTBChunksWorldConfig.CONFIG.save(file);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!sp.getUUID().equals(player.getUUID())) {
                    new ServerConfigResponsePacket(config).sendTo(player);
                }
            }
        }
    }
}
