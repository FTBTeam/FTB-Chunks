package dev.ftb.mods.ftbchunks.client.map;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface MapTask {
	void runMapTask(MapManager manager) throws Exception;

	default boolean cancelOtherTasks() {
		return false;
	}
}