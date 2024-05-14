package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * @author LatvianModder
 */
public record RequestMapDataPacket(int fromX, int fromZ, int toX, int toZ) implements CustomPacketPayload {
	public static final Type<RequestMapDataPacket> TYPE = new Type<>(FTBChunksAPI.rl("request_map_data_packet"));

	public static final StreamCodec<FriendlyByteBuf, RequestMapDataPacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, RequestMapDataPacket::fromX,
			ByteBufCodecs.INT, RequestMapDataPacket::fromZ,
			ByteBufCodecs.INT, RequestMapDataPacket::toX,
			ByteBufCodecs.INT, RequestMapDataPacket::toZ,
			RequestMapDataPacket::new
	);

	@Override
	public Type<RequestMapDataPacket> type() {
		return TYPE;
	}

	public static void handle(RequestMapDataPacket message, NetworkManager.PacketContext context) {
		//FIXME: SendMapDataPacket.send(Objects.requireNonNull(context.get().getSender())));
	}
}