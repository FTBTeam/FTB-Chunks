package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;

import java.util.Date;

public class UpdateForceLoadExpiryPacket extends BaseC2SMessage {
    private final ChunkDimPos pos;
    private final long relativeExpiryTime;

    public UpdateForceLoadExpiryPacket(ChunkDimPos pos, Date expiryDate) {
        this.pos = pos;
        this.relativeExpiryTime = expiryDate == null ? 0L : Math.max(0L, expiryDate.getTime() - System.currentTimeMillis());
    }

    public UpdateForceLoadExpiryPacket(FriendlyByteBuf buf) {
        pos = new ChunkDimPos(ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation()), buf.readInt(), buf.readInt());
        relativeExpiryTime = buf.readLong();
    }

    @Override
    public MessageType getType() {
        return FTBChunksNet.UPDATE_FORCE_LOAD_EXPIRY;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(pos.dimension().location());
        buf.writeInt(pos.x());
        buf.writeInt(pos.z());
        buf.writeLong(relativeExpiryTime);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof ServerPlayer sp && sp.level().dimension().equals(pos.dimension())) {
            ClaimedChunk chunk = ClaimedChunkManagerImpl.getInstance().getChunk(pos);
            if (chunk != null && chunk.getTeamData().getTeam().getRankForPlayer(sp.getUUID()).isMemberOrBetter() && chunk.isForceLoaded()) {
                chunk.setForceLoadExpiryTime(relativeExpiryTime == 0L ? 0L : System.currentTimeMillis() + relativeExpiryTime);
                SendChunkPacket packet = new SendChunkPacket(pos.dimension(), chunk.getTeamData().getTeam().getId(),
                        new SendChunkPacket.SingleChunk(System.currentTimeMillis(), chunk.getPos().x(), chunk.getPos().z(), chunk));
                packet.sendTo(sp);
            }
        }
    }
}
