package dev.ftb.mods.ftbchunks.integration.kubejs;

import dev.architectury.event.CompoundEventResult;
import dev.ftb.mods.ftbchunks.data.ClaimResult;
import dev.ftb.mods.ftbchunks.data.ClaimResults;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftbchunks.event.ClaimedChunkEvent;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
import net.minecraft.commands.CommandSourceStack;

public class FTBChunksKubeJSPlugin extends KubeJSPlugin {
	@Override
	public void init() {
		ClaimedChunkEvent.BEFORE_CLAIM.register((source, chunk) -> before(source, chunk, "claim"));
		ClaimedChunkEvent.BEFORE_LOAD.register((source, chunk) -> before(source, chunk, "load"));
		ClaimedChunkEvent.BEFORE_UNCLAIM.register((source, chunk) -> before(source, chunk, "unclaim"));
		ClaimedChunkEvent.BEFORE_UNLOAD.register((source, chunk) -> before(source, chunk, "unload"));
		ClaimedChunkEvent.AFTER_CLAIM.register((source, chunk) -> after(source, chunk, "claim"));
		ClaimedChunkEvent.AFTER_LOAD.register((source, chunk) -> after(source, chunk, "load"));
		ClaimedChunkEvent.AFTER_UNCLAIM.register((source, chunk) -> after(source, chunk, "unclaim"));
		ClaimedChunkEvent.AFTER_UNLOAD.register((source, chunk) -> after(source, chunk, "unload"));
	}

	@Override
	public void registerEvents() {
		FTBChunksKubeJSEvents.EVENT_GROUP.register();
	}

	@Override
	public void registerTypeWrappers(ScriptType type, TypeWrappers typeWrappers) {
		typeWrappers.register(ClaimResult.class, (ctx, o) -> ClaimResults.valueOf(o.toString().toUpperCase()));
    }

	private CompoundEventResult<ClaimResult> before(CommandSourceStack source, ClaimedChunk chunk, String id) {
		BeforeEventJS kjsEvent = new BeforeEventJS(source, chunk);

        if (FTBChunksKubeJSEvents.BEFORE.post(id, kjsEvent)) {
			return CompoundEventResult.interrupt(false, kjsEvent.getResult());
		}

		return CompoundEventResult.pass();
	}

	private void after(CommandSourceStack source, ClaimedChunk chunk, String id) {
        FTBChunksKubeJSEvents.AFTER.post(id, new AfterEventJS(source, chunk));
	}
}
