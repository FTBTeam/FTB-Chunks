package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket.SingleChunk;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class ChunkSendingUtils {
    public static void sendChunkToAll(MinecraftServer server, ChunkTeamData teamData, SendChunkPacket packet) {
        if (!teamData.shouldHideClaims()) {
            packet.sendToAll(server);
        } else {
            SendChunkPacket hiddenPacket = new SendChunkPacket(packet.dimension, Util.NIL_UUID, packet.chunk.hidden());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                (teamData.isAlly(player.getUUID()) ? packet : hiddenPacket).sendTo(player);
            }
        }
    }

    public static void sendManyChunksToAll(MinecraftServer server, ChunkTeamData teamData, SendManyChunksPacket packet) {
        if (!teamData.shouldHideClaims()) {
            packet.sendToAll(server);
        } else {
            // shallow copy of packet.chunks OK here, it only contains primitive elements
            SendManyChunksPacket hiddenPacket = new SendManyChunksPacket(packet.dimension, Util.NIL_UUID,
                    packet.chunks.stream().map(SingleChunk::hidden).toList());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                (teamData.isAlly(player.getUUID()) ? packet : hiddenPacket).sendTo(player);
            }
        }
    }

    public static void sendManyChunksToPlayer(ServerPlayer player, ChunkTeamData teamData, SendManyChunksPacket packet) {
        if (!teamData.shouldHideClaims()) {
            packet.sendTo(player);
        } else {
            // shallow copy of packet.chunks OK here, it only contains primitive elements
            SendManyChunksPacket hiddenPacket = new SendManyChunksPacket(packet.dimension, Util.NIL_UUID,
                    packet.chunks.stream().map(SingleChunk::hidden).toList());
            (teamData.isAlly(player.getUUID()) ? packet : hiddenPacket).sendTo(player);
        }
    }
}
