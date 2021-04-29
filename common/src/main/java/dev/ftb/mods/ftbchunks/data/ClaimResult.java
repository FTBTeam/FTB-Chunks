package dev.ftb.mods.ftbchunks.data;

/**
 * @author LatvianModder
 */
public interface ClaimResult {
	default boolean isSuccess() {
		return false;
	}

	default void setClaimedTime(long time) {
	}

	default void setForceLoadedTime(long time) {
	}
}