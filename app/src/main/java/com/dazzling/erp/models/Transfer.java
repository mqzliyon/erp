package com.dazzling.erp.models;

import java.util.Date;

public class Transfer {
    private double quantity;
    private Date date;
    private String note;

    public Transfer() {}
    public Transfer(double quantity, Date date, String note) {
        this.quantity = quantity;
        this.date = date;
        this.note = note;
    }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
} 