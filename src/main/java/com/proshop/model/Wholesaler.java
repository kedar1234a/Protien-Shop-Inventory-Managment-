package com.proshop.model;

public class Wholesaler {
	private Long id;
	private String wholesalerName;
	private String phoneNo;
	private String address;

	public Wholesaler(Long id, String wholesalerName, String phoneNo, String address) {
		this.id = id;
		this.wholesalerName = wholesalerName;
		this.phoneNo = phoneNo;
		this.address = address;
	}

	public Wholesaler(String wholesalerName, String phoneNo, String address) {
		this(null, wholesalerName, phoneNo, address);
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

	// Generate a unique key for grouping wholesalers
	public String getUniqueKey() {
		return wholesalerName + "|" + (phoneNo != null ? phoneNo : "") + "|" + (address != null ? address : "");
	}
}