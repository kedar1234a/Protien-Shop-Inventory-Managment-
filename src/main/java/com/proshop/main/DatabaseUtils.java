package com.proshop.main;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.proshop.connection.DBUtil;
import com.proshop.model.Bill;
import com.proshop.model.Payment;
import com.proshop.model.Product;
import com.proshop.model.WholesalerPurchase;

public class DatabaseUtils {
	public static List<WholesalerPurchase> fetchWholesalers() {
		List<WholesalerPurchase> wholesalers = new ArrayList<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			String sql = "SELECT id, wholesalerName, phoneNo, address FROM Wholesaler";
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				WholesalerPurchase wholesaler = new WholesalerPurchase();
				wholesaler.setId(rs.getLong("id"));
				wholesaler.setWholesalerName(rs.getString("wholesalerName"));
				wholesaler.setPhoneNo(rs.getString("phoneNo"));
				wholesaler.setAddress(rs.getString("address"));
				wholesalers.add(wholesaler);
			}
		} catch (SQLException ex) {
			throw new RuntimeException("Error fetching wholesalers: " + ex.getMessage(), ex);
		} finally {
			closeResources(rs, pstmt, conn);
		}
		return wholesalers;
	}

	public static void deleteWholesaler(WholesalerPurchase wholesaler) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			conn.setAutoCommit(false);
			String sql = "DELETE FROM Wholesaler WHERE id = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, wholesaler.getId());
			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected > 0) {
				conn.commit();
			} else {
				conn.rollback();
				throw new SQLException("Failed to delete wholesaler.");
			}
		} catch (SQLException ex) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ignored) {
			}
			throw new RuntimeException("Database error: " + ex.getMessage(), ex);
		} finally {
			closeResources(null, pstmt, conn);
			try {
				if (conn != null)
					conn.setAutoCommit(true);
			} catch (SQLException ignored) {
			}
		}
	}

	public static Map<WholesalerPurchase, Map<Bill, List<Product>>> loadWholesalerData() {
		Map<Long, WholesalerPurchase> wholesalerMap = new HashMap<>();
		Map<WholesalerPurchase, Map<Bill, List<Product>>> wholesalerData = new HashMap<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			String sql = "SELECT w.id AS wholesalerId, w.wholesalerName, w.phoneNo, w.address, "
					+ "b.id AS billId, b.date, b.shippingCharges, b.billAmount, "
					+ "p.id AS productId, p.productName, p.quantity, p.perPieceRate, p.expiry, p.total, p.wholesalerId AS productWholesalerId, "
					+ "bp.quantity AS billProductQuantity, "
					+ "(SELECT pendingAmount FROM Payment pay WHERE pay.billId = b.id ORDER BY pay.paidDate DESC, pay.id DESC LIMIT 1) AS latestPendingAmount, "
					+ "(SELECT COALESCE(SUM(pay.paidAmount), 0) FROM Payment pay WHERE pay.billId = b.id) AS totalPaid "
					+ "FROM Wholesaler w " + "LEFT JOIN Bill b ON w.id = b.wholesalerId "
					+ "LEFT JOIN BillProduct bp ON b.id = bp.billId "
					+ "LEFT JOIN Product p ON bp.productId = p.id AND p.wholesalerId = w.id";
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();

			while (rs.next()) {
				Long wholesalerId = rs.getLong("wholesalerId");
				String wholesalerName = rs.getString("wholesalerName");
				String phoneNo = rs.getString("phoneNo");
				String address = rs.getString("address");

				WholesalerPurchase wholesaler = wholesalerMap.computeIfAbsent(wholesalerId, k -> {
					WholesalerPurchase w = new WholesalerPurchase();
					w.setId(wholesalerId);
					w.setWholesalerName(wholesalerName);
					w.setPhoneNo(phoneNo);
					w.setAddress(address);
					return w;
				});

				Map<Bill, List<Product>> billProducts = wholesalerData.computeIfAbsent(wholesaler,
						k -> new HashMap<>());

				Long billId = rs.getLong("billId");
				if (billId != null && !rs.wasNull()) {
					LocalDate date = rs.getObject("date", LocalDate.class);
					BigDecimal shippingCharges = rs.getBigDecimal("shippingCharges");
					BigDecimal billAmount = rs.getBigDecimal("billAmount");
					BigDecimal latestPendingAmount = rs.getBigDecimal("latestPendingAmount");
					BigDecimal totalPaid = rs.getBigDecimal("totalPaid");

					Bill bill = billProducts.keySet().stream().filter(b -> b.getId().equals(billId)).findFirst()
							.orElseGet(() -> {
								try {
									if (date == null || billAmount == null) {
										System.err.println("Skipping invalid bill for wholesalerId: " + wholesalerId
												+ ", billId: " + billId);
										return null;
									}
									Bill newBill = new Bill(billId, date,
											shippingCharges != null ? shippingCharges : BigDecimal.ZERO, billAmount);
									newBill.setBillAmount(
											latestPendingAmount != null ? latestPendingAmount : billAmount);
									wholesaler.setAmountPaid(wholesaler.getAmountPaid() != null
											? wholesaler.getAmountPaid().add(totalPaid)
											: totalPaid);
									billProducts.put(newBill, new ArrayList<>());
									return newBill;
								} catch (RuntimeException ex) {
									System.err.println("Error creating bill for wholesalerId: " + wholesalerId
											+ ", billId: " + billId + ": " + ex.getMessage());
									return null;
								}
							});

					if (bill != null) {
						List<Product> products = billProducts.get(bill);

						Long productId = rs.getLong("productId");
						if (productId != null && !rs.wasNull()) {
							String productName = rs.getString("productName");
							int billProductQuantity = rs.getInt("billProductQuantity");
							int quantity = rs.getInt("quantity");
							BigDecimal perPieceRate = rs.getBigDecimal("perPieceRate");
							LocalDate expiry = rs.getObject("expiry", LocalDate.class);
							BigDecimal total = rs.getBigDecimal("total");
							Long productWholesalerId = rs.getLong("productWholesalerId");

							Product product = new Product(productId, productName,
									billProductQuantity != 0 ? billProductQuantity : quantity, perPieceRate, expiry,
									productWholesalerId);
							product.setTotal(total);
							if (!products.contains(product)) {
								products.add(product);
							}
						}
					}
				}
			}
		} catch (SQLException ex) {
			throw new RuntimeException("Error loading data: " + ex.getMessage(), ex);
		} finally {
			closeResources(rs, pstmt, conn);
		}
		return wholesalerData;
	}

	public static void addWholesaler(WholesalerPurchase wholesaler) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			conn.setAutoCommit(false);
			String sql = "INSERT INTO Wholesaler (wholesalerName, phoneNo, address) VALUES (?, ?, ?)";
			pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
			pstmt.setString(1, wholesaler.getWholesalerName());
			pstmt.setString(2, wholesaler.getPhoneNo() != null ? wholesaler.getPhoneNo() : null);
			pstmt.setString(3, wholesaler.getAddress() != null ? wholesaler.getAddress() : null);
			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected > 0) {
				ResultSet rs = pstmt.getGeneratedKeys();
				if (rs.next()) {
					wholesaler.setId(rs.getLong(1));
				}
				conn.commit();
			} else {
				conn.rollback();
				throw new SQLException("Failed to add wholesaler.");
			}
		} catch (SQLException ex) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ignored) {
			}
			throw new RuntimeException("Database error: " + ex.getMessage(), ex);
		} finally {
			closeResources(null, pstmt, conn);
			try {
				if (conn != null)
					conn.setAutoCommit(true);
			} catch (SQLException ignored) {
			}
		}
	}

	public static void updateWholesaler(WholesalerPurchase wholesaler) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			conn.setAutoCommit(false);
			String sql = "UPDATE Wholesaler SET wholesalerName = ?, phoneNo = ?, address = ? WHERE id = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, wholesaler.getWholesalerName());
			pstmt.setString(2, wholesaler.getPhoneNo() != null ? wholesaler.getPhoneNo() : null);
			pstmt.setString(3, wholesaler.getAddress() != null ? wholesaler.getAddress() : null);
			pstmt.setLong(4, wholesaler.getId());
			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected > 0) {
				conn.commit();
			} else {
				conn.rollback();
				throw new SQLException("Failed to update wholesaler.");
			}
		} catch (SQLException ex) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ignored) {
			}
			throw new RuntimeException("Database error: " + ex.getMessage(), ex);
		} finally {
			closeResources(null, pstmt, conn);
			try {
				if (conn != null)
					conn.setAutoCommit(true);
			} catch (SQLException ignored) {
			}
		}
	}

	public static List<Product> fetchProductsForWholesaler(Long wholesalerId) {
		List<Product> products = new ArrayList<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			String sql = "SELECT p.id, p.productName, bp.quantity, p.perPieceRate, p.expiry, p.total, b.date "
					+ "FROM Product p " + "LEFT JOIN BillProduct bp ON p.id = bp.productId "
					+ "LEFT JOIN Bill b ON bp.billId = b.id " + "WHERE p.wholesalerId = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, wholesalerId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				Product product = new Product(rs.getLong("id"), rs.getString("productName"), rs.getInt("quantity"),
						rs.getBigDecimal("perPieceRate"), rs.getObject("expiry", LocalDate.class), wholesalerId);
				product.setTotal(rs.getBigDecimal("total"));
				products.add(product);
			}
		} catch (SQLException ex) {
			throw new RuntimeException("Error loading products: " + ex.getMessage(), ex);
		} finally {
			closeResources(rs, pstmt, conn);
		}
		return products;
	}

	public static List<LocalDate> fetchBillDatesForWholesaler(Long wholesalerId) {
		List<LocalDate> billDates = new ArrayList<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			String sql = "SELECT DISTINCT date FROM Bill WHERE wholesalerId = ? ORDER BY date DESC";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, wholesalerId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				billDates.add(rs.getObject("date", LocalDate.class));
			}
		} catch (SQLException ex) {
			throw new RuntimeException("Error loading dates: " + ex.getMessage(), ex);
		} finally {
			closeResources(rs, pstmt, conn);
		}
		return billDates;
	}

	public static List<Bill> fetchBillsForDate(Long wholesalerId, LocalDate date) {
		List<Bill> bills = new ArrayList<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			String sql = "SELECT b.id AS billId, b.date, b.shippingCharges, "
					+ "COALESCE(SUM(pay.paidAmount), 0) AS totalPaid, "
					+ "(SELECT pendingAmount FROM Payment pay WHERE pay.billId = b.id ORDER BY pay.paidDate DESC, pay.id DESC LIMIT 1) AS pendingAmount "
					+ "FROM Bill b " + "LEFT JOIN Payment pay ON b.id = pay.billId "
					+ "WHERE b.wholesalerId = ? AND b.date = ? " + "GROUP BY b.id, b.date, b.shippingCharges";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, wholesalerId);
			pstmt.setObject(2, java.sql.Date.valueOf(date));
			rs = pstmt.executeQuery();
			while (rs.next()) {
				Bill bill = new Bill(rs.getLong("billId"), rs.getObject("date", LocalDate.class),
						rs.getBigDecimal("shippingCharges"), fetchTotalBillAmountForBill(rs.getLong("billId")));
				BigDecimal pendingAmount = rs.getBigDecimal("pendingAmount");
				bill.setBillAmount(pendingAmount != null ? pendingAmount : bill.getBillAmount());
				bills.add(bill);
			}
		} catch (SQLException ex) {
			throw new RuntimeException("Error loading bills: " + ex.getMessage(), ex);
		} finally {
			closeResources(rs, pstmt, conn);
		}
		return bills;
	}

	public static List<Product> fetchProductsForBill(Long billId, Long wholesalerId) {
		List<Product> products = new ArrayList<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			String sql = "SELECT p.id, p.productName, bp.quantity, p.perPieceRate, p.expiry, p.total "
					+ "FROM Product p " + "JOIN BillProduct bp ON p.id = bp.productId "
					+ "WHERE bp.billId = ? AND p.wholesalerId = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, billId);
			pstmt.setLong(2, wholesalerId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				Product product = new Product(rs.getLong("id"), rs.getString("productName"), rs.getInt("quantity"),
						rs.getBigDecimal("perPieceRate"), rs.getObject("expiry", LocalDate.class), wholesalerId);
				product.setTotal(rs.getBigDecimal("total"));
				products.add(product);
			}
		} catch (SQLException ex) {
			throw new RuntimeException("Error loading products: " + ex.getMessage(), ex);
		} finally {
			closeResources(rs, pstmt, conn);
		}
		return products;
	}

	@SuppressWarnings("resource")
	public static void addBill(Bill bill, Long wholesalerId, List<Product> products) {
		Connection conn = null;
		PreparedStatement billPstmt = null;
		PreparedStatement productPstmt = null;
		PreparedStatement billProductPstmt = null;
		PreparedStatement stockPstmt = null;
		ResultSet generatedKeys = null;

		try {
			conn = DBUtil.getConnection();
			conn.setAutoCommit(false);

			// 1. INSERT BILL
			String billSql = "INSERT INTO Bill (date, shippingCharges, billAmount, wholesalerId) VALUES (?, ?, ?, ?)";
			billPstmt = conn.prepareStatement(billSql, PreparedStatement.RETURN_GENERATED_KEYS);
			billPstmt.setObject(1, java.sql.Date.valueOf(bill.getDate()));
			billPstmt.setBigDecimal(2, bill.getShippingCharges());
			billPstmt.setBigDecimal(3, bill.getBillAmount());
			billPstmt.setLong(4, wholesalerId);
			if (billPstmt.executeUpdate() == 0)
				throw new SQLException("Failed to insert bill.");

			generatedKeys = billPstmt.getGeneratedKeys();
			if (!generatedKeys.next())
				throw new SQLException("Failed to retrieve bill ID.");
			long billId = generatedKeys.getLong(1);
			bill.setId(billId);

			// 2. PREPARE STATEMENTS
			String productSql = "INSERT INTO Product (productName, quantity, perPieceRate, expiry, total, wholesalerId) "
					+ "VALUES (?, ?, ?, ?, ?, ?)";
			productPstmt = conn.prepareStatement(productSql, PreparedStatement.RETURN_GENERATED_KEYS);

			String billProductSql = "INSERT INTO BillProduct (billId, productId, quantity) VALUES (?, ?, ?)";
			billProductPstmt = conn.prepareStatement(billProductSql);

			String stockSql = "INSERT INTO Stock (productName, quantity, perPieceRate, totalAmount, expiryDate, purchaseDate, productId) "
					+ "VALUES (?, 1, ?, ?, ?, ?, ?)";
			stockPstmt = conn.prepareStatement(stockSql);

			// 3. PROCESS EACH PRODUCT
			for (Product product : products) {
				// Insert Product (type)
				productPstmt.setString(1, product.getProductName());
				productPstmt.setInt(2, product.getQuantity());
				productPstmt.setBigDecimal(3, product.getPerPieceRate());
				productPstmt.setObject(4,
						product.getExpiry() != null ? java.sql.Date.valueOf(product.getExpiry()) : null);
				productPstmt.setBigDecimal(5, product.getTotal());
				productPstmt.setLong(6, wholesalerId);
				if (productPstmt.executeUpdate() == 0)
					throw new SQLException("Failed to insert product.");

				generatedKeys = productPstmt.getGeneratedKeys();
				if (!generatedKeys.next())
					throw new SQLException("Failed to retrieve product ID.");
				long productId = generatedKeys.getLong(1);
				product.setId(productId);

				// Link Bill → Product
				billProductPstmt.setLong(1, billId);
				billProductPstmt.setLong(2, productId);
				billProductPstmt.setInt(3, product.getQuantity());
				if (billProductPstmt.executeUpdate() == 0)
					throw new SQLException("Failed to link bill-product.");

				// Insert ONE stock row PER UNIT
				int qty = product.getQuantity();
				java.sql.Date expirySql = product.getExpiry() != null ? java.sql.Date.valueOf(product.getExpiry())
						: null;
				java.sql.Date purchaseSql = java.sql.Date.valueOf(bill.getDate());
				BigDecimal unitTotal = product.getPerPieceRate();

				for (int i = 0; i < qty; i++) {
					stockPstmt.setString(1, product.getProductName());
					stockPstmt.setBigDecimal(2, product.getPerPieceRate());
					stockPstmt.setBigDecimal(3, unitTotal);
					stockPstmt.setObject(4, expirySql);
					stockPstmt.setObject(5, purchaseSql);
					stockPstmt.setLong(6, productId); // LINK TO PRODUCT
					if (stockPstmt.executeUpdate() == 0)
						throw new SQLException("Failed to insert stock unit " + (i + 1));
				}
			}

			conn.commit();
		} catch (SQLException ex) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ignored) {
			}
			throw new RuntimeException("Database error: " + ex.getMessage(), ex);
		} finally {
			closeResources(generatedKeys, billPstmt, null);
			closeResources(null, productPstmt, null);
			closeResources(null, billProductPstmt, null);
			closeResources(null, stockPstmt, conn);
			try {
				if (conn != null)
					conn.setAutoCommit(true);
			} catch (SQLException ignored) {
			}
		}
	}

	public static void updateBill(Bill bill) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = DBUtil.getConnection();
			conn.setAutoCommit(false);
			String sql = "UPDATE Bill SET date = ?, shippingCharges = ?, billAmount = ? WHERE id = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setObject(1, java.sql.Date.valueOf(bill.getDate()));
			pstmt.setBigDecimal(2, bill.getShippingCharges());
			pstmt.setBigDecimal(3, bill.getBillAmount());
			pstmt.setLong(4, bill.getId());
			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected > 0) {
				conn.commit();
			} else {
				conn.rollback();
				throw new SQLException("Failed to update bill.");
			}
		} catch (SQLException ex) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ignored) {
			}
			throw new RuntimeException("Database error: " + ex.getMessage(), ex);
		} finally {
			closeResources(null, pstmt, conn);
			try {
				if (conn != null)
					conn.setAutoCommit(true);
			} catch (SQLException ignored) {
			}
		}
	}

	@SuppressWarnings("resource")
	public static void updateProduct(Product product, Bill bill) {
		Connection conn = null;
		PreparedStatement productPstmt = null;
		PreparedStatement deleteStockPstmt = null;
		PreparedStatement insertStockPstmt = null;
		PreparedStatement billProductPstmt = null;

		try {
			conn = DBUtil.getConnection();
			conn.setAutoCommit(false);

			// 1. UPDATE PRODUCT TYPE
			String productSql = "UPDATE Product SET productName = ?, quantity = ?, perPieceRate = ?, expiry = ?, total = ? WHERE id = ?";
			productPstmt = conn.prepareStatement(productSql);
			productPstmt.setString(1, product.getProductName());
			productPstmt.setInt(2, product.getQuantity());
			productPstmt.setBigDecimal(3, product.getPerPieceRate());
			productPstmt.setObject(4, product.getExpiry() != null ? java.sql.Date.valueOf(product.getExpiry()) : null);
			productPstmt.setBigDecimal(5, product.getTotal());
			productPstmt.setLong(6, product.getId());
			if (productPstmt.executeUpdate() == 0)
				throw new SQLException("Failed to update product.");

			// 2. DELETE ONLY STOCK UNITS FOR THIS productId
			String deleteSql = "DELETE FROM Stock WHERE productId = ?";
			deleteStockPstmt = conn.prepareStatement(deleteSql);
			deleteStockPstmt.setLong(1, product.getId());
			deleteStockPstmt.executeUpdate();

			// 3. RE-INSERT ONE ROW PER UNIT
			String insertSql = "INSERT INTO Stock (productName, quantity, perPieceRate, totalAmount, expiryDate, purchaseDate, productId) "
					+ "VALUES (?, 1, ?, ?, ?, ?, ?)";
			insertStockPstmt = conn.prepareStatement(insertSql);

			int qty = product.getQuantity();
			java.sql.Date expirySql = product.getExpiry() != null ? java.sql.Date.valueOf(product.getExpiry()) : null;
			java.sql.Date purchaseSql = java.sql.Date.valueOf(bill.getDate());
			BigDecimal unitTotal = product.getPerPieceRate();

			for (int i = 0; i < qty; i++) {
				insertStockPstmt.setString(1, product.getProductName());
				insertStockPstmt.setBigDecimal(2, product.getPerPieceRate());
				insertStockPstmt.setBigDecimal(3, unitTotal);
				insertStockPstmt.setObject(4, expirySql);
				insertStockPstmt.setObject(5, purchaseSql);
				insertStockPstmt.setLong(6, product.getId());
				if (insertStockPstmt.executeUpdate() == 0)
					throw new SQLException("Failed to insert stock unit " + (i + 1));
			}

			// 4. UPDATE BILL-PRODUCT LINK
			String bpSql = "UPDATE BillProduct SET quantity = ? WHERE billId = ? AND productId = ?";
			billProductPstmt = conn.prepareStatement(bpSql);
			billProductPstmt.setInt(1, product.getQuantity());
			billProductPstmt.setLong(2, bill.getId());
			billProductPstmt.setLong(3, product.getId());
			if (billProductPstmt.executeUpdate() == 0)
				throw new SQLException("Failed to update bill-product link.");

			conn.commit();
		} catch (SQLException ex) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ignored) {
			}
			throw new RuntimeException("Database error: " + ex.getMessage(), ex);
		} finally {
			closeResources(null, productPstmt, null);
			closeResources(null, deleteStockPstmt, null);
			closeResources(null, insertStockPstmt, null);
			closeResources(null, billProductPstmt, conn);
			try {
				if (conn != null)
					conn.setAutoCommit(true);
			} catch (SQLException ignored) {
			}
		}
	}

	@SuppressWarnings("resource")
	public static void updatePayment(Long paymentId, BigDecimal paidAmount, LocalDate paidDate,
			BigDecimal currentPendingAmount) {
		if (paymentId == null || paymentId <= 0) {
			throw new IllegalArgumentException("Invalid payment ID.");
		}
		if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Paid amount must be greater than zero.");
		}
		if (paidDate == null) {
			throw new IllegalArgumentException("Paid date cannot be null.");
		}
		if (currentPendingAmount == null || currentPendingAmount.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Current pending amount cannot be null or negative.");
		}

		Connection conn = null;
		PreparedStatement pstmt = null;
		PreparedStatement shopPstmt = null;
		try {
			conn = DBUtil.getConnection();
			conn.setAutoCommit(false);

			// Update Payment table
			String sql = "UPDATE Payment SET paidAmount = ?, paidDate = ?, pendingAmount = ? WHERE id = ?";
			pstmt = conn.prepareStatement(sql);
			BigDecimal newPendingAmount = currentPendingAmount.subtract(paidAmount);
			pstmt.setBigDecimal(1, paidAmount);
			pstmt.setObject(2, java.sql.Date.valueOf(paidDate));
			pstmt.setBigDecimal(3, newPendingAmount);
			pstmt.setLong(4, paymentId);
			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected == 0) {
				throw new SQLException("Failed to update payment record.");
			}

			// Update shop_amount table (assuming one shop_amount entry per payment)
			String shopSql = "UPDATE shop_amount SET shopAmount = ?, amountDate = ? WHERE shop_description = ?";
			shopPstmt = conn.prepareStatement(shopSql);
			shopPstmt.setBigDecimal(1, paidAmount.negate());
			shopPstmt.setObject(2, java.sql.Date.valueOf(paidDate));
			shopPstmt.setString(3, "Payment for bill ID " + paymentId);
			int shopRowsAffected = shopPstmt.executeUpdate();
			if (shopRowsAffected == 0) {
				// If no shop_amount entry exists, insert a new one
				String insertShopSql = "INSERT INTO shop_amount (shopAmount, amountDate, shop_description) VALUES (?, ?, ?)";
				shopPstmt = conn.prepareStatement(insertShopSql);
				shopPstmt.setBigDecimal(1, paidAmount.negate());
				shopPstmt.setObject(2, java.sql.Date.valueOf(paidDate));
				shopPstmt.setString(3, "Payment for bill ID " + paymentId);
				shopRowsAffected = shopPstmt.executeUpdate();
				if (shopRowsAffected == 0) {
					throw new SQLException("Failed to insert shop amount record.");
				}
			}

			conn.commit();
		} catch (SQLException ex) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ignored) {
			}
			throw new RuntimeException("Database error while updating payment: " + ex.getMessage(), ex);
		} finally {
			closeResources(null, pstmt, null);
			closeResources(null, shopPstmt, conn);
			try {
				if (conn != null)
					conn.setAutoCommit(true);
			} catch (SQLException ignored) {
			}
		}
	}

	public static BigDecimal getCurrentPendingAmount(Long billId) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			String sql = "SELECT pendingAmount FROM Payment WHERE billId = ? ORDER BY paidDate DESC, id DESC LIMIT 1";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, billId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				BigDecimal pendingAmount = rs.getBigDecimal("pendingAmount");
				return pendingAmount != null ? pendingAmount : fetchTotalBillAmountForBill(billId);
			}
			return fetchTotalBillAmountForBill(billId);
		} catch (SQLException ex) {
			throw new RuntimeException("Error retrieving pending amount for billId: " + billId + ": " + ex.getMessage(),
					ex);
		} finally {
			closeResources(rs, pstmt, conn);
		}
	}

	public static void addPayment(Long billId, BigDecimal paidAmount, LocalDate paidDate,
			BigDecimal currentPendingAmount) throws SQLException {
		if (billId == null || billId <= 0) {
			throw new IllegalArgumentException("Invalid bill ID.");
		}
		if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Paid amount must be greater than zero.");
		}
		if (paidDate == null) {
			throw new IllegalArgumentException("Paid date cannot be null.");
		}
		if (currentPendingAmount == null || currentPendingAmount.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Current pending amount cannot be null or negative.");
		}
		if (paidAmount.compareTo(currentPendingAmount) > 0) {
			throw new IllegalArgumentException("Paid amount cannot exceed pending amount.");
		}

		Connection conn = null;
		PreparedStatement pstmt = null;
		PreparedStatement shopPstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			conn.setAutoCommit(false);

			// Lock shop_amount table to prevent concurrent modifications
