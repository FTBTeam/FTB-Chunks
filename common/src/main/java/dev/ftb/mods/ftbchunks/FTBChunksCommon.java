package dev.ftb.mods.ftbchunks;

import dev.ftb.mods.ftbchunks.client.map.RegionSyncKey;
import dev.ftb.mods.ftbchunks.net.LoginDataPacket;
import dev.ftb.mods.ftbchunks.net.PlayerDeathPacket;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftbchunks.net.SendManyChunksPacket;
import dev.ftb.mods.ftbchunks.net.SendVisiblePlayerListPacket;
import net.minecraft.server.level.ServerLevel;

/**
 * @author LatvianModder
 */
public class FTBChunksCommon {
	public void init() {
	}

	public void login(LoginDataPacket loginData) {
	}

	public void updateGeneralData(SendGeneralDataPacket packet) {
	}

	public void updateChunk(SendChunkPacket packet) {
	}

	public void updateAllChunks(SendManyChunksPacket packet) {
	}

	public void updateVisiblePlayerList(SendVisiblePlayerListPacket packet) {
	}

	public void importWorldMap(ServerLevel world) {
	}

	public void syncRegion(RegionSyncKey key, int offset, int total, byte[] data) {
	}

	public void playerDeath(PlayerDeathPacket packet) {
	}

	public int blockColor() {
		return 0;
	}
}