package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.data.XZ;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;

import java.util.Date;

/**
 * @author LatvianModder
 */
public class UpdateChunkFromServerTask implements MapTask {
	private final MapDimension dimension;
	private final SendChunkPacket.SingleChunk chunk;
	private final Date now;

	public UpdateChunkFromServerTask(MapDimension d, SendChunkPacket.SingleChunk c, Date date) {
		dimension = d;
		chunk = c;
		now = date;
	}

	@Override
	public void runMapTask() {
		if (MapManager.inst == dimension.manager) {
			dimension.getRegion(XZ.regionFromChunk(chunk.x, chunk.z)).getDataBlocking().getChunk(XZ.of(chunk.x, chunk.z)).updateFrom(now, chunk);
		}
	}

	@Override
	public String toString() {
		return "UpdateChunkFromServerTask@" + dimension + ":" + chunk.x + "," + chunk.z;
	}
}