package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftblibrary.net.snm.PacketID;
import dev.ftb.mods.ftblibrary.net.snm.SimpleNetworkManager;

public interface FTBChunksNet {
	SimpleNetworkManager MAIN = SimpleNetworkManager.create(FTBChunks.MOD_ID);

	PacketID REQUEST_MAP_DATA = MAIN.registerC2S("request_map_data", RequestMapDataPacket::new);
	PacketID SEND_ALL_CHUNKS = MAIN.registerS2C("send_all_chunks", SendManyChunksPacket::new);
	PacketID LOGIN_DATA = MAIN.registerS2C("login_data", LoginDataPacket::new);
	PacketID REQUEST_CHUNK_CHANGE = MAIN.registerC2S("request_chunk_change", RequestChunkChangePacket::new);
	PacketID SEND_CHUNK = MAIN.registerS2C("send_chunk", SendChunkPacket::new);
	PacketID SEND_GENERAL_DATA = MAIN.registerS2C("send_general_data", SendGeneralDataPacket::new);
	PacketID TELEPORT_FROM_MAP = MAIN.registerC2S("teleport_from_map", TeleportFromMapPacket::new);
	PacketID PLAYER_DEATH = MAIN.registerS2C("player_death", PlayerDeathPacket::new);
	PacketID SEND_VISIBLE_PLAYER_LIST = MAIN.registerS2C("send_visible_player_list", SendVisiblePlayerListPacket::new);
	PacketID SYNC_TX = MAIN.registerC2S("sync_tx", SyncTXPacket::new);
	PacketID SYNC_RX = MAIN.registerS2C("sync_rx", SyncRXPacket::new);

	static void init() {
	}
}