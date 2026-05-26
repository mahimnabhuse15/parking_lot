package com.parking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String invoiceId;

    @Column(nullable = false)
    private String vehicleNumber;

    @Column(nullable = false)
    private String vehicleType;

    @Column(nullable = false)
    private int hoursParked;

    @Column(nullable = false)
    private int hourlyRate;

    @Column(nullable = false)
    private double subtotal;

    @Column(nullable = false)
    private double cgst;

    @Column(nullable = false)
    private double sgst;

    @Column(nullable = false)
    private double grandTotal;

    @Column(nullable = false)
    private LocalDateTime issueTime;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String formattedReceipt;

    public Invoice() {}

    public Invoice(String invoiceId, String vehicleNumber, String vehicleType, int hoursParked, int hourlyRate,
                   double subtotal, double cgst, double sgst, double grandTotal, LocalDateTime issueTime, String formattedReceipt) {
        this.invoiceId = invoiceId;
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.hoursParked = hoursParked;
        this.hourlyRate = hourlyRate;
        this.subtotal = subtotal;
        this.cgst = cgst;
        this.sgst = sgst;
        this.grandTotal = grandTotal;
        this.issueTime = issueTime;
        this.formattedReceipt = formattedReceipt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public int getHoursParked() { return hoursParked; }
    public void setHoursParked(int hoursParked) { this.hoursParked = hoursParked; }

    public int getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(int hourlyRate) { this.hourlyRate = hourlyRate; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getCgst() { return cgst; }
    public void setCgst(double cgst) { this.cgst = cgst; }

    public double getSgst() { return sgst; }
    public void setSgst(double sgst) { this.sgst = sgst; }

    public double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }

    public LocalDateTime getIssueTime() { return issueTime; }
    public void setIssueTime(LocalDateTime issueTime) { this.issueTime = issueTime; }

    public String getFormattedReceipt() { return formattedReceipt; }
    public void setFormattedReceipt(String formattedReceipt) { this.formattedReceipt = formattedReceipt; }
}
