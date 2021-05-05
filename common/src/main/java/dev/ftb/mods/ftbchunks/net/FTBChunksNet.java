package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftblibrary.net.FTBNetworkHandler;
import dev.ftb.mods.ftblibrary.net.PacketID;

public interface FTBChunksNet {
	FTBNetworkHandler MAIN = FTBNetworkHandler.create(FTBChunks.MOD_ID);

	PacketID REQUEST_MAP_DATA = MAIN.register("request_map_data", RequestMapDataPacket::new);
	PacketID SEND_ALL_CHUNKS = MAIN.register("send_all_chunks", SendManyChunksPacket::new);
	PacketID LOGIN_DATA = MAIN.register("login_data", LoginDataPacket::new);
	PacketID REQUEST_CHUNK_CHANGE = MAIN.register("request_chunk_change", RequestChunkChangePacket::new);
	PacketID SEND_CHUNK = MAIN.register("send_chunk", SendChunkPacket::new);
	PacketID SEND_GENERAL_DATA = MAIN.register("send_general_data", SendGeneralDataPacket::new);
	PacketID TELEPORT_FROM_MAP = MAIN.register("teleport_from_map", TeleportFromMapPacket::new);
	PacketID PLAYER_DEATH = MAIN.register("player_death", PlayerDeathPacket::new);
	PacketID SEND_VISIBLE_PLAYER_LIST = MAIN.register("send_visible_player_list", SendVisiblePlayerListPacket::new);
	PacketID SYNC_TX = MAIN.register("sync_tx", SyncTXPacket::new);
	PacketID SYNC_RX = MAIN.register("sync_rx", SyncRXPacket::new);

	static void init() {
	}
}