//            String lockSql = "LOCK TABLES shop_amount WRITE";
//            pstmt = conn.prepareStatement(lockSql);
//            pstmt.execute();
//            pstmt.close();

			// Check current shop balance
			String balanceSql = "SELECT SUM(shopAmount) as totalShopAmount FROM shop_amount";
			pstmt = conn.prepareStatement(balanceSql);
			rs = pstmt.executeQuery();
			BigDecimal shopBalance = BigDecimal.ZERO;
			if (rs.next()) {
				double sum = rs.getDouble("totalShopAmount");
				if (!rs.wasNull()) {
					shopBalance = BigDecimal.valueOf(sum);
				}
			}
			rs.close();
			pstmt.close();
			System.out.println("Current shop balance: " + shopBalance + ", payment amount: " + paidAmount
					+ " for bill ID: " + billId);

			// Validate shop balance
			if (shopBalance.compareTo(BigDecimal.ZERO) <= 0) {
				throw new SQLException(
						"Cannot process payment: Shop balance is zero or negative (current: " + shopBalance + ")");
			}
			BigDecimal newShopBalance = shopBalance.subtract(paidAmount);
			if (newShopBalance.compareTo(BigDecimal.ZERO) < 0) {
				throw new SQLException("Cannot process payment: Insufficient shop balance (current: " + shopBalance
						+ ", required: " + paidAmount + ", new: " + newShopBalance + ")");
			}

			// Insert payment record
			String paymentSql = "INSERT INTO Payment (paidAmount, paidDate, pendingAmount, billId) VALUES (?, ?, ?, ?)";
			pstmt = conn.prepareStatement(paymentSql);
			BigDecimal newPendingAmount = currentPendingAmount.subtract(paidAmount);
			pstmt.setBigDecimal(1, paidAmount);
			pstmt.setDate(2, Date.valueOf(paidDate));
			pstmt.setBigDecimal(3, newPendingAmount);
			pstmt.setLong(4, billId);
			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected == 0) {
				throw new SQLException("Failed to insert payment record for bill ID: " + billId);
			}
			pstmt.close();

			// Insert shop amount record
			String shopSql = "INSERT INTO shop_amount (shopAmount, amountDate, shop_description) VALUES (?, ?, ?)";
			shopPstmt = conn.prepareStatement(shopSql);
			shopPstmt.setBigDecimal(1, paidAmount.negate());
			shopPstmt.setDate(2, Date.valueOf(paidDate));
			shopPstmt.setString(3, "Payment for bill ID: " + billId);
			int shopRowsAffected = shopPstmt.executeUpdate();
			if (shopRowsAffected == 0) {
				throw new SQLException("Failed to insert shop amount record for bill ID: " + billId);
			}

			System.out.println("Shop balance updated: " + shopBalance + " - " + paidAmount + " = " + newShopBalance
					+ " for bill ID: " + billId);

			conn.commit();
		} catch (SQLException ex) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ignored) {
			}
			throw new SQLException(
					"Database error while adding payment for bill ID: " + billId + ": " + ex.getMessage(), ex);
		} finally {
			closeResources(rs, pstmt, null);
			closeResources(null, shopPstmt, null);
			try {
				if (conn != null) {
					conn.prepareStatement("UNLOCK TABLES").execute();
					conn.setAutoCommit(true);
					conn.close();
				}
			} catch (SQLException ignored) {
			}
		}
	}

	public static class PaymentSummary {
		private final List<Payment> payments;
		private final BigDecimal totalPaidAmount;
		private final BigDecimal latestPendingAmount;

		public PaymentSummary(List<Payment> payments, BigDecimal totalPaidAmount, BigDecimal latestPendingAmount) {
			this.payments = payments;
			this.totalPaidAmount = totalPaidAmount;
			this.latestPendingAmount = latestPendingAmount;
		}

		public List<Payment> getPayments() {
			return payments;
		}

		public BigDecimal getTotalPaidAmount() {
			return totalPaidAmount;
		}

		public BigDecimal getLatestPendingAmount() {
			return latestPendingAmount;
		}
	}

	public static PaymentSummary fetchPaymentsForBill(Long billId) {
		List<Payment> payments = new ArrayList<>();
		BigDecimal totalPaidAmount = BigDecimal.ZERO;
		BigDecimal latestPendingAmount = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			String sql = "SELECT id, billId, paidAmount, paidDate, pendingAmount "
					+ "FROM Payment WHERE billId = ? ORDER BY paidDate DESC, id DESC";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, billId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				Payment payment = new Payment(rs.getLong("id"), rs.getLong("billId"), rs.getBigDecimal("paidAmount"),
						rs.getObject("paidDate", LocalDate.class), rs.getBigDecimal("pendingAmount"));
				payments.add(payment);
				totalPaidAmount = totalPaidAmount.add(rs.getBigDecimal("paidAmount"));
				if (latestPendingAmount == null) {
					latestPendingAmount = rs.getBigDecimal("pendingAmount");
				}
			}
			if (latestPendingAmount == null) {
				latestPendingAmount = fetchTotalBillAmountForBill(billId);
			}
		} catch (SQLException ex) {
			throw new RuntimeException("Error fetching payments for billId: " + billId + ": " + ex.getMessage(), ex);
		} finally {
			closeResources(rs, pstmt, conn);
		}
		return new PaymentSummary(payments, totalPaidAmount, latestPendingAmount);
	}

	public static BigDecimal fetchTotalBillAmountForBill(Long billId) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			String sql = "SELECT COALESCE(SUM(p.total), 0) AS totalBillAmount " + "FROM Product p "
					+ "JOIN BillProduct bp ON p.id = bp.productId " + "WHERE bp.billId = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, billId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getBigDecimal("totalBillAmount");
			}
			return BigDecimal.ZERO;
		} catch (SQLException ex) {
			throw new RuntimeException(
					"Error fetching total bill amount for billId: " + billId + ": " + ex.getMessage(), ex);
		} finally {
			closeResources(rs, pstmt, conn);
		}
	}

	@SuppressWarnings("resource")
	public static void deleteBill(Long billId) {
		Connection conn = null;
		PreparedStatement deleteStockPstmt = null;
		PreparedStatement deleteProductPstmt = null;
		PreparedStatement deleteBillPstmt = null;
		ResultSet rs = null;

		try {
			conn = DBUtil.getConnection();
			conn.setAutoCommit(false);

			// 1. GET ALL productIds for this bill
			String fetchSql = "SELECT productId FROM BillProduct WHERE billId = ?";
			try (PreparedStatement pstmt = conn.prepareStatement(fetchSql)) {
				pstmt.setLong(1, billId);
				rs = pstmt.executeQuery();
				List<Long> productIds = new ArrayList<>();
				while (rs.next()) {
					productIds.add(rs.getLong("productId"));
				}

				// 2. DELETE ALL STOCK UNITS (by productId)
				String deleteStockSql = "DELETE FROM Stock WHERE productId = ?";
				deleteStockPstmt = conn.prepareStatement(deleteStockSql);
				for (Long pid : productIds) {
					deleteStockPstmt.setLong(1, pid);
					deleteStockPstmt.executeUpdate();
				}

				// 3. DELETE ALL PRODUCTS → BillProduct auto-deleted via CASCADE
				String deleteProductSql = "DELETE FROM Product WHERE id = ?";
				deleteProductPstmt = conn.prepareStatement(deleteProductSql);
				for (Long pid : productIds) {
					deleteProductPstmt.setLong(1, pid);
					deleteProductPstmt.executeUpdate();
				}
			}

			// 4. DELETE BILL
			String deleteBillSql = "DELETE FROM Bill WHERE id = ?";
			deleteBillPstmt = conn.prepareStatement(deleteBillSql);
			deleteBillPstmt.setLong(1, billId);
			int rows = deleteBillPstmt.executeUpdate();

			if (rows > 0) {
				conn.commit();
			} else {
				conn.rollback();
				throw new SQLException("Bill not found: " + billId);
			}

		} catch (SQLException ex) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ignored) {
			}
			throw new RuntimeException("Error deleting bill: " + ex.getMessage(), ex);
		} finally {
			closeResources(rs, null, null);
			closeResources(null, deleteStockPstmt, null);
			closeResources(null, deleteProductPstmt, null);
			closeResources(null, deleteBillPstmt, conn);
			try {
				if (conn != null)
					conn.setAutoCommit(true);
			} catch (SQLException ignored) {
			}
		}
	}

	@SuppressWarnings("resource")
	public static void addProductToBill(Long billId, Long wholesalerId, Product product, LocalDate billDate) {
		Connection conn = null;
		PreparedStatement productPstmt = null;
		PreparedStatement billProductPstmt = null;
		PreparedStatement stockPstmt = null;
		ResultSet generatedKeys = null;

		try {
			conn = DBUtil.getConnection();
			conn.setAutoCommit(false);

			// 1. INSERT INTO Product (one row per type)
			String productSql = "INSERT INTO Product (productName, quantity, perPieceRate, expiry, total, wholesalerId) "
					+ "VALUES (?, ?, ?, ?, ?, ?)";
			productPstmt = conn.prepareStatement(productSql, PreparedStatement.RETURN_GENERATED_KEYS);
			productPstmt.setString(1, product.getProductName());
			productPstmt.setInt(2, product.getQuantity());
			productPstmt.setBigDecimal(3, product.getPerPieceRate());
			productPstmt.setObject(4, product.getExpiry() != null ? java.sql.Date.valueOf(product.getExpiry()) : null);
			productPstmt.setBigDecimal(5, product.getTotal());
			productPstmt.setLong(6, wholesalerId);

			if (productPstmt.executeUpdate() == 0)
				throw new SQLException("Failed to insert product.");

			generatedKeys = productPstmt.getGeneratedKeys();
			if (!generatedKeys.next())
				throw new SQLException("Failed to retrieve product ID.");
			long productId = generatedKeys.getLong(1);
			product.setId(productId);

			// 2. INSERT INTO BillProduct (link bill to product)
			String billProductSql = "INSERT INTO BillProduct (billId, productId, quantity) VALUES (?, ?, ?)";
			billProductPstmt = conn.prepareStatement(billProductSql);
			billProductPstmt.setLong(1, billId);
			billProductPstmt.setLong(2, productId);
			billProductPstmt.setInt(3, product.getQuantity());
			if (billProductPstmt.executeUpdate() == 0)
				throw new SQLException("Failed to insert bill-product link.");

			// 3. INSERT ONE ROW PER UNIT INTO Stock (id auto-generated, productId linked)
			String stockSql = "INSERT INTO Stock (productName, quantity, perPieceRate, totalAmount, expiryDate, purchaseDate, productId) "
					+ "VALUES (?, 1, ?, ?, ?, ?, ?)";
			stockPstmt = conn.prepareStatement(stockSql);

			int qty = product.getQuantity();
			java.sql.Date expirySql = product.getExpiry() != null ? java.sql.Date.valueOf(product.getExpiry()) : null;
			java.sql.Date purchaseSql = java.sql.Date.valueOf(billDate); // Use bill date
			BigDecimal unitTotal = product.getPerPieceRate();

			for (int i = 0; i < qty; i++) {
				stockPstmt.setString(1, product.getProductName());
				stockPstmt.setBigDecimal(2, product.getPerPieceRate());
				stockPstmt.setBigDecimal(3, unitTotal);
				stockPstmt.setObject(4, expirySql);
				stockPstmt.setObject(5, purchaseSql);
				stockPstmt.setLong(6, productId); // LINK TO PRODUCT BATCH
				if (stockPstmt.executeUpdate() == 0)
					throw new SQLException("Failed to insert stock unit " + (i + 1));
			}

			conn.commit();
		} catch (SQLException ex) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ignored) {
			}
			throw new RuntimeException("Database error while adding product: " + ex.getMessage(), ex);
		} finally {
			closeResources(generatedKeys, productPstmt, null);
			closeResources(null, billProductPstmt, null);
			closeResources(null, stockPstmt, conn);
			try {
				if (conn != null)
					conn.setAutoCommit(true);
			} catch (SQLException ignored) {
			}
		}
	}

	@SuppressWarnings("resource")
	public static void deleteProduct(Long productId, Long billId) {
		Connection conn = null;
		PreparedStatement deleteStockPstmt = null;
		PreparedStatement deleteProductPstmt = null;

		try {
			conn = DBUtil.getConnection();
			conn.setAutoCommit(false);

			// 1. DELETE ALL STOCK UNITS
			String stockSql = "DELETE FROM Stock WHERE productId = ?";
			deleteStockPstmt = conn.prepareStatement(stockSql);
			deleteStockPstmt.setLong(1, productId);
			deleteStockPstmt.executeUpdate();

			// 2. DELETE PRODUCT → BillProduct auto-deleted via CASCADE
			String productSql = "DELETE FROM Product WHERE id = ?";
			deleteProductPstmt = conn.prepareStatement(productSql);
			deleteProductPstmt.setLong(1, productId);
			if (deleteProductPstmt.executeUpdate() == 0) {
				throw new SQLException("Product not found: " + productId);
			}

			conn.commit();
		} catch (SQLException ex) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ignored) {
			}
			throw new RuntimeException("Error deleting product: " + ex.getMessage(), ex);
		} finally {
			closeResources(null, deleteStockPstmt, null);
			closeResources(null, deleteProductPstmt, conn);
			try {
				if (conn != null)
					conn.setAutoCommit(true);
			} catch (SQLException ignored) {
			}
		}
	}

	/**
	 * Returns a list of distinct product names that contain the typed text
	 * (case-insensitive) and have a quantity >= 0. Compatible with Java 8 and
	 * later.
	 */
	public static List<String> getProductNameSuggestions(String partialName) {
		List<String> suggestions = new ArrayList<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();

			// ---- multi-line SQL built with simple string concatenation ----
			String sql = "SELECT DISTINCT productName " + "FROM Stock " + "WHERE LOWER(productName) LIKE ? "
					+ "  AND quantity >= 0 " + "ORDER BY productName";

			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, "%" + partialName.toLowerCase() + "%");
			rs = pstmt.executeQuery();

			while (rs.next()) {
				suggestions.add(rs.getString("productName"));
			}
		} catch (SQLException ex) {
			throw new RuntimeException("Error fetching product suggestions: " + ex.getMessage(), ex);
		} finally {
			closeResources(rs, pstmt, conn);
		}
		return suggestions;
	}

	/**
	 * Refreshes the supplied JComboBox with the suggestions returned from the DB.
	 * Keeps the currently typed text and shows the popup.
	 */
	public static void updateProductComboBox(javax.swing.JComboBox<String> comboBox, String typedText) {
		List<String> suggestions = getProductNameSuggestions(typedText);

		comboBox.removeAllItems();
		comboBox.addItem(""); // optional empty entry
		for (String s : suggestions) {
			comboBox.addItem(s);
		}

		// Restore the text the user has already typed
		if (!typedText.isEmpty()) {
			comboBox.getEditor().setItem(typedText);
		}
		comboBox.showPopup();
	}

	private static void closeResources(ResultSet rs, PreparedStatement pstmt, Connection conn) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				System.err.println("Error closing ResultSet: " + e.getMessage());
			}
		}
		if (pstmt != null) {
			try {
				pstmt.close();
			} catch (SQLException e) {
				System.err.println("Error closing PreparedStatement: " + e.getMessage());
			}
		}
		DBUtil.closeConnection(conn);
	}
}