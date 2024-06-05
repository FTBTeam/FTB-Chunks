package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftbchunks.data.ChunkSyncInfo;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Date;

public record UpdateForceLoadExpiryPacket(ChunkDimPos pos, long relativeExpiryTime) implements CustomPacketPayload {
    public static final Type<UpdateForceLoadExpiryPacket> TYPE = new Type<>(FTBChunksAPI.rl("update_force_load_expiry_packet"));

    public static final StreamCodec<FriendlyByteBuf, UpdateForceLoadExpiryPacket> STREAM_CODEC = StreamCodec.composite(
            ChunkDimPos.STREAM_CODEC, UpdateForceLoadExpiryPacket::pos,
            ByteBufCodecs.VAR_LONG, UpdateForceLoadExpiryPacket::relativeExpiryTime,
            UpdateForceLoadExpiryPacket::new
    );

    public UpdateForceLoadExpiryPacket(ChunkDimPos pos, Date expiryDate) {
        this(pos, expiryDate == null ? 0L : Math.max(0L, expiryDate.getTime() - System.currentTimeMillis()));
    }

    @Override
    public Type<UpdateForceLoadExpiryPacket> type() {
        return TYPE;
    }

    public static void handle(UpdateForceLoadExpiryPacket message, NetworkManager.PacketContext context) {
        ChunkDimPos pos = message.pos;

        if (context.getPlayer() instanceof ServerPlayer sp && sp.level().dimension().equals(pos.dimension())) {
            ClaimedChunk chunk = ClaimedChunkManagerImpl.getInstance().getChunk(pos);
            if (chunk != null && chunk.getTeamData().getTeam().getRankForPlayer(sp.getUUID()).isMemberOrBetter() && chunk.isForceLoaded()) {
                chunk.setForceLoadExpiryTime(message.relativeExpiryTime == 0L ? 0L : System.currentTimeMillis() + message.relativeExpiryTime);
                SendChunkPacket packet = new SendChunkPacket(pos.dimension(), chunk.getTeamData().getTeam().getId(),
                        ChunkSyncInfo.create(System.currentTimeMillis(), chunk.getPos().x(), chunk.getPos().z(), chunk));
                NetworkManager.sendToPlayer(sp, packet);
            }
        }
    }
}
