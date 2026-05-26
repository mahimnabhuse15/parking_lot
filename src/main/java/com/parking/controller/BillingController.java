package com.parking.controller;

import com.parking.model.Invoice;
import com.parking.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final InvoiceService invoiceService;

    @Autowired
    public BillingController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/invoice")
    public ResponseEntity<?> generateInvoice(@RequestBody InvoiceRequest request) {
        try {
            Invoice invoice = invoiceService.createInvoice(
                    request.getVehicleNumber(),
                    request.getVehicleType(),
                    request.getHoursParked(),
                    request.getHourlyRate()
            );
            return ResponseEntity.ok(invoice);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<Invoice>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<?> getInvoiceByInvoiceId(@PathVariable String invoiceId) {
        Optional<Invoice> invoiceOpt = invoiceService.getInvoiceByInvoiceId(invoiceId);
        if (invoiceOpt.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invoice not found!");
            return ResponseEntity.status(404).body(error);
        }
        return ResponseEntity.ok(invoiceOpt.get());
    }

    // Input request DTO
    public static class InvoiceRequest {
        private String vehicleNumber;
        private String vehicleType;
        private int hoursParked;
        private int hourlyRate;

        public String getVehicleNumber() { return vehicleNumber; }
        public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

        public String getVehicleType() { return vehicleType; }
        public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

        public int getHoursParked() { return hoursParked; }
        public void setHoursParked(int hoursParked) { this.hoursParked = hoursParked; }

        public int getHourlyRate() { return hourlyRate; }
        public void setHourlyRate(int hourlyRate) { this.hourlyRate = hourlyRate; }
    }
}
