package com.proshop.model;

import java.time.LocalDate;
import java.math.BigDecimal;

public class Bill {
    private Long id;
    private LocalDate date;
    private BigDecimal shippingCharges;
    private BigDecimal billAmount;

    public Bill(Long id, LocalDate date, BigDecimal shippingCharges, BigDecimal billAmount) {
        this.id = id;
        this.date = date;
        this.shippingCharges = shippingCharges;
        this.billAmount = billAmount;
    }

    public Bill(LocalDate date, BigDecimal shippingCharges, BigDecimal billAmount) {
        this(null, date, shippingCharges, billAmount);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getShippingCharges() {
        return shippingCharges;
    }

    public void setShippingCharges(BigDecimal shippingCharges) {
        this.shippingCharges = shippingCharges;
    }

    public BigDecimal getBillAmount() {
        return billAmount;
    }

    public void setBillAmount(BigDecimal billAmount) {
        this.billAmount = billAmount;
    }
}