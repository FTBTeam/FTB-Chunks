package dev.ftb.mods.ftbchunks.api;

/**
 * @author LatvianModder
 */
public abstract class FTBChunksAPI {
	public static FTBChunksAPI INSTANCE;

	public abstract ClaimedChunkManager getManager();
}