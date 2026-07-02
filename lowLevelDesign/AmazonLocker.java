import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// ==========================================
// 1. Core Enums & Data Models
// ==========================================
enum LockerSize {
    SMALL, MEDIUM, LARGE
}

enum LockerState {
    AVAILABLE, BOOKED, CLOSED_WITH_PACKAGE
}

class Package {
    private final String id;
    private final LockerSize size;

    public Package(String id, LockerSize size) {
        this.id = id;
        this.size = size;
    }

    public String getId() { return id; }
    public LockerSize getSize() { return size; }
}

// ==========================================
// 2. Locker Component
// ==========================================
class Locker {
    private final String id;
    private final LockerSize size;
    private LockerState state;
    private Package currentPackage;
    private String accessCode;

    public Locker(String id, LockerSize size) {
        this.id = id;
        this.size = size;
        this.state = LockerState.AVAILABLE;
        this.currentPackage = null;
        this.accessCode = null;
    }

    public synchronized boolean assignPackage(Package pkg, String code) {
        if (this.state != LockerState.AVAILABLE) return false;
        this.currentPackage = pkg;
        this.accessCode = code;
        this.state = LockerState.CLOSED_WITH_PACKAGE;
        return true;
    }

    public synchronized Package removePackage() {
        Package pkg = this.currentPackage;
        this.currentPackage = null;
        this.accessCode = null;
        this.state = LockerState.AVAILABLE;
        return pkg;
    }

    public String getId() { return id; }
    public LockerSize getSize() { return size; }
    public LockerState getState() { return state; }
    public String getAccessCode() { return accessCode; }
}

// ==========================================
// 3. Locker Service Orchestrator
// ==========================================
class LockerService {
    // Organizes lockers by size for O(1) or fast structural matching
    private final Map<LockerSize, List<Locker>> lockerPool;
    // Map to quickly find which locker has the code during pickup
    private final Map<String, Locker> codeToLockerMap;

    public LockerService() {
        this.lockerPool = new ConcurrentHashMap<>();
        this.codeToLockerMap = new ConcurrentHashMap<>();
        
        for (LockerSize size : LockerSize.values()) {
            lockerPool.put(size, new ArrayList<>());
        }
    }

    public void addLocker(Locker locker) {
        lockerPool.get(locker.getSize()).add(locker);
    }

    /**
     * Allocates a locker for a delivery package and generates an access code.
     * Strategy: Tries to find the exact match first, then looks for larger options.
     */
    public synchronized String depositPackage(Package pkg) {
        Locker targetLocker = null;

        // Iterate through locker sizes starting from the required size upwards
        for (LockerSize size : LockerSize.values()) {
            if (size.ordinal() >= pkg.getSize().ordinal()) {
                targetLocker = findAvailableLocker(size);
                if (targetLocker != null) break;
            }
        }

        if (targetLocker == null) {
            System.out.println("❌ No matching locker available for package: " + pkg.getId());
            return null;
        }

        String accessCode = generateUniqueCode();
        targetLocker.assignPackage(pkg, accessCode);
        codeToLockerMap.put(accessCode, targetLocker);
        
        System.out.println("📦 Package " + pkg.getId() + " (" + pkg.getSize() + ") deposited into Locker " 
                            + targetLocker.getId() + " (" + targetLocker.getSize() + "). Code generated: " + accessCode);
        return accessCode;
    }

    /**
     * Simulates a customer picking up a package using their code.
     */
    public Package pickupPackage(String code) {
        Locker locker = codeToLockerMap.get(code);
        
        if (locker == null || !code.equals(locker.getAccessCode())) {
            System.out.println("❌ Invalid access code: " + code);
            return null;
        }

        synchronized (locker) {
            Package pkg = locker.removePackage();
            codeToLockerMap.remove(code);
            System.out.println("🔓 Locker " + locker.getId() + " opened. Customer collected package: " + pkg.getId());
            return pkg;
        }
    }

    private Locker findAvailableLocker(LockerSize size) {
        for (Locker locker : lockerPool.get(size)) {
            if (locker.getState() == LockerState.AVAILABLE) {
                return locker;
            }
        }
        return null;
    }

    private String generateUniqueCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}

// ========================================== A
// 4. Main Execution Driver
// ==========================================
public class AmazonLocker {
    public static void main(String[] args) {
        LockerService deliveryHub = new LockerService();

        // Setup the physical infrastructure
        deliveryHub.addLocker(new Locker("L-Small-1", LockerSize.SMALL));
        deliveryHub.addLocker(new Locker("L-Med-1", LockerSize.MEDIUM));
        deliveryHub.addLocker(new Locker("L-Large-1", LockerSize.LARGE));

        System.out.println("--- Amazon Locker System Operational ---\n");

        // 1. Create a few items to ship
        Package techGadget = new Package("PKG-IPHONE", LockerSize.SMALL);
        Package heavyBoots = new Package("PKG-BOOTS", LockerSize.MEDIUM);
        Package overstuffedBox = new Package("PKG-MICROWAVE", LockerSize.LARGE);

        // 2. Simulating Deliveries
        String code1 = deliveryHub.depositPackage(techGadget);
        String code2 = deliveryHub.depositPackage(heavyBoots);
        
        // This package should trigger the backup allocation strategy if exact matches run out
        Package backupItem = new Package("PKG-AIRPODS", LockerSize.SMALL); 
        String code3 = deliveryHub.depositPackage(backupItem); // Should fall back into the Large slot if available

        System.out.println("\n--- Customer Pickups ---");
        // 3. Simulating Customer Collections
        deliveryHub.pickupPackage(code1);
        deliveryHub.pickupPackage("000000"); // Invalid Code Test
        deliveryHub.pickupPackage(code2);
    }
}