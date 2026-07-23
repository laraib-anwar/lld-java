import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// ==========================================
// 1. Core Enums and Models
// ==========================================
enum VehicleType {
    BIKE, CAR, TRUCK
}

enum SpotStatus {
    AVAILABLE, OCCUPIED
}

class Vehicle {
    private final String licensePlate;
    private final VehicleType type;

    public Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }
    public String getLicensePlate() { return licensePlate; }
    public VehicleType getType() { return type; }
}

class ParkingSpot {
    private final String spotId;
    private final VehicleType supportedType;
    private SpotStatus status;
    private Vehicle parkedVehicle;

    public ParkingSpot(String spotId, VehicleType supportedType) {
        this.spotId = spotId;
        this.supportedType = supportedType;
        this.status = SpotStatus.AVAILABLE;
        this.parkedVehicle = null;
    }

    // Thread-safe isolation for spot allocation
    public synchronized boolean assignVehicle(Vehicle vehicle) {
        if (this.status == SpotStatus.AVAILABLE && vehicle.getType() == this.supportedType) {
            this.status = SpotStatus.OCCUPIED;
            this.parkedVehicle = vehicle;
            return true;
        }
        return false;
    }

    public synchronized void removeVehicle() {
        this.status = SpotStatus.AVAILABLE;
        this.parkedVehicle = null;
    }

    public String getSpotId() { return spotId; }
    public VehicleType getSupportedType() { return supportedType; }
    public SpotStatus getStatus() { return status; }
}

// ==========================================
// 2. Extensible Strategy Pattern: Fee Engine
// ==========================================
interface FeeStrategy {
    double calculateFee(long durationInSeconds);
}

class HourlyFeeStrategy implements FeeStrategy {
    private final double hourlyRate;
    public HourlyFeeStrategy(double hourlyRate) { this.hourlyRate = hourlyRate; }

    @Override
    public double calculateFee(long durationInSeconds) {
        // Rounding up to the nearest hour for standard business logic
        double hours = Math.ceil(durationInSeconds / 3600.0);
        if (hours == 0) hours = 1; // Minimum baseline floor rate charge
        return hours * hourlyRate;
    }
}

// ==========================================
// 3. Ticket Context Lifecycle
// ==========================================
class Ticket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot assignedSpot;
    private final long startTime;

    public Ticket(String ticketId, Vehicle vehicle, ParkingSpot assignedSpot) {
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.assignedSpot = assignedSpot;
        this.startTime = System.currentTimeMillis();
    }

    public String getTicketId() { return ticketId; }
    public Vehicle getVehicle() { return vehicle; }
    public ParkingSpot getAssignedSpot() { return assignedSpot; }
    public long getStartTime() { return startTime; }
}

// ==========================================
// 4. System Orchestrator: ParkingLot Manager
// ==========================================
class ParkingLot {
    private static volatile ParkingLot instance = null;
    private final List<ParkingSpot> spotInventory = new CopyOnWriteArrayList<>();
    private final Map<String, Ticket> activeTickets = new ConcurrentHashMap<>();
    private final FeeStrategy feeStrategy;

    private ParkingLot(FeeStrategy feeStrategy) {
        this.feeStrategy = feeStrategy;
    }

    /**
     * Singleton Double-Checked Locking initialization
     */
    public static ParkingLot getInstance(FeeStrategy feeStrategy) {
        if (instance == null) {
            synchronized (ParkingLot.class) {
                if (instance == null) {
                    instance = new ParkingLot(feeStrategy);
                }
            }
        }
        return instance;
    }

    public void addSpot(ParkingSpot spot) {
        spotInventory.add(spot);
    }

