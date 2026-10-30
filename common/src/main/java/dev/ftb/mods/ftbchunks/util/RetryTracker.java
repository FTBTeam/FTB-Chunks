package dev.ftb.mods.ftbchunks.util;

/**
 * Utility class which tracks attempts at some operation which is prone to failure, tracking the number of attempts
 * and the delay until a new attempt is made.
 */
public class RetryTracker {
    private static final long BASE_RETRY_DELAY_MS = 5_000L;
    private static final long MAX_RETRY_DELAY_MS = 5 * 60_000L;

    private final long baseRetryMs;
    private final long maxRetryMs;

    private int consecutiveFailures = 0;
    private long nextRetryAllowedAtMs = 0L;

    public RetryTracker() {
        this(BASE_RETRY_DELAY_MS, MAX_RETRY_DELAY_MS);
    }

    public RetryTracker(long baseRetryMs, long maxRetryMs) {
        this.baseRetryMs = baseRetryMs;
        this.maxRetryMs = maxRetryMs;
    }

    public void succeeded() {
        consecutiveFailures = 0;
    }

    public void failed() {
        consecutiveFailures++;
        nextRetryAllowedAtMs = System.currentTimeMillis() + getDelay();
    }

    public long getDelay() {
        return Math.min(
                baseRetryMs * (1L << Math.min(consecutiveFailures, 6)),
                maxRetryMs
        );
    }

    public boolean readyToTry() {
        return consecutiveFailures == 0 || System.currentTimeMillis() >= nextRetryAllowedAtMs;
    }

    public int failureCount() {
        return consecutiveFailures;
    }
}
