package com.parking;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ParkingRestController {

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("connected", DatabaseManager.isDbConnected());
        response.put("host", DatabaseManager.getDbHost());
        response.put("port", DatabaseManager.getDbPort());
        response.put("database", DatabaseManager.getDbName());
        return response;
    }

    @GetMapping("/slots")
    public List<DatabaseManager.SlotData> getSlots() {
        if (!DatabaseManager.isDbConnected()) {
            return Collections.emptyList();
        }
        return DatabaseManager.loadSlots();
    }

    @PostMapping("/slots/park")
    public Map<String, Object> parkVehicle(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String plate = request.get("plate");
        String type = request.get("type"); // "CAR" or "BIKE"

        if (plate == null || plate.trim().isEmpty() || type == null) {
            response.put("success", false);
            response.put("message", "Invalid input parameters");
            return response;
        }

        plate = plate.trim().toUpperCase();
        type = type.trim().toUpperCase();

        if (!DatabaseManager.isDbConnected()) {
            response.put("success", false);
            response.put("message", "Database not connected");
            return response;
        }

        // Check duplicate
        List<DatabaseManager.SlotData> currentSlots = DatabaseManager.loadSlots();
        for (DatabaseManager.SlotData s : currentSlots) {
            if (!s.isEmpty() && s.vehicleNumber.equalsIgnoreCase(plate)) {
                response.put("success", false);
                response.put("message", "Vehicle already parked in Slot #" + s.slotNumber);
                return response;
            }
        }

        // Find empty slot
        int freeSlotNum = -1;
        for (DatabaseManager.SlotData s : currentSlots) {
            if (s.isEmpty()) {
                freeSlotNum = s.slotNumber;
                break;
            }
        }

        if (freeSlotNum == -1) {
            response.put("success", false);
            response.put("message", "Parking lot is full");
            return response;
        }

        LocalDateTime now = LocalDateTime.now();
        DatabaseManager.saveParking(freeSlotNum, plate, type, now);

        response.put("success", true);
        response.put("slotNumber", freeSlotNum);
        response.put("plate", plate);
        response.put("type", type);
        response.put("entryTime", now.toString());
        return response;
    }

    @PostMapping("/slots/release")
    public Map<String, Object> releaseSlot(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        String plate = (String) request.get("plate");
        
        int hours = 1;
        if (request.get("hours") != null) {
            hours = ((Number) request.get("hours")).intValue();
        }

        if (plate == null || plate.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Plate number is required");
            return response;
        }

        plate = plate.trim().toUpperCase();

        if (!DatabaseManager.isDbConnected()) {
            response.put("success", false);
            response.put("message", "Database not connected");
            return response;
        }

        List<DatabaseManager.SlotData> currentSlots = DatabaseManager.loadSlots();
        DatabaseManager.SlotData targetSlot = null;
        for (DatabaseManager.SlotData s : currentSlots) {
            if (!s.isEmpty() && s.vehicleNumber.equalsIgnoreCase(plate)) {
                targetSlot = s;
                break;
            }
        }

        if (targetSlot == null) {
            response.put("success", false);
            response.put("message", "Vehicle not found");
            return response;
        }

        int rate = "CAR".equalsIgnoreCase(targetSlot.vehicleType) ? 50 : 20;
        int fee = hours * rate;

        DatabaseManager.releaseSlot(targetSlot.slotNumber);
        DatabaseManager.recordTransaction(plate, targetSlot.vehicleType.toUpperCase(), targetSlot.slotNumber, targetSlot.entryTime, LocalDateTime.now(), fee);

        response.put("success", true);
        response.put("slotNumber", targetSlot.slotNumber);
        response.put("plate", plate);
        response.put("fee", fee);
        response.put("hours", hours);
        return response;
    }

    @GetMapping("/revenue")
    public Map<String, Object> getRevenue() {
        Map<String, Object> response = new HashMap<>();
        response.put("revenue", DatabaseManager.getTotalRevenue());
        return response;
    }

    @GetMapping("/history")
    public List<DatabaseManager.TransactionData> getHistory() {
        return DatabaseManager.getHistory(30);
    }
}
