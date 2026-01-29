package com.proshop.model;

import java.time.LocalDate;

public class GymBill {
    private LocalDate date;
    private double totalBill;
    private double amountPaid;
    private double pendingAmount;

    public GymBill(LocalDate date, double totalBill, double amountPaid, double pendingAmount) {
        this.date = date;
        this.totalBill = totalBill;
        this.amountPaid = amountPaid;
        this.pendingAmount = pendingAmount;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getTotalBill() {
        return totalBill;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public double getPendingAmount() {
        return pendingAmount;
    }
}