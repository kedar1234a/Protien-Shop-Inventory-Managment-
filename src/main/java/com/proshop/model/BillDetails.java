package com.proshop.model;

import java.time.LocalDate;

public class BillDetails {
    private long billId;
    private double rent;
    private double lightBill;
    private double maintenanceBill;
    private double salary;
    private double productPurchase;
    private double parcelBillAmount;
    private String parcelBillDescription;
    private double bankEmi;
    private double othersAmount;
    private String othersDescription;
    private double productSale;
    private LocalDate billDate;

    public BillDetails() {
    }

    public BillDetails(long billId, double rent, double lightBill, double maintenanceBill, double salary,
            double productPurchase, double parcelBillAmount, String parcelBillDescription, double bankEmi,
            double othersAmount, String othersDescription, double productSale, LocalDate billDate) {
        this.billId = billId;
        this.rent = rent;
        this.lightBill = lightBill;
        this.maintenanceBill = maintenanceBill;
        this.salary = salary;
        this.productPurchase = productPurchase;
        this.parcelBillAmount = parcelBillAmount;
        this.parcelBillDescription = parcelBillDescription;
        this.bankEmi = bankEmi;
        this.othersAmount = othersAmount;
        this.othersDescription = othersDescription;
        this.productSale = productSale;
        this.billDate = billDate;
    }

    // Getters and setters
    public long getBillId() {
        return billId;
    }

    public void setBillId(long billId) {
        this.billId = billId;
    }

    public double getRent() {
        return rent;
    }

    public void setRent(double rent) {
        this.rent = rent;
    }

    public double getLightBill() {
        return lightBill;
    }

    public void setLightBill(double lightBill) {
        this.lightBill = lightBill;
    }

    public double getMaintenanceBill() {
        return maintenanceBill;
    }

    public void setMaintenanceBill(double maintenanceBill) {
        this.maintenanceBill = maintenanceBill;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public double getProductPurchase() {
        return productPurchase;
    }

    public void setProductPurchase(double productPurchase) {
        this.productPurchase = productPurchase;
    }

    public double getParcelBillAmount() {
        return parcelBillAmount;
    }

    public void setParcelBillAmount(double parcelBillAmount) {
        this.parcelBillAmount = parcelBillAmount;
    }

    public String getParcelBillDescription() {
        return parcelBillDescription;
    }

    public void setParcelBillDescription(String parcelBillDescription) {
        this.parcelBillDescription = parcelBillDescription;
    }

    public double getBankEmi() {
        return bankEmi;
    }

    public void setBankEmi(double bankEmi) {
        this.bankEmi = bankEmi;
    }

    public double getOthersAmount() {
        return othersAmount;
    }

    public void setOthersAmount(double othersAmount) {
        this.othersAmount = othersAmount;
    }

    public String getOthersDescription() {
        return othersDescription;
    }

    public void setOthersDescription(String othersDescription) {
        this.othersDescription = othersDescription;
    }

    public double getProductSale() {
        return productSale;
    }

    public void setProductSale(double productSale) {
        this.productSale = productSale;
    }

    public LocalDate getBillDate() {
        return billDate;
    }

    public void setBillDate(LocalDate billDate) {
        this.billDate = billDate;
    }
}