    /**
     * Core Transaction Method: Thread-safe spot selection and check-in
     */
    public Ticket parkVehicle(Vehicle vehicle) {
        System.out.println("🚗 [Arrival Request] Vehicle: " + vehicle.getLicensePlate() + " (" + vehicle.getType() + ")");
        
        for (ParkingSpot spot : spotInventory) {
            if (spot.getSupportedType() == vehicle.getType() && spot.getStatus() == SpotStatus.AVAILABLE) {
                // Try to acquire lock directly at spot instance level
                if (spot.assignVehicle(vehicle)) {
                    String ticketId = "TKID-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
                    Ticket ticket = new Ticket(ticketId, vehicle, spot);
                    activeTickets.put(ticketId, ticket);
                    System.out.println("✅ [Spot Allocated] Ticket Generated: " + ticketId + " at Spot: " + spot.getSpotId());
                    return ticket;
                }
            }
        }
        System.out.println("❌ [Parking Refused] No available spots remaining for type: " + vehicle.getType());
        return null;
    }

    /**
     * Core Transaction Method: Checkout and fee generation
     */
    public double exitVehicle(String ticketId) {
        Ticket ticket = activeTickets.remove(ticketId);
        if (ticket == null) {
            System.out.println("🚨 [Invalid Ticket] System lookup failed for code: " + ticketId);
            return 0.0;
        }

        ParkingSpot spot = ticket.getAssignedSpot();
        spot.removeVehicle();

        // Simulating a duration gap elapsed time for calculation presentation
        // In real execution, use: (System.currentTimeMillis() - ticket.getStartTime()) / 1000
        long simulatedDurationSeconds = 7200; // Hardcoded to 2 hours simulation
        double cost = feeStrategy.calculateFee(simulatedDurationSeconds);

        System.out.println("🏁 [Checkout Complete] Vehicle " + ticket.getVehicle().getLicensePlate() 
                           + " vacated Spot " + spot.getSpotId() + ". Total processing fee: INR " + cost);
        return cost;
    }
}

// ==========================================
// 5. Main Operational Driver
// ==========================================
public class ParkingLotDriver {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== INITIALIZING DIGITAL PARKING SYSTEM ===\n");

        // Step 1: Provision configuration setup using 50.0 INR/hr flat rule strategy
        FeeStrategy simpleHourly = new HourlyFeeStrategy(50.0);
        ParkingLot facility = ParkingLot.getInstance(simpleHourly);

        // Step 2: Inject spots into inventory pool
        facility.addSpot(new ParkingSpot("P1-CAR-01", VehicleType.CAR));
        facility.addSpot(new ParkingSpot("P1-CAR-02", VehicleType.CAR));
        facility.addSpot(new ParkingSpot("P1-BIKE-01", VehicleType.BIKE));

        // Step 3: Simulate normal arrivals
        Vehicle car1 = new Vehicle("KA-01-AA-1111", VehicleType.CAR);
        Vehicle car2 = new Vehicle("KA-02-BB-2222", VehicleType.CAR);
        Vehicle car3 = new Vehicle("KA-03-CC-3333", VehicleType.CAR); // Will hit capacity floor limit

        Ticket t1 = facility.parkVehicle(car1);
        Ticket t2 = facility.parkVehicle(car2);
        Ticket t3 = facility.parkVehicle(car3); // Expecting denial

        System.out.println();

        // Step 4: Simulate a concurrent race condition flash
        // Two separate consumers attempt to park at the exact same moment on a vacated space
        System.out.println(">>> Vacating Spot P1-CAR-01 to monitor race condition execution behavior...");
        if (t1 != null) {
            facility.exitVehicle(t1.getTicketId());
        }

        System.out.println("\n>>> Firing two simultaneous entry threads at the single free slot...");
        Vehicle concurrentCar1 = new Vehicle("DL-1C-9999", VehicleType.CAR);
        Vehicle concurrentCar2 = new Vehicle("MH-02-XYZ-77", VehicleType.CAR);

        Thread thread1 = new Thread(() -> facility.parkVehicle(concurrentCar1));
        Thread thread2 = new Thread(() -> facility.parkVehicle(concurrentCar2));

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();
    }
}