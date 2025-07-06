package com.dazzling.erp.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Lot model for comprehensive lot management with tracking of all operations
 */
public class Lot {
    @DocumentId
    private String id;
    private String lotNumber;
    private String fabricType;
    private String color;
    private String supplier;
    private String customer;
    private String orderNumber;
    
    // Quantities in KG
    private double totalFabricKg;
    private double cuttingKg;
    private int cuttingPcs; // Total cutting quantity in pieces
    private double embroideryReceiveKg;
    private double embroideryRejectKg;
    private double officeShipmentKg;

    // Quantities in Pcs (new fields)
    private int embroideryReceivePcs;
    private int embroideryRejectPcs;
    private int officeShipmentPcs;
    
    // Factory Balance
    private double factoryBalanceAGradeKg;
    private double factoryBalanceBGradeKg;
    private double factoryBalanceRejectKg;
    
    // Status tracking
    private String status; // active, completed, cancelled
    private String priority; // low, medium, high, urgent
    private String quality; // A, B, C, reject
    
    // Dates
    private Date orderDate;
    private Date deliveryDate;
    private Date cuttingStartDate;
    private Date cuttingEndDate;
    private Date embroideryStartDate;
    private Date embroideryEndDate;
    private Date shipmentDate;
    
    // Metadata
    private String notes;
    private String createdBy;
    private String updatedBy;
    
    @ServerTimestamp
    private Date createdAt;
    
    private Date updatedAt;

    // Required empty constructor for Firestore
    public Lot() {}

    public Lot(String lotNumber, String fabricType, String color, String supplier, 
               String customer, String orderNumber, double totalFabricKg, 
               Date orderDate, Date deliveryDate, String priority, String createdBy) {
        this.lotNumber = lotNumber;
        this.fabricType = fabricType;
        this.color = color;
        this.supplier = supplier;
        this.customer = customer;
        this.orderNumber = orderNumber;
        this.totalFabricKg = totalFabricKg;
        this.orderDate = orderDate;
        this.deliveryDate = deliveryDate;
        this.priority = priority;
        this.status = "active";
        this.quality = "A";
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

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public double getTotalFabricKg() { return totalFabricKg; }
    public void setTotalFabricKg(double totalFabricKg) { this.totalFabricKg = totalFabricKg; }

    public double getCuttingKg() { return cuttingKg; }
    public void setCuttingKg(double cuttingKg) { this.cuttingKg = cuttingKg; }

    public int getCuttingPcs() { return cuttingPcs; }
    public void setCuttingPcs(int cuttingPcs) { this.cuttingPcs = cuttingPcs; }

    public double getEmbroideryReceiveKg() { return embroideryReceiveKg; }
    public void setEmbroideryReceiveKg(double embroideryReceiveKg) { this.embroideryReceiveKg = embroideryReceiveKg; }

    public double getEmbroideryRejectKg() { return embroideryRejectKg; }
    public void setEmbroideryRejectKg(double embroideryRejectKg) { this.embroideryRejectKg = embroideryRejectKg; }

    public double getOfficeShipmentKg() { return officeShipmentKg; }
    public void setOfficeShipmentKg(double officeShipmentKg) { this.officeShipmentKg = officeShipmentKg; }

    public int getEmbroideryReceivePcs() { return embroideryReceivePcs; }
    public void setEmbroideryReceivePcs(int embroideryReceivePcs) { this.embroideryReceivePcs = embroideryReceivePcs; }

    public int getEmbroideryRejectPcs() { return embroideryRejectPcs; }
    public void setEmbroideryRejectPcs(int embroideryRejectPcs) { this.embroideryRejectPcs = embroideryRejectPcs; }

    public int getOfficeShipmentPcs() { return officeShipmentPcs; }
    public void setOfficeShipmentPcs(int officeShipmentPcs) { this.officeShipmentPcs = officeShipmentPcs; }

    public double getFactoryBalanceAGradeKg() { return factoryBalanceAGradeKg; }
    public void setFactoryBalanceAGradeKg(double factoryBalanceAGradeKg) { this.factoryBalanceAGradeKg = factoryBalanceAGradeKg; }

    public double getFactoryBalanceBGradeKg() { return factoryBalanceBGradeKg; }
    public void setFactoryBalanceBGradeKg(double factoryBalanceBGradeKg) { this.factoryBalanceBGradeKg = factoryBalanceBGradeKg; }

    public double getFactoryBalanceRejectKg() { return factoryBalanceRejectKg; }
    public void setFactoryBalanceRejectKg(double factoryBalanceRejectKg) { this.factoryBalanceRejectKg = factoryBalanceRejectKg; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }

    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }

    public Date getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(Date deliveryDate) { this.deliveryDate = deliveryDate; }

    public Date getCuttingStartDate() { return cuttingStartDate; }
    public void setCuttingStartDate(Date cuttingStartDate) { this.cuttingStartDate = cuttingStartDate; }

    public Date getCuttingEndDate() { return cuttingEndDate; }
    public void setCuttingEndDate(Date cuttingEndDate) { this.cuttingEndDate = cuttingEndDate; }

    public Date getEmbroideryStartDate() { return embroideryStartDate; }
    public void setEmbroideryStartDate(Date embroideryStartDate) { this.embroideryStartDate = embroideryStartDate; }

    public Date getEmbroideryEndDate() { return embroideryEndDate; }
    public void setEmbroideryEndDate(Date embroideryEndDate) { this.embroideryEndDate = embroideryEndDate; }

    public Date getShipmentDate() { return shipmentDate; }
    public void setShipmentDate(Date shipmentDate) { this.shipmentDate = shipmentDate; }

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

    // Calculated properties
    public double getTotalFactoryBalanceKg() {
        return factoryBalanceAGradeKg + factoryBalanceBGradeKg + factoryBalanceRejectKg;
    }

    public double getRemainingFabricKg() {
        return totalFabricKg - cuttingKg;
    }

    public double getEmbroideryNetKg() {
        return embroideryReceiveKg - embroideryRejectKg;
    }

    public String getDisplayName() {
        return lotNumber + " - " + fabricType + " (" + totalFabricKg + " KG)";
    }

    public boolean isActive() {
        return "active".equals(status);
    }

    public boolean isCompleted() {
        return "completed".equals(status);
    }

    public boolean isUrgent() {
        return "urgent".equals(priority);
    }
} 