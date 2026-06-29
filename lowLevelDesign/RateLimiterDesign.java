import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// ==========================================
// 1. Core Enums and Models
// ==========================================
enum UserTier {
    FREE, PREMIUM
}

// Defines limits per tier
class TierLimit {
    final long capacity;
    final long windowOrRefillRate; // Refill rate for TokenBucket; Window size in ms for windows

    public TierLimit(long capacity, long windowOrRefillRate) {
        this.capacity = capacity;
        this.windowOrRefillRate = windowOrRefillRate;
    }
}

// ==========================================
// 2. Strategy Pattern Interfaces
// ==========================================
interface RateLimitingStrategy {
    boolean allowRequest(String clientId, TierLimit limit);
}

// ==========================================
// 3. Concrete Strategy Implementations
// ==========================================

/**
 * 1. TOKEN BUCKET STRATEGY
 * Lazily computes and tracks tokens per client.
 */
class TokenBucketStrategy implements RateLimitingStrategy {
    private final Map<String, BucketState> states = new ConcurrentHashMap<>();

    private static class BucketState {
        double tokens;
        long lastRefillTime;

        BucketState(double capacity) {
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }
    }

    @Override
    public boolean allowRequest(String clientId, TierLimit limit) {
        BucketState state = states.computeIfAbsent(clientId, k -> new BucketState(limit.capacity));

        synchronized (state) {
            long now = System.currentTimeMillis();
            long elapsedTime = now - state.lastRefillTime;
            
            // Refill tokens lazily (windowOrRefillRate acts as tokens-per-second)
            double tokensToAdd = (elapsedTime / 1000.0) * limit.windowOrRefillRate;
            if (tokensToAdd > 0) {
                state.tokens = Math.min(limit.capacity, state.tokens + tokensToAdd);
                state.lastRefillTime = now;
            }

            if (state.tokens >= 1.0) {
                state.tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}

/**
 * 2. FIXED WINDOW STRATEGY
 * Divides time into fixed structural windows (e.g., 1-minute blocks).
 */
class FixedWindowStrategy implements RateLimitingStrategy {
    private final Map<String, WindowState> states = new ConcurrentHashMap<>();

    private static class WindowState {
        long windowId;
        long count;

        WindowState(long windowId) {
            this.windowId = windowId;
            this.count = 0;
        }
    }

    @Override
    public boolean allowRequest(String clientId, TierLimit limit) {
        WindowState state = states.computeIfAbsent(clientId, k -> new WindowState(System.currentTimeMillis() / limit.windowOrRefillRate));

        synchronized (state) {
            long currentWindowId = System.currentTimeMillis() / limit.windowOrRefillRate;

            // If the window boundary is crossed, reset the counter
            if (currentWindowId != state.windowId) {
                state.windowId = currentWindowId;
                state.count = 0;
            }

            if (state.count < limit.capacity) {
                state.count++;
                return true;
            }
            return false;
        }
    }
}

/**
 * 3. SLIDING WINDOW LOG STRATEGY
 * Tracks precise historical timestamps to prevent boundary bursts.
 */
class SlidingWindowLogStrategy implements RateLimitingStrategy {
    private final Map<String, Queue<Long>> userLogs = new ConcurrentHashMap<>();

    @Override
    public boolean allowRequest(String clientId, TierLimit limit) {
        Queue<Long> timestamps = userLogs.computeIfAbsent(clientId, k -> new LinkedList<>());

        synchronized (timestamps) {
            long now = System.currentTimeMillis();
            long windowStart = now - limit.windowOrRefillRate; // windowOrRefillRate is window size in ms

            // Evict outdated entries outside the sliding window boundary
            while (!timestamps.isEmpty() && timestamps.peek() < windowStart) {
                timestamps.poll();
            }

            if (timestamps.size() < limit.capacity) {
                timestamps.add(now);
                return true;
            }
            return false;
        }
    }
}

// ==========================================
// 4. Rate Limiter Orchestrator (Context Class)
// ==========================================
class FlexibleRateLimiter {
    private final RateLimitingStrategy strategy;
    private final Map<UserTier, TierLimit> tierRules;

    public FlexibleRateLimiter(RateLimitingStrategy strategy) {
        this.strategy = strategy;
        this.tierRules = new HashMap<>();
        configureDefaultTiers();
    }

    private void configureDefaultTiers() {
        // FREE tier: Max 2 requests. 
        // For window strategies, window is 2000ms. For token bucket, refill rate is 1 token/sec.
        tierRules.put(UserTier.FREE, new TierLimit(2, 2000));
        
        // PREMIUM tier: High throughput (Max 5 requests)
        tierRules.put(UserTier.PREMIUM, new TierLimit(5, 2000));
    }

    public boolean isAllowed(String clientId, UserTier tier) {
        TierLimit limit = tierRules.get(tier);
        return strategy.allowRequest(clientId, limit);
    }
}

// ==========================================
// 5. Main Driver Execution
// ==========================================
public class Solution {
    public static void main(String[] args) throws InterruptedException {
        // Switch out strategies instantly here: 
        // new TokenBucketStrategy() OR new FixedWindowStrategy() OR new SlidingWindowLogStrategy()
        RateLimitingStrategy chosenStrategy = new FixedWindowStrategy(); 
        
        FlexibleRateLimiter rateLimiter = new FlexibleRateLimiter(chosenStrategy);

        System.out.println("--- Testing with Strategy: " + chosenStrategy.getClass().getSimpleName() + " ---\n");

        // Client 1: Free Tier (Limit: 2 requests per window)
        String freeUser = "user_free_anonymous";
        System.out.println(">>> Executing requests for FREE User (Limit: 2)");
        for (int i = 1; i <= 3; i++) {
            boolean allowed = rateLimiter.isAllowed(freeUser, UserTier.FREE);
            System.out.println("Request " + i + " -> Allowed: " + allowed);
        }

        System.out.println();

        // Client 2: Premium Tier (Limit: 5 requests per window)
        String premiumUser = "user_premium_vip";
        System.out.println(">>> Executing requests for PREMIUM User (Limit: 5)");
        for (int i = 1; i <= 6; i++) {
            boolean allowed = rateLimiter.isAllowed(premiumUser, UserTier.PREMIUM);
            System.out.println("Request " + i + " -> Allowed: " + allowed);
        }
    }
}