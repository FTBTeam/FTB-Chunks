package dev.ftb.mods.ftbchunks.core;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;

/**
 * @author LatvianModder
 */
public interface ChunkMapFTBC {
	Long2ObjectLinkedOpenHashMap<ChunkHolder> getChunksFTBC();
}
