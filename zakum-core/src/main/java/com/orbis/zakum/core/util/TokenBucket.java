package com.orbis.zakum.core.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Lock-free Token Bucket for high-concurrency rate limiting.
 * Prevents packet spam from crashing the main thread.
 */
public class TokenBucket {
    private final long capacity;
    private final long refillTokensPerSecond;
    private final AtomicLong lastRefillTimestamp;
    private final AtomicLong tokens;

    public TokenBucket(long maxTokens, long refillTokensPerSecond) {
        this.capacity = maxTokens;
        this.refillTokensPerSecond = refillTokensPerSecond;
        this.lastRefillTimestamp = new AtomicLong(System.nanoTime());
        this.tokens = new AtomicLong(maxTokens);
    }

    public boolean tryConsume(long cost) {
        refill();
        long currentTokens = tokens.get();
        if (currentTokens >= cost) {
            while (currentTokens >= cost) {
                if (tokens.compareAndSet(currentTokens, currentTokens - cost)) {
                    return true;
                }
                currentTokens = tokens.get();
            }
        }
        return false;
    }

    private void refill() {
        long now = System.nanoTime();
        long last = lastRefillTimestamp.get();
        long nanosPerToken = 1_000_000_000 / refillTokensPerSecond;
        if (now - last > nanosPerToken) {
            long newTokens = (now - last) / nanosPerToken;
            if (newTokens > 0) {
                if (lastRefillTimestamp.compareAndSet(last, now)) {
                    tokens.updateAndGet(t -> Math.min(capacity, t + newTokens));
                }
            }
        }
    }
}
