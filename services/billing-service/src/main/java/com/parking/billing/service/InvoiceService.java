package com.parking.billing.service;

import com.parking.billing.model.Invoice;
import com.parking.billing.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final Random random = new Random();

    @Value("${billing.tax.cgst.rate:0.09}")
    private double cgstRate;

    @Value("${billing.tax.sgst.rate:0.09}")
    private double sgstRate;

    @Autowired
    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public Invoice createInvoice(String vehicleNumber, String vehicleType, int hoursParked, int hourlyRate) {
        // Mathematical calculations
        double subtotal = hoursParked * hourlyRate;
        double cgst = subtotal * cgstRate;
        double sgst = subtotal * sgstRate;
        double grandTotal = subtotal + cgst + sgst;

        // Unique Sequential ID generation: INV-YYYYMMDD-XXXX
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String invoiceId;
        do {
            int randomNum = 1000 + random.nextInt(9000);
            invoiceId = "INV-" + dateStr + "-" + randomNum;
        } while (invoiceRepository.existsByInvoiceId(invoiceId));

        LocalDateTime issueTime = LocalDateTime.now();
        String formattedReceipt = generateAsciiReceipt(invoiceId, vehicleNumber, vehicleType, hoursParked, hourlyRate, subtotal, cgst, sgst, grandTotal, issueTime);

        Invoice invoice = new Invoice(
                invoiceId,
                vehicleNumber.toUpperCase(),
                vehicleType.toUpperCase(),
                hoursParked,
                hourlyRate,
                subtotal,
                cgst,
                sgst,
                grandTotal,
                issueTime,
                formattedReceipt
        );

        return invoiceRepository.save(invoice);
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public Optional<Invoice> getInvoiceByInvoiceId(String invoiceId) {
        return invoiceRepository.findByInvoiceId(invoiceId);
    }

    private String generateAsciiReceipt(String invoiceId, String vehicleNumber, String vehicleType, int hoursParked, int hourlyRate,
                                        double subtotal, double cgst, double sgst, double grandTotal, LocalDateTime issueTime) {
        String timeStr = issueTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return String.format(
                "=========================================\n" +
                "         AUTOPARK CLOUD BILLING          \n" +
                "=========================================\n" +
                "INVOICE ID  : %s\n" +
                "DATE        : %s\n" +
                "VEHICLE NO  : %s (%s)\n" +
                "HOURS PARKED: %d Hours\n" +
                "HOURLY RATE : ₹%d.00 / hour\n" +
                "-----------------------------------------\n" +
                "SUBTOTAL    :               ₹%.2f\n" +
                "CGST (%.1f%%) :               ₹%.2f\n" +
                "SGST (%.1f%%) :               ₹%.2f\n" +
                "-----------------------------------------\n" +
                "GRAND TOTAL :               ₹%.2f\n" +
                "=========================================\n" +
                "      THANK YOU FOR USING AUTOPARK       \n" +
                "=========================================",
                invoiceId,
                timeStr,
                vehicleNumber.toUpperCase(),
                vehicleType.toUpperCase(),
                hoursParked,
                hourlyRate,
                subtotal,
                cgstRate * 100,
                cgst,
                sgstRate * 100,
                sgst,
                grandTotal
        );
    }
}
