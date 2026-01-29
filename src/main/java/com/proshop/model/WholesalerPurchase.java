package com.proshop.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class WholesalerPurchase {
	private Long id;
	private String wholesalerName;
	private String phoneNo;
	private String address;
	private String productName;
	private Integer quantity;
	private LocalDate expiry;
	private BigDecimal perPieceRate;
	private BigDecimal finalAmount;
	private BigDecimal shippingCharges;
	private LocalDate shippingDate;
	private BigDecimal shippingAmountPerProduct;
	private LocalDate purchaseDate;
	private LocalDate paidDate;
	private BigDecimal amountPaid;
	private BigDecimal pendingAmount;

	// Default constructor
	public WholesalerPurchase() {
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

	public String getPhoneNo() {
		return phoneNo;
	}

	public void setPhoneNo(String phoneNo) {
		this.phoneNo = phoneNo;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public LocalDate getExpiry() {
		return expiry;
	}

	public void setExpiry(LocalDate expiry) {
		this.expiry = expiry;
	}

	public BigDecimal getPerPieceRate() {
		return perPieceRate;
	}

	public void setPerPieceRate(BigDecimal perPieceRate) {
		this.perPieceRate = perPieceRate;
	}

	public BigDecimal getFinalAmount() {
		return finalAmount;
	}

	public void setFinalAmount(BigDecimal finalAmount) {
		this.finalAmount = finalAmount;
	}

	public BigDecimal getShippingCharges() {
		return shippingCharges;
	}

	public void setShippingCharges(BigDecimal shippingCharges) {
		this.shippingCharges = shippingCharges;
	}

	public LocalDate getShippingDate() {
		return shippingDate;
	}

	public void setShippingDate(LocalDate shippingDate) {
		this.shippingDate = shippingDate;
	}

	public BigDecimal getShippingAmountPerProduct() {
		return shippingAmountPerProduct;
	}

	public void setShippingAmountPerProduct(BigDecimal shippingAmountPerProduct) {
		this.shippingAmountPerProduct = shippingAmountPerProduct;
	}

	public LocalDate getPurchaseDate() {
		return purchaseDate;
	}

	public void setPurchaseDate(LocalDate purchaseDate) {
		this.purchaseDate = purchaseDate;
	}

	public LocalDate getPaidDate() {
		return paidDate;
	}

	public void setPaidDate(LocalDate paidDate) {
		this.paidDate = paidDate;
	}

	public BigDecimal getAmountPaid() {
		return amountPaid;
	}

	public void setAmountPaid(BigDecimal amountPaid) {
		this.amountPaid = amountPaid;
	}

	public BigDecimal getPendingAmount() {
		return pendingAmount;
	}

	public void setPendingAmount(BigDecimal pendingAmount) {
		this.pendingAmount = pendingAmount;
	}

	@Override
	public String toString() {
		return "WholesalerPurchase{" + "id=" + id + ", wholesalerName='" + wholesalerName + '\'' + ", phoneNo='"
				+ phoneNo + '\'' + ", address='" + address + '\'' + ", productName='" + productName + '\''
				+ ", quantity=" + quantity + ", expiry=" + expiry + ", perPieceRate=" + perPieceRate + ", finalAmount="
				+ finalAmount + ", shippingCharges=" + shippingCharges + ", shippingDate=" + shippingDate
				+ ", shippingAmountPerProduct=" + shippingAmountPerProduct + ", purchaseDate=" + purchaseDate
				+ ", paidDate=" + paidDate + ", amountPaid=" + amountPaid + ", pendingAmount=" + pendingAmount + '}';
	}
}