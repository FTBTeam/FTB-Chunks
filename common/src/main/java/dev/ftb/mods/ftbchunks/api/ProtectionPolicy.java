package dev.ftb.mods.ftbchunks.api;

public enum ProtectionPolicy {
	/**
	 * Carry out extra checks before deciding if the action should be permitted
	 */
	CHECK,
	/**
	 * Unconditionally deny permission
	 */
	DENY,
	/**
	 * Unconditionally grant permission
	 */
	ALLOW;

	public boolean isOverride() {
		return this != CHECK;
	}

	public boolean shouldPreventInteraction() {
		return this == DENY;
	}
}
