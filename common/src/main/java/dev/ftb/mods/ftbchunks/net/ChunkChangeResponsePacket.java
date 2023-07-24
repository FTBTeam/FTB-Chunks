package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.client.ChunkScreen;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;

public class ChunkChangeResponsePacket extends BaseS2CMessage {
    private final int totalChunks;
    private final int changedChunks;
    private final Map<String,Integer> problems;

    public ChunkChangeResponsePacket(int totalChunks, int changedChunks, Map<String,Integer> problems) {
        this.totalChunks = totalChunks;
        this.changedChunks = changedChunks;
        this.problems = problems;
    }

    ChunkChangeResponsePacket(FriendlyByteBuf buf) {
        totalChunks = buf.readVarInt();
        changedChunks = buf.readVarInt();
        problems = buf.readMap(buf1 -> buf1.readUtf(Short.MAX_VALUE), FriendlyByteBuf::readVarInt);
    }

    @Override
    public MessageType getType() {
        return FTBChunksNet.CHUNK_CHANGE_RESPONSE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(totalChunks);
        buf.writeVarInt(changedChunks);
        buf.writeMap(problems, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        ChunkScreen.notifyChunkUpdates(totalChunks, changedChunks, problems);
    }
}
