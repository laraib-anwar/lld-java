import java.util.*;
import java.util.concurrent.*;

// ==========================================
// 1. Core Enums and Models
// ==========================================
enum SeatStatus {
    AVAILABLE, TEMPORARILY_LOCKED, BOOKED
}

class Seat {
    private final String seatId;
    private SeatStatus status;
    private String lockedByUserId;
    private long lockTimestamp;

    public Seat(String seatId) {
        this.seatId = seatId;
        this.status = SeatStatus.AVAILABLE;
        this.lockedByUserId = null;
        this.lockTimestamp = 0;
    }

    // Thread-safe isolation for structural seat clashing
    public synchronized boolean acquireTemporaryLock(String userId) {
        if (this.status == SeatStatus.AVAILABLE) {
            this.status = SeatStatus.TEMPORARILY_LOCKED;
            this.lockedByUserId = userId;
            this.lockTimestamp = System.currentTimeMillis();
            return true;
        }
        return false; // Already locked or booked
    }

    public synchronized void releaseLock() {
        if (this.status == SeatStatus.TEMPORARILY_LOCKED) {
            this.status = SeatStatus.AVAILABLE;
            this.lockedByUserId = null;
            this.lockTimestamp = 0;
        }
    }

    public synchronized boolean confirmBooking(String userId) {
        if (this.status == SeatStatus.TEMPORARILY_LOCKED && userId.equals(this.lockedByUserId)) {
            this.status = SeatStatus.BOOKED;
            return true;
        }
        return false;
    }

    public String getSeatId() { return seatId; }
    public SeatStatus getStatus() { return status; }
    public String getLockedByUserId() { return lockedByUserId; }
}

// ==========================================
// 2. Booking Orchestrator (The System Service)
// ==========================================
class BookingService {
    private final Map<String, Seat> seatInventory = new ConcurrentHashMap<>();
    
    // Simulates Redis background expiration service
    private final ScheduledExecutorService lockExpiryScheduler = Executors.newScheduledThreadPool(1);
    private final long LOCK_TIMEOUT_MS = 3000; // 3 seconds for fast simulation (change to 5 mins in production)

    public void addSeatToInventory(Seat seat) {
        seatInventory.put(seat.getSeatId(), seat);
    }

    /**
     * Step 1: Lock the seat atomically to prevent clashing.
     */
    public boolean selectAndLockSeat(String seatId, String userId) {
        Seat seat = seatInventory.get(seatId);
        if (seat == null) return false;

        boolean lockAcquired = seat.acquireTemporaryLock(userId);

        if (lockAcquired) {
            System.out.println("🔒 [Lock Acquired] Seat " + seatId + " successfully locked by " + userId);
            
            // Schedule automated expiry (Simulating Redis Key TTL expiration behavior)
            lockExpiryScheduler.schedule(() -> {
                synchronized (seat) {
                    if (seat.getStatus() == SeatStatus.TEMPORARILY_LOCKED && userId.equals(seat.getLockedByUserId())) {
                        seat.releaseLock();
                        System.out.println("⏰ [Lock Expired] Seat " + seatId + " lock held by " + userId + " expired. Released back to inventory.");
                    }
                }
            }, LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            return true;
        } else {
            System.out.println("❌ [Seat Clash!] " + userId + " failed to grab Seat " + seatId + ". Already occupied.");
            return false;
        }
    }

    /**
     * Step 2: Confirm and convert the lock into a final booking.
     */
    public void completePaymentAndBook(String seatId, String userId) {
        Seat seat = seatInventory.get(seatId);
        if (seat == null) return;

        synchronized (seat) {
            boolean success = seat.confirmBooking(userId);
            if (success) {
                System.out.println("🎉 [Success] Payment complete! Seat " + seatId + " permanently booked for " + userId);
            } else {
                System.out.println("🚨 [Booking Failed] Could not complete booking for " + userId + " on Seat " + seatId + " (Lock might have expired).");
            }
        }
    }
}

// ==========================================
// 3. Main Execution Driver
// ==========================================
public class BookMyShowSeatClashgit  {
    public static void main(String[] args) throws InterruptedException {
        BookingService bookMyShow = new BookingService();
        bookMyShow.addSeatToInventory(new Seat("A1"));
        bookMyShow.addSeatToInventory(new Seat("A2"));

        System.out.println("=== BOOKMYSHOW CLASH ENGINE RUNNING ===\n");

        // Use standard ExecutorService to fire concurrent threads at the exact same millisecond
        ExecutorService multiThreadExecutor = Executors.newFixedThreadPool(2);

        // Scenario 1: Fast Clash Testing (Two users hit Seat A1 at once)
        System.out.println(">>> Initializing Seat Clash Scenario on Seat [A1]...");
        multiThreadExecutor.submit(() -> bookMyShow.selectAndLockSeat("A1", "User_Rahul"));
        multiThreadExecutor.submit(() -> bookMyShow.selectAndLockSeat("A1", "User_Amit"));

        Thread.sleep(500); // Small pause to decouple prints cleanly

        // Rahul successfully grabbed the lock first. Let's process his payment quickly before timeout.
        System.out.println("\n>>> Processing Fast Payment Flow...");
        bookMyShow.completePaymentAndBook("A1", "User_Rahul");

        System.out.println("\n-------------------------------------------");

        // Scenario 2: Expiration Testing (User Priya locks A2 but delays/abandons payment)
        System.out.println(">>> Initializing Abandoned Session Expiration on Seat [A2]...");
        bookMyShow.selectAndLockSeat("A2", "User_Priya");

        System.out.println("\n⏱️ Simulating user delay... waiting 4 seconds for lock expiry thread to clear...");
        Thread.sleep(4000); // Deliberately exceeding the 3-second lock boundary

        // Priya wakes up late and tries to pay
        System.out.println("\n>>> Late Payment Attempt Processing...");
        bookMyShow.completePaymentAndBook("A2", "User_Priya");

        // Shutdown pools cleanly
        multiThreadExecutor.shutdown();
        System.exit(0);
    }
}