package com.proshop.model;

import java.time.LocalDate;

public class GymWholesaler {
	private Long id;
	private String wholesalerName;
	private String mobileNo;
	private String productName;
	private int productQuantity;
	private double buyingPrice;
	private double sellingPrice;
	private double netProfit;
	private String paymentMode;
	private LocalDate dateOfPurchase;
	private double totalBill;
	private String description;
	private String address;

	public GymWholesaler() {
	}

	public GymWholesaler(Long id, String wholesalerName, String mobileNo, String productName, int productQuantity,
			double buyingPrice, double sellingPrice, double netProfit, String paymentMode, LocalDate dateOfPurchase,
			String address) {
		this.id = id;
		this.wholesalerName = wholesalerName;
		this.mobileNo = mobileNo;
		this.productName = productName;
		this.productQuantity = productQuantity;
		this.buyingPrice = buyingPrice;
		this.sellingPrice = sellingPrice;
		this.netProfit = netProfit;
		this.paymentMode = paymentMode;
		this.dateOfPurchase = dateOfPurchase;
		this.totalBill = buyingPrice * productQuantity;
		this.description = null;
		this.address = address;
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getWholesalerName() {
		return wholesalerName;
	}

	public void setWholesalerName(String wholesalerName) {
		this.wholesalerName = wholesalerName;
	}

	public String getMobileNo() {
		return mobileNo;
	}

	public void setMobileNo(String mobileNo) {
		this.mobileNo = mobileNo;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public int getProductQuantity() {
		return productQuantity;
	}

	public void setProductQuantity(int productQuantity) {
		this.productQuantity = productQuantity;
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

	public LocalDate getDateOfPurchase() {
		return dateOfPurchase;
	}

	public void setDateOfPurchase(LocalDate dateOfPurchase) {
		this.dateOfPurchase = dateOfPurchase;
	}

	public double getTotalBill() {
		return totalBill;
	}

	public void setTotalBill(double totalBill) {
		this.totalBill = totalBill;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
}