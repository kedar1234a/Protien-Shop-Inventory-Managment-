package com.proshop.main;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import com.proshop.connection.DBUtil;
import com.proshop.model.Bill;
import com.proshop.model.Payment;
import com.proshop.model.Product;
import com.proshop.model.Wholesaler;

public class WholesalerDAO {

	public void addWholesaler(Wholesaler wholesaler) throws SQLException {
		String sql = "INSERT INTO wholesalers (wholesaler_name, phone_no, address) VALUES (?, ?, ?)";
		try (Connection conn = DBUtil.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			stmt.setString(1, wholesaler.getWholesalerName());
			stmt.setString(2, wholesaler.getPhoneNo());
			stmt.setString(3, wholesaler.getAddress());
			stmt.executeUpdate();
			try (ResultSet rs = stmt.getGeneratedKeys()) {
				if (rs.next()) {
					wholesaler.setId(rs.getLong(1));
				}
			}
		}
	}

	public List<Wholesaler> getAllWholesalers() throws SQLException {
		String sql = "SELECT * FROM wholesalers";
		try (Connection conn = DBUtil.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql);
				ResultSet rs = stmt.executeQuery()) {
			return java.util.stream.Stream.generate(() -> {
				try {
					return rs.next()
							? new Wholesaler(rs.getLong("id"), rs.getString("wholesaler_name"),
									rs.getString("phone_no"), rs.getString("address"))
							: null;
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}).takeWhile(wholesaler -> wholesaler != null).collect(Collectors.toList());
		}
	}

