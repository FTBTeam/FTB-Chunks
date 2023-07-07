package dev.ftb.mods.ftbchunks.api.event;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import net.minecraft.commands.CommandSourceStack;

/**
 * Events which are fired before and after a chunk operation is carried out: claiming, un-claiming, force-loading, or
 * un-force-loading.
 * <p>
 * The "before" events are cancellable. They must <strong>not</strong> be used to alter any mod state that you maintain,
 * since they may be fired for a simulated operation. Use the "after" events if you intend to update any state.
 */
public interface ClaimedChunkEvent {
	/**
	 * Fired when a chunk is about to be claimed. This can be cancelled by returning a non-null claim result; it is
	 * recommended to use {@link ClaimResult#customProblem(String)} for this.
	 */
	Event<Before> BEFORE_CLAIM = EventFactory.createCompoundEventResult();
	/**
	 * Fired when a chunk is about to be force-loaded. This can be cancelled by returning a non-null claim result; it is
	 * recommended to use {@link ClaimResult#customProblem(String)} for this.
	 */
	Event<Before> BEFORE_LOAD = EventFactory.createCompoundEventResult();
	/**
	 * Fired when a chunk is about to be un-claimed. This can be cancelled by returning a non-null claim result; it is
	 * recommended to use {@link ClaimResult#customProblem(String)} for this.
	 */
	Event<Before> BEFORE_UNCLAIM = EventFactory.createCompoundEventResult();
	/**
	 * Fired when a chunk is about to be un-force-loaded. This can be cancelled by returning a non-null claim result; it is
	 * recommended to use {@link ClaimResult#customProblem(String)} for this.
	 */
	Event<Before> BEFORE_UNLOAD = EventFactory.createCompoundEventResult();

	/**
	 * Fired after a chunk has been claimed. This event is not cancellable.
	 */
	Event<After> AFTER_CLAIM = EventFactory.createLoop();
	/**
	 * Fired after a chunk has been force-loaded. This event is not cancellable.
	 */
	Event<After> AFTER_LOAD = EventFactory.createLoop();
	/**
	 * Fired after a chunk has been un-claimed. This event is not cancellable.
	 */
	Event<After> AFTER_UNCLAIM = EventFactory.createLoop();
	/**
	 * Fired after a chunk has been un-force-loaded. This event is not cancellable.
	 */
	Event<After> AFTER_UNLOAD = EventFactory.createLoop();

	interface Before {
		/**
		 * For the "before" events.
		 *
		 * @param sourceStack the source of the operation (player or console)
		 * @param claimedChunk the chunk in question
		 * @return {@link ClaimResult#success()} to allow the operation, or either {@link ClaimResult#customProblem(String)}
		 * or a member of {@link ClaimResult.StandardProblem} to prevent the operation
		 */
		CompoundEventResult<ClaimResult> before(CommandSourceStack sourceStack, ClaimedChunk claimedChunk);
	}

	interface After {
		/**
		 * For the "after" events.
		 *
		 * @param sourceStack the source of the operation (player or console)
		 * @param claimedChunk the chunk in question
		 */
		void after(CommandSourceStack sourceStack, ClaimedChunk claimedChunk);
	}
}