package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.client.gui.ChunkScreen;
import net.minecraft.network.FriendlyByteBuf;

import java.util.EnumMap;

public class ChunkChangeResponsePacket extends BaseS2CMessage {
    private final int totalChunks;
    private final int changedChunks;
    private final EnumMap<ClaimResult.StandardProblem,Integer> problems;

    public ChunkChangeResponsePacket(int totalChunks, int changedChunks, EnumMap<ClaimResult.StandardProblem,Integer> problems) {
        this.totalChunks = totalChunks;
        this.changedChunks = changedChunks;
        this.problems = problems;
    }

    ChunkChangeResponsePacket(FriendlyByteBuf buf) {
        totalChunks = buf.readVarInt();
        changedChunks = buf.readVarInt();
        problems = new EnumMap<>(ClaimResult.StandardProblem.class);

        int nProblems = buf.readVarInt();
        for (int i = 0; i < nProblems; i++) {
            String name = buf.readUtf(Short.MAX_VALUE);
            int count = buf.readVarInt();
            ClaimResult.StandardProblem.forName(name).ifPresent(res -> problems.put(res, count));
        }
    }

    @Override
    public MessageType getType() {
        return FTBChunksNet.CHUNK_CHANGE_RESPONSE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(totalChunks);
        buf.writeVarInt(changedChunks);
        buf.writeVarInt(problems.size());
        problems.forEach((res, count) -> {
            buf.writeUtf(res.getId());
            buf.writeVarInt(count);
        });
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        ChunkScreen.notifyChunkUpdates(totalChunks, changedChunks, problems);
    }
}
