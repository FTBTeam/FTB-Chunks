package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket.SingleChunk;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class ChunkSendingUtils {
    public static void sendChunkToAll(MinecraftServer server, ChunkTeamData teamData, SendChunkPacket packet) {
        if (!teamData.shouldHideClaims()) {
            NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), packet);
        } else {
            SendChunkPacket hiddenPacket = new SendChunkPacket(packet.dimension(), Util.NIL_UUID, packet.chunk().hidden());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                NetworkManager.sendToPlayer(player, teamData.isAlly(player.getUUID()) ? packet : hiddenPacket);
            }
        }
    }

    public static void sendManyChunksToAll(MinecraftServer server, ChunkTeamData teamData, SendManyChunksPacket packet) {
        if (!teamData.shouldHideClaims()) {
            NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), packet);
        } else {
            // shallow copy of packet.chunks OK here, it only contains primitive elements
            SendManyChunksPacket hiddenPacket = new SendManyChunksPacket(packet.dimension(), Util.NIL_UUID,
                    packet.chunks().stream().map(SingleChunk::hidden).toList());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                NetworkManager.sendToPlayer(player, teamData.isAlly(player.getUUID()) ? packet : hiddenPacket);
            }
        }
    }

    public static void sendManyChunksToPlayer(ServerPlayer player, ChunkTeamData teamData, SendManyChunksPacket packet) {
        if (!teamData.shouldHideClaims()) {
            NetworkManager.sendToPlayer(player, packet);
        } else {
            // shallow copy of packet.chunks OK here, it only contains primitive elements
            SendManyChunksPacket hiddenPacket = new SendManyChunksPacket(packet.dimension(), Util.NIL_UUID,
                    packet.chunks().stream().map(SingleChunk::hidden).toList());
            NetworkManager.sendToPlayer(player, teamData.isAlly(player.getUUID()) ? packet : hiddenPacket);
        }
    }
}
