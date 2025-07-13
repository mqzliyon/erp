package com.dazzling.erp.models;

import com.google.firebase.firestore.DocumentId;

/**
 * Stock Summary model for storing stock data
 */
public class StockSummary {
    @DocumentId
    private String id;
    private String productType; // Polo, T-Shirt, Stripe Polo
    private int openingStock;
    private int receiptFromFactory;
    private int returnProduct;
    private int todaySaleQuantity;
    private int closingStock;
    private String date;
    private long createdAt;
    private String createdBy; // User ID who created this entry
    private String office; // Office location (Rongdhonu, Uttara)

    // Required empty constructor for Firestore
    public StockSummary() {}

    public StockSummary(String productType, int openingStock, int receiptFromFactory, 
                       int returnProduct, int todaySaleQuantity, int closingStock, String date, String createdBy, String office) {
        this.productType = productType;
        this.openingStock = openingStock;
        this.receiptFromFactory = receiptFromFactory;
        this.returnProduct = returnProduct;
        this.todaySaleQuantity = todaySaleQuantity;
        this.closingStock = closingStock;
        this.date = date;
        this.createdAt = System.currentTimeMillis();
        this.createdBy = createdBy;
        this.office = office;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public int getOpeningStock() { return openingStock; }
    public void setOpeningStock(int openingStock) { this.openingStock = openingStock; }

    public int getReceiptFromFactory() { return receiptFromFactory; }
    public void setReceiptFromFactory(int receiptFromFactory) { this.receiptFromFactory = receiptFromFactory; }

    public int getReturnProduct() { return returnProduct; }
    public void setReturnProduct(int returnProduct) { this.returnProduct = returnProduct; }

    public int getTodaySaleQuantity() { return todaySaleQuantity; }
    public void setTodaySaleQuantity(int todaySaleQuantity) { this.todaySaleQuantity = todaySaleQuantity; }

    public int getClosingStock() { return closingStock; }
    public void setClosingStock(int closingStock) { this.closingStock = closingStock; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getOffice() { return office; }
    public void setOffice(String office) { this.office = office; }

    // Calculate closing stock automatically
    public void calculateClosingStock() {
        this.closingStock = openingStock + receiptFromFactory + returnProduct - todaySaleQuantity;
    }

    // Validate data
    public boolean isValid() {
        return productType != null && !productType.isEmpty() &&
               date != null && !date.isEmpty() &&
               openingStock >= 0 && receiptFromFactory >= 0 && 
               returnProduct >= 0 && todaySaleQuantity >= 0;
    }
} 