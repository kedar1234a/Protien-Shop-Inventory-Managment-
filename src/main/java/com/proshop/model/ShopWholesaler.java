package com.proshop.model;

import java.time.LocalDate;

public class ShopWholesaler {
	private Long id;
	private String wholesalerName;
	private String mobileNo;
	private String productName;
	private int productQuantity;
	private double buyingPrice;
	private double sellingPrice; // New field
	private String address;
	private LocalDate dateOfPurchase;

	public ShopWholesaler(Long id, String wholesalerName, String mobileNo, String productName, int productQuantity,
			double buyingPrice, double sellingPrice, String address, LocalDate dateOfPurchase) {
		this.id = id;
		this.wholesalerName = wholesalerName;
		this.mobileNo = mobileNo;
		this.productName = productName;
		this.productQuantity = productQuantity;
		this.buyingPrice = buyingPrice;
		this.sellingPrice = sellingPrice;
		this.address = address;
		this.dateOfPurchase = dateOfPurchase;
	}

	// Getters and setters
	public Long getId() {
		return id;
	}

	public String getWholesalerName() {
		return wholesalerName;
	}

	public String getMobileNo() {
		return mobileNo;
	}

	public String getProductName() {
		return productName;
	}

	public int getProductQuantity() {
		return productQuantity;
	}

	public double getBuyingPrice() {
		return buyingPrice;
	}

	public double getSellingPrice() {
		return sellingPrice;
	}

	public String getAddress() {
		return address;
	}

	public LocalDate getDateOfPurchase() {
		return dateOfPurchase;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setWholesalerName(String wholesalerName) {
		this.wholesalerName = wholesalerName;
	}

	public void setMobileNo(String mobileNo) {
		this.mobileNo = mobileNo;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public void setProductQuantity(int productQuantity) {
		this.productQuantity = productQuantity;
	}

	public void setBuyingPrice(double buyingPrice) {
		this.buyingPrice = buyingPrice;
	}

	public void setSellingPrice(double sellingPrice) {
		this.sellingPrice = sellingPrice;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setDateOfPurchase(LocalDate dateOfPurchase) {
		this.dateOfPurchase = dateOfPurchase;
	}
}