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
		ClaimedChunkEvent.BEFORE_CLAIM.register((source, chunk) -> before(source, chunk, "ftbchunks.before.claim"));
		ClaimedChunkEvent.BEFORE_LOAD.register((source, chunk) -> before(source, chunk, "ftbchunks.before.load"));
		ClaimedChunkEvent.BEFORE_UNCLAIM.register((source, chunk) -> before(source, chunk, "ftbchunks.before.unclaim"));
		ClaimedChunkEvent.BEFORE_UNLOAD.register((source, chunk) -> before(source, chunk, "ftbchunks.before.unload"));
		ClaimedChunkEvent.AFTER_CLAIM.register((source, chunk) -> after(source, chunk, "ftbchunks.after.claim"));
		ClaimedChunkEvent.AFTER_LOAD.register((source, chunk) -> after(source, chunk, "ftbchunks.after.load"));
		ClaimedChunkEvent.AFTER_UNCLAIM.register((source, chunk) -> after(source, chunk, "ftbchunks.after.unclaim"));
		ClaimedChunkEvent.AFTER_UNLOAD.register((source, chunk) -> after(source, chunk, "ftbchunks.after.unload"));
	}

	@Override
	public void addTypeWrappers(ScriptType type, TypeWrappers typeWrappers) {
		typeWrappers.register(ClaimResult.class, o -> ClaimResults.valueOf(o.toString().toUpperCase()));
	}

	private CompoundEventResult<ClaimResult> before(CommandSourceStack source, ClaimedChunk chunk, String id) {
		BeforeEventJS e = new BeforeEventJS(source, chunk);

		if (e.post(ScriptType.SERVER, id) && e.getResult() != null) {
			return CompoundEventResult.interrupt(false, e.getResult());
		}

		return CompoundEventResult.pass();
	}

	private void after(CommandSourceStack source, ClaimedChunk chunk, String id) {
		new AfterEventJS(source, chunk).post(ScriptType.SERVER, id);
	}
}
