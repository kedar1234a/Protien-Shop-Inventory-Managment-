package com.proshop.model;

import java.time.LocalDate;

public class WholesalerPayment {
	private Long id;
	private Long wholesalerId;
	private String paymentMode;
	private LocalDate dateOfAmountPaid;
	private Double amountPaid;
	private Double pendingAmount;

	public WholesalerPayment() {
	}

	public WholesalerPayment(Long id, Long wholesalerId, String paymentMode, LocalDate dateOfAmountPaid,
			Double amountPaid, Double paymentAmount, Double pendingAmount, Double payment) {
		this.id = id;
		this.wholesalerId = wholesalerId;
		this.paymentMode = paymentMode;
		this.dateOfAmountPaid = dateOfAmountPaid;
		this.amountPaid = amountPaid;
		this.pendingAmount = pendingAmount;
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getWholesalerId() {
		return wholesalerId;
	}

	public void setWholesalerId(Long wholesalerId) {
		this.wholesalerId = wholesalerId;
	}

	public String getPaymentMode() {
		return paymentMode;
	}

	public void setPaymentMode(String paymentMode) {
		this.paymentMode = paymentMode;
	}

	public LocalDate getDateOfAmountPaid() {
		return dateOfAmountPaid;
	}

	public void setDateOfAmountPaid(LocalDate dateOfAmountPaid) {
		this.dateOfAmountPaid = dateOfAmountPaid;
	}

	public Double getAmountPaid() {
		return amountPaid;
	}

	public void setAmountPaid(Double amountPaid) {
		this.amountPaid = amountPaid;
	}

	public Double getPendingAmount() {
		return pendingAmount;
	}

	public void setPendingAmount(Double pendingAmount) {
		this.pendingAmount = pendingAmount;
	}
}