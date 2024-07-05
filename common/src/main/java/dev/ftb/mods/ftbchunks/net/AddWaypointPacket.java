package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AddWaypointPacket(String name, GlobalPos position, int color, boolean useGui) implements CustomPacketPayload {
    public static final Type<AddWaypointPacket> TYPE = new Type<>(FTBChunksAPI.rl("add_waypoint_packet"));

    public static final StreamCodec<FriendlyByteBuf, AddWaypointPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AddWaypointPacket::name,
            GlobalPos.STREAM_CODEC, AddWaypointPacket::position,
            ByteBufCodecs.INT, AddWaypointPacket::color,
            ByteBufCodecs.BOOL, AddWaypointPacket::useGui,
            AddWaypointPacket::new
    );

    @Override
    public Type<AddWaypointPacket> type() {
        return TYPE;
    }

    public static void handle(AddWaypointPacket message, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if(message.useGui()) {
                StringConfig configName = new StringConfig();
                configName.setValue(message.name);
                new FTBChunksClient.WaypointAddScreen(configName, context.getPlayer(), message.position).openGui();
            }else {
                FTBChunksClient.addWaypoint(message.name, message.position, message.color);
            }
        });
    }
}
