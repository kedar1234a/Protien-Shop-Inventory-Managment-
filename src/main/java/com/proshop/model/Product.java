package com.proshop.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Product {
	private Long id;
	private String productName;
	private Integer quantity;
	private BigDecimal perPieceRate;
	private LocalDate expiry;
	private BigDecimal total;
	private Long wholesalerId;

	public Product(Long id, String productName, Integer quantity, BigDecimal perPieceRate, LocalDate expiry) {
		this.id = id;
		this.productName = productName;
		this.quantity = quantity;
		this.perPieceRate = perPieceRate;
		this.expiry = expiry;
		this.total = perPieceRate.multiply(new BigDecimal(quantity));
	}

	public Product(Long id, String productName, Integer quantity, BigDecimal perPieceRate, LocalDate expiry,
			Long wholesalerId) {
		this.id = id;
		this.productName = productName;
		this.quantity = quantity;
		this.perPieceRate = perPieceRate;
		this.expiry = expiry;
		this.total = perPieceRate.multiply(new BigDecimal(quantity));
		this.wholesalerId = wholesalerId;
	}

	public Long getId() {
		return id;
	}

	public String getProductName() {
		return productName;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public BigDecimal getPerPieceRate() {
		return perPieceRate;
	}

	public LocalDate getExpiry() {
		return expiry;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public Long getWholesalerId() {
		return wholesalerId;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public void setPerPieceRate(BigDecimal perPieceRate) {
		this.perPieceRate = perPieceRate;
	}

	public void setExpiry(LocalDate expiry) {
		this.expiry = expiry;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public void setWholesalerId(Long wholesalerId) {
		this.wholesalerId = wholesalerId;
	}
	
}