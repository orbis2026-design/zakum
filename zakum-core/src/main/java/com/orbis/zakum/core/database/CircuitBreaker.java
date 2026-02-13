package com.orbis.zakum.core.database;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Protects the server from hanging when the Database goes offline.
 * Switches to "Fail-Open" mode (Read-Only/Volatile) automatically.
 */
public class CircuitBreaker {
    private final AtomicBoolean isOpen = new AtomicBoolean(false);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);

    private static final int FAILURE_THRESHOLD = 5;
    private static final long RETRY_DELAY = 30_000; 

    public boolean canExecute() {
        if (isOpen.get()) {
            if (System.currentTimeMillis() - lastFailureTime.get() > RETRY_DELAY) {
                return true; // Probe
            }
            return false;
        }
        return true;
    }

    public void recordSuccess() {
        failureCount.set(0);
        if (isOpen.compareAndSet(true, false)) {
            System.out.println("[Orbis] Database connection recovered. Persistence resumed.");
        }
    }

    public void recordFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        if (!isOpen.get() && failureCount.incrementAndGet() >= FAILURE_THRESHOLD) {
            isOpen.set(true);
            System.err.println("[Orbis] Database unstable! Circuit OPEN. Saving disabled.");
        }
    }
}
