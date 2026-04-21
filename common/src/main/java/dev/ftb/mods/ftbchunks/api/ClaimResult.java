package dev.ftb.mods.ftbchunks.api;

import dev.ftb.mods.ftblibrary.util.NameMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Optional;

/// Represents the result of an operation on a chunk: claiming, un-claiming, force-loading or un-force-loading.
public interface ClaimResult {
	/// Get the result's unique ID. Primarily used to generate translation keys for display purposes.
	///
	/// @return the ID
	String getResultId();

	/// Was the operation successful?
	///
	/// @return true if the operation succeeded, false is there was a problem
	default boolean isSuccess() {
		return false;
	}

	/// Get the message which will be displayed in the chunk claiming GUI. This should be kept to a short phrase,
	/// easily readable, since it can be used in a transient message display.
	///
	/// @return the message to be displayed
	default MutableComponent getMessage() {
		return Component.translatable(getResultId());
	}

	/// Create a custom claim failure result. This may be of use to mods which add extra checks to claiming/forcing/etc.
	/// via the events in [dev.ftb.mods.ftbchunks.api.event.ChunkChangeEvent]; such mods can return a custom claim
	/// result from the "before" event handler as appropriate.
	///
	/// @param translationKey the translation key for message display
	/// @return a custom result
	static ClaimResult customProblem(String translationKey) {
		return new CustomProblem(translationKey);
	}

	/// Convenience method to return a successful claim outcome.
	///
	/// @return a successful outcome
	static ClaimResult success() {
		return SUCCESS;
	}

	/// Collection of standard reasons for a claim operation failing. These are returned by methods in FTB Chunks itself.
	enum StandardProblem implements ClaimResult {
		NOT_OWNER("not_owner"),
		NOT_ENOUGH_POWER("not_enough_power"),
		ALREADY_CLAIMED("already_claimed"),
		DIMENSION_FORBIDDEN("dimension_forbidden"),
		NOT_CLAIMED("not_claimed"),
		ALREADY_LOADED("already_loaded"),
		NOT_LOADED("not_loaded"),
		;

		public static final NameMap<StandardProblem> NAME_MAP = NameMap.of(NOT_OWNER, values()).baseNameKey("ftbchunks.standard_problem").create();

		private final String resultId;

		StandardProblem(String resultId) {
			this.resultId = "ftbchunks.claim_result." + resultId;
		}

		public static Optional<StandardProblem> forName(String name) {
			return Optional.ofNullable(NAME_MAP.getNullable(name));
		}

		@Override
		public String getResultId() {
			return resultId;
		}
	}

	/// This class can be used to represent custom reasons why a claim operation cannot be completed. Intended for use
	/// by other mods when creating a return value for the claim events in [dev.ftb.mods.ftbchunks.api.event.ChunkChangeEvent].
	/// Use [ClaimResult#customProblem(String)] to create instances of this class.
	class CustomProblem implements ClaimResult {
		private final String translationKey;

		private CustomProblem(String translationKey) {
			this.translationKey = translationKey;
		}

		@Override
		public String getResultId() {
			return translationKey;
		}
	}

	ClaimResult SUCCESS = new ClaimResult() {
		@Override
		public String getResultId() {
			return "gui.ok";
		}

		@Override
		public boolean isSuccess() {
			return true;
		}
	};

}
