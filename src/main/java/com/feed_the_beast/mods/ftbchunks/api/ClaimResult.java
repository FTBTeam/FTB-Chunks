package com.feed_the_beast.mods.ftbchunks.api;

import javax.annotation.Nullable;
import java.time.Instant;

/**
 * @author LatvianModder
 */
public interface ClaimResult {
	default boolean isSuccess() {
		return false;
	}

	default void setClaimedTime(Instant time) {
	}

	default void setForceLoadedTime(@Nullable Instant time) {
	}
}