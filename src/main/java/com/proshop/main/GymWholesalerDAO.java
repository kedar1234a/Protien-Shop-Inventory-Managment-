package com.proshop.main;

import java.awt.Component;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.proshop.connection.DBUtil;
import com.proshop.model.GymWholesaler;

public class GymWholesalerDAO {
    private static final Logger LOGGER = Logger.getLogger(GymWholesalerDAO.class.getName());

    private void closeResources(ResultSet rs, PreparedStatement pstmt, Connection conn) {
        try {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

 // =====================================================
    // 1. GET STOCK QUANTITY (FROM `stock` TABLE ONLY)
    // =====================================================
    public int getStockQuantity(String productName) {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM stock WHERE LOWER(productName) = LOWER(?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            LOGGER.warning("getStockQuantity failed: " + ex.getMessage());
            return 0;
        }
    }

    // =====================================================
    // 2. GET MULTIPLE STOCK QUANTITIES (FROM `stock` ONLY)
    // =====================================================
    public Map<String, Integer> getStockQuantities(List<String> productNames) {
        Map<String, Integer> map = new HashMap<>();
        if (productNames == null || productNames.isEmpty()) return map;

        String placeholders = String.join(",", Collections.nCopies(productNames.size(), "?"));
        String sql = "SELECT LOWER(productName) AS name, COALESCE(SUM(quantity), 0) AS qty " +
                     "FROM stock WHERE LOWER(productName) IN (" + placeholders + ") " +
                     "GROUP BY productName";

        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            for (int i = 0; i < productNames.size(); i++) {
                ps.setString(i + 1, productNames.get(i).trim().toLowerCase());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("name"), rs.getInt("qty"));
                }
            }
        } catch (SQLException ex) {
            LOGGER.warning("getStockQuantities failed: " + ex.getMessage());
        }
        return map;
    }
    // =====================================================
    // 3. UPDATE STOCK (DIRECTLY ON `stock` TABLE)
    // =====================================================
    private boolean updateStockQuantity(Connection conn, String productName, int qtyChange, double perPieceRate) throws SQLException {
        if (productName == null || productName.trim().isEmpty()) return false;

        String name = productName.trim();
        boolean deduct = qtyChange > 0;
        int qty = Math.abs(qtyChange);

        if (deduct) {
            // DEDUCT: Find matching stock by name + rate
            String sel = "SELECT id, quantity FROM stock WHERE LOWER(productName) = LOWER(?) AND perPieceRate = ? AND quantity > 0 ORDER BY id FOR UPDATE";
            List<long[]> updates = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sel)) {
                ps.setString(1, name);
                ps.setDouble(2, perPieceRate);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next() && qty > 0) {
                        long id = rs.getLong("id");
                        int cur = rs.getInt("quantity");
                        int take = Math.min(cur, qty);
                        updates.add(new long[]{id, take});
                        qty -= take;
                    }
                }
            }
            if (qty > 0) {
                LOGGER.warning("Not enough stock for: " + name + " (rate: " + perPieceRate + ")");
                return false;
            }

            String upd = "UPDATE stock SET quantity = quantity - ?, totalAmount = (quantity - ?) * perPieceRate WHERE id = ?";
            for (long[] u : updates) {
                try (PreparedStatement ps = conn.prepareStatement(upd)) {
                    ps.setLong(1, u[1]);
                    ps.setLong(2, u[1]);
                    ps.setLong(3, u[0]);
                    ps.executeUpdate();
                }
            }
            return true;

        } else if (qty > 0) {
            // ADD: Insert new rows (1 per unit)
            String ins = "INSERT INTO stock (productName, perPieceRate, quantity, totalAmount, purchaseDate, productId) " +
                         "VALUES (?, ?, 1, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(ins)) {
                java.sql.Date today = java.sql.Date.valueOf(LocalDate.now());

                // Find any productId for this productName (or use 1 if none)
                Long productId = 1L;
                String pidSql = "SELECT id FROM product WHERE LOWER(productName) = LOWER(?) LIMIT 1";
                try (PreparedStatement pidPs = conn.prepareStatement(pidSql)) {
                    pidPs.setString(1, name);
                    try (ResultSet rs = pidPs.executeQuery()) {
                        if (rs.next()) productId = rs.getLong("id");
                    }
                }

                for (int i = 0; i < qty; i++) {
                    ps.setString(1, name);
                    ps.setDouble(2, perPieceRate);
                    ps.setDouble(3, perPieceRate);
                    ps.setDate(4, today);
                    ps.setLong(5, productId);
                    ps.addBatch();
                }
                int[] res = ps.executeBatch();
                return Arrays.stream(res).allMatch(r -> r >= 0);
            }
        }
        return true;
    }

    // =====================================================
    // 4. SAVE PRODUCTS (JAVA 8+ COMPATIBLE - NO TEXT BLOCKS)
    // =====================================================
    public void saveProducts(List<GymWholesaler> products, LocalDate dateOfPurchase, JPanel parent) {
        if (products == null || products.isEmpty()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "No products to save.", "Info", JOptionPane.INFORMATION_MESSAGE));
            return;
        }

        // NORMAL STRING (NO TEXT BLOCK)
        String sql = "INSERT INTO gym_wholesaler " +
                     "(wholesalerName, mobileNo, address, productName, quantity, " +
                     "buyingPrice, sellingPrice, totalBill, netProfit, " +
                     "paymentMode, dateOfPurchase, description) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // VALIDATE STOCK
            List<String> names = products.stream()
                    .map(p -> p.getProductName().trim())
                    .distinct()
                    .toList();

            Map<String, Integer> stockMap = getStockQuantities(names);

            for (GymWholesaler p : products) {
                String key = p.getProductName().trim().toLowerCase();
                int needed = p.getProductQuantity();
                int available = stockMap.getOrDefault(key, 0);

                if (needed > available) {
                    conn.rollback();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                            "Insufficient stock for \"" + p.getProductName() +
                                    "\". Need: " + needed + ", Available: " + available,
                            "Stock Error", JOptionPane.ERROR_MESSAGE));
                    return;
                }
            }

            // BATCH INSERT
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (GymWholesaler p : products) {
                    ps.setString(1, p.getWholesalerName());
                    ps.setString(2, p.getMobileNo());
                    ps.setString(3, p.getAddress());
                    ps.setString(4, p.getProductName().trim());
                    ps.setInt(5, p.getProductQuantity());
                    ps.setDouble(6, p.getBuyingPrice());
                    ps.setDouble(7, p.getSellingPrice());
                    ps.setDouble(8, p.getTotalBill());
                    ps.setDouble(9, p.getNetProfit());
                    ps.setString(10, p.getPaymentMode() != null ? p.getPaymentMode() : "Cash");
                    ps.setObject(11, java.sql.Date.valueOf(dateOfPurchase));
                    ps.setString(12, p.getDescription());
                    ps.addBatch();
                }
                int[] results = ps.executeBatch();
                if (Arrays.stream(results).anyMatch(r -> r < 0)) {
                    throw new SQLException("Batch insert failed");
                }
            }

            // DEDUCT STOCK
            for (GymWholesaler p : products) {
                if (!updateStockQuantity(conn, p.getProductName().trim(), p.getProductQuantity(), p.getBuyingPrice())) {
                    throw new SQLException("Failed to deduct stock for: " + p.getProductName());
                }
            }

            conn.commit();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                    "Products saved & stock updated!", "Success", JOptionPane.INFORMATION_MESSAGE));

        } catch (SQLException ex) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            LOGGER.severe("saveProducts failed: " + ex.getMessage());
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                    "Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE));
        } finally {
            closeResources(null, null, conn);
        }
    }
    
 // NEW METHOD: GET STOCK BY PRODUCT + PRICE
    public int getStockQuantityByPrice(String productName, double perPieceRate) {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM stock WHERE LOWER(productName) = LOWER(?) AND perPieceRate = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productName.trim());
            ps.setDouble(2, perPieceRate);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            LOGGER.warning("getStockQuantityByPrice failed: " + ex.getMessage());
            return 0;
        }
    }
    // =====================================================
    // 5. UPDATE PRODUCT (FIXED: ONE-BY-ONE RESTOCK ON DECREASE)
    // =====================================================
    public void updateProduct(long id, int newQuantity, double newSellingPrice, double buyingPrice, String productName, JPanel parent) {
        String selectSql = "SELECT quantity FROM gym_wholesaler WHERE id = ?";
        String updateSql = "UPDATE gym_wholesaler SET quantity = ?, sellingPrice = ?, totalBill = ?, netProfit = ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            int oldQuantity = 0;
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                                "Product not found.", "Error", JOptionPane.ERROR_MESSAGE));
                        return;
                    }
                    oldQuantity = rs.getInt("quantity");
                }
            }

            int qtyChange = newQuantity - oldQuantity;

            // CASE 1: INCREASING QUANTITY → DEDUCT FROM STOCK
            if (qtyChange > 0) {
                int available = getStockQuantity(productName);
                if (available < qtyChange) {
                    conn.rollback();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                            "Insufficient stock! Need: " + qtyChange + ", Available: " + available,
                            "Error", JOptionPane.ERROR_MESSAGE));
                    return;
                }
                if (!updateStockQuantity(conn, productName.trim(), qtyChange, buyingPrice)) {
                    conn.rollback();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                            "Failed to deduct stock", "Error", JOptionPane.ERROR_MESSAGE));
                    return;
                }
            }

            // CASE 2: DECREASING QUANTITY → RESTORE ONE-BY-ONE
            else if (qtyChange < 0) {
                int qtyToRestore = -qtyChange; // positive number

                String ins = "INSERT INTO stock (productName, perPieceRate, quantity, totalAmount, purchaseDate, productId) " +
                             "VALUES (?, ?, 1, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(ins)) {
                    java.sql.Date today = java.sql.Date.valueOf(LocalDate.now());

                    // Get productId
                    Long productId = 1L;
                    String pidSql = "SELECT id FROM product WHERE LOWER(productName) = LOWER(?) LIMIT 1";
                    try (PreparedStatement pidPs = conn.prepareStatement(pidSql)) {
                        pidPs.setString(1, productName.trim());
                        try (ResultSet rs = pidPs.executeQuery()) {
                            if (rs.next()) productId = rs.getLong("id");
                        }
                    }

                    for (int i = 0; i < qtyToRestore; i++) {
                        ps.setString(1, productName.trim());
                        ps.setDouble(2, buyingPrice);
                        ps.setDouble(3, buyingPrice);
                        ps.setDate(4, today);
                        ps.setLong(5, productId);
                        ps.addBatch();
                    }

                    int[] results = ps.executeBatch();
                    boolean allOk = Arrays.stream(results).allMatch(r -> r >= 0);
                    if (!allOk) {
                        conn.rollback();
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                                "Failed to restore " + qtyToRestore + " unit(s) to stock", "Error", JOptionPane.ERROR_MESSAGE));
                        return;
                    }
                }
            }

            // NO CHANGE → skip stock update

            // UPDATE gym_wholesaler
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setInt(1, newQuantity);
                ps.setDouble(2, newSellingPrice);
                ps.setDouble(3, newQuantity * newSellingPrice);
                ps.setDouble(4, newQuantity * (newSellingPrice - buyingPrice));
                ps.setLong(5, id);
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    conn.commit();
                    if (qtyChange < 0) {
                    } else if (qtyChange > 0) {
                    }
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Success", "", JOptionPane.INFORMATION_MESSAGE));
                } else {
                    conn.rollback();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "No changes made.", "Info", JOptionPane.INFORMATION_MESSAGE));
                }
            }

        } catch (SQLException ex) {
            LOGGER.severe("Update failed: " + ex.getMessage());
            try { conn.rollback(); } catch (SQLException ignored) {}
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                    "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
        } finally {
            closeResources(null, null, conn);
        }
    }
    // =====================================================
    // 6. DELETE PRODUCT + RESTORE STOCK (ONE-BY-ONE)
    // =====================================================
    public void deleteProduct(long id, String productName, JPanel parent) {
        String selectSql = "SELECT quantity, buyingPrice FROM gym_wholesaler WHERE id = ?";
        String deleteSql = "DELETE FROM gym_wholesaler WHERE id = ?";

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            int quantity = 0;
            double buyingPrice = 0.0;

            // 1. GET QUANTITY & BUYING PRICE
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                                "Product not found.", "Error", JOptionPane.ERROR_MESSAGE));
                        return;
                    }
                    quantity = rs.getInt("quantity");
                    buyingPrice = rs.getDouble("buyingPrice");
                }
            }

            // 2. RESTORE STOCK: INSERT ONE-BY-ONE (LIKE ADDING)
            if (quantity > 0) {
                String ins = "INSERT INTO stock (productName, perPieceRate, quantity, totalAmount, purchaseDate, productId) " +
                             "VALUES (?, ?, 1, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(ins)) {
                    java.sql.Date today = java.sql.Date.valueOf(LocalDate.now());

                    // Get productId from product table
                    Long productId = 1L;
                    String pidSql = "SELECT id FROM product WHERE LOWER(productName) = LOWER(?) LIMIT 1";
                    try (PreparedStatement pidPs = conn.prepareStatement(pidSql)) {
                        pidPs.setString(1, productName.trim());
                        try (ResultSet rs = pidPs.executeQuery()) {
                            if (rs.next()) productId = rs.getLong("id");
                        }
                    }

                    for (int i = 0; i < quantity; i++) {
                        ps.setString(1, productName.trim());
                        ps.setDouble(2, buyingPrice);
                        ps.setDouble(3, buyingPrice);
                        ps.setDate(4, today);
                        ps.setLong(5, productId);
                        ps.addBatch();
                    }

                    int[] results = ps.executeBatch();
                    boolean allOk = Arrays.stream(results).allMatch(r -> r >= 0);
                    if (!allOk) {
                        conn.rollback();
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                                "Failed to restore stock for: " + productName, "Error", JOptionPane.ERROR_MESSAGE));
                        return;
                    }
                }
            }

            // 3. DELETE FROM gym_wholesaler
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setLong(1, id);
                int deleted = ps.executeUpdate();
                if (deleted > 0) {
                    conn.commit();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                            "Product deleted &  unit(s) restored to stock!", 
                            "Success", JOptionPane.INFORMATION_MESSAGE));
                } else {
                    conn.rollback();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                            "Failed to delete product.", "Error", JOptionPane.ERROR_MESSAGE));
                }
            }

        } catch (SQLException ex) {
            LOGGER.severe("Delete product failed: " + ex.getMessage());
            try { conn.rollback(); } catch (SQLException ignored) {}
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                    "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
        } finally {
            closeResources(null, null, conn);
        }
    }

    // =====================================================
    // 7. DELETE WHOLESALER + RESTORE STOCK
    // =====================================================
    public void deleteWholesaler(GymWholesaler wholesaler, JPanel parent) {
        String selectSql = "SELECT productName, quantity, buyingPrice FROM gym_wholesaler WHERE wholesalerName = ? AND mobileNo = ?";
        String deleteSql = "DELETE FROM gym_wholesaler WHERE wholesalerName = ? AND mobileNo = ?";

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            List<String> names = new ArrayList<>();
            List<Integer> qtys = new ArrayList<>();
            List<Double> prices = new ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, wholesaler.getWholesalerName());
                ps.setString(2, wholesaler.getMobileNo());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String n = rs.getString("productName");
                        if (n != null) {
                            names.add(n.trim());
                            qtys.add(rs.getInt("quantity"));
                            prices.add(rs.getDouble("buyingPrice"));
                        }
                    }
                }
            }

            boolean ok = true;
            for (int i = 0; i < names.size(); i++) {
                if (!updateStockQuantity(conn, names.get(i), qtys.get(i), prices.get(i))) {
                    ok = false;
                    break;
                }
            }

            int deleted;
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setString(1, wholesaler.getWholesalerName());
                ps.setString(2, wholesaler.getMobileNo());
                deleted = ps.executeUpdate();
            }

            if (deleted > 0 && ok) {
                conn.commit();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Wholesaler deleted!", "Success", JOptionPane.INFORMATION_MESSAGE));
            } else {
                conn.rollback();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Failed to delete or restore stock.", "Error", JOptionPane.ERROR_MESSAGE));
            }
        } catch (SQLException ex) {
            LOGGER.severe("Delete wholesaler failed: " + ex.getMessage());
            try { conn.rollback(); } catch (SQLException ignored) {}
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
        } finally {
            closeResources(null, null, conn);
        }
    }

    // =====================================================
    // 8. OTHER METHODS (UNCHANGED, BUT SAFE)
    // =====================================================
    public List<GymWholesaler> fetchWholesalers() throws SQLException {
        List<GymWholesaler> list = new ArrayList<>();
        String sql = "SELECT DISTINCT wholesalerName, mobileNo, address FROM gym_wholesaler";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new GymWholesaler(null, rs.getString(1), rs.getString(2), null, 0, 0, 0, 0, null, null, rs.getString(3)));
            }
        }
        return list;
    }

    public void addWholesaler(GymWholesaler w, JPanel p) {
        String sql = "INSERT INTO gym_wholesaler (wholesalerName, mobileNo, address) VALUES (?, ?, ?)";
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            c.setAutoCommit(false);
            ps.setString(1, w.getWholesalerName());
            ps.setString(2, w.getMobileNo());
            ps.setString(3, w.getAddress());
            if (ps.executeUpdate() > 0) {
                c.commit();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(p, "Added!", "Success", JOptionPane.INFORMATION_MESSAGE));
            } else c.rollback();
        } catch (SQLException ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(p, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
        }
    }

    
    // =====================================================
    // 11. FETCH PRODUCTS AND RATES
    // =====================================================
    public Map<String, List<Double>> fetchProductsAndRates() {
        Map<String, List<Double>> productRates = new HashMap<>();
        String sql = "SELECT p.productName, s.perPieceRate FROM stock s JOIN product p ON s.productId = p.id WHERE s.quantity > 0";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("productName").toLowerCase();
                double rate = rs.getDouble("perPieceRate");
                productRates.computeIfAbsent(name, k -> new ArrayList<>()).add(rate);
            }
        } catch (SQLException ex) {
            LOGGER.severe("Error fetching products: " + ex.getMessage());
        }
        return productRates;
    }

    // =====================================================
    // 12. FETCH PURCHASE DATES
    // =====================================================
    public List<LocalDate> fetchPurchaseDates(GymWholesaler wholesaler) {
        List<LocalDate> dates = new ArrayList<>();
        String sql = "SELECT DISTINCT dateOfPurchase FROM gym_wholesaler WHERE wholesalerName = ? AND mobileNo = ? AND productName IS NOT NULL ORDER BY dateOfPurchase";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, wholesaler.getWholesalerName());
            pstmt.setString(2, wholesaler.getMobileNo());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getObject("dateOfPurchase", LocalDate.class);
                    if (date != null) dates.add(date);
                }
            }
        } catch (SQLException ex) {
            LOGGER.severe("Error fetching dates: " + ex.getMessage());
        }
        return dates;
    }

    // =====================================================
    // 13. FETCH PRODUCTS BY DATE
    // =====================================================
    public List<GymWholesaler> fetchProductsByDate(GymWholesaler wholesaler, LocalDate date) {
        List<GymWholesaler> products = new ArrayList<>();
        String sql = "SELECT id, productName, quantity, buyingPrice, sellingPrice, totalBill, netProfit, description " +
                     "FROM gym_wholesaler WHERE wholesalerName = ? AND mobileNo = ? AND dateOfPurchase = ? AND productName IS NOT NULL";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, wholesaler.getWholesalerName());
            pstmt.setString(2, wholesaler.getMobileNo());
            pstmt.setObject(3, date);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    GymWholesaler p = new GymWholesaler(
                        rs.getLong("id"), wholesaler.getWholesalerName(), wholesaler.getMobileNo(),
                        rs.getString("productName"), rs.getInt("quantity"),
                        rs.getDouble("buyingPrice"), rs.getDouble("sellingPrice"), rs.getDouble("netProfit"),
                        null, date, wholesaler.getAddress()
                    );
                    p.setDescription(rs.getString("description"));
                    p.setTotalBill(rs.getDouble("totalBill"));
                    products.add(p);
                }
            }
        } catch (SQLException ex) {
            LOGGER.severe("Error fetching products by date: " + ex.getMessage());
        }
        return products;
    }

    // =====================================================
    // 14. RECORD PAYMENT
    // =====================================================
    public void recordPayment(GymWholesaler wholesaler, double amountPaid, String paymentMode, LocalDate paymentDate, JPanel parent) {
        String paymentSql = "INSERT INTO wholesaler_payment (wholesalerId, paymentMode, dateOfAmountPaid, amountPaid, pendingAmount) VALUES (?, ?, ?, ?, ?)";
        String shopSql = "INSERT INTO shop_amount (shopAmount, amountDate) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            long wholesalerId = getWholesalerId(wholesaler, conn);
            if (wholesalerId == -1) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Wholesaler not found.", "Error", JOptionPane.ERROR_MESSAGE));
                conn.rollback();
                return;
            }

            double pendingAmount = calculatePendingAmount(wholesalerId, amountPaid, conn);
            int rows1, rows2;
            try (PreparedStatement pstmt = conn.prepareStatement(paymentSql)) {
                pstmt.setLong(1, wholesalerId);
                pstmt.setString(2, paymentMode);
                pstmt.setObject(3, paymentDate);
                pstmt.setDouble(4, amountPaid);
                pstmt.setDouble(5, pendingAmount);
                rows1 = pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement(shopSql)) {
                pstmt.setDouble(1, amountPaid);
                pstmt.setObject(2, paymentDate);
                rows2 = pstmt.executeUpdate();
            }

            if (rows1 > 0 && rows2 > 0) {
                conn.commit();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Payment recorded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE));
            } else {
                conn.rollback();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Failed to record payment.", "Error", JOptionPane.ERROR_MESSAGE));
            }
        } catch (SQLException ex) {
            LOGGER.severe("Error recording payment: " + ex.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ignored) {}
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
        } finally {
            closeResources(null, null, conn);
        }
    }

    // =====================================================
    // 15. FETCH PAYMENT DETAILS
    // =====================================================
    public Map<String, Object> fetchPaymentDetails(GymWholesaler wholesaler) {
        Map<String, Object> result = new HashMap<>();
        List<Object[]> bills = new ArrayList<>();
        List<Object[]> payments = new ArrayList<>();
        double totalBill = 0.0, totalPaid = 0.0;

        String billSql = "SELECT dateOfPurchase, SUM(totalBill) as totalBill FROM gym_wholesaler WHERE wholesalerName = ? AND mobileNo = ? AND productName IS NOT NULL GROUP BY dateOfPurchase";
        String paymentSql = "SELECT wp.dateOfAmountPaid, wp.amountPaid FROM wholesaler_payment wp JOIN gym_wholesaler gw ON wp.wholesalerId = gw.id WHERE gw.wholesalerName = ? AND gw.mobileNo = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement bp = conn.prepareStatement(billSql);
             PreparedStatement pp = conn.prepareStatement(paymentSql)) {

            bp.setString(1, wholesaler.getWholesalerName());
            bp.setString(2, wholesaler.getMobileNo());
            try (ResultSet rs = bp.executeQuery()) {
                while (rs.next()) {
                    LocalDate d = rs.getObject("dateOfPurchase", LocalDate.class);
                    double amt = rs.getDouble("totalBill");
                    totalBill += amt;
                    bills.add(new Object[]{ d != null ? d.toString() : "", String.format("%.2f", amt) });
                }
            }

            pp.setString(1, wholesaler.getWholesalerName());
            pp.setString(2, wholesaler.getMobileNo());
            try (ResultSet rs = pp.executeQuery()) {
                while (rs.next()) {
                    LocalDate d = rs.getObject("dateOfAmountPaid", LocalDate.class);
                    double amt = rs.getDouble("amountPaid");
                    totalPaid += amt;
                    payments.add(new Object[]{ d != null ? d.toString() : "", String.format("%.2f", amt) });
                }
            }

            result.put("bills", bills);
            result.put("payments", payments);
            result.put("totalBill", totalBill);
            result.put("totalPaid", totalPaid);
        } catch (SQLException ex) {
            LOGGER.severe("Error fetching payment details: " + ex.getMessage());
        }
        return result;
    }

    // =====================================================
    // 16. FETCH ALL PRODUCTS
    // =====================================================
    public List<GymWholesaler> fetchAllProducts(GymWholesaler wholesaler) {
        List<GymWholesaler> products = new ArrayList<>();
        String sql = "SELECT productName, quantity, buyingPrice, sellingPrice, totalBill, netProfit, dateOfPurchase FROM gym_wholesaler WHERE wholesalerName = ? AND mobileNo = ? AND productName IS NOT NULL";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, wholesaler.getWholesalerName());
            pstmt.setString(2, wholesaler.getMobileNo());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    GymWholesaler p = new GymWholesaler(
                        null, wholesaler.getWholesalerName(), wholesaler.getMobileNo(),
                        rs.getString("productName"), rs.getInt("quantity"),
                        rs.getDouble("buyingPrice"), rs.getDouble("sellingPrice"), rs.getDouble("netProfit"),
                        null, rs.getObject("dateOfPurchase", LocalDate.class), wholesaler.getAddress()
                    );
                    p.setTotalBill(rs.getDouble("totalBill"));
                    products.add(p);
                }
            }
        } catch (SQLException ex) {
            LOGGER.severe("Error fetching all products: " + ex.getMessage());
        }
        return products;
    }

    // =====================================================
    // 17. UPDATE PAYMENT
    // =====================================================
    public void updatePayment(GymWholesaler wholesaler, int rowIndex, double newAmount, String newPaymentMode, LocalDate newPaymentDate, Component parent) {
        String selectSql = "SELECT wp.id, wp.amountPaid, wp.dateOfAmountPaid FROM wholesaler_payment wp JOIN gym_wholesaler gw ON wp.wholesalerId = gw.id WHERE gw.wholesalerName = ? AND gw.mobileNo = ? ORDER BY wp.dateOfAmountPaid LIMIT 1 OFFSET ?";
        String updatePaymentSql = "UPDATE wholesaler_payment SET paymentMode = ?, dateOfAmountPaid = ?, amountPaid = ?, pendingAmount = ? WHERE id = ?";
        String updateShopSql = "UPDATE shop_amount SET shopAmount = ?, amountDate = ? WHERE amountDate = ? AND shopAmount = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            long paymentId = -1;
            double oldAmount = 0.0;
            LocalDate oldDate = null;
            pstmt = conn.prepareStatement(selectSql);
            pstmt.setString(1, wholesaler.getWholesalerName());
            pstmt.setString(2, wholesaler.getMobileNo());
            pstmt.setInt(3, rowIndex);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                paymentId = rs.getLong("id");
                oldAmount = rs.getDouble("amountPaid");
                oldDate = rs.getObject("dateOfAmountPaid", LocalDate.class);
            } else {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Payment not found.", "Error", JOptionPane.ERROR_MESSAGE));
                conn.rollback();
                return;
            }
            closeResources(rs, pstmt, null);

            long wholesalerId = getWholesalerId(wholesaler, conn);
            if (wholesalerId == -1) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Wholesaler not found.", "Error", JOptionPane.ERROR_MESSAGE));
                conn.rollback();
                return;
            }

            double pendingAmount = calculatePendingAmount(wholesalerId, newAmount - oldAmount, conn);

            int rows1, rows2;
            pstmt = conn.prepareStatement(updatePaymentSql);
            pstmt.setString(1, newPaymentMode);
            pstmt.setObject(2, newPaymentDate);
            pstmt.setDouble(3, newAmount);
            pstmt.setDouble(4, pendingAmount);
            pstmt.setLong(5, paymentId);
            rows1 = pstmt.executeUpdate();
            closeResources(null, pstmt, null);

            pstmt = conn.prepareStatement(updateShopSql);
            pstmt.setDouble(1, newAmount);
            pstmt.setObject(2, newPaymentDate);
            pstmt.setObject(3, oldDate);
            pstmt.setDouble(4, oldAmount);
            rows2 = pstmt.executeUpdate();

            if (rows1 > 0 && rows2 > 0) {
                conn.commit();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Payment updated!", "Success", JOptionPane.INFORMATION_MESSAGE));
            } else {
                conn.rollback();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Failed to update payment.", "Error", JOptionPane.ERROR_MESSAGE));
            }
        } catch (SQLException ex) {
            LOGGER.severe("Error updating payment: " + ex.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ignored) {}
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
        } finally {
            closeResources(rs, pstmt, conn);
        }
    }

    // =====================================================
    // 18. HELPER: GET WHOLESALER ID
    // =====================================================
    private long getWholesalerId(GymWholesaler wholesaler, Connection conn) {
        String sql = "SELECT id FROM gym_wholesaler WHERE wholesalerName = ? AND mobileNo = ? LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, wholesaler.getWholesalerName());
            pstmt.setString(2, wholesaler.getMobileNo());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getLong("id") : -1;
            }
        } catch (SQLException ex) {
            LOGGER.severe("Error getting wholesaler ID: " + ex.getMessage());
            return -1;
        }
    }

    // =====================================================
    // 19. HELPER: CALCULATE PENDING
    // =====================================================
    private double calculatePendingAmount(long wholesalerId, double newPayment, Connection conn) {
        String billSql = "SELECT COALESCE(SUM(totalBill), 0) FROM gym_wholesaler WHERE id = ?";
        String paidSql = "SELECT COALESCE(SUM(amountPaid), 0) FROM wholesaler_payment WHERE wholesalerId = ?";
        try (PreparedStatement bp = conn.prepareStatement(billSql);
             PreparedStatement pp = conn.prepareStatement(paidSql)) {
            bp.setLong(1, wholesalerId);
            double totalBill = 0.0;
            try (ResultSet rs = bp.executeQuery()) { if (rs.next()) totalBill = rs.getDouble(1); }
            pp.setLong(1, wholesalerId);
            double totalPaid = 0.0;
            try (ResultSet rs = pp.executeQuery()) { if (rs.next()) totalPaid = rs.getDouble(1); }
            return Math.max(0.0, totalBill - (totalPaid + newPayment));
        } catch (SQLException ex) {
            LOGGER.severe("Error calculating pending: " + ex.getMessage());
            return 0.0;
        }
    }
    
    // =====================================================
    // 9. UPDATE WHOLESALER (NAME / MOBILE / ADDRESS)
    // =====================================================
    /**
     * Updates the wholesaler information in every row that currently belongs to
     * {@code oldWholesaler}. Only the three wholesaler fields are changed.
     *
     * @param oldWholesaler the current wholesaler object (used to locate rows)
     * @param newWholesaler the new wholesaler data (only name, mobileNo, address are used)
     * @param parent        component used for Swing message dialogs
     */
    public void updateWholesaler(GymWholesaler oldWholesaler,
                                 GymWholesaler newWholesaler,
                                 JPanel parent) {
        if (oldWholesaler == null || newWholesaler == null) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                    "Invalid wholesaler data.", "Error", JOptionPane.ERROR_MESSAGE));
            return;
        }

        String oldName = oldWholesaler.getWholesalerName();
        String oldMobile = oldWholesaler.getMobileNo();

        String newName = newWholesaler.getWholesalerName();
        String newMobile = newWholesaler.getMobileNo();
        String newAddress = newWholesaler.getAddress();

        // basic validation (same as in the form)
        if (newName == null || newName.trim().isEmpty() ||
            newAddress == null || newAddress.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                    "Wholesaler Name and Address are required.", "Error", JOptionPane.ERROR_MESSAGE));
            return;
        }
        if (newMobile == null || !newMobile.matches("\\d{10}")) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                    "Mobile No must be a 10-digit number.", "Error", JOptionPane.ERROR_MESSAGE));
            return;
        }

        String sql = "UPDATE gym_wholesaler " +
                     "SET wholesalerName = ?, mobileNo = ?, address = ? " +
                     "WHERE wholesalerName = ? AND mobileNo = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement(sql);
            ps.setString(1, newName.trim());
            ps.setString(2, newMobile.trim());
            ps.setString(3, newAddress.trim());
            ps.setString(4, oldName);
            ps.setString(5, oldMobile);

            int updated = ps.executeUpdate();

            if (updated > 0) {
                conn.commit();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                        "Wholesaler updated successfully!\n" +
                        updated + " record(s) affected.",
                        "Success", JOptionPane.INFORMATION_MESSAGE));
            } else {
                conn.rollback();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                        "No records were updated – wholesaler not found.",
                        "Info", JOptionPane.INFORMATION_MESSAGE));
            }
        } catch (SQLException ex) {
            LOGGER.severe("updateWholesaler failed: " + ex.getMessage());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                    "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE));
        } finally {
            closeResources(null, ps, conn);
        }
    }
}