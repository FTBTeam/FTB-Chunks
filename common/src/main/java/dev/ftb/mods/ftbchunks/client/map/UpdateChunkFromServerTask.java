package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.data.XZ;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;

import java.util.Date;
import java.util.UUID;

/**
 * @author LatvianModder
 */
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
		if (MapManager.inst == dimension.manager) {
			dimension.getRegion(XZ.regionFromChunk(chunk.x, chunk.z)).getDataBlocking().getChunk(XZ.of(chunk.x, chunk.z)).updateFrom(now, chunk, teamId);
		}
	}

	@Override
	public String toString() {
		return "UpdateChunkFromServerTask@" + dimension + ":" + chunk.x + "," + chunk.z;
	}
}