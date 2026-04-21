package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.platform.network.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestMapDataPacket(int fromX, int fromZ, int toX, int toZ) implements CustomPacketPayload {
	public static final Type<RequestMapDataPacket> TYPE = new Type<>(FTBChunksAPI.id("request_map_data_packet"));

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

	public static void handle(RequestMapDataPacket ignoredMessage, PacketContext ignoredContext) {
		//FIXME: SendMapDataPacket.send(Objects.requireNonNull(context.get().getSender())));
	}
}
