package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftblibrary.util.NetworkHelper;

public class FTBChunksNet {
	public static void init() {
		NetworkHelper.registerC2S(RequestChunkChangePacket.TYPE, RequestChunkChangePacket.STREAM_CODEC, RequestChunkChangePacket::handle);
		NetworkHelper.registerC2S(RequestMapDataPacket.TYPE, RequestMapDataPacket.STREAM_CODEC, RequestMapDataPacket::handle);
		NetworkHelper.registerC2S(ServerConfigRequestPacket.TYPE, ServerConfigRequestPacket.STREAM_CODEC, ServerConfigRequestPacket::handle);
		NetworkHelper.registerC2S(TeleportFromMapPacket.TYPE, TeleportFromMapPacket.STREAM_CODEC, TeleportFromMapPacket::handle);
		NetworkHelper.registerC2S(UpdateForceLoadExpiryPacket.TYPE, UpdateForceLoadExpiryPacket.STREAM_CODEC, UpdateForceLoadExpiryPacket::handle);
		NetworkHelper.registerC2S(SyncTXPacket.TYPE, SyncTXPacket.STREAM_CODEC, SyncTXPacket::handle);
		NetworkHelper.registerC2S(ShareWaypointPacket.TYPE, ShareWaypointPacket.STREAM_CODEC, ShareWaypointPacket::handle);

		NetworkHelper.registerS2C(AddWaypointPacket.TYPE, AddWaypointPacket.STREAM_CODEC, AddWaypointPacket::handle);
		NetworkHelper.registerS2C(ChunkChangeResponsePacket.TYPE, ChunkChangeResponsePacket.STREAM_CODEC, ChunkChangeResponsePacket::handle);
		NetworkHelper.registerS2C(LoadedChunkViewPacket.TYPE, LoadedChunkViewPacket.STREAM_CODEC, LoadedChunkViewPacket::handle);
		NetworkHelper.registerS2C(LoginDataPacket.TYPE, LoginDataPacket.STREAM_CODEC, LoginDataPacket::handle);
		NetworkHelper.registerS2C(PlayerDeathPacket.TYPE, PlayerDeathPacket.STREAM_CODEC, PlayerDeathPacket::handle);
		NetworkHelper.registerS2C(PlayerVisibilityPacket.TYPE, PlayerVisibilityPacket.STREAM_CODEC, PlayerVisibilityPacket::handle);
		NetworkHelper.registerS2C(RequestBlockColorPacket.TYPE, RequestBlockColorPacket.STREAM_CODEC, RequestBlockColorPacket::handle);
		NetworkHelper.registerS2C(SendChunkPacket.TYPE, SendChunkPacket.STREAM_CODEC, SendChunkPacket::handle);
		NetworkHelper.registerS2C(SendGeneralDataPacket.TYPE, SendGeneralDataPacket.STREAM_CODEC, SendGeneralDataPacket::handle);
		NetworkHelper.registerS2C(SendManyChunksPacket.TYPE, SendManyChunksPacket.STREAM_CODEC, SendManyChunksPacket::handle);
		NetworkHelper.registerS2C(SendPlayerPositionPacket.TYPE, SendPlayerPositionPacket.STREAM_CODEC, SendPlayerPositionPacket::handle);
		NetworkHelper.registerS2C(ServerConfigResponsePacket.TYPE, ServerConfigResponsePacket.STREAM_CODEC, ServerConfigResponsePacket::handle);
		NetworkHelper.registerS2C(SyncRXPacket.TYPE, SyncRXPacket.STREAM_CODEC, SyncRXPacket::handle);
	}
}