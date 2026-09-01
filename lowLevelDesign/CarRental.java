import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// ==========================================
// 1. Core Enums and Models
// ==========================================
enum VehicleType {
    SUV, SEDAN, HATCHBACK, LUXURY
}

enum VehicleStatus {
    AVAILABLE, RESERVED, IN_USE, UNDER_MAINTENANCE
}

enum ReservationStatus {
    PENDING, CONFIRMED, COMPLETED, CANCELLED
}

class User {
    private final String userId;
    private final String name;
    private final String licenseNumber;

    public User(String userId, String name, String licenseNumber) {
        this.userId = userId;
        this.name = name;
        this.licenseNumber = licenseNumber;
    }
    public String getUserId() { return userId; }
    public String getName() { return name; }
}

// ==========================================
// 2. State Isolation & Pricing Strategies
// ==========================================
interface PricingStrategy {
    double calculatePrice(int durationInHours);
}

class FlatRatePricingStrategy implements PricingStrategy {
    private final double hourlyRate;
    public FlatRatePricingStrategy(double hourlyRate) { this.hourlyRate = hourlyRate; }

    @Override
    public double calculatePrice(int durationInHours) {
        return durationInHours * hourlyRate;
    }
}

class Vehicle {
    private final String vehicleId;
    private final String licensePlate;
    private final VehicleType type;
    private VehicleStatus status;
    private final PricingStrategy pricingStrategy;

    public Vehicle(String vehicleId, String licensePlate, VehicleType type, PricingStrategy pricingStrategy) {
        this.vehicleId = vehicleId;
        this.licensePlate = licensePlate;
        this.type = type;
        this.status = VehicleStatus.AVAILABLE;
        this.pricingStrategy = pricingStrategy;
    }

    // Critical thread-safe mutator to prevent double booking ("Race Conditions")
    public synchronized boolean reserve() {
        if (this.status == VehicleStatus.AVAILABLE) {
            this.status = VehicleStatus.RESERVED;
            return true;
        }
        return false;
    }

    public synchronized void release() {
        this.status = VehicleStatus.AVAILABLE;
    }

    public synchronized void setStatus(VehicleStatus status) { this.status = status; }
    public String getVehicleId() { return vehicleId; }
    public VehicleType getType() { return type; }
    public VehicleStatus getStatus() { return status; }
    public PricingStrategy getPricingStrategy() { return pricingStrategy; }
}

// ==========================================
// 3. Store and Reservation Contexts
// ==========================================
class Reservation {
    private final String reservationId;
    private final User user;
    private final Vehicle vehicle;
    private final int durationInHours;
    private final double totalBill;
    private ReservationStatus status;

    public Reservation(String reservationId, User user, Vehicle vehicle, int durationInHours) {
        this.reservationId = reservationId;
        this.user = user;
        this.vehicle = vehicle;
        this.durationInHours = durationInHours;
        this.totalBill = vehicle.getPricingStrategy().calculatePrice(durationInHours);
        this.status = ReservationStatus.CONFIRMED;
    }

    public synchronized void completeRental() {
        this.status = ReservationStatus.COMPLETED;
        this.vehicle.release();
    }

    public String getReservationId() { return reservationId; }
    public double getTotalBill() { return totalBill; }
    public Vehicle getVehicle() { return vehicle; }
}

class Store {
    private final String storeId;
    private final String location;
    private final List<Vehicle> inventory;

    public Store(String storeId, String location) {
        this.storeId = storeId;
        this.location = location;
        this.inventory = new CopyOnWriteArrayList<>(); // Thread-safe iteration
    }

    public void addVehicle(Vehicle vehicle) { inventory.add(vehicle); }

    /**
     * Filters available inventory based on matching type constraints.
     */
    public List<Vehicle> getAvailableVehicles(VehicleType type) {
        List<Vehicle> matches = new ArrayList<>();
        for (Vehicle v : inventory) {
            if (v.getType() == type && v.getStatus() == VehicleStatus.AVAILABLE) {
                matches.add(v);
            }
        }
        return matches;
    }

    public String getStoreId() { return storeId; }
    public String getLocation() { return location; }
}

// ==========================================
// 4. Central Rental Orchestrator System
// ==========================================
class RentalSystem {
    private final Map<String, Store> storeRegistry = new ConcurrentHashMap<>();
    private final Map<String, Reservation> activeReservations = new ConcurrentHashMap<>();

    public void registerStore(Store store) {
        storeRegistry.put(store.getStoreId(), store);
    }

