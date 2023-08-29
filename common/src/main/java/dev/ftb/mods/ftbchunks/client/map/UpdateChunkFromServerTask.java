package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftblibrary.math.XZ;

import java.util.Date;
import java.util.UUID;

public class UpdateChunkFromServerTask implements MapTask {
	private final MapDimension dimension;
	private final SendChunkPacket.SingleChunk chunk;
	private final UUID teamId;
	private final Date now;

	public UpdateChunkFromServerTask(MapDimension d, SendChunkPacket.SingleChunk c, UUID i, Date date) {
		dimension = d;
		chunk = c;
		teamId = i;
		now = date;
	}

	@Override
	public void runMapTask() {
		dimension.getRegion(XZ.regionFromChunk(chunk.getX(), chunk.getZ()))
				.getChunkForAbsoluteChunkPos(XZ.of(chunk.getX(), chunk.getZ()))
				.updateFromServer(now, chunk, teamId);
	}

	@Override
	public String toString() {
		return "UpdateChunkFromServerTask@" + dimension + ":" + chunk.getX() + "," + chunk.getZ();
	}
}