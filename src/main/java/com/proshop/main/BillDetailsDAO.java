package com.proshop.main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.proshop.connection.DBUtil;
import com.proshop.model.BillDetails;

public class BillDetailsDAO {

    // ===================================================================
    // [0] CONSTANTS & COLORS
    // ===================================================================
    private static final Color BACKGROUND_COLOR = new Color(33, 33, 33);     // #212121
    private static final Color TEXT_COLOR = Color.WHITE;                    // #FFFFFF
    private static final Color ACCENT_COLOR = new Color(255, 215, 0);       // #FFD700 (Gold)
    private static final Color SHADOW_COLOR = new Color(224, 224, 224);     // #E0E0E0

    // ===================================================================
    // [1] PUBLIC API: LOAD DATA
    // ===================================================================

    // [1.1] Load all bill details from database
    public List<BillDetails> loadBillDetails() throws SQLException {
        List<BillDetails> billDetailsList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT billId, rent, lightBill, maintenanceBill, salary, parcelBillAmount, "
                       + "parcelBillDescription, bankEmi, othersAmount, othersDescription, productSale, billDate "
                       + "FROM billdetails";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                BillDetails bill = new BillDetails(
                    rs.getLong("billId"),
                    rs.getDouble("rent"),
                    rs.getDouble("lightBill"),
                    rs.getDouble("maintenanceBill"),
                    rs.getDouble("salary"),
                    0.0, // productPurchase removed
                    rs.getDouble("parcelBillAmount"),
                    rs.getString("parcelBillDescription"),
                    rs.getDouble("bankEmi"),
                    rs.getDouble("othersAmount"),
                    rs.getString("othersDescription"),
                    rs.getDouble("productSale"),
                    rs.getObject("billDate", LocalDate.class)
                );
                billDetailsList.add(bill);
            }
        } finally {
            closeResources(rs, pstmt, conn);
        }
        return billDetailsList;
    }

    // [1.2] Get current shop balance
    public double getShopBalance() throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        double shopBalance = 0.0;
        try {
            conn = DBUtil.getConnection();
            String shopSql = "SELECT SUM(shopAmount) as totalShopAmount FROM shop_amount";
            pstmt = conn.prepareStatement(shopSql);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                shopBalance = rs.getDouble("totalShopAmount");
                if (rs.wasNull()) shopBalance = 0.0;
            }
        } finally {
            closeResources(rs, pstmt, conn);
        }
        return shopBalance;
    }

    // [1.3] Load shop amount history for display
    public List<Object[]> loadShopAmountData() throws SQLException {
        List<Object[]> shopAmounts = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT shopAmount, shop_description, amountDate FROM shop_amount";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                double amount = rs.getDouble("shopAmount");
                String description = rs.getString("shop_description");
                LocalDate date = rs.getObject("amountDate", LocalDate.class);
                shopAmounts.add(new Object[] { String.format("%.2f", amount), description, date });
            }
        } finally {
            closeResources(rs, pstmt, conn);
        }
        return shopAmounts;
    }

    // ===================================================================
    // [2] PUBLIC API: INSERT OPERATIONS
    // ===================================================================

    // [2.1] Add money to shop (e.g., capital injection)
    public void addShopAmount(double amount, String description, Date shopDate) throws SQLException {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            updateShopBalance(conn, amount, description, shopDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            conn.commit();
        } catch (SQLException ex) {
            rollback(conn);
            throw ex;
        } finally {
            resetAutoCommitAndClose(conn);
        }
    }

    // [2.2] Add expense entry into billdetails + deduct from shop balance
    public void addBillDetails(String amountFieldName, String descriptionFieldName,
                               double amount, String descriptionText, Date billDate) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            String sql = "INSERT INTO billdetails (" + amountFieldName + ", " + descriptionFieldName + ", billDate) VALUES (?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1, amount);
            pstmt.setString(2, descriptionText.isEmpty() ? null : descriptionText);
            pstmt.setObject(3, billDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                updateShopBalance(conn, -amount, amountFieldName);
                conn.commit();
            } else {
                conn.rollback();
                throw new SQLException("Failed to add " + amountFieldName.toLowerCase() + ".");
            }
        } catch (SQLException ex) {
            rollback(conn);
            throw ex;
        } finally {
            closeResources(null, pstmt, conn);
            resetAutoCommitAndClose(conn);
        }
    }

    // ===================================================================
    // [3] PUBLIC API: UPDATE & DELETE
    // ===================================================================

    // [3.1] Update existing bill detail
    public void updateBillDetails(long billId, String amountField, String descriptionField,
                                  double amount, String descriptionText, Date billDate, double oldAmount) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            String sql = "UPDATE billdetails SET "
                       + "rent = ?, lightBill = ?, maintenanceBill = ?, salary = ?, "
                       + "parcelBillAmount = ?, parcelBillDescription = ?, bankEmi = ?, "
                       + "othersAmount = ?, othersDescription = ?, billDate = ? "
                       + "WHERE billId = ?";

            pstmt = conn.prepareStatement(sql);
            int paramIndex = 1;
            pstmt.setDouble(paramIndex++, amountField.equals("rent") ? amount : 0.0);
            pstmt.setDouble(paramIndex++, amountField.equals("lightBill") ? amount : 0.0);
            pstmt.setDouble(paramIndex++, amountField.equals("maintenanceBill") ? amount : 0.0);
            pstmt.setDouble(paramIndex++, amountField.equals("salary") ? amount : 0.0);
            pstmt.setDouble(paramIndex++, amountField.equals("parcelBillAmount") ? amount : 0.0);
            pstmt.setString(paramIndex++, amountField.equals("parcelBillAmount") ? (descriptionText.isEmpty() ? null : descriptionText) : null);
            pstmt.setDouble(paramIndex++, amountField.equals("bankEmi") ? amount : 0.0);
            pstmt.setDouble(paramIndex++, amountField.equals("othersAmount") ? amount : 0.0);
            pstmt.setString(paramIndex++, !amountField.equals("parcelBillAmount") && amount > 0 ? (descriptionText.isEmpty() ? null : descriptionText) : null);
            pstmt.setObject(paramIndex++, billDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            pstmt.setLong(paramIndex, billId);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                double difference = amount - oldAmount;
                if (difference != 0) {
                    updateShopBalance(conn, -difference, getDisplayNameByField(amountField) + " Adjustment");
                }
                conn.commit();
            } else {
                conn.rollback();
                throw new SQLException("Failed to update bill.");
            }
        } catch (SQLException ex) {
            rollback(conn);
            throw ex;
        } finally {
            closeResources(null, pstmt, conn);
            resetAutoCommitAndClose(conn);
        }
    }

    // [3.2] Delete bill and refund amount to shop balance
    @SuppressWarnings("resource")
    public void deleteBillDetails(long billId) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // Fetch current values
            String selectSql = "SELECT rent, lightBill, maintenanceBill, salary, parcelBillAmount, bankEmi, othersAmount FROM billdetails WHERE billId = ?";
            pstmt = conn.prepareStatement(selectSql);
            pstmt.setLong(1, billId);
            rs = pstmt.executeQuery();

            double[] amounts = new double[7];
            if (rs.next()) {
                amounts[0] = rs.getDouble("rent");
                amounts[1] = rs.getDouble("lightBill");
                amounts[2] = rs.getDouble("maintenanceBill");
                amounts[3] = rs.getDouble("salary");
                amounts[4] = rs.getDouble("parcelBillAmount");
                amounts[5] = rs.getDouble("bankEmi");
                amounts[6] = rs.getDouble("othersAmount");
            } else {
                throw new SQLException("Bill not found.");
            }
            rs.close();
            pstmt.close();

            // Delete record
            String sql = "DELETE FROM billdetails WHERE billId = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, billId);
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                String[] fields = { "Rent", "Light Bill", "Maintenance Bill", "Salary", "Parcel Bill", "Bank EMI", "Others Amount" };
                int nonZeroIndex = -1;
                for (int i = 0; i < amounts.length; i++) {
                    if (amounts[i] > 0) {
                        if (nonZeroIndex != -1)
                            throw new IllegalArgumentException("Multiple non-zero amounts detected in bill.");
                        nonZeroIndex = i;
                    }
                }
                if (nonZeroIndex != -1) {
                    updateShopBalance(conn, amounts[nonZeroIndex], fields[nonZeroIndex] + " Refund");
                }
                conn.commit();
            } else {
                conn.rollback();
                throw new SQLException("Failed to delete bill.");
            }
        } catch (SQLException | IllegalArgumentException ex) {
            rollback(conn);
            throw new SQLException(ex.getMessage());
        } finally {
            closeResources(rs, pstmt, conn);
            resetAutoCommitAndClose(conn);
        }
    }

    // ===================================================================
    // [4] PUBLIC API: SUMMARY & UI TOTALS
    // ===================================================================

    // [4.1] Load and display all financial totals in cards
    public void loadTotalsData(JPanel cardsPanel, JLabel shopBalanceLabel,
                               JLabel totalProfitLabel, JLabel capitalLabel) throws SQLException {
        cardsPanel.removeAll();
        double totalProfit = 0.0, totalShopBalance = 0.0, totalStockAmount = 0.0;
        double customerPaid = 0.0, gymWholesalerPaid = 0.0;
        double billExpenses = 0.0, wholesalerPaid = 0.0;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();

            // [4.1.1] Bill Expenses
            billExpenses = sum(conn, "SELECT SUM(rent + lightBill + maintenanceBill + salary + parcelBillAmount + bankEmi + othersAmount) as expenses FROM billdetails");
            cardsPanel.add(createTotalsCard("Bill Details", new String[]{"Total Expenses: %.2f"}, new double[]{billExpenses}));

            // [4.1.2] Customer Sales & Profit
            pstmt = conn.prepareStatement("SELECT SUM(amount_paid) as totalPaid, SUM(finalBill) as sales, SUM(netProfit) as profit FROM customer");
            rs = pstmt.executeQuery();
            if (rs.next()) {
                customerPaid = nullSafe(rs, "totalPaid");
                double sales = nullSafe(rs, "sales");
                double profit = nullSafe(rs, "profit");
                totalProfit += profit;
                cardsPanel.add(createTotalsCard("Customer", new String[]{"Amount Paid: %.2f", "Total Sales: %.2f", "Net Profit: %.2f"},
                    new double[]{customerPaid, sales, profit}));
            }
            closeStmtRs(pstmt, rs);

            // [4.1.3] Gym/Wholesaler Sales
            pstmt = conn.prepareStatement("SELECT SUM(wp.amountPaid) as totalPaid, SUM(gw.totalBill) as sales, SUM(gw.netProfit) as profit "
                                        + "FROM gym_wholesaler gw LEFT JOIN wholesaler_payment wp ON gw.id = wp.wholesalerId");
            rs = pstmt.executeQuery();
            if (rs.next()) {
                gymWholesalerPaid = nullSafe(rs, "totalPaid");
                double sales = nullSafe(rs, "sales");
                double profit = nullSafe(rs, "profit");
                totalProfit += profit;
                cardsPanel.add(createTotalsCard("Gym/Shop Client", new String[]{"Amount Paid: %.2f", "Total Sales: %.2f", "Net Profit: %.2f"},
                    new double[]{gymWholesalerPaid, sales, profit}));
            }
            closeStmtRs(pstmt, rs);

            // [4.1.4] Stock Value
            totalStockAmount = sum(conn, "SELECT SUM(totalAmount) as totalStock FROM stock");
            cardsPanel.add(createTotalsCard("Stock", new String[]{"Total Stock Amount: %.2f"}, new double[]{totalStockAmount}));

            // [4.1.5] Wholesaler Bills
            double wholesalerTotalBill = sum(conn, "SELECT SUM(total) as totalBill FROM product");
            wholesalerPaid = sum(conn, "SELECT SUM(paidAmount) as totalPaid FROM payment");
            cardsPanel.add(createTotalsCard("Wholesaler", new String[]{"Total Bill: %.2f", "Amount Paid: %.2f"},
                new double[]{wholesalerTotalBill, wholesalerPaid}));

            // [4.1.6] Shop Balance
            totalShopBalance = sum(conn, "SELECT SUM(shopAmount) as totalShopAmount FROM shop_amount");

            // Update Labels
            shopBalanceLabel.setText(String.format("Shop Balance: %.2f", totalShopBalance));
            totalProfitLabel.setText(String.format("Total Net Profit: %.2f", totalProfit));
            capitalLabel.setText(String.format("Capital: %.2f", totalShopBalance + totalStockAmount));

        } finally {
            closeResources(rs, pstmt, conn);
        }
    }

    // ===================================================================
    // [5] PRIVATE HELPER: SHOP BALANCE LOGIC
    // ===================================================================

    // [5.1] Update shop balance with validation
    private void updateShopBalance(Connection conn, double amount, String description, LocalDate date) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            double currentBalance = sum(conn, "SELECT SUM(shopAmount) as totalShopAmount FROM shop_amount");

            if (currentBalance + amount < 0) {
                throw new SQLException("Insufficient shop balance for " + description);
            }

            String insertSql = "INSERT INTO shop_amount (shopAmount, amountDate, shop_description) VALUES (?, ?, ?)";
            pstmt = conn.prepareStatement(insertSql);
            pstmt.setDouble(1, amount);
            pstmt.setObject(2, date);
            pstmt.setString(3, description);
            if (pstmt.executeUpdate() == 0) {
                throw new SQLException("Failed to update shop balance for " + description);
            }
        } finally {
            closeStmtRs(rs, pstmt);
        }
    }

    // [5.2] Overloaded version with current date
    private void updateShopBalance(Connection conn, double amount, String description) throws SQLException {
        updateShopBalance(conn, amount, description, LocalDate.now());
    }

    // ===================================================================
    // [6] PRIVATE HELPER: UI COMPONENTS
    // ===================================================================

    // [6.1] Create styled summary card
    private JPanel createTotalsCard(String title, String[] labels, double[] values) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BACKGROUND_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SHADOW_COLOR, 1),
            new EmptyBorder(10, 10, 10, 10)
        ));
        card.setPreferredSize(new Dimension(200, 100));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(ACCENT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(5));

        for (int i = 0; i < labels.length; i++) {
            JLabel label = new JLabel(String.format(labels[i], values[i]));
            label.setFont(new Font("Arial", Font.BOLD, 16));
            label.setForeground(TEXT_COLOR);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(label);
            card.add(Box.createVerticalStrut(2));
        }
        return card;
    }

    // ===================================================================
    // [7] PRIVATE UTILITIES: DB & NAMING
    // ===================================================================

    // [7.1] Get human-readable name from DB field
    private String getDisplayNameByField(String amountField) {
        String[] fields = { "rent", "lightBill", "maintenanceBill", "salary", "parcelBillAmount", "bankEmi", "othersAmount" };
        String[] displayNames = { "Rent", "Light Bill", "Maintenance Bill", "Salary", "Parcel Bill", "Bank EMI", "Others Amount" };
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].equals(amountField)) {
                return displayNames[i];
            }
        }
        return "";
    }

    // [7.2] Safe sum query
    private double sum(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? nullSafe(rs, rs.getMetaData().getColumnName(1)) : 0.0;
        }
    }

    // [7.3] Null-safe double getter
    private double nullSafe(ResultSet rs, String column) throws SQLException {
        double val = rs.getDouble(column);
        return rs.wasNull() ? 0.0 : val;
    }

    // [7.4] Close PreparedStatement & ResultSet
    private void closeStmtRs(AutoCloseable... resources) {
        for (AutoCloseable r : resources) {
            if (r != null) try { r.close(); } catch (Exception ignored) {}
        }
    }

    // [7.5] Rollback wrapper
    private void rollback(Connection conn) {
        if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
    }

    // [7.6] Reset autocommit and close connection
    private void resetAutoCommitAndClose(Connection conn) {
        if (conn != null) {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            DBUtil.closeConnection(conn);
        }
    }

    // [7.7] Master resource closer
    private void closeResources(ResultSet rs, PreparedStatement pstmt, Connection conn) {
        closeStmtRs(rs, pstmt);
        DBUtil.closeConnection(conn);
    }
}