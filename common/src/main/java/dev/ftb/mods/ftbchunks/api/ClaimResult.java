package dev.ftb.mods.ftbchunks.api;

import dev.ftb.mods.ftblibrary.config.NameMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Optional;

/**
 * Represents the result of an operation on a chunk: claiming, un-claiming, force-loading or un-force-loading.
 */
public interface ClaimResult {
	String getId();

	default boolean isSuccess() {
		return false;
	}

	/**
	 * Get the message which will be displayed in the chunk claiming GUI. This should be kept to a short phrase,
	 * easily readable, since it can be used in a transient message display.
	 *
	 * @return the message to be displayed
	 */
	default MutableComponent getMessage() {
		return Component.translatable("ftbchunks.claim_result." + getId());
	}

	/**
	 * Create a custom claim failure result. This may be of use to mods which add checks to claiming/forcing/etc. via
	 * the events in {@link dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent}; such mods can return a custom claim
	 * result from the "before" event handler as appropriate.
	 *
	 * @param translationKey the translation for message display
	 * @return a custom result
	 */
	static ClaimResult customProblem(String translationKey) {
		return new CustomProblem(translationKey);
	}

	/**
	 * Convenience method to return a successul claim outcome. This method should be used rather than returning a
	 * literal {@code null} value from "before" event handlers for future compatibility purposes.
	 *
	 * @implNote a null return from "before" events is currently treated as successful, but this should not be
	 * relied upon; use this method.
	 *
	 * @return a successful outcome
	 */
	static ClaimResult success() {
		return null;
	}

	/**
	 * Collection of standard reasons for a claim operation failing. These are returned by methods in FTB Chunks itself.
	 */
	enum StandardProblem implements ClaimResult {
		NOT_OWNER("not_owner"),
		NOT_ENOUGH_POWER("not_enough_power"),
		ALREADY_CLAIMED("already_claimed"),
		DIMENSION_FORBIDDEN("dimension_forbidden"),
		NOT_CLAIMED("not_claimed"),
		ALREADY_LOADED("already_loaded"),
		NOT_LOADED("not_loaded"),
		;

		public static final NameMap<StandardProblem> NAME_MAP = NameMap.of(NOT_OWNER, values()).create();

		private final String resultName;

		StandardProblem(String resultName) {
			this.resultName = resultName;
		}

		public static Optional<StandardProblem> forName(String name) {
			return Optional.ofNullable(NAME_MAP.get(name));
		}

		@Override
		public String getId() {
			return resultName;
		}
	}

	/**
	 * This class can be used to represent custom reasons why a claim operation cannot be completed. Intended for use
	 * by other mods when creating a return value for the claim events in {@link dev.ftb.mods.ftbchunks.api.event.ClaimedChunkEvent}.
	 * Use {@link ClaimResult#customProblem(String)} to create instances of this class.
	 */
	class CustomProblem implements ClaimResult {
		private final String name;

		private CustomProblem(String name) {
			this.name = name;
		}

		@Override
		public String getId() {
			return name;
		}

		@Override
		public MutableComponent getMessage() {
			return Component.translatable(name);
		}
	}
}