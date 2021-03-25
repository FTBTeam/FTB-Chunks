package dev.ftb.mods.ftbchunks.data;

/**
 * @author LatvianModder
 */
public abstract class FTBChunksAPI {
	public static FTBChunksAPI INSTANCE;

	public abstract ClaimedChunkManager getManager();
}