package dev.ftb.mods.ftbchunks.data;

/**
 * @author LatvianModder
 */
public enum PrivacyMode {
	PRIVATE("private"),
	ALLIES("allies"),
	PUBLIC("public");

	public static final PrivacyMode[] VALUES = values();

	public static PrivacyMode get(String name) {
		if (name.equals("private")) {
			return PRIVATE;
		} else if (name.equals("public")) {
			return PUBLIC;
		}

		return ALLIES;
	}

	public final String name;

	PrivacyMode(String n) {
		name = n;
	}
}