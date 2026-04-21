package dev.ftb.mods.ftbchunks.neoforge;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksAPIImpl;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.api.event.ChunkChangeEvent;
import dev.ftb.mods.ftbchunks.api.event.CustomMinYEvent;
import dev.ftb.mods.ftbchunks.api.neoforge.FTBChunksEvent;
import dev.ftb.mods.ftbchunks.neoforge.integration.FTBBackups3Integration;
import dev.ftb.mods.ftbchunks.util.platform.PlatformProtections;
import dev.ftb.mods.ftblibrary.platform.event.NativeEventPosting;
import dev.ftb.mods.ftblibrary.util.neoforge.NeoEventHelper;
import dev.ftb.mods.ftblibrary.util.result.DataOutcome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(FTBChunks.MOD_ID)
public class FTBChunksNeoForge {
    public FTBChunksNeoForge(IEventBus modEventBus) {
        FTBChunks ftbChunks = new FTBChunks();

		new NeoEventListeners(ftbChunks);

		if (ModList.get().isLoaded("ftbbackups3")) {
			FTBBackups3Integration.init();
		}

		ForceLoading.init(modEventBus);

		FTBChunksAPIImpl.INSTANCE.setPlatformProtections(new PlatformProtections.Impl(
				Protection.EDIT_BLOCK,
				Protection.INTERACT_BLOCK,
				Protection.EDIT_BLOCK
		));

		registerNeoEventPosters();
	}

	private static void registerNeoEventPosters() {
		NeoEventHelper.registerNeoEventPoster(NeoForge.EVENT_BUS, CustomMinYEvent.Data.class, FTBChunksEvent.RegisterCustomMinYCalculator::new);

		NativeEventPosting.INSTANCE.registerEventWithResult(ChunkChangeEvent.Pre.TYPE, data -> {
			FTBChunksEvent.ChunkChange.Pre event = new FTBChunksEvent.ChunkChange.Pre(data);
			NeoForge.EVENT_BUS.post(event);
            return event.getResult().isSuccess() ? DataOutcome.success() : DataOutcome.fail(event.getResult());
		});

		NeoEventHelper.registerNeoEventPoster(NeoForge.EVENT_BUS, ChunkChangeEvent.Post.Data.class, FTBChunksEvent.ChunkChange.Post::new);
	}
}
