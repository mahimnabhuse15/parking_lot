package com.parking;

import java.util.*;

// Base Vehicle class
abstract class Vehicle {
    String number;

    Vehicle(String number) {
        this.number = number;
    }

    abstract int calculateFee(int hours);
}

class Car extends Vehicle {
    Car(String number) {
        super(number);
    }

    @Override
    int calculateFee(int hours) {
        return hours * 50;
    }
}

class Bike extends Vehicle {
    Bike(String number) {
        super(number);
    }

    @Override
    int calculateFee(int hours) {
        return hours * 20;
    }
}

class ParkingSlot {
    int slotNumber;
    Vehicle vehicle;
    String entryTime; // Persistent timestamp

    ParkingSlot(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    boolean isEmpty() {
        return vehicle == null;
    }
}

class ParkingLot {
    ArrayList<ParkingSlot> slots = new ArrayList<>();

    ParkingLot(int size) {
        if (DatabaseManager.isDbConnected()) {
            // Automatically restore database state on startup!
            List<DatabaseManager.SlotData> dbSlots = DatabaseManager.loadSlots();
            if (dbSlots.size() == size) {
                for (DatabaseManager.SlotData dbSlot : dbSlots) {
                    ParkingSlot slot = new ParkingSlot(dbSlot.slotNumber);
                    if (!dbSlot.isEmpty()) {
                        Vehicle vehicle;
                        if ("CAR".equalsIgnoreCase(dbSlot.vehicleType)) {
                            vehicle = new Car(dbSlot.vehicleNumber);
                        } else {
                            vehicle = new Bike(dbSlot.vehicleNumber);
                        }
                        slot.vehicle = vehicle;
                        slot.entryTime = dbSlot.entryTime;
                    }
                    slots.add(slot);
                }
            } else {
                // Reinitialize in database with new capacity size
                DatabaseManager.reinitializeSlots(size);
                for (int i = 1; i <= size; i++) {
                    slots.add(new ParkingSlot(i));
                }
            }
        } else {
            // Database is offline: Do not fallback to local in-memory slots
        }
    }

    // Returns the occupied ParkingSlot, or throws exception if full
    ParkingSlot parkVehicle(Vehicle vehicle) {
        for (ParkingSlot slot : slots) {
            if (slot.isEmpty()) {
                slot.vehicle = vehicle;
                java.time.LocalDateTime entryTime = java.time.LocalDateTime.now();
                slot.entryTime = entryTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
                if (DatabaseManager.isDbConnected()) {
                    String typeName = (vehicle instanceof Car) ? "CAR" : "BIKE";
                    DatabaseManager.saveParking(slot.slotNumber, vehicle.number, typeName, entryTime);
                }
                
                System.out.println(vehicle.number + " parked at slot " + slot.slotNumber);
                return slot;
            }
        }
        throw new IllegalStateException("Parking Full");
    }

    // Helper class to return removal details
    public static class RemovalResult {
        public final Vehicle vehicle;
        public final int slotNumber;
        public final int fee;
        public final String entryTime;

        public RemovalResult(Vehicle vehicle, int slotNumber, int fee, String entryTime) {
            this.vehicle = vehicle;
            this.slotNumber = slotNumber;
            this.fee = fee;
            this.entryTime = entryTime;
        }
    }

    // Returns RemovalResult, or throws exception if not found
    RemovalResult removeVehicle(String number, int hours) {
        for (ParkingSlot slot : slots) {
            if (!slot.isEmpty() && slot.vehicle.number.equalsIgnoreCase(number)) {
                Vehicle v = slot.vehicle;
                int fee = v.calculateFee(hours);
                int slotNo = slot.slotNumber;
                String entry = slot.entryTime;

                // Set memory state back to vacant
                slot.vehicle = null;
                slot.entryTime = null;

                if (DatabaseManager.isDbConnected()) {
                    // Sync Database Slot state to NULL
                    DatabaseManager.releaseSlot(slotNo);

                    // Insert dynamic transaction history into DB
                    String typeName = (v instanceof Car) ? "CAR" : "BIKE";
                    DatabaseManager.recordTransaction(v.number, typeName, slotNo, entry, java.time.LocalDateTime.now(), fee);
                }

                System.out.println(number + " removed. Fee = ₹" + fee);
                return new RemovalResult(v, slotNo, fee, entry);
            }
        }
        throw new IllegalArgumentException("Vehicle not found: " + number);
    }

    void display() {
        for (ParkingSlot slot : slots) {
            if (slot.isEmpty()) {
                System.out.println("Slot " + slot.slotNumber + " Empty");
            } else {
                System.out.println("Slot " + slot.slotNumber + " Occupied by " + slot.vehicle.number);
            }
        }
    }
}
