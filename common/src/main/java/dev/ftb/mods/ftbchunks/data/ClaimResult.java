package dev.ftb.mods.ftbchunks.data;

/**
 * @author LatvianModder
 */
public interface ClaimResult {
	String claimResultName();

	default boolean isSuccess() {
		return false;
	}

	default void setClaimedTime(long time) {
	}

	default void setForceLoadedTime(long time) {
	}

	default String getTranslationKey() {
		return "ftbchunks.claim_result." + claimResultName();
	}
}