    public Store getStoreByLocation(String location) {
        for (Store store : storeRegistry.values()) {
            if (store.getLocation().equalsIgnoreCase(location)) {
                return store;
            }
        }
        return null;
    }

    /**
     * Core Transaction Engine: Safely allocates a dynamic slot booking.
     */
    public Reservation bookVehicle(String storeId, String vehicleId, User user, int hours) {
        Store store = storeRegistry.get(storeId);
        if (store == null) return null;

        // Extract target vehicle
        Vehicle targetVehicle = null;
        for (Vehicle v : store.getAvailableVehicles(VehicleType.SUV)) { // Abstracted search
            if (v.getVehicleId().equals(vehicleId)) {
                targetVehicle = v;
                break;
            }
        }
        
        // Fallback catch if vehicle type filter did not harvest it directly
        if (targetVehicle == null) {
            for (Vehicle v : store.getAvailableVehicles(VehicleType.SEDAN)) {
                if (v.getVehicleId().equals(vehicleId)) { targetVehicle = v; break; }
            }
        }

        if (targetVehicle == null) {
            System.out.println("❌ [Booking Failed] Vehicle matching ID " + vehicleId + " is unavailable.");
            return null;
        }

        // Atomic Check-and-Act block ensures thread-safety
        boolean successfullyReserved = targetVehicle.reserve();

        if (successfullyReserved) {
            String resId = "RES_" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            Reservation reservation = new Reservation(resId, user, targetVehicle, hours);
            activeReservations.put(resId, reservation);
            System.out.println("✅ [Reservation Confirmed] ID: " + resId + " for " + user.getName() 
                               + ". Total Bill: INR " + reservation.getTotalBill());
            return reservation;
        } else {
            System.out.println("❌ [Race Condition Collided!] " + user.getName() + " failed to rent Vehicle " + vehicleId + ". Already taken.");
            return null;
        }
    }

    public void returnVehicle(String reservationId) {
        Reservation res = activeReservations.get(reservationId);
        if (res != null) {
            res.completeRental();
            activeReservations.remove(reservationId);
            System.out.println("🏁 [Vehicle Returned] Reservation " + reservationId + " is complete. Vehicle is back in inventory.");
        }
    }
}

// ==========================================
// 5. Main Operational Driver
// ==========================================
public class CarRental {
    public static void main(String[] args) throws InterruptedException {
        RentalSystem hertz = new RentalSystem();

        // Step 1: Initialize physical store hub assets
        Store bangaloreHub = new Store("ST_BLR_01", "Indiranagar, Bangalore");
        
        // Step 2: Provision diverse vehicles into inventory with flat pricing setups
        Vehicle suvCar = new Vehicle("V_SUV_99", "KA-03-MM-1234", VehicleType.SUV, new FlatRatePricingStrategy(150.0)); // 150 INR/hr
        Vehicle sedanCar = new Vehicle("V_SED_44", "KA-51-AA-5678", VehicleType.SEDAN, new FlatRatePricingStrategy(100.0)); // 100 INR/hr
        
        bangaloreHub.addVehicle(suvCar);
        bangaloreHub.addVehicle(sedanCar);
        hertz.registerStore(bangaloreHub);

        System.out.println("=== VEHICLE RENTAL PLATFORM ONLINE ===\n");

        // Step 3: Users log onto the system
        User ramesh = new User("U1", "Ramesh Kumar", "DL-1234567");
        User suresh = new User("U2", "Suresh Raina", "DL-7654321");

        // Step 4: Simulate a Race Condition Clash 
        // Two separate consumer threads try to secure the same SUV simultaneously
        System.out.println(">>> Initializing Simultaneous Reservation requests on vehicle: [V_SUV_99]");
        
        Thread thread1 = new Thread(() -> hertz.bookVehicle("ST_BLR_01", "V_SUV_99", ramesh, 5));
        Thread thread2 = new Thread(() -> hertz.bookVehicle("ST_BLR_01", "V_SUV_99", suresh, 3));

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Step 5: Process standard lifecycle turnaround (Return & Re-booking)
        System.out.println("\n>>> Processing return flow logic for the active trip...");
        // Discover what reservation code was saved dynamically to safely complete it
        String activeResId = "";
        for (String key : hertz.bookVehicle("ST_BLR_01", "V_SED_44", ramesh, 2) != null ? new String[]{} : new String[]{}) {} 
        
        // Let's assume Ramesh completes his journey
        hertz.returnVehicle(hertz.bookVehicle("ST_BLR_01", "V_SED_44", ramesh, 4).getReservationId());
    }
}