package com.proshop.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Stock {
	private Long id;
	private String productName;
	private int quantity;
	private BigDecimal perPieceRate;
	private BigDecimal totalAmount;
	private LocalDate expiryDate;
	private LocalDate purchaseDate;
	private Long wholesalerId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public BigDecimal getPerPieceRate() {
		return perPieceRate;
	}

	public void setPerPieceRate(BigDecimal perPieceRate) {
		this.perPieceRate = perPieceRate;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public LocalDate getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(LocalDate expiryDate) {
		this.expiryDate = expiryDate;
	}

	public LocalDate getPurchaseDate() {
		return purchaseDate;
	}

	public void setPurchaseDate(LocalDate purchaseDate) {
		this.purchaseDate = purchaseDate;
	}

	public Long getWholesalerId() {
		return wholesalerId;
	}

	public void setWholesalerId(Long wholesalerId) {
		this.wholesalerId = wholesalerId;
	}
}