package com.dazzling.erp.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Cutting model for tracking cutting operations in KG units
 */
public class Cutting {
    @DocumentId
    private String id;
    private String lotNumber;
    private String fabricType;
    private String color;
    private double quantityKg;
    private double quantityPcs; // NEW: quantity in pieces
    private Double originalQuantityKg; // NEW: original kg at conversion (nullable)
    private String cuttingType; // manual, machine
    private String operator;
    private String machineId;
    private String quality;
    private String notes;
    private String status; // pending, in_progress, completed, rejected
    private String createdBy;
    private String updatedBy;
    
    @ServerTimestamp
    private Date createdAt;
    
    private Date updatedAt;
    private Date completedAt;

    // Required empty constructor for Firestore
    public Cutting() {}

    public Cutting(String lotNumber, String fabricType, String color, double quantityKg, 
                   String cuttingType, String operator, String machineId, String quality, 
                   String notes, String createdBy) {
        this.lotNumber = lotNumber;
        this.fabricType = fabricType;
        this.color = color;
        this.quantityKg = quantityKg;
        this.cuttingType = cuttingType;
        this.operator = operator;
        this.machineId = machineId;
        this.quality = quality;
        this.notes = notes;
        this.status = "pending";
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }

    public String getFabricType() { return fabricType; }
    public void setFabricType(String fabricType) { this.fabricType = fabricType; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public double getQuantityKg() { return quantityKg; }
    public void setQuantityKg(double quantityKg) { this.quantityKg = quantityKg; }

    public double getQuantityPcs() { return quantityPcs; }
    public void setQuantityPcs(double quantityPcs) { this.quantityPcs = quantityPcs; }

    public Double getOriginalQuantityKg() { return originalQuantityKg; }
    public void setOriginalQuantityKg(Double originalQuantityKg) { this.originalQuantityKg = originalQuantityKg; }

    public String getCuttingType() { return cuttingType; }
    public void setCuttingType(String cuttingType) { this.cuttingType = cuttingType; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { 
        this.status = status; 
        if ("completed".equals(status)) {
            this.completedAt = new Date();
        }
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    public String getDisplayName() {
        return lotNumber + " - " + fabricType + " (" + quantityKg + " KG)";
    }

    public boolean isCompleted() {
        return "completed".equals(status);
    }

    public boolean isInProgress() {
        return "in_progress".equals(status);
    }
} 