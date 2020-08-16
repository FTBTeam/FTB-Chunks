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
	public final long now;

	public SyncTXTask(MapRegion r, long s)
	{
		region = r;
		now = s;
	}

	@Override
	public void runMapTask()
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(out))))
		{
			stream.writeUTF(region.dimension.dimension);
			stream.writeInt(region.pos.x);
			stream.writeInt(region.pos.z);
			stream.writeShort(region.getChunks().size());

			byte[] imgData = region.getDataImage().getBytes();
			stream.writeInt(imgData.length);
			stream.write(imgData);

			for (MapChunk chunk : region.getChunks().values())
			{
				stream.writeByte(chunk.pos.x);
				stream.writeByte(chunk.pos.z);
				stream.writeLong(now - chunk.modified);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
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