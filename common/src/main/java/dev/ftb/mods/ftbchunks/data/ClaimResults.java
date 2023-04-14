package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftblibrary.config.NameMap;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public enum ClaimResults implements ClaimResult {
	OTHER("other"),
	NOT_OWNER("not_owner"),
	NOT_ENOUGH_POWER("not_enough_power"),
	ALREADY_CLAIMED("already_claimed"),
	DIMENSION_FORBIDDEN("dimension_forbidden"),
	NOT_CLAIMED("not_claimed"),
	ALREADY_LOADED("already_loaded"),
	NOT_LOADED("not_loaded"),
	;

	public static final NameMap<ClaimResults> NAME_MAP = NameMap.of(OTHER, values()).create();

	private final String resultName;

	ClaimResults(String resultName) {
		this.resultName = resultName;
	}

	public static Optional<ClaimResults> forName(String name) {
		return Optional.ofNullable(NAME_MAP.get(name));
	}

	@Override
	public String claimResultName() {
		return resultName;
	}
}