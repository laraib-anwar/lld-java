import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

// ==========================================
// 1. Core Domain Models (Keep them lightweight)
// ==========================================
enum TripStatus { CREATED, IN_PROGRESS, COMPLETED }

class Location {
    double lat, lon;
    public Location(double lat, double lon) { this.lat = lat; this.lon = lon; }
    
    public double distanceTo(Location other) {
        return Math.sqrt(Math.pow(this.lat - other.lat, 2) + Math.pow(this.lon - other.lon, 2));
    }
}

class Rider {
    String id, name;
    Location location;
    public Rider(String id, String name, Location loc) { this.id = id; this.name = name; this.location = loc; }
}

class Driver {
    String id, name;
    Location location;
    boolean isAvailable = true;

    public Driver(String id, String name, Location loc) { this.id = id; this.name = name; this.location = loc; }
    
    public synchronized boolean reserve() {
        if (isAvailable) { isAvailable = false; return true; }
        return false;
    }
    public synchronized void makeAvailable() { this.isAvailable = true; }
}

// ==========================================
// 2. The Core Entity (Handles its own internal state)
// ==========================================
class Trip {
    String id;
    Rider rider;
    Driver driver;
    TripStatus status;
    double fare;

    public Trip(Rider r, Driver d, double fare) {
        this.id = "TRIP_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        this.rider = r;
        this.driver = d;
        this.fare = fare;
        this.status = TripStatus.CREATED;
    }

    public synchronized void start() { this.status = TripStatus.IN_PROGRESS; }
    public synchronized void complete() { 
        this.status = TripStatus.COMPLETED; 
        this.driver.makeAvailable(); 
    }
}

// ==========================================
// 3. Central Manager (The Orchestrator)
// ==========================================
class RideManager {
    private final List<Driver> drivers = new CopyOnWriteArrayList<>();
    private boolean isSurge = false; // Simple toggle for pricing strategy

    public void registerDriver(Driver d) { drivers.add(d); }
    public void toggleSurge(boolean surge) { this.isSurge = surge; }

    // Core Matching & Booking Logic
    public Trip bookRide(Rider rider, Location destination) {
        Driver closestDriver = null;
        double minDistance = Double.MAX_VALUE;

        // Brute-force lookup (Completely acceptable for machine coding time limits)
        for (Driver d : drivers) {
            if (d.isAvailable) {
                double dist = d.location.distanceTo(rider.location);
                if (dist < minDistance) {
                    minDistance = dist;
                    closestDriver = d;
                }
            }
        }

        if (closestDriver == null) {
            System.out.println("❌ No drivers available for " + rider.name);
            return null;
        }

        // Concurrency check: Atomically reserve the driver
        if (!closestDriver.reserve()) {
            return bookRide(rider, destination); // Retry if multi-thread collision occurs
        }

        // Simple inline calculation
        double distance = rider.location.distanceTo(destination);
        double fare = distance * 15.0 * (isSurge ? 2.0 : 1.0);

        Trip trip = new Trip(rider, closestDriver, fare);
        System.out.println("🚗 Match Found! " + closestDriver.name + " assigned to " + rider.name + 
                           " | Fare: INR " + String.format("%.2f", fare) + " | Trip ID: " + trip.id);
        return trip;
    }
}

// ==========================================
// 4. Test Driver Execution
// ==========================================
public class UberDesign {
    public static void main(String[] args) {
        RideManager uber = new RideManager();
        
        uber.registerDriver(new Driver("D1", "Rahul", new Location(12.97, 77.59)));
        uber.registerDriver(new Driver("D2", "Amit", new Location(13.05, 77.65)));

        Rider priya = new Rider("R1", "Priya", new Location(12.971, 77.591)); // Close to Rahul
        Location dest = new Location(12.50, 76.50);

        System.out.println("--- Booting Uber MVP ---");
        Trip trip = uber.bookRide(priya, dest);

        if (trip != null) {
            trip.start();
            System.out.println("Trip status: " + trip.status);
            trip.complete();
            System.out.println("Trip completed. Driver availability: " + trip.driver.isAvailable);
        }
    }
}