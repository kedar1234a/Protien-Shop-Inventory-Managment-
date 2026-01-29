package com.proshop.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Payment {
    private Long id;
    private Long billId;
    private BigDecimal paidAmount;
    private LocalDate paidDate;
    private BigDecimal pendingAmount;

    // Constructor
    public Payment(Long id, Long billId, BigDecimal paidAmount, LocalDate paidDate, BigDecimal pendingAmount) {
        this.id = id;
        this.billId = billId;
        this.paidAmount = paidAmount;
        this.paidDate = paidDate;
        this.pendingAmount = pendingAmount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDate paidDate) {
        this.paidDate = paidDate;
    }

    public BigDecimal getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(BigDecimal pendingAmount) {
        this.pendingAmount = pendingAmount;
    }
}