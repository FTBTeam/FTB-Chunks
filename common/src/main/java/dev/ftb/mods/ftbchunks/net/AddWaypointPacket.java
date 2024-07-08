package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;

public class AddWaypointPacket extends BaseS2CMessage {
    private final String name;
    private final GlobalPos position;
    private final int color;
    private final boolean useGui;

    public AddWaypointPacket(String name, GlobalPos position, int color, boolean useGui) {
        this.name = name;
        this.position = position;
        this.color = color;
        this.useGui = useGui;
    }

    public AddWaypointPacket(FriendlyByteBuf buf) {
        name = buf.readUtf();
        position = buf.readGlobalPos();
        color = buf.readInt();
        useGui = buf.readBoolean();
    }

    @Override
    public MessageType getType() {
        return FTBChunksNet.ADD_WAYPOINT;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeGlobalPos(position);
        buf.writeInt(color);
        buf.writeBoolean(useGui);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            if(useGui) {
                StringConfig configName = new StringConfig();
                configName.setValue(name);
                new FTBChunksClient.WaypointAddScreen(configName, context.getPlayer(), position).openGui();
            }else {
                FTBChunksClient.addWaypoint(name, position, color);
            }
        });
    }
}
