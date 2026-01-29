package com.proshop.model;

import java.time.LocalDate;

public class ShopWholesalerPayment {

	private Long id;

	private Long wholesalerId;

	private String paymentMode;

	private LocalDate dateOfAmountPaid;

	private Double amountPaid;

	private Double payment;

	private Double pendingAmount;

	public ShopWholesalerPayment() {

	}

	public ShopWholesalerPayment(Long id, Long wholesalerId, String paymentMode, LocalDate dateOfAmountPaid,
			Double amountPaid, Double payment, Double pendingAmount) {
		super();
		this.id = id;
		this.wholesalerId = wholesalerId;
		this.paymentMode = paymentMode;
		this.dateOfAmountPaid = dateOfAmountPaid;
		this.amountPaid = amountPaid;
		this.payment = payment;
		this.pendingAmount = pendingAmount;
	}

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

	public Double getPayment() {
		return payment;
	}

	public void setPayment(Double payment) {
		this.payment = payment;
	}

	public Double getPendingAmount() {
		return pendingAmount;
	}

	public void setPendingAmount(Double pendingAmount) {
		this.pendingAmount = pendingAmount;
	}

}
