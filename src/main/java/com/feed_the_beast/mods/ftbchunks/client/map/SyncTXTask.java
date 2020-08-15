package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
import com.feed_the_beast.mods.ftbchunks.net.SyncTXPacket;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author LatvianModder
 */
public class SyncTXTask implements MapTask
{
	public final MapRegion region;
	public final long start;

	public SyncTXTask(MapRegion r, long s)
	{
		region = r;
		start = s;
	}

	@Override
	public void runMapTask()
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(out))))
		{
			stream.writeInt(region.pos.x);
			stream.writeInt(region.pos.z);
			stream.writeShort(region.getChunks().size());

			for (MapChunk chunk : region.getChunks().values())
			{
				stream.writeByte(chunk.pos.x);
				stream.writeByte(chunk.pos.z);
				stream.writeLong(start - chunk.modified);
			}

			byte[] imgData = region.getDataImage().getBytes();
			stream.writeInt(imgData.length);
			stream.write(imgData);
		}
		catch (Exception ex)
		{
			return;
		}

		FTBChunksNet.MAIN.sendToServer(new SyncTXPacket(out.toByteArray()));
	}

	@Override
	public boolean cancelOtherTasks()
	{
		return true;
	}
}