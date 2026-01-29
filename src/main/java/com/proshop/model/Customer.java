package com.proshop.model;

import java.sql.Date;

public class Customer {
    private long id;
    private String customerName;
    private String productName;
    private int quantity;
    private double buyingPrice;
    private double sellingPrice;
    private double totalAmount;
    private double netProfit;
    private String paymentMode;
    private Date date;
    private double discount;
    private String status;
    private String mobileNo;
    private double finalBill;
    private double amountPaid;
    private Date paymentDate;
    private double pendingAmount;

    public Customer() {
    }

    public Customer(long id, String customerName, String productName, int quantity, double buyingPrice,
            double sellingPrice, double totalAmount, double netProfit, String paymentMode, Date date, double discount,
            String status, String mobileNo, double finalBill, double amountPaid, Date paymentDate, double pendingAmount) {
        this.id = id;
        this.customerName = customerName;
        this.productName = productName;
        this.quantity = quantity;
        this.buyingPrice = buyingPrice;
        this.sellingPrice = sellingPrice;
        this.totalAmount = totalAmount;
        this.netProfit = netProfit;
        this.paymentMode = paymentMode;
        this.date = date;
        this.discount = discount;
        this.status = status;
        this.mobileNo = mobileNo;
        this.finalBill = finalBill;
        this.amountPaid = amountPaid;
        this.paymentDate = paymentDate;
        this.pendingAmount = pendingAmount;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getBuyingPrice() {
        return buyingPrice;
    }

    public void setBuyingPrice(double buyingPrice) {
        this.buyingPrice = buyingPrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(double netProfit) {
        this.netProfit = netProfit;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    public double getFinalBill() {
        return finalBill;
    }

    public void setFinalBill(double finalBill) {
        this.finalBill = finalBill;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public double getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(double pendingAmount) {
        this.pendingAmount = pendingAmount;
    }
}