package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.SNBTNet;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import net.minecraft.network.FriendlyByteBuf;

public class ServerConfigResponsePacket extends BaseS2CMessage {
    private final SNBTCompoundTag config;

    public ServerConfigResponsePacket(SNBTCompoundTag config) {
        this.config = config;
    }

    public ServerConfigResponsePacket(FriendlyByteBuf buf) {
        config = SNBTNet.readCompound(buf);
    }

    @Override
    public MessageType getType() {
        return FTBChunksNet.SERVER_CONFIG_RESPONSE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        SNBTNet.write(buf, config);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        FTBChunks.LOGGER.info("Received FTB Chunks server config from server");
        FTBChunksWorldConfig.CONFIG.read(config);

        FTBTeamsAPI.getManager().getTeams().forEach(team -> FTBChunksAPI.getManager().getData(team).updateLimits());
    }
}