	public Wholesaler getWholesalerById(Long id) throws SQLException {
		String sql = "SELECT * FROM wholesalers WHERE id = ?";
		try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, id);
			try (ResultSet rs = stmt.executeQuery()) {
				return rs.next()
						? new Wholesaler(rs.getLong("id"), rs.getString("wholesaler_name"), rs.getString("phone_no"),
								rs.getString("address"))
						: null;
			}
		}
	}

	public void updateWholesaler(Wholesaler wholesaler, String name, String phoneNo, String address)
			throws SQLException {
		String sql = "UPDATE wholesalers SET wholesaler_name = ?, phone_no = ?, address = ? WHERE id = ?";
		try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, name);
			stmt.setString(2, phoneNo);
			stmt.setString(3, address);
			stmt.setLong(4, wholesaler.getId());
			stmt.executeUpdate();
		}
	}

	public void deleteWholesaler(long selectedWholesaler) throws SQLException {
		String sql = "DELETE FROM wholesalers WHERE id = ?";
		try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, selectedWholesaler);
			stmt.executeUpdate();
		}
	}

	public List<Bill> fetchBillsForWholesaler(Long wholesalerId) throws SQLException {
		String sql = "SELECT * FROM bills WHERE wholesaler_id = ?";
		try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, wholesalerId);
			try (ResultSet rs = stmt.executeQuery()) {
				return java.util.stream.Stream.generate(() -> {
					try {
						return rs.next()
								? new Bill(rs.getLong("id"), rs.getDate("bill_date").toLocalDate(),
										rs.getBigDecimal("shipping_charges"), rs.getBigDecimal("bill_amount"))
								: null;
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}
				}).takeWhile(bill -> bill != null).collect(Collectors.toList());
			}
		}
	}

	public List<Product> fetchProductsForBill(Wholesaler wholesaler, Long billId) throws SQLException {
		String sql = "SELECT * FROM products WHERE bill_id = ?";
		try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, billId);
			try (ResultSet rs = stmt.executeQuery()) {
				return java.util.stream.Stream.generate(() -> {
					try {
						if (rs.next()) {
							Product product = new Product(rs.getLong("id"), rs.getString("product_name"),
									rs.getInt("quantity"), rs.getBigDecimal("per_piece_rate"),
									rs.getDate("expiry") != null ? rs.getDate("expiry").toLocalDate() : null,
									wholesaler.getId());
							product.setTotal(rs.getBigDecimal("total"));
							return product;
						}
						return null;
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}
				}).takeWhile(product -> product != null).collect(Collectors.toList());
			}
		}
	}

	public List<Payment> fetchPaymentsForBill(Long billId) throws SQLException {
		String sql = "SELECT * FROM payments WHERE bill_id = ?";
		try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, billId);
			try (ResultSet rs = stmt.executeQuery()) {
				return java.util.stream.Stream.generate(() -> {
					try {
						return rs.next()
								? new Payment(rs.getLong("id"), rs.getLong("bill_id"), rs.getBigDecimal("paid_amount"),
										rs.getDate("paid_date").toLocalDate(), rs.getBigDecimal("pending_amount"))
								: null;
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}
				}).takeWhile(payment -> payment != null).collect(Collectors.toList());
			}
		}
	}

	public BigDecimal getCurrentPendingAmount(Long billId) throws SQLException {
		String sql = "SELECT bill_amount FROM bills WHERE id = ?";
		try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, billId);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					BigDecimal billAmount = rs.getBigDecimal("bill_amount");
					BigDecimal totalPaid = fetchPaymentsForBill(billId).stream().map(Payment::getPaidAmount)
							.reduce(BigDecimal.ZERO, BigDecimal::add);
					return billAmount.subtract(totalPaid);
				}
			}
		}
		return null;
	}

	public void addBillAndProducts(Wholesaler wholesaler, LocalDate date, BigDecimal shippingCharges,
			BigDecimal billAmount, List<Product> products) throws SQLException {
		try (Connection conn = DBUtil.getConnection()) {
			conn.setAutoCommit(false);
			String billSql = "INSERT INTO bills (wholesaler_id, bill_date, shipping_charges, bill_amount) VALUES (?, ?, ?, ?)";
			Long billId;
			try (PreparedStatement stmt = conn.prepareStatement(billSql, Statement.RETURN_GENERATED_KEYS)) {
				stmt.setLong(1, wholesaler.getId());
				stmt.setDate(2, Date.valueOf(date));
				stmt.setBigDecimal(3, shippingCharges);
				stmt.setBigDecimal(4, billAmount);
				stmt.executeUpdate();
				try (ResultSet rs = stmt.getGeneratedKeys()) {
					if (rs.next()) {
						billId = rs.getLong(1);
					} else {
						throw new SQLException("Failed to retrieve bill ID.");
					}
				}
			}

			String productSql = "INSERT INTO products (bill_id, wholesaler_id, product_name, quantity, per_piece_rate, expiry, total, purchase_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(productSql)) {
				for (Product product : products) {
					stmt.setLong(1, billId);
					stmt.setLong(2, wholesaler.getId());
					stmt.setString(3, product.getProductName());
					stmt.setInt(4, product.getQuantity());
					stmt.setBigDecimal(5, product.getPerPieceRate());
					stmt.setDate(6, product.getExpiry() != null ? Date.valueOf(product.getExpiry()) : null);
					stmt.setBigDecimal(7, product.getTotal());
					stmt.setDate(8, Date.valueOf(date));
					stmt.executeUpdate();
				}
			}

			conn.commit();
		} catch (SQLException e) {
			try (Connection conn = DBUtil.getConnection()) {
				conn.rollback();
			}
			throw e;
		}
	}

	public void addPayment(Long billId, BigDecimal paidAmount, LocalDate paidDate, BigDecimal billAmount)
			throws SQLException {
		try (Connection conn = DBUtil.getConnection()) {
			conn.setAutoCommit(false);

			// Validate pending amount
			BigDecimal currentPending = getCurrentPendingAmount(billId);
			if (currentPending == null) {
				currentPending = billAmount;
			}
			BigDecimal newPending = currentPending.subtract(paidAmount);
			if (newPending.compareTo(BigDecimal.ZERO) < 0) {
				throw new SQLException("Payment amount exceeds pending amount.");
			}

			// Insert payment record
			String paymentSql = "INSERT INTO payments (bill_id, paid_amount, paid_date, pending_amount) VALUES (?, ?, ?, ?)";
			try (PreparedStatement stmt = conn.prepareStatement(paymentSql, Statement.RETURN_GENERATED_KEYS)) {
				stmt.setLong(1, billId);
				stmt.setBigDecimal(2, paidAmount);
				stmt.setDate(3, Date.valueOf(paidDate));
				stmt.setBigDecimal(4, newPending);
				stmt.executeUpdate();
				try (ResultSet rs = stmt.getGeneratedKeys()) {
					if (!rs.next()) {
						throw new SQLException("Failed to retrieve payment ID.");
					}
				}
			}

			// Update shop balance
			String description = "Payment for bill ID: " + billId;
			updateShopBalance(conn, paidAmount.doubleValue(), description);

			conn.commit();
		} catch (SQLException e) {
			try (Connection conn = DBUtil.getConnection()) {
				conn.rollback();
			}
			throw e;
		}
	}

	public static void updateShopBalance(Connection conn, double amount, String description) throws SQLException {
		try (PreparedStatement pstmt = conn
				.prepareStatement("SELECT SUM(shopAmount) as totalShopAmount FROM shop_amount");
				ResultSet rs = pstmt.executeQuery()) {
			double currentBalance = rs.next() && !rs.wasNull() ? rs.getDouble("totalShopAmount") : 0.0;

			if (currentBalance + amount < 0) {
				throw new SQLException("Insufficient shop balance for " + description);
			}

			try (PreparedStatement insertStmt = conn.prepareStatement(
					"INSERT INTO shop_amount (shopAmount, amountDate, shop_description) VALUES (?, ?, ?)")) {
				insertStmt.setDouble(1, amount);
				insertStmt.setDate(2, Date.valueOf(LocalDate.now(ZoneId.of("Asia/Kolkata"))));
				insertStmt.setString(3, description);
				int rows = insertStmt.executeUpdate();
				if (rows == 0) {
					throw new SQLException("Failed to update shop balance for " + description);
				}
			}

			double newBalance = currentBalance + amount;
			System.out.println("Balance update: " + currentBalance + " + (" + amount + ") = " + newBalance);
		}
	}

	public void updateBill(Bill bill, LocalDate date, BigDecimal shippingCharges, BigDecimal billAmount)
			throws SQLException {
		String sql = "UPDATE bills SET bill_date = ?, shipping_charges = ?, bill_amount = ? WHERE id = ?";
		try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setDate(1, Date.valueOf(date));
			stmt.setBigDecimal(2, shippingCharges);
			stmt.setBigDecimal(3, billAmount);
			stmt.setLong(4, bill.getId());
			stmt.executeUpdate();
		}
	}

	public void updateProduct(Product product, Bill bill, String productName, int quantity, BigDecimal perPieceRate,
			LocalDate expiry, BigDecimal total, LocalDate purchaseDate) throws SQLException {
		String sql = "UPDATE products SET product_name = ?, quantity = ?, per_piece_rate = ?, expiry = ?, total = ?, purchase_date = ? WHERE id = ? AND bill_id = ?";
		try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, productName);
			stmt.setInt(2, quantity);
			stmt.setBigDecimal(3, perPieceRate);
			stmt.setDate(4, expiry != null ? Date.valueOf(expiry) : null);
			stmt.setBigDecimal(5, total);
			stmt.setDate(6, Date.valueOf(purchaseDate));
			stmt.setLong(7, product.getId());
			stmt.setLong(8, bill.getId());
			stmt.executeUpdate();
		}
	}
}