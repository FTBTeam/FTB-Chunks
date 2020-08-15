package com.feed_the_beast.mods.ftbchunks.client.map;

/**
 * @author LatvianModder
 */
public class SyncRXTask implements MapTask
{
	public final byte[] data;

	public SyncRXTask(byte[] d)
	{
		data = d;
	}

	@Override
	public void runMapTask()
	{
		System.out.println("RX " + data.length);
	}

	@Override
	public boolean cancelOtherTasks()
	{
		return true;
	}
}