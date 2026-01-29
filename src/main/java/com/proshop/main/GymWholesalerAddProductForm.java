package com.proshop.main;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;

import com.proshop.connection.DBUtil;
import com.proshop.model.GymWholesaler;
import com.toedter.calendar.JDateChooser;

public class GymWholesalerAddProductForm extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(GymWholesalerAddProductForm.class.getName());

    private final GymWholesaler wholesaler;
    private final CardLayout cardLayout;
    private final JPanel mainContentPanel;
    private final GymWholesalerDAO dao;

    private final DefaultTableModel tableModel;
    private final JTable productTable;
    private final JComboBox<String> productComboBox;
    private final JComboBox<Double> buyingPriceComboBox;
    private final JTextField quantityField;
    private final JTextField stockQuantityField;
    private final JTextField sellingPriceField;
    private final JTextField totalAmountField;
    private final JTextField descriptionField;
    private final JDateChooser dateChooser;

    private Map<String, List<Double>> productRates;
    private boolean updating = false;

    public GymWholesalerAddProductForm(GymWholesaler wholesaler, CardLayout cardLayout, JPanel mainContentPanel, GymWholesalerDAO dao) {
        this.wholesaler = wholesaler;
        this.cardLayout = cardLayout;
        this.mainContentPanel = mainContentPanel;
        this.dao = dao;

        try {
            this.productRates = dao.fetchProductsAndRates();
        } catch (Exception e) {
            this.productRates = new HashMap<>();
            LOGGER.severe("Failed to load products: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to load products.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);

        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        mainPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel formPanel = UIUtils.createStyledFormPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        productComboBox = new JComboBox<>();
        productComboBox.setEditable(true);
        productComboBox.setFont(UIUtils.TEXT_FONT.deriveFont(14f));
        productComboBox.setPreferredSize(new Dimension(200, 30));
        productComboBox.setForeground(UIUtils.TEXT_COLOR);
        productComboBox.setBackground(UIUtils.BACKGROUND_COLOR);

        buyingPriceComboBox = new JComboBox<>();
        buyingPriceComboBox.setFont(UIUtils.TEXT_FONT.deriveFont(14f));
        buyingPriceComboBox.setPreferredSize(new Dimension(200, 30));
        buyingPriceComboBox.setForeground(UIUtils.TEXT_COLOR);
        buyingPriceComboBox.setBackground(UIUtils.BACKGROUND_COLOR);

        quantityField = UIUtils.createStyledTextField(15);
        quantityField.setFont(UIUtils.TEXT_FONT.deriveFont(14f));
        quantityField.setPreferredSize(new Dimension(200, 30));

        stockQuantityField = UIUtils.createStyledTextField(10);
        stockQuantityField.setFont(UIUtils.TEXT_FONT.deriveFont(14f));
        stockQuantityField.setPreferredSize(new Dimension(100, 30));
        stockQuantityField.setEditable(false);
        stockQuantityField.setText("0");

        sellingPriceField = UIUtils.createStyledTextField(15);
        sellingPriceField.setFont(UIUtils.TEXT_FONT.deriveFont(14f));
        sellingPriceField.setPreferredSize(new Dimension(200, 30));

        totalAmountField = UIUtils.createStyledTextField(15);
        totalAmountField.setFont(UIUtils.TEXT_FONT.deriveFont(14f));
        sellingPriceField.setPreferredSize(new Dimension(200, 30));
        totalAmountField.setEditable(false);

        descriptionField = UIUtils.createStyledTextField(20);
        descriptionField.setFont(UIUtils.TEXT_FONT.deriveFont(14f));
        descriptionField.setPreferredSize(new Dimension(200, 30));

        dateChooser = UIUtils.createStyledDateChooser();
        dateChooser.setFont(UIUtils.TEXT_FONT.deriveFont(14f));
        dateChooser.setPreferredSize(new Dimension(200, 30));
        dateChooser.setDate(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));

        // AUTO-SUGGEST
        JTextField editor = (JTextField) productComboBox.getEditor().getEditorComponent();
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { scheduleUpdate(); }
            @Override public void removeUpdate(DocumentEvent e) { scheduleUpdate(); }
            @Override public void changedUpdate(DocumentEvent e) { scheduleUpdate(); }

            private void scheduleUpdate() {
                if (updating) return;
                SwingUtilities.invokeLater(() -> {
                    if (!updating) autoFillProductDetails();
                });
            }
        });

        // Update prices and stock when product selected
        productComboBox.addActionListener(e -> {
            if (updating) return;
            Object selected = productComboBox.getSelectedItem();
            String text = editor.getText();
            if (selected != null && selected.toString().equals(text) && !text.trim().isEmpty()) {
                updateBuyingPriceAndStock();
            }
        });

        // Total amount updater
        KeyAdapter totalUpdater = new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { updateTotalAmount(); }
        };
        quantityField.addKeyListener(totalUpdater);
        sellingPriceField.addKeyListener(totalUpdater);
        buyingPriceComboBox.addActionListener(e -> {
            updateStockForSelectedPrice();
            updateTotalAmount();
        });

        loadProductNames();

        JLabel[] labels = {
            new JLabel("Product Name:"), new JLabel("Buying Price:"), new JLabel("Quantity (Stock):"),
            new JLabel("Selling Price:"), new JLabel("Total Amount:"), new JLabel("Description:"),
            new JLabel("Date of Purchase:")
        };
        for (JLabel lbl : labels) lbl.setFont(UIUtils.LABEL_FONT.deriveFont(16f));

        int row = 0;
        UIUtils.addFormField(formPanel, gbc, "Product Name:", productComboBox, row++, 0);
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        formPanel.add(labels[2], gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        formPanel.add(quantityField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        formPanel.add(stockQuantityField, gbc);
        row++;
        UIUtils.addFormField(formPanel, gbc, "Buying Price:", buyingPriceComboBox, row++, 0);
        UIUtils.addFormField(formPanel, gbc, "Selling Price:", sellingPriceField, row++, 0);
        UIUtils.addFormField(formPanel, gbc, "Total Amount:", totalAmountField, row++, 0);
        UIUtils.addFormField(formPanel, gbc, "Description:", descriptionField, row++, 0);
        UIUtils.addFormField(formPanel, gbc, "Date of Purchase:", dateChooser, row++, 0);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        JButton addBtn = UIUtils.createStyledButton("Add");
        JButton updateBtn = UIUtils.createStyledButton("Update");
        JButton clearBtn = UIUtils.createStyledButton("Clear");
        btnPanel.add(addBtn); btnPanel.add(updateBtn); btnPanel.add(clearBtn);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 3;
        formPanel.add(btnPanel, gbc);

        tableModel = new DefaultTableModel(new String[]{"Product Name", "Quantity", "Buying Price", "Selling Price", "Total Amount", "Description"}, 0);
        productTable = new JTable(tableModel);
        productTable.setRowHeight(30);
        productTable.setBackground(UIUtils.BACKGROUND_COLOR);
        productTable.setForeground(UIUtils.TEXT_COLOR);

        productTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            private final Color LIGHT = new Color(50, 50, 50);
            private final Color DARK = new Color(33, 33, 33);
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                c.setBackground(col % 2 == 0 ? LIGHT : DARK);
                c.setForeground(UIUtils.TEXT_COLOR);
                setHorizontalAlignment(CENTER);
                return c;
            }
        });

        JTableHeader header = productTable.getTableHeader();
        header.setFont(UIUtils.TABLE_HEADER_FONT.deriveFont(16f));
        header.setBackground(new Color(66, 66, 66));
        header.setForeground(UIUtils.TEXT_COLOR);

        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && productTable.getSelectedRow() >= 0) {
                populateFieldsFromTable();
            }
        });

        JScrollPane scroll = new JScrollPane(productTable);
        scroll.setPreferredSize(new Dimension(600, 400));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150)));

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(UIUtils.BACKGROUND_COLOR);
        tablePanel.add(scroll, BorderLayout.CENTER);

        mainPanel.add(formPanel);
        mainPanel.add(tablePanel);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        JButton saveBtn = UIUtils.createStyledButton("Save Products");
        JButton backBtn = UIUtils.createStyledButton("Back");
        bottomPanel.add(saveBtn);
        bottomPanel.add(backBtn);

        add(mainPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> safeRun(this::addProductToTable));
        updateBtn.addActionListener(e -> safeRun(this::updateProductInTable));
        clearBtn.addActionListener(e -> clearFields());
        saveBtn.addActionListener(e -> safeRun(this::saveProducts));
        backBtn.addActionListener(e -> cardLayout.show(mainContentPanel, "GYM_WHOLESALER"));
    }

    private void safeRun(Runnable r) {
        try { r.run(); } catch (Exception ex) {
            LOGGER.severe("Error: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Operation failed.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadProductNames() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement("SELECT DISTINCT productName FROM stock WHERE quantity >= 0");
            rs = ps.executeQuery();
            List<String> names = new ArrayList<>();
            while (rs.next()) {
                String n = rs.getString(1);
                if (n != null && !n.trim().isEmpty()) names.add(n);
            }
            productComboBox.removeAllItems();
            productComboBox.addItem("");
            names.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).forEach(productComboBox::addItem);
            productRates.clear();
            names.forEach(this::loadPricesForProduct);
        } catch (SQLException ex) {
            LOGGER.severe("Load error: " + ex.getMessage());
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    private void loadPricesForProduct(String name) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement("SELECT DISTINCT perPieceRate FROM stock WHERE LOWER(productName) = ? AND quantity >= 0 AND perPieceRate IS NOT NULL ORDER BY perPieceRate");
            ps.setString(1, name.toLowerCase());
            rs = ps.executeQuery();
            List<Double> prices = new ArrayList<>();
            while (rs.next()) prices.add(rs.getDouble(1));
            productRates.put(name, prices.isEmpty() ? List.of(0.0) : prices);
        } catch (SQLException ex) {
            productRates.put(name, List.of(0.0));
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    private void autoFillProductDetails() {
        if (updating) return;
        updating = true;

        JTextField editor = (JTextField) productComboBox.getEditor().getEditorComponent();
        String typed = editor.getText();
        String search = typed.trim().toLowerCase();

        if (typed.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                loadProductNames();
                updating = false;
            });
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement("SELECT DISTINCT productName FROM stock WHERE LOWER(productName) LIKE ? AND quantity >= 0 ORDER BY productName");
            ps.setString(1, "%" + search + "%");
            rs = ps.executeQuery();

            List<String> matches = new ArrayList<>();
            while (rs.next()) matches.add(rs.getString(1));

            String finalText = editor.getText();

            productComboBox.removeAllItems();
            productComboBox.addItem("");
            matches.forEach(productComboBox::addItem);
            productComboBox.getEditor().setItem(finalText);

            if (!matches.isEmpty()) productComboBox.showPopup();
            else productComboBox.hidePopup();

            boolean exact = matches.stream().anyMatch(m -> m.trim().toLowerCase().equals(search));
            if (exact) {
                String match = matches.stream()
                        .filter(m -> m.trim().toLowerCase().equals(search))
                        .findFirst().orElse(finalText);
                productComboBox.setSelectedItem(match);
                updateBuyingPriceAndStock();
            }

            SwingUtilities.invokeLater(() -> {
                editor.setText(finalText);
                editor.setCaretPosition(finalText.length());
                editor.requestFocusInWindow();
                updating = false;
            });

        } catch (SQLException ex) {
            updating = false;
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    private void closeResources(ResultSet rs, Statement s, Connection c) {
        try { if (rs != null) rs.close(); } catch (Exception ignored) {}
        try { if (s != null) s.close(); } catch (Exception ignored) {}
        try { if (c != null) c.close(); } catch (Exception ignored) {}
    }

    private void updateBuyingPriceAndStock() {
        String sel = (String) productComboBox.getSelectedItem();
        buyingPriceComboBox.removeAllItems();

        List<Double> prices = (sel != null && !sel.trim().isEmpty())
                ? productRates.entrySet().stream()
                        .filter(e -> e.getKey().toLowerCase().equals(sel.toLowerCase()))
                        .flatMap(e -> e.getValue().stream())
                        .distinct().sorted().collect(Collectors.toList())
                : List.of(0.0);

        prices.forEach(buyingPriceComboBox::addItem);
        updateStockForSelectedPrice();
        updateTotalAmount();
    }

    private void updateStockForSelectedPrice() {
        String productName = (String) productComboBox.getSelectedItem();
        Double selectedPrice = (Double) buyingPriceComboBox.getSelectedItem();

        if (productName == null || productName.trim().isEmpty() || selectedPrice == null) {
            stockQuantityField.setText("0");
            return;
        }

        int stock = dao.getStockQuantityByPrice(productName.trim(), selectedPrice);
        stockQuantityField.setText(String.format("%d", stock));
        quantityField.setText(String.format("%d", stock));
    }

    private void updateTotalAmount() {
        try {
            int q = quantityField.getText().trim().isEmpty() ? 0 : Integer.parseInt(quantityField.getText().trim());
            double p = sellingPriceField.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(sellingPriceField.getText().trim());
            totalAmountField.setText(String.format("%.2f", q * p));
        } catch (Exception e) {
            totalAmountField.setText("0.00");
        }
    }

    private void addProductToTable() {
        String name = ((String) productComboBox.getSelectedItem()).trim();
        Double buy = (Double) buyingPriceComboBox.getSelectedItem();
        String qty = quantityField.getText().trim();
        String sell = sellingPriceField.getText().trim();
        String total = totalAmountField.getText().trim();
        String desc = descriptionField.getText().trim();
        if (desc.isEmpty()) desc = null;

        if (name.isEmpty() || buy == null || qty.isEmpty() || sell.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int q = Integer.parseInt(qty);
        int available = dao.getStockQuantityByPrice(name, buy);
        if (q <= 0 || q > available) {
            JOptionPane.showMessageDialog(this, "Only " + available + " units available at ₹" + buy, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        tableModel.addRow(new Object[]{name, q, buy, Double.parseDouble(sell), Double.parseDouble(total), desc});
        clearFields();
    }

    private void updateProductInTable() {
        int row = productTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a row.", "Error", JOptionPane.ERROR_MESSAGE); return; }

        String name = ((String) productComboBox.getSelectedItem()).trim();
        Double buy = (Double) buyingPriceComboBox.getSelectedItem();
        String qty = quantityField.getText().trim();
        String sell = sellingPriceField.getText().trim();
        String total = totalAmountField.getText().trim();
        String desc = descriptionField.getText().trim();
        if (desc.isEmpty()) desc = null;

        if (name.isEmpty() || buy == null || qty.isEmpty() || sell.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int q = Integer.parseInt(qty);
        int oldQ = (Integer) tableModel.getValueAt(row, 1);
        int available = dao.getStockQuantityByPrice(name, buy);
        if (q <= 0 || available + oldQ - q < 0) {
            JOptionPane.showMessageDialog(this, "Only " + (available + oldQ) + " units available at ₹" + buy, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        tableModel.setValueAt(name, row, 0);
        tableModel.setValueAt(q, row, 1);
        tableModel.setValueAt(buy, row, 2);
        tableModel.setValueAt(Double.parseDouble(sell), row, 3);
        tableModel.setValueAt(Double.parseDouble(total), row, 4);
        tableModel.setValueAt(desc, row, 5);
        clearFields();
    }

    private void populateFieldsFromTable() {
        int row = productTable.getSelectedRow();
        if (row < 0) return;
        String name = (String) tableModel.getValueAt(row, 0);
        productComboBox.setSelectedItem(name);
        Double buy = (Double) tableModel.getValueAt(row, 2);
        if (buy != null && !productRates.getOrDefault(name, List.of()).contains(buy)) {
            buyingPriceComboBox.addItem(buy);
        }
        buyingPriceComboBox.setSelectedItem(buy);
        quantityField.setText(tableModel.getValueAt(row, 1).toString());
        sellingPriceField.setText(tableModel.getValueAt(row, 3).toString());
        totalAmountField.setText(tableModel.getValueAt(row, 4).toString());
        descriptionField.setText(tableModel.getValueAt(row, 5) != null ? tableModel.getValueAt(row, 5).toString() : "");
        updateStockForSelectedPrice();
    }

    private void clearFields() {
        productComboBox.setSelectedIndex(0);
        buyingPriceComboBox.removeAllItems();
        quantityField.setText("");
        stockQuantityField.setText("0");
        sellingPriceField.setText("");
        totalAmountField.setText("");
        descriptionField.setText("");
        productTable.clearSelection();
        loadProductNames();
    }

    private void saveProducts() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No products.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalDate date = dateChooser.getDate() != null
                ? dateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();

        List<GymWholesaler> list = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String name = (String) tableModel.getValueAt(i, 0);
            int qty = (Integer) tableModel.getValueAt(i, 1);
            double buy = (Double) tableModel.getValueAt(i, 2);
            double sell = (Double) tableModel.getValueAt(i, 3);
            double total = (Double) tableModel.getValueAt(i, 4);
            String desc = tableModel.getValueAt(i, 5) != null ? tableModel.getValueAt(i, 5).toString() : null;

            GymWholesaler p = new GymWholesaler(null, wholesaler.getWholesalerName(), wholesaler.getMobileNo(),
                    name, qty, buy, sell, qty * (sell - buy), null, date, wholesaler.getAddress());
            p.setTotalBill(total);
            p.setDescription(desc);
            list.add(p);
        }

        dao.saveProducts(list, date, this);
        tableModel.setRowCount(0);
        cardLayout.show(mainContentPanel, "WHOLESALER_ACTION");
    }
}