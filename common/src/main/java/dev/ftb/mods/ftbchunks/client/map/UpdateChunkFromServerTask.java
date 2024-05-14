package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.data.ChunkSyncInfo;
import dev.ftb.mods.ftblibrary.math.XZ;

import java.util.Date;
import java.util.UUID;

public record UpdateChunkFromServerTask(MapDimension dimension, ChunkSyncInfo info, UUID teamId, Date date) implements MapTask {
	@Override
	public void runMapTask() {
		dimension.getRegion(XZ.regionFromChunk(info.x(), info.z()))
				.getChunkForAbsoluteChunkPos(XZ.of(info.x(), info.z()))
				.updateFromServer(date, info, teamId);
	}
}