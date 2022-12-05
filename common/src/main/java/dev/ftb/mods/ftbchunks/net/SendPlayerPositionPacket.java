package dev.ftb.mods.ftbchunks.net;

import com.mojang.authlib.GameProfile;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.FTBChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class SendPlayerPositionPacket extends BaseS2CMessage {
    private final GameProfile gameProfile;
    private final BlockPos pos;
    private final boolean valid;

    public SendPlayerPositionPacket(ServerPlayer player, BlockPos pos) {
        this.pos = pos == null ? BlockPos.ZERO : pos;
        this.valid = pos != null;
        this.gameProfile = player.getGameProfile();
    }

    public SendPlayerPositionPacket(FriendlyByteBuf buf) {
        pos = buf.readBlockPos();
        valid = buf.readBoolean();
        gameProfile = new GameProfile(buf.readUUID(), buf.readUtf());
    }

    @Override
    public MessageType getType() {
        return FTBChunksNet.SEND_PLAYER_POSITION;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(valid);
        buf.writeUUID(gameProfile.getId());
        buf.writeUtf(gameProfile.getName());
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        FTBChunks.PROXY.updateTrackedPlayerPos(gameProfile, pos, valid);
    }
}
