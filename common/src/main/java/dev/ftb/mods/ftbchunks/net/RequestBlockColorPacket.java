package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.network.FriendlyByteBuf;

public class RequestBlockColorPacket extends BaseS2CMessage {
    public RequestBlockColorPacket() {
    }

    public RequestBlockColorPacket(FriendlyByteBuf buf) {
    }

    @Override
    public MessageType getType() {
        return FTBChunksNet.REQUEST_BLOCK_COLOR;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        FTBChunksClient.handleBlockColorRequest();
    }
}
