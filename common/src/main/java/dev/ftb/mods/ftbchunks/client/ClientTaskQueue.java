package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.map.MapTask;

import java.util.ArrayDeque;

public class ClientTaskQueue {
    private static final ArrayDeque<MapTask> taskQueue = new ArrayDeque<>();

    public static void queue(MapTask task) {
        taskQueue.addLast(task);
    }

    static void runQueuedTasks() {
        int nTasks = Math.min(taskQueue.size(), FTBChunksClientConfig.TASK_QUEUE_MAX.get());

        if (nTasks > 0) {
            MapTask[] tasks = new MapTask[nTasks];

            for (int i = 0; i < nTasks; i++) {
                tasks[i] = taskQueue.pollFirst();
                if (tasks[i] == null || tasks[i].cancelOtherTasks()) {
                    break;
                }
            }

            for (MapTask task : tasks) {
                if (task != null) {
                    try {
                        task.runMapTask();
                    } catch (Exception ex) {
                        FTBChunks.LOGGER.error("Failed to run task " + task);
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public static void flushTasks() {
        MapTask task;
        while ((task = taskQueue.pollFirst()) != null) {
            try {
                task.runMapTask();
            } catch (Exception ex) {
                FTBChunks.LOGGER.error("Failed to run task " + task);
                ex.printStackTrace();
            }
        }
    }

    public static void dumpTaskInfo() {
        FTBChunks.LOGGER.info("=== Task Queue: " + taskQueue.size());
        taskQueue.stream().map(Object::toString).forEach(FTBChunks.LOGGER::info);
        FTBChunks.LOGGER.info("===");
    }

    public static int queueSize() {
        return taskQueue.size();
    }
}
