package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.proshop.connection.DBUtil;
import com.proshop.model.Customer;
import com.toedter.calendar.JDateChooser;

public class CustomerFormDB {

    private final CustomerForm form;
    private final CustomerFormUI formUI;
    private final CustomerFormPDF formPDF;
    private final GymWholesalerDAO dao; // DAO instance

    // ========================================================================
    // 0. CONSTRUCTOR & INITIALIZATION
    // ========================================================================
    public CustomerFormDB(CustomerForm form, CustomerFormUI formUI) {
        this.form = form;
        this.formUI = formUI;
        this.formPDF = new CustomerFormPDF(form);
        this.dao = new GymWholesalerDAO(); // Initialize DAO
        setupProductAutoSuggest();
        setupMobileNumberRestriction();
        setupTableDateListener();
        setupBuyingPriceListener(); // Listener for price change
    }

    // 1. Setup Product Auto-Suggest
    private void setupProductAutoSuggest() {
        JTextField editor = (JTextField) formUI.getProductNameComboBox().getEditor().getEditorComponent();
        editor.addKeyListener(new KeyAdapter() {
            private String lastText = "";

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN
                        || e.getKeyCode() == KeyEvent.VK_ENTER) {
                    return;
                }

                String currentText = editor.getText();
                if (!currentText.equals(lastText)) {
                    lastText = currentText;
                    SwingUtilities.invokeLater(() -> autoFillProductDetailsPreserveCaret(currentText));
                }
            }
        });
    }
    
    private void autoFillProductDetailsPreserveCaret(String currentInput) {
        if (currentInput.isEmpty()) {
            formUI.getProductNameComboBox().removeAllItems();
            formUI.getProductNameComboBox().addItem("");
            formUI.getProductNameComboBox().showPopup();
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT DISTINCT productName FROM stock WHERE LOWER(productName) LIKE ? AND quantity > 0 ORDER BY productName";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "%" + currentInput.toLowerCase() + "%");
            rs = pstmt.executeQuery();

            List<String> matches = new ArrayList<>();
            while (rs.next()) {
                matches.add(rs.getString("productName"));
            }

            JComboBox<String> combo = formUI.getProductNameComboBox();
            combo.removeAllItems();
            combo.addItem("");
            matches.forEach(combo::addItem);

            String exactMatch = matches.stream()
                    .filter(m -> m.equalsIgnoreCase(currentInput))
                    .findFirst()
                    .orElse(null);

            if (exactMatch != null) {
                combo.setSelectedItem(exactMatch);
                loadPricesForProduct(exactMatch);
                updateBuyingPriceComboBox();
                combo.getEditor().setItem(currentInput); // Preserve typed text
            } else {
                combo.getEditor().setItem(currentInput);
                if (!matches.isEmpty()) {
                    combo.showPopup();
                }
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(form, "Auto-fill error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeResources(rs, pstmt, conn);
        }
    }

    // 2. Restrict Mobile Number
    private void setupMobileNumberRestriction() {
        formUI.getMobileNoField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                String text = formUI.getMobileNoField().getText();
                if (!(Character.isDigit(c) || c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE)) {
                    e.consume();
                    return;
                }
                if (text.length() >= 10 && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                }
            }
        });
    }
    
    

    // 3. Table Date Listener
    private void setupTableDateListener() {
        formUI.getTableDateChooser().addPropertyChangeListener("date", evt -> {
            if (evt.getNewValue() != null && !evt.getNewValue().equals(evt.getOldValue())) {
                loadTableData();
            }
        });
    }

    // 4. Buying Price Change Listener
    private void setupBuyingPriceListener() {
        formUI.getBuyingPriceComboBox().addActionListener(e -> {
            String product = (String) formUI.getProductNameComboBox().getSelectedItem();
            if (product != null && !product.trim().isEmpty()) {
                updateStockForSelectedPrice();
            }
        });
    }

    // ========================================================================
    // DB UTILS
    // ========================================================================
    private void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
        try { if (stmt != null) stmt.close(); } catch (SQLException ignored) {}
        try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
    }

    private void closeConnection(Connection conn) {
        if (conn != null) {
            try { conn.setAutoCommit(true); conn.close(); }
            catch (SQLException ignored) {}
        }
    }

    private void rollback(Connection conn, String msg) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            JOptionPane.showMessageDialog(form, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ========================================================================
    // STOCK: GET productId FROM stock
    // ========================================================================
    private Long getProductId(Connection conn, String productName) throws SQLException {
        String sql = "SELECT DISTINCT productId FROM stock WHERE LOWER(productName) = LOWER(?) LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, productName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getLong("productId") : null;
            }
        }
    }

    // ========================================================================
    // PRODUCT NAMES & PRICES FROM stock ONLY
    // ========================================================================
    public void loadProductNames() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT DISTINCT productName FROM stock WHERE quantity > 0 ORDER BY productName";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            formUI.getProductNameComboBox().removeAllItems();
            formUI.getProductPriceMap().clear();
            formUI.getProductNameComboBox().addItem("");

            while (rs.next()) {
                String name = rs.getString("productName");
                formUI.getProductNameComboBox().addItem(name);
                loadPricesForProduct(name);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(form, "Error loading products: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeResources(rs, pstmt, conn);
        }
    }

    private void loadPricesForProduct(String productName) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT DISTINCT perPieceRate FROM stock WHERE LOWER(productName) = LOWER(?) AND quantity > 0 ORDER BY perPieceRate";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, productName);
            rs = pstmt.executeQuery();

            List<Double> prices = new ArrayList<>();
            while (rs.next()) {
                prices.add(rs.getDouble("perPieceRate"));
            }
            formUI.getProductPriceMap().put(productName, prices.isEmpty() ? List.of(0.0) : prices);
        } catch (SQLException ex) {
            formUI.getProductPriceMap().put(productName, List.of(0.0));
        } finally {
            closeResources(rs, pstmt, conn);
        }
    }

    public void updateBuyingPriceComboBox() {
        String input = formUI.getProductNameComboBox().getEditor().getItem().toString().trim();
        if (input.isEmpty()) {
            formUI.getBuyingPriceComboBox().removeAllItems();
            formUI.getStockQuantityLabel().setText("Available Stock: 0");
            formUI.getQuantityField().setText("");
            return;
        }

        String productKey = formUI.getProductPriceMap().keySet().stream()
                .filter(k -> k.equalsIgnoreCase(input))
                .findFirst()
                .orElse(input);

        List<Double> prices = formUI.getProductPriceMap().getOrDefault(productKey, List.of());
        formUI.getBuyingPriceComboBox().removeAllItems();
        prices.forEach(formUI.getBuyingPriceComboBox()::addItem);
        if (prices.isEmpty()) formUI.getBuyingPriceComboBox().addItem(0.0);

        updateStockForSelectedPrice();
        calculateNetProfit();
    }
    // NEW: Update stock when price changes
    private void updateStockForSelectedPrice() {
        String productName = (String) formUI.getProductNameComboBox().getSelectedItem();
        Double selectedPrice = (Double) formUI.getBuyingPriceComboBox().getSelectedItem();

        if (productName == null || productName.trim().isEmpty() || selectedPrice == null) {
            formUI.getStockQuantityLabel().setText("Available Stock: 0");
            formUI.getQuantityField().setText("");
            return;
        }

        int stock = dao.getStockQuantityByPrice(productName.trim(), selectedPrice); // FROM DAO
        formUI.getStockQuantityLabel().setText(
            "Available Stock at Rs." + String.format("%.2f", selectedPrice) + ": " + stock
        );
        formUI.getQuantityField().setText(String.valueOf(stock));
    }

    public void autoFillProductDetails() {
        String input = formUI.getProductNameComboBox().getEditor().getItem().toString().trim();
        if (input.isEmpty()) {
            formUI.getProductNameComboBox().removeAllItems();
            formUI.getProductNameComboBox().addItem("");
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT DISTINCT productName FROM stock WHERE LOWER(productName) LIKE ? AND quantity > 0 ORDER BY productName";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "%" + input.toLowerCase() + "%");
            rs = pstmt.executeQuery();

            formUI.getProductNameComboBox().removeAllItems();
            formUI.getProductNameComboBox().addItem("");
            List<String> matches = new ArrayList<>();
            while (rs.next()) {
                matches.add(rs.getString("productName"));
            }
            matches.forEach(formUI.getProductNameComboBox()::addItem);

            String exact = matches.stream()
                    .filter(m -> m.equalsIgnoreCase(input))
                    .findFirst()
                    .orElse(null);

            if (exact != null) {
                formUI.getProductNameComboBox().setSelectedItem(exact);
                loadPricesForProduct(exact);
                updateBuyingPriceComboBox();
            } else {
                formUI.getProductNameComboBox().setSelectedItem(input);
                formUI.getProductNameComboBox().showPopup();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(form, "Auto-fill error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeResources(rs, pstmt, conn);
        }
    }

    // ========================================================================
    // CRUD: addCustomer, updateCustomer, deleteCustomer
    // ========================================================================
    public void addCustomer(ActionEvent e) {
        Connection conn = null;
        try {
            Customer c = createCustomerFromFields();
            if (!validateCustomer(c)) return;

            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            if (c.getQuantity() > 0) {
                int avail = dao.getStockQuantityByPrice(c.getProductName(), c.getBuyingPrice());
                if (avail < c.getQuantity()) {
                    JOptionPane.showMessageDialog(form, "Not enough stock at ₹" + c.getBuyingPrice() + "!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!updateStockQuantity(conn, c.getProductName(), c.getBuyingPrice(), c.getQuantity())) {
                    conn.rollback();
                    return;
                }
            }

            String sql = "INSERT INTO customer (customerName, productName, quantity, buyingPrice, sellingPrice, totalAmount, finalBill, netProfit, paymentMode, date, discount, status, mobileNo, amount_paid, payment_date, pending_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement p = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setCustomerStatement(p, c);
                p.executeUpdate();
                try (ResultSet rs = p.getGeneratedKeys()) {
                    if (rs.next() && c.getAmountPaid() > 0) {
                        insertPayment(conn, rs.getLong(1), c.getAmountPaid(), c.getDate().toLocalDate(), c.getPaymentMode());
                    }
                }
                conn.commit();
                refreshUI();
                JOptionPane.showMessageDialog(form, "Customer added!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            rollback(conn, ex.getMessage());
        } finally {
            closeConnection(conn);
        }
    }

    public void updateCustomer(ActionEvent e) {
        int selectedRow = formUI.getCustomerTable().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(form, "Please select a customer to update.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Connection conn = null;
        try {
            int modelRow = formUI.getCustomerTable().convertRowIndexToModel(selectedRow);
            Long customerId = Long.parseLong(formUI.getTableModel().getValueAt(modelRow, 0).toString());
            Customer newCustomer = createCustomerFromFields();
            if (!validateCustomer(newCustomer)) return;

            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            String fetchOldSql = "SELECT productName, quantity, buyingPrice, amount_paid, paymentMode FROM customer WHERE id = ?";
            String oldProductName = null; int oldQuantity = 0; double oldBuyingPrice = 0, oldAmountPaid = 0; String oldPaymentMode = null;
            try (PreparedStatement pstmt = conn.prepareStatement(fetchOldSql)) {
                pstmt.setLong(1, customerId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Customer not found: " + customerId);
                    oldProductName = rs.getString("productName");
                    oldQuantity = rs.getInt("quantity");
                    oldBuyingPrice = rs.getDouble("buyingPrice");
                    oldAmountPaid = rs.getDouble("amount_paid");
                    oldPaymentMode = rs.getString("paymentMode");
                }
            }

            // RESTORE OLD STOCK
            if (oldQuantity > 0) {
                if (!updateStockQuantity(conn, oldProductName, oldBuyingPrice, -oldQuantity)) {
                    conn.rollback();
                    JOptionPane.showMessageDialog(form, "Failed to restore old stock", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // DEDUCT NEW STOCK
            int newQty = newCustomer.getQuantity();
            if (newQty > 0) {
                int available = dao.getStockQuantityByPrice(newCustomer.getProductName(), newCustomer.getBuyingPrice());
                if (available < newQty) {
                    int confirm = JOptionPane.showConfirmDialog(form,
                            "Only " + available + " units available at ₹" + newCustomer.getBuyingPrice() + ". Proceed?", "Low Stock",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirm != JOptionPane.YES_OPTION) {
                        conn.rollback();
                        return;
                    }
                }
                if (!updateStockQuantity(conn, newCustomer.getProductName(), newCustomer.getBuyingPrice(), newQty)) {
                    conn.rollback();
                    JOptionPane.showMessageDialog(form, "Failed to deduct new stock", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            String updateSql = "UPDATE customer SET customerName = ?, productName = ?, quantity = ?, buyingPrice = ?, "
                    + "sellingPrice = ?, totalAmount = ?, finalBill = ?, netProfit = ?, paymentMode = ?, "
                    + "date = ?, discount = ?, status = ?, mobileNo = ?, amount_paid = ?, payment_date = ?, "
                    + "pending_amount = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                setCustomerStatement(pstmt, newCustomer);
                pstmt.setLong(17, customerId);
                pstmt.executeUpdate();
            }

            double paymentDiff = newCustomer.getAmountPaid() - oldAmountPaid;
            if (paymentDiff != 0) {
                String mode = newCustomer.getPaymentMode() != null ? newCustomer.getPaymentMode() : oldPaymentMode;
                if (paymentDiff > 0) {
                    insertPayment(conn, customerId, paymentDiff, LocalDate.now(), mode);
                } else {
                    logShopEntry(conn, paymentDiff, "Refund for update ID: " + customerId);
                }
            }

            conn.commit();
            refreshUI();
            JOptionPane.showMessageDialog(form, "Customer updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            rollback(conn, "Database error: " + ex.getMessage());
        } catch (Exception ex) {
            rollback(conn, "Error: " + ex.getMessage());
        } finally {
            closeConnection(conn);
        }
    }

    public void deleteCustomer(ActionEvent e) {
        int selectedRow = formUI.getCustomerTable().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(form, "Please select a customer to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(form, "Are you sure? All items will be returned to stock.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        Connection conn = null;
        try {
            int modelRow = formUI.getCustomerTable().convertRowIndexToModel(selectedRow);
            Long customerId = Long.parseLong(formUI.getTableModel().getValueAt(modelRow, 0).toString());
            String productName = (String) formUI.getTableModel().getValueAt(modelRow, 2);
            int quantity = Integer.parseInt(formUI.getTableModel().getValueAt(modelRow, 3).toString());
            double buyingPrice = Double.parseDouble(formUI.getTableModel().getValueAt(modelRow, 4).toString());
            double amountPaid = Double.parseDouble(formUI.getTableModel().getValueAt(modelRow, 12).toString());

            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            if (quantity > 0) {
                if (!updateStockQuantity(conn, productName, buyingPrice, -quantity)) {
                    conn.rollback();
                    JOptionPane.showMessageDialog(form, "Failed to restore stock", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM payment_history WHERE customer_id = ?")) {
                pstmt.setLong(1, customerId);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM customer WHERE id = ?")) {
                pstmt.setLong(1, customerId);
                pstmt.executeUpdate();
            }
            if (amountPaid > 0) {
                logShopEntry(conn, -amountPaid, "Refund for deleted customer ID: " + customerId);
            }

            conn.commit();
            refreshUI();
            JOptionPane.showMessageDialog(form, "Customer deleted & stock restored!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            rollback(conn, "Database error: " + ex.getMessage());
        } finally {
            closeConnection(conn);
        }
    }

    // ========================================================================
    // PAYMENT & PROFIT HANDLING
    // ========================================================================
    private long insertPayment(Connection conn, long customerId, double amount, LocalDate paymentDate, String paymentMode) throws SQLException {
        String sql = "INSERT INTO payment_history (customer_id, amount_paid, payment_date, payment_mode) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, customerId);
            pstmt.setDouble(2, amount);
            pstmt.setDate(3, java.sql.Date.valueOf(paymentDate));
            pstmt.setString(4, paymentMode);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long paymentId = rs.getLong(1);
                    String shopSql = "INSERT INTO shop_amount (shopAmount, amountDate, shop_description) VALUES (?, ?, ?)";
                    try (PreparedStatement shopPstmt = conn.prepareStatement(shopSql)) {
                        shopPstmt.setDouble(1, amount);
                        shopPstmt.setDate(2, java.sql.Date.valueOf(paymentDate));
                        shopPstmt.setString(3, "Customer payment for ID: " + customerId + ", Payment ID: " + paymentId);
                        shopPstmt.executeUpdate();
                    }
                    return paymentId;
                }
            }
        }
        throw new SQLException("Failed to insert payment");
    }

    private void logShopEntry(Connection conn, double amount, String desc) throws SQLException {
        String sql = "INSERT INTO shop_amount (shopAmount, amountDate, shop_description) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
            pstmt.setString(3, desc);
            pstmt.executeUpdate();
        }
    }

    public void payPendingAmount(ActionEvent e) {
        int selectedRow = formUI.getCustomerTable().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(form, "Please select a customer.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = formUI.getCustomerTable().convertRowIndexToModel(selectedRow);
        String status = (String) formUI.getTableModel().getValueAt(modelRow, 10);
        if (!"Pending".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(form, "Not pending.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long id = Long.parseLong(formUI.getTableModel().getValueAt(modelRow, 0).toString());
        double pendingAmount = Double.parseDouble(formUI.getTableModel().getValueAt(modelRow, 14).toString());
        double finalBill = Double.parseDouble(formUI.getTableModel().getValueAt(modelRow, 7).toString());
        double currentAmountPaid = Double.parseDouble(formUI.getTableModel().getValueAt(modelRow, 12).toString());
        double buyingPrice = Double.parseDouble(formUI.getTableModel().getValueAt(modelRow, 4).toString());
        int quantity = Integer.parseInt(formUI.getTableModel().getValueAt(modelRow, 3).toString());

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        JTextField pendingField = new JTextField(10); pendingField.setText(String.format("%.2f", pendingAmount)); pendingField.setEditable(false);
        JTextField amountField = new JTextField(10);
        JDateChooser paymentDateChooser = new JDateChooser(); paymentDateChooser.setDateFormatString("yyyy-MM-dd");
        JComboBox<String> paymentModeComboBox = new JComboBox<>(new String[] { "Online", "Cash" });

        panel.add(new JLabel("Pending Amount:")); panel.add(pendingField);
        panel.add(new JLabel("Amount to Pay:")); panel.add(amountField);
        panel.add(new JLabel("Payment Date:")); panel.add(paymentDateChooser);
        panel.add(new JLabel("Payment Mode:")); panel.add(paymentModeComboBox);

        int result = JOptionPane.showConfirmDialog(form, panel, "Pay Pending Amount", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                double amountToPay = Double.parseDouble(amountField.getText().trim());
                if (amountToPay <= 0 || amountToPay > pendingAmount || currentAmountPaid + amountToPay > finalBill) {
                    JOptionPane.showMessageDialog(form, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Date paymentDate = paymentDateChooser.getDate();
                if (paymentDate == null) {
                    JOptionPane.showMessageDialog(form, "Payment date required.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                LocalDate localDate = paymentDate.toInstant().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
                Connection conn = null;
                try {
                    conn = DBUtil.getConnection();
                    conn.setAutoCommit(false);
                    insertPayment(conn, id, amountToPay, localDate, (String) paymentModeComboBox.getSelectedItem());
                    double newAmountPaid = currentAmountPaid + amountToPay;
                    double newPendingAmount = pendingAmount - amountToPay;
                    String newStatus = newPendingAmount <= 0 ? "Paid" : "Pending";
                    double newNetProfit = newAmountPaid - (buyingPrice * quantity);
                    String sql = "UPDATE customer SET amount_paid = ?, payment_date = ?, pending_amount = ?, status = ?, netProfit = ? WHERE id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setDouble(1, newAmountPaid);
                        pstmt.setDate(2, java.sql.Date.valueOf(localDate));
                        pstmt.setDouble(3, newPendingAmount);
                        pstmt.setString(4, newStatus);
                        pstmt.setDouble(5, newNetProfit);
                        pstmt.setLong(6, id);
                        pstmt.executeUpdate();
                    }
                    conn.commit();
                    refreshUI();
                    JOptionPane.showMessageDialog(form, "Payment recorded!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {
                    rollback(conn, "Database error: " + ex.getMessage());
                } finally {
                    closeConnection(conn);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(form, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ========================================================================
    // UI & DATA HANDLING
    // ========================================================================
    public Customer createCustomerFromFields() throws Exception {
        String customerName = formUI.getCustomerNameField().getText().trim().toLowerCase();
        String mobileNo = formUI.getMobileNoField().getText().trim().toLowerCase();
        String quantityText = formUI.getQuantityField().getText().trim();
        String productName = (String) formUI.getProductNameComboBox().getSelectedItem();
        if (productName == null || productName.trim().isEmpty()) throw new Exception("Product Name is required.");
        Double buyingPrice = (Double) formUI.getBuyingPriceComboBox().getSelectedItem();
        String sellingPriceText = formUI.getSellingPriceField().getText().trim();
        String paymentMode = (String) formUI.getPaymentModeComboBox().getSelectedItem();
        Date date = formUI.getDateChooser().getDate();
        String discountPercentageText = formUI.getDiscountPercentageField().getText().trim();
        String status = (String) formUI.getStatusComboBox().getSelectedItem();
        String amountPaidText = formUI.getAmountPaidField().getText().trim();

        if (buyingPrice == null) throw new Exception("Buying Price is required.");
        if (paymentMode == null || paymentMode.isEmpty()) throw new Exception("Payment Mode is required.");
        if (status == null || status.isEmpty()) throw new Exception("Status is required.");
        if (date == null) throw new Exception("Date is required.");

        double quantity = parseDoubleOrThrow(quantityText, "Invalid quantity.");
        if (quantity <= 0) throw new Exception("Quantity must be positive.");

        double sellingPrice = parseDoubleOrThrow(sellingPriceText, "Invalid selling price.");
        if (sellingPrice < 0) throw new Exception("Selling price cannot be negative.");

        double discountPercentage = discountPercentageText.isEmpty() ? 0.0 : parseDoubleOrThrow(discountPercentageText, "Invalid discount.");
        if (discountPercentage < 0 || discountPercentage > 100) throw new Exception("Discount 0-100.");

        if (!mobileNo.isEmpty() && (mobileNo.length() != 10 || !Pattern.matches("\\d{10}", mobileNo))) {
            throw new Exception("Mobile must be 10 digits.");
        }

        double amountPaid = amountPaidText.isEmpty() ? 0.0 : parseDoubleOrThrow(amountPaidText, "Invalid amount paid.");
        if (amountPaid < 0) throw new Exception("Amount Paid cannot be negative.");

        double totalAmount = sellingPrice * quantity;
        double discountAmount = (sellingPrice * discountPercentage / 100.0) * quantity;
        double finalBill = totalAmount - discountAmount;
        double netProfit = amountPaid - (buyingPrice * quantity);
        double pendingAmount = finalBill - amountPaid;

        if (amountPaid > finalBill) throw new Exception("Amount paid cannot exceed final bill.");
        int intQuantity = (int) quantity;
        if (quantity != intQuantity) throw new Exception("Quantity must be whole number.");

        java.sql.Date sqlDate = new java.sql.Date(date.getTime());
        return new Customer(0L, customerName.isEmpty() ? null : customerName, productName.toLowerCase(), intQuantity,
                buyingPrice, sellingPrice, totalAmount, netProfit, paymentMode, sqlDate, discountAmount, status,
                mobileNo.isEmpty() ? null : mobileNo, finalBill, amountPaid, null, pendingAmount);
    }

    private double parseDoubleOrThrow(String text, String message) throws Exception {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new Exception(message);
        }
    }

    private boolean validateCustomer(Customer customer) {
        if (customer.getAmountPaid() > customer.getFinalBill()) {
            JOptionPane.showMessageDialog(form, "Amount paid > final bill.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (customer.getPendingAmount() < 0) {
            JOptionPane.showMessageDialog(form, "Pending amount negative.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void setCustomerStatement(PreparedStatement pstmt, Customer customer) throws SQLException {
        pstmt.setString(1, customer.getCustomerName());
        pstmt.setString(2, customer.getProductName());
        pstmt.setInt(3, customer.getQuantity());
        pstmt.setDouble(4, customer.getBuyingPrice());
        pstmt.setDouble(5, customer.getSellingPrice());
        pstmt.setDouble(6, customer.getTotalAmount());
        pstmt.setDouble(7, customer.getFinalBill());
        pstmt.setDouble(8, customer.getNetProfit());
        pstmt.setString(9, customer.getPaymentMode());
        pstmt.setDate(10, customer.getDate());
        pstmt.setDouble(11, customer.getDiscount());
        pstmt.setString(12, customer.getStatus());
        pstmt.setString(13, customer.getMobileNo());
        pstmt.setDouble(14, customer.getAmountPaid());
        pstmt.setDate(15, customer.getAmountPaid() > 0 ? customer.getDate() : null);
        pstmt.setDouble(16, customer.getPendingAmount());
    }

    public void calculateNetProfit() {
        try {
            double quantity = formUI.getQuantityField().getText().trim().isEmpty() ? 0.0
                    : Double.parseDouble(formUI.getQuantityField().getText().trim());
            Double buyingPrice = (Double) formUI.getBuyingPriceComboBox().getSelectedItem();
            if (buyingPrice == null) buyingPrice = 0.0;
            double sellingPrice = formUI.getSellingPriceField().getText().trim().isEmpty() ? 0.0
                    : Double.parseDouble(formUI.getSellingPriceField().getText().trim());
            double discountPercentage = formUI.getDiscountPercentageField().getText().trim().isEmpty() ? 0.0
                    : Double.parseDouble(formUI.getDiscountPercentageField().getText().trim());
            double amountPaid = formUI.getAmountPaidField().getText().trim().isEmpty() ? 0.0
                    : Double.parseDouble(formUI.getAmountPaidField().getText().trim());

            double totalAmount = sellingPrice * quantity;
            double discountAmount = (sellingPrice * discountPercentage / 100.0) * quantity;
            double finalBill = totalAmount - discountAmount;
            double netProfit = amountPaid - (buyingPrice * quantity);

            formUI.getTotalAmountField().setText(String.format("%.2f", totalAmount));
            formUI.getFinalBillField().setText(String.format("%.2f", finalBill));
            formUI.getNetProfitField().setText(String.format("%.2f", netProfit));
        } catch (NumberFormatException ex) {
            formUI.getTotalAmountField().setText("");
            formUI.getFinalBillField().setText("");
            formUI.getNetProfitField().setText("");
        }
    }

    public void populateFields(int row) {
        try {
            int modelRow = formUI.getCustomerTable().convertRowIndexToModel(row);
            String productName = (String) formUI.getTableModel().getValueAt(modelRow, 2);
            Double storedBuyingPrice = Double.valueOf(formUI.getTableModel().getValueAt(modelRow, 4).toString().replaceAll(",", ""));

            formUI.getCustomerNameField().setText((String) formUI.getTableModel().getValueAt(modelRow, 1));
            formUI.getProductNameComboBox().setSelectedItem(productName);
            loadPricesForProduct(productName);

            List<Double> currentPrices = new ArrayList<>(formUI.getProductPriceMap().getOrDefault(productName, List.of()));
            if (!currentPrices.contains(storedBuyingPrice)) {
                currentPrices.add(storedBuyingPrice);
                formUI.getProductPriceMap().put(productName, currentPrices);
            }
            formUI.getBuyingPriceComboBox().removeAllItems();
            currentPrices.stream().sorted().forEach(formUI.getBuyingPriceComboBox()::addItem);
            formUI.getBuyingPriceComboBox().setSelectedItem(storedBuyingPrice);

            formUI.getQuantityField().setText(formUI.getTableModel().getValueAt(modelRow, 3).toString());
            formUI.getSellingPriceField().setText(formUI.getTableModel().getValueAt(modelRow, 5).toString());
            formUI.getTotalAmountField().setText(formUI.getTableModel().getValueAt(modelRow, 6).toString());
            formUI.getFinalBillField().setText(formUI.getTableModel().getValueAt(modelRow, 7).toString());
            formUI.getNetProfitField().setText(formUI.getTableModel().getValueAt(modelRow, 8).toString());
            formUI.getPaymentModeComboBox().setSelectedItem(formUI.getTableModel().getValueAt(modelRow, 9));
            formUI.getStatusComboBox().setSelectedItem(formUI.getTableModel().getValueAt(modelRow, 10));
            formUI.getMobileNoField().setText((String) formUI.getTableModel().getValueAt(modelRow, 11));
            formUI.getAmountPaidField().setText((String) formUI.getTableModel().getValueAt(modelRow, 12));
            LocalDate paymentDate = (LocalDate) formUI.getTableModel().getValueAt(modelRow, 13);
            formUI.getDateChooser().setDate(paymentDate != null ? java.sql.Date.valueOf(paymentDate) : null);

            double total = Double.parseDouble(formUI.getTableModel().getValueAt(modelRow, 6).toString().replaceAll(",", ""));
            double finalBill = Double.parseDouble(formUI.getTableModel().getValueAt(modelRow, 7).toString().replaceAll(",", ""));
            double discountPct = total > 0 ? ((total - finalBill) / total) * 100.0 : 0.0;
            formUI.getDiscountPercentageField().setText(String.format("%.2f", discountPct));

            updateStockForSelectedPrice(); // Update stock on edit
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(form, "Error populating fields: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void clearFields() {
        formUI.getCustomerNameField().setText("");
        formUI.getProductNameComboBox().setSelectedIndex(0);
        formUI.getBuyingPriceComboBox().removeAllItems();
        formUI.getQuantityField().setText("");
        formUI.getSellingPriceField().setText("");
        formUI.getNetProfitField().setText("");
        formUI.getPaymentModeComboBox().setSelectedIndex(-1);
        formUI.getTotalAmountField().setText("");
        formUI.getFinalBillField().setText("");
        formUI.getDiscountPercentageField().setText("");
        formUI.getStatusComboBox().setSelectedIndex(-1);
        formUI.getMobileNoField().setText("");
        formUI.getAmountPaidField().setText("");
//        formUI.getDateChooser().setDate(java.sql.Date.valueOf(LocalDate.now()));
        formUI.getStockQuantityLabel().setText("Available Stock: 0");
    }

    private void refreshUI() {
        loadTableData();
        loadProductNames();
        clearFields();
    }

    // ========================================================================
    // 7. TABLE DATA & FILTERING
    // ========================================================================
    public void loadTableData() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT * FROM customer WHERE DATE(date) = ?";
            pstmt = conn.prepareStatement(sql);
            Date tableDate = formUI.getTableDateChooser().getDate();
            pstmt.setDate(1, tableDate != null ? new java.sql.Date(tableDate.getTime())
                    : java.sql.Date.valueOf(LocalDate.now()));
            rs = pstmt.executeQuery();
            DefaultTableModel model = formUI.getTableModel();
            model.setRowCount(0);
            List<Object[]> rows = new ArrayList<>();
            while (rs.next()) {
                double buyingPrice = rs.getDouble("buyingPrice");
                int quantity = rs.getInt("quantity");
                double amountPaid = rs.getDouble("amount_paid");
                double netProfit = amountPaid - (buyingPrice * quantity);
                rows.add(new Object[] {
                    rs.getLong("id"), rs.getString("customerName"), rs.getString("productName"),
                    quantity, String.format("%.2f", buyingPrice),
                    String.format("%.2f", rs.getDouble("sellingPrice")),
                    String.format("%.2f", rs.getDouble("totalAmount")),
                    String.format("%.2f", rs.getDouble("finalBill")), String.format("%.2f", netProfit),
                    rs.getString("paymentMode"), rs.getString("status"), rs.getString("mobileNo"),
                    String.format("%.2f", amountPaid), rs.getObject("payment_date", LocalDate.class),
                    String.format("%.2f", rs.getDouble("pending_amount"))
                });
            }
            rows.forEach(model::addRow);
            formUI.updateSummaryLabels();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(form, "Error loading table data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeResources(rs, pstmt, conn);
        }
    }

    public void viewAllData(ActionEvent e) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT * FROM customer ORDER BY date DESC";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            DefaultTableModel model = formUI.getTableModel();
            model.setRowCount(0);
            List<Object[]> rows = new ArrayList<>();
            while (rs.next()) {
                double buyingPrice = rs.getDouble("buyingPrice");
                int quantity = rs.getInt("quantity");
                double amountPaid = rs.getDouble("amount_paid");
                double netProfit = amountPaid - (buyingPrice * quantity);
                rows.add(new Object[] {
                    rs.getLong("id"), rs.getString("customerName"), rs.getString("productName"),
                    quantity, String.format("%.2f", buyingPrice),
                    String.format("%.2f", rs.getDouble("sellingPrice")),
                    String.format("%.2f", rs.getDouble("totalAmount")),
                    String.format("%.2f", rs.getDouble("finalBill")), String.format("%.2f", netProfit),
                    rs.getString("paymentMode"), rs.getString("status"), rs.getString("mobileNo"),
                    String.format("%.2f", amountPaid), rs.getObject("payment_date", LocalDate.class),
                    String.format("%.2f", rs.getDouble("pending_amount"))
                });
            }
            rows.forEach(model::addRow);
            formUI.updateSummaryLabels();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(form, "Error loading all data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeResources(rs, pstmt, conn);
        }
    }

    public void filterTable() {
        String searchText = formUI.getSearchField().getText().trim().toLowerCase();
        Date startDate = formUI.getStartDateChooser().getDate();
        Date endDate = formUI.getEndDateChooser().getDate();

        RowFilter<DefaultTableModel, Object> rowFilter = new RowFilter<DefaultTableModel, Object>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                String customerName = ((String) entry.getValue(1) != null ? (String) entry.getValue(1) : "").toLowerCase();
                String mobileNo = ((String) entry.getValue(11) != null ? (String) entry.getValue(11) : "").toLowerCase();
                boolean matchesSearch = searchText.isEmpty() || customerName.contains(searchText) || mobileNo.contains(searchText);
                if (!matchesSearch) return false;
                if (startDate == null || endDate == null) return true;
                LocalDate rowDate = (LocalDate) entry.getValue(13);
                if (rowDate == null) return false;
                LocalDate start = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate end = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                return !rowDate.isBefore(start) && !rowDate.isAfter(end);
            }
        };
        formUI.getSorter().setRowFilter(rowFilter);
    }

    public void shiftTableDate(boolean forward) {
        Date currentDate = formUI.getTableDateChooser().getDate();
        if (currentDate == null) {
            currentDate = java.sql.Date.valueOf(LocalDate.now());
        }
        LocalDate localDate = currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        localDate = forward ? localDate.plusDays(1) : localDate.minusDays(1);
        formUI.getTableDateChooser().setDate(java.sql.Date.valueOf(localDate));
        loadTableData();
    }

    public void refreshTableDate() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        formUI.getTableDateChooser().setDate(java.sql.Date.valueOf(today));
        loadTableData();
    }

    // ========================================================================
    // 8. REPORTS & PDF
    // ========================================================================
    public void viewPaymentHistory(ActionEvent e) {
        int selectedRow = formUI.getCustomerTable().getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(form, "Please select a customer to view payment history.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = formUI.getCustomerTable().convertRowIndexToModel(selectedRow);
        Long customerId = Long.parseLong(formUI.getTableModel().getValueAt(modelRow, 0).toString());
        String customerName = (String) formUI.getTableModel().getValueAt(modelRow, 1);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT amount_paid, payment_date, payment_mode FROM payment_history WHERE customer_id = ? ORDER BY payment_date DESC";
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, customerId);
            rs = pstmt.executeQuery();
            String[] columnNames = { "Amount Paid", "Payment Date", "Payment Mode" };
            DefaultTableModel paymentTableModel = new DefaultTableModel(columnNames, 0);
            List<Object[]> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new Object[] {
                    String.format("%.2f", rs.getDouble("amount_paid")),
                    rs.getObject("payment_date", LocalDate.class),
                    rs.getString("payment_mode")
                });
            }
            rows.forEach(paymentTableModel::addRow);
            JTable paymentTable = new JTable(paymentTableModel);
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(form),
                    "Payment History for " + customerName, true);
            dialog.setLayout(new BorderLayout());
            dialog.setSize(500, 300);
            dialog.setLocationRelativeTo(form);
            dialog.add(new JScrollPane(paymentTable), BorderLayout.CENTER);
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(eve -> dialog.dispose());
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.add(closeButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            dialog.setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(form, "Error loading payment history: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeResources(rs, pstmt, conn);
        }
    }

    public void showMonthlyReport(ActionEvent e) {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JLabel yearLabel = new JLabel("Select Year:");
        JComboBox<Integer> yearComboBox = new JComboBox<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT DISTINCT YEAR(date) AS year FROM customer ORDER BY year DESC";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                yearComboBox.addItem(rs.getInt("year"));
            }
            if (yearComboBox.getItemCount() == 0) {
                yearComboBox.addItem(LocalDate.now().getYear());
            }
            yearComboBox.setSelectedIndex(0);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(form, "Error loading years: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            closeResources(rs, pstmt, conn);
        }
        panel.add(yearLabel); panel.add(yearComboBox);
        int result = JOptionPane.showConfirmDialog(form, panel, "Select Year for Monthly Report",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            Integer selectedYear = (Integer) yearComboBox.getSelectedItem();
            if (selectedYear == null) {
                JOptionPane.showMessageDialog(form, "Please select a year.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                conn = DBUtil.getConnection();
                String sql = "SELECT MONTH(date) AS month, SUM(finalBill) AS total_sales, SUM(netProfit) AS total_profit FROM customer WHERE YEAR(date) = ? GROUP BY MONTH(date) ORDER BY month";
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, selectedYear);
                rs = pstmt.executeQuery();
                String[] columnNames = { "Month", "Total Sales", "Total Profit" };
                DefaultTableModel reportTableModel = new DefaultTableModel(columnNames, 0);
                DecimalFormat df = new DecimalFormat("#,##0.00");
                String[] monthNames = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };
                double[] sales = new double[12]; double[] profits = new double[12];
                while (rs.next()) {
                    int month = rs.getInt("month") - 1;
                    sales[month] = rs.getDouble("total_sales");
                    profits[month] = rs.getDouble("total_profit");
                }
                List<Object[]> reportRows = new ArrayList<>();
                for (int i = 0; i < 12; i++) {
                    if (sales[i] > 0 || profits[i] != 0) {
                        reportRows.add(new Object[] { monthNames[i], df.format(sales[i]), df.format(profits[i]) });
                    }
                }
                reportRows.forEach(reportTableModel::addRow);
                JTable reportTable = new JTable(reportTableModel);
                JDialog reportDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(form),
                        "Monthly Sales & Profit Report - " + selectedYear, true);
                reportDialog.setLayout(new BorderLayout());
                reportDialog.setSize(450, 400);
                reportDialog.setLocationRelativeTo(form);
                reportDialog.add(new JScrollPane(reportTable), BorderLayout.CENTER);
                JButton closeButton = new JButton("Close");
                closeButton.addActionListener(ev -> reportDialog.dispose());
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                buttonPanel.add(closeButton);
                reportDialog.add(buttonPanel, BorderLayout.SOUTH);
                reportDialog.setVisible(true);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(form, "Error generating report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                closeResources(rs, pstmt, conn);
            }
        }
    }

    public void showWeeklyReport(ActionEvent e) {
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        JLabel yearLabel = new JLabel("Select Year:");
        JComboBox<Integer> yearComboBox = new JComboBox<>();
        JLabel monthLabel = new JLabel("Select Month:");
        JComboBox<String> monthComboBox = new JComboBox<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "SELECT DISTINCT YEAR(date) AS year FROM customer ORDER BY year DESC";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                yearComboBox.addItem(rs.getInt("year"));
            }
            if (yearComboBox.getItemCount() == 0) {
                yearComboBox.addItem(LocalDate.now().getYear());
            }
            yearComboBox.setSelectedIndex(0);
            String[] months = { "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER" };
            for (String month : months) monthComboBox.addItem(month);
            monthComboBox.setSelectedItem("JULY");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(form, "Error loading years: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            closeResources(rs, pstmt, conn);
        }
        panel.add(yearLabel); panel.add(yearComboBox);
        panel.add(monthLabel); panel.add(monthComboBox);
        int result = JOptionPane.showConfirmDialog(form, panel, "Select Year and Month for Weekly Report",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            Integer selectedYear = (Integer) yearComboBox.getSelectedItem();
            String selectedMonth = (String) monthComboBox.getSelectedItem();
            if (selectedYear == null || selectedMonth == null) {
                JOptionPane.showMessageDialog(form, "Please select a year and month.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                conn = DBUtil.getConnection();
                int monthNumber = Arrays.asList("JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY",
                        "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER").indexOf(selectedMonth.toUpperCase()) + 1;
                String sql = "SELECT DAY(date) AS day, SUM(finalBill) AS total_sales, SUM(netProfit) AS total_profit "
                        + "FROM customer WHERE YEAR(date) = ? AND MONTH(date) = ? GROUP BY DAY(date) ORDER BY day";
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, selectedYear);
                pstmt.setInt(2, monthNumber);
                rs = pstmt.executeQuery();
                DecimalFormat df = new DecimalFormat("#,##0.00");
                double[] sales = new double[32]; double[] profits = new double[32];
                while (rs.next()) {
                    int day = rs.getInt("day");
                    sales[day] = rs.getDouble("total_sales");
                    profits[day] = rs.getDouble("total_profit");
                }
                int daysInMonth = YearMonth.of(selectedYear, monthNumber).lengthOfMonth();
                double[] weeklySales = new double[5]; double[] weeklyProfits = new double[5];
                for (int day = 1; day <= daysInMonth; day++) {
                    if (sales[day] > 0 || profits[day] > 0) {
                        int rangeIndex = getRangeIndexForDay(day, daysInMonth);
                        weeklySales[rangeIndex] += sales[day];
                        weeklyProfits[rangeIndex] += profits[day];
                    }
                }
                List<Object[]> reportRows = new ArrayList<>();
                String[] rangeLabels = {
                    selectedMonth.toUpperCase() + ": 1-7",
                    selectedMonth.toUpperCase() + ": 8-14",
                    selectedMonth.toUpperCase() + ": 15-21",
                    selectedMonth.toUpperCase() + ": 22-27",
                    selectedMonth.toUpperCase() + ": 28-" + (daysInMonth >= 30 ? "30/31" : "28")
                };
                for (int i = 0; i < 5; i++) {
                    if (weeklySales[i] > 0 || weeklyProfits[i] > 0) {
                        reportRows.add(new Object[] { rangeLabels[i], df.format(weeklySales[i]), df.format(weeklyProfits[i]) });
                    }
                }
                String[] columnNames = { "Date Range", "Total Sales", "Total Profit" };
                DefaultTableModel reportTableModel = new DefaultTableModel(columnNames, 0);
                reportRows.forEach(reportTableModel::addRow);
                JTable reportTable = new JTable(reportTableModel);
                JDialog reportDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(form),
                        "Weekly Sales & Profit Report - " + selectedYear + " " + selectedMonth, true);
                reportDialog.setLayout(new BorderLayout());
                reportDialog.setSize(450, 400);
                reportDialog.setLocationRelativeTo(form);
                reportDialog.add(new JScrollPane(reportTable), BorderLayout.CENTER);
                JButton closeButton = new JButton("Close");
                closeButton.addActionListener(ev -> reportDialog.dispose());
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                buttonPanel.add(closeButton);
                reportDialog.add(buttonPanel, BorderLayout.SOUTH);
                reportDialog.setVisible(true);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(form, "Error generating weekly report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                closeResources(rs, pstmt, conn);
            }
        }
    }

    private int getRangeIndexForDay(int day, int daysInMonth) {
        if (day <= 7) return 0;
        if (day <= 14) return 1;
        if (day <= 21) return 2;
        if (day <= 27) return 3;
        return 4;
    }

    public void generatePDF(ActionEvent e) {
        formPDF.generatePDF(e);
    }

    public void generateIndividualBillPDF(ActionEvent e) {
        formPDF.generateIndividualBillPDF(e);
    }

    // ========================================================================
    // STOCK UPDATE (EXACTLY LIKE GymWholesalerDAO)
    // ========================================================================
    private boolean updateStockQuantity(Connection conn, String productName, double price, double qtyChange) throws SQLException {
        boolean deduct = qtyChange > 0;
        double qty = Math.abs(qtyChange);
        String name = productName.trim();

        if (deduct) {
            String sel = "SELECT id, quantity FROM stock WHERE LOWER(productName) = LOWER(?) AND perPieceRate = ? AND quantity > 0 ORDER BY id FOR UPDATE";
            List<long[]> updates = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sel)) {
                ps.setString(1, name);
                ps.setDouble(2, price);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next() && qty > 0) {
                        long id = rs.getLong("id");
                        double cur = rs.getDouble("quantity");
                        double take = Math.min(cur, qty);
                        updates.add(new long[]{id, (long) take});
                        qty -= take;
                    }
                }
            }
            if (qty > 0) return false;

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
            String ins = "INSERT INTO stock (productId, productName, perPieceRate, quantity, totalAmount, purchaseDate) VALUES (?, ?, ?, 1, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(ins)) {
                java.sql.Date today = java.sql.Date.valueOf(LocalDate.now());
                Long productId = getProductId(conn, name);
                if (productId == null) productId = 1L;

                for (int i = 0; i < (int) qty; i++) {
                    ps.setLong(1, productId);
                    ps.setString(2, name);
                    ps.setDouble(3, price);
                    ps.setDouble(4, price);
                    ps.setDate(5, today);
                    ps.addBatch();
                }
                int[] res = ps.executeBatch();
                return Arrays.stream(res).allMatch(r -> r >= 0);
            }
        }
        return true;
    }
}