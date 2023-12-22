package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class AddWaypointPacket extends BaseS2CMessage {
    private final String name;
    private final BlockPos position;
    private final int color;

    public AddWaypointPacket(String name, BlockPos position, int color) {
        this.name = name;
        this.position = position;
        this.color = color;
    }

    public AddWaypointPacket(FriendlyByteBuf buf) {
        name = buf.readUtf();
        position = buf.readBlockPos();
        color = buf.readInt();
    }

    @Override
    public MessageType getType() {
        return FTBChunksNet.ADD_WAYPOINT;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeBlockPos(position);
        buf.writeInt(color);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        FTBChunksClient.addWaypoint(context.getPlayer(), name, position, color);
    }
}
