package dev.ftb.mods.ftbchunks.fabric;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksAPIImpl;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.api.event.ChunkChangeEvent;
import dev.ftb.mods.ftbchunks.api.event.CustomMinYEvent;
import dev.ftb.mods.ftbchunks.api.fabric.FTBChunksEvents;
import dev.ftb.mods.ftbchunks.util.platform.PlatformProtections;
import dev.ftb.mods.ftblibrary.util.fabric.FabricEventHelper;
import net.fabricmc.api.ModInitializer;

public class FTBChunksFabric implements ModInitializer {

    @Override
	public void onInitialize() {
        FTBChunks chunks = new FTBChunks();

		FabricEventListeners.INSTANCE.init(chunks);

		FTBChunksAPIImpl.INSTANCE.setForceLoadHandler((level, _, chunkX, chunkZ, add) ->
				level.setChunkForced(chunkX, chunkZ, add)
		);

		FTBChunksAPIImpl.INSTANCE.setPlatformProtections(new PlatformProtections.Impl(
				Protection.EDIT_AND_INTERACT_BLOCK,
				Protection.EDIT_AND_INTERACT_BLOCK,
				Protection.EDIT_AND_INTERACT_BLOCK
		));

		registerFabricEventPosters();
	}

	private static void registerFabricEventPosters() {
		FabricEventHelper.registerFabricEventPoster(CustomMinYEvent.Data.class, FTBChunksEvents.CUSTOM_MIN_Y);
		FabricEventHelper.registerFabricEventPosterFunction(ChunkChangeEvent.Pre.TYPE, FTBChunksEvents.CHUNK_CHANGE_PRE);
		FabricEventHelper.registerFabricEventPoster(ChunkChangeEvent.Post.Data.class, FTBChunksEvents.CHUNK_CHANGE_POST);
	}

}
