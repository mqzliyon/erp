package com.dazzling.erp.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Fabric model for tracking fabric inventory in KG units
 */
public class Fabric {
    @DocumentId
    private String id;
    private String fabricType;
    private String color;
    private double quantityKg;
    private String supplier;
    private String lotNumber;
    private String quality;
    private String location;
    private String notes;
    private String createdBy;
    private String updatedBy;
    
    @ServerTimestamp
    private Date createdAt;
    
    private Date updatedAt;

    // Required empty constructor for Firestore
    public Fabric() {}

    public Fabric(String fabricType, String color, double quantityKg, String supplier, 
                  String lotNumber, String quality, String location, String notes, String createdBy) {
        this.fabricType = fabricType;
        this.color = color;
        this.quantityKg = quantityKg;
        this.supplier = supplier;
        this.lotNumber = lotNumber;
        this.quality = quality;
        this.location = location;
        this.notes = notes;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFabricType() { return fabricType; }
    public void setFabricType(String fabricType) { this.fabricType = fabricType; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public double getQuantityKg() { return quantityKg; }
    public void setQuantityKg(double quantityKg) { this.quantityKg = quantityKg; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getDisplayName() {
        return fabricType + " - " + color + " (" + quantityKg + " KG)";
    }
} 