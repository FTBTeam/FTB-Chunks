package dev.ftb.mods.ftbchunks.event;

import dev.ftb.mods.ftbchunks.data.ClaimResult;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import me.shedaniel.architectury.event.CompoundEventResult;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;
import net.minecraft.commands.CommandSourceStack;

/**
 * @author LatvianModder
 */
public interface ClaimedChunkEvent {
	Event<Before> BEFORE_CLAIM = EventFactory.createCompoundEventResult();
	Event<Before> BEFORE_LOAD = EventFactory.createCompoundEventResult();
	Event<Before> BEFORE_UNCLAIM = EventFactory.createCompoundEventResult();
	Event<Before> BEFORE_UNLOAD = EventFactory.createCompoundEventResult();
	Event<After> AFTER_CLAIM = EventFactory.createLoop();
	Event<After> AFTER_LOAD = EventFactory.createLoop();
	Event<After> AFTER_UNCLAIM = EventFactory.createLoop();
	Event<After> AFTER_UNLOAD = EventFactory.createLoop();

	interface Before {
		CompoundEventResult<ClaimResult> before(CommandSourceStack s, ClaimedChunk a);
	}

	interface After {
		void after(CommandSourceStack s, ClaimedChunk a);
	}
}