package dev.ftb.mods.ftbchunks.client.map;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface MapTask extends Runnable {
	void runMapTask(MapManager manager) throws Exception;

	@Override
	default void run() {
		try {
			runMapTask(MapManager.inst);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	default boolean cancelOtherTasks() {
		return false;
	}
}