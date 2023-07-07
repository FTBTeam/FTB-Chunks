package dev.ftb.mods.ftbchunks.client.map;

@FunctionalInterface
public interface MapTask extends Runnable {
	void runMapTask() throws Exception;

	@Override
	default void run() {
		try {
			runMapTask();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	default boolean cancelOtherTasks() {
		return false;
	}
}