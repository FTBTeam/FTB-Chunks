package dev.ftb.mods.ftbchunks.client.map;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface MapTask {
	void runMapTask(MapManager manager);

	default boolean cancelOtherTasks() {
		return false;
	}
}