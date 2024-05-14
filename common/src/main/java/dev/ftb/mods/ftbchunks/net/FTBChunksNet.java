package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftblibrary.util.NetworkHelper;

public class FTBChunksNet {
//	SimpleNetworkManager MAIN = SimpleNetworkManager.create(FTBChunks.MOD_ID);

//	MessageType REQUEST_MAP_DATA = MAIN.registerC2S("request_map_data", RequestMapDataPacket::new);
//	MessageType SEND_ALL_CHUNKS = MAIN.registerS2C("send_all_chunks", SendManyChunksPacket::new);
//	MessageType LOGIN_DATA = MAIN.registerS2C("login_data", LoginDataPacket::new);
//	MessageType REQUEST_CHUNK_CHANGE = MAIN.registerC2S("request_chunk_change", RequestChunkChangePacket::new);
//	MessageType SEND_CHUNK = MAIN.registerS2C("send_chunk", SendChunkPacket::new);
//	MessageType SEND_GENERAL_DATA = MAIN.registerS2C("send_general_data", SendGeneralDataPacket::new);
//	MessageType TELEPORT_FROM_MAP = MAIN.registerC2S("teleport_from_map", TeleportFromMapPacket::new);
//
//	MessageType PLAYER_DEATH = MAIN.registerS2C("player_death", PlayerDeathPacket::new);
//	MessageType SEND_VISIBLE_PLAYER_LIST = MAIN.registerS2C("send_visible_player_list", PlayerVisibilityPacket::new);
//	MessageType SYNC_TX = MAIN.registerC2S("sync_tx", SyncTXPacket::new);
//	MessageType SYNC_RX = MAIN.registerS2C("sync_rx", SyncTXRXPacket::new);
//	MessageType LOADED_CHUNK_VIEW = MAIN.registerS2C("loaded_chunk_view", LoadedChunkViewPacket::new);
//	MessageType SEND_PLAYER_POSITION = MAIN.registerS2C("send_player_position", SendPlayerPositionPacket::new);
//	MessageType UPDATE_FORCE_LOAD_EXPIRY = MAIN.registerC2S("update_force_load_expiry", UpdateForceLoadExpiryPacket::new);
//	MessageType SERVER_CONFIG_REQUEST = MAIN.registerC2S("server_config_request", ServerConfigRequestPacket::new);
//	MessageType SERVER_CONFIG_RESPONSE = MAIN.registerS2C("server_config_response", ServerConfigResponsePacket::new);
//	MessageType CHUNK_CHANGE_RESPONSE = MAIN.registerS2C("chunk_change_response", ChunkChangeResponsePacket::new);
//	MessageType REQUEST_BLOCK_COLOR = MAIN.registerS2C("request_block_color", RequestBlockColorPacket::new);
//	MessageType ADD_WAYPOINT = MAIN.registerS2C("add_waypoint", AddWaypointPacket::new);

	public static void init() {
		NetworkHelper.registerC2S(RequestChunkChangePacket.TYPE, RequestChunkChangePacket.STREAM_CODEC, RequestChunkChangePacket::handle);
		NetworkHelper.registerC2S(RequestMapDataPacket.TYPE, RequestMapDataPacket.STREAM_CODEC, RequestMapDataPacket::handle);
		NetworkHelper.registerC2S(ServerConfigRequestPacket.TYPE, ServerConfigRequestPacket.STREAM_CODEC, ServerConfigRequestPacket::handle);
		NetworkHelper.registerC2S(TeleportFromMapPacket.TYPE, TeleportFromMapPacket.STREAM_CODEC, TeleportFromMapPacket::handle);
		NetworkHelper.registerC2S(UpdateForceLoadExpiryPacket.TYPE, UpdateForceLoadExpiryPacket.STREAM_CODEC, UpdateForceLoadExpiryPacket::handle);
		NetworkHelper.registerC2S(SyncTXPacket.TYPE, SyncTXPacket.STREAM_CODEC, SyncTXPacket::handle);

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