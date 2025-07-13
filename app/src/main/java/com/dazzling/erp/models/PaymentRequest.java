package com.dazzling.erp.models;

import java.util.Date;

public class PaymentRequest {
    private String id;
    private String paymentMethod;
    private String invoiceNumber;
    private double amount;
    private String date;
    private String time;
    private String status;
    private String userId;
    private String office;
    private long createdAt;

    // Default constructor for Firestore
    public PaymentRequest() {
    }

    public PaymentRequest(String paymentMethod, String invoiceNumber, double amount, 
                         String date, String time, String userId, String office) {
        this.paymentMethod = paymentMethod;
        this.invoiceNumber = invoiceNumber;
        this.amount = amount;
        this.date = date;
        this.time = time;
        this.status = "Pending";
        this.userId = userId;
        this.office = office;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOffice() {
        return office;
    }

    public void setOffice(String office) {
        this.office = office;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
} 