package com.proshop.main;

import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import com.proshop.model.Bill;
import com.proshop.model.Product;
import com.proshop.model.WholesalerPurchase;
import com.toedter.calendar.JDateChooser;

public class AddProductForm extends JPanel {
    private static final long serialVersionUID = 1L;

    // === FIELDS ===
    private JComboBox<String> productNameComboBox;
    private JTextField quantityField;
    private JTextField perPieceRateField;
    private JDateChooser expiryDateChooser;
    private DefaultTableModel productTableModel;
    private JTable productTable;
    private List<Product> products;
    private int selectedRow = -1;
    private WholesalerPurchase wholesaler;
    private boolean updating = false; // Prevent recursive updates

    // === COLORS ===
    private static final Color BACKGROUND_COLOR = new Color(33, 33, 33);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color SHADOW_COLOR = new Color(224, 224, 224);
    private static final Color BACK_COLOR = new Color(120, 120, 120);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color LIGHT_GRAY = new Color(50, 50, 50);
    private static final Color DARK_GRAY = new Color(33, 33, 33);

    // ===================================================================
    // [1] CONSTRUCTOR
    // ===================================================================
    public AddProductForm(WholesalerPurchase wholesaler,
                          Map<Bill, List<Product>> billProducts,
                          LocalDate purchaseDate,
                          String shippingCharges,
                          String billAmount,
                          CardLayout cardLayout,
                          JPanel mainContentPanel) {
        this.wholesaler = wholesaler;
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SHADOW_COLOR, 1),
                new EmptyBorder(10, 10, 10, 10)));

        products = new ArrayList<>();

        // [1.1] Header
        JLabel titleLabel = new JLabel("Add Products for New Bill", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(SUCCESS_COLOR);
        titleLabel.setBackground(BACKGROUND_COLOR);
        titleLabel.setOpaque(true);
        titleLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
        add(titleLabel, BorderLayout.NORTH);

        // [1.2] Form Panel
        JPanel formContainer = new JPanel(new BorderLayout());
        formContainer.setBackground(BACKGROUND_COLOR);
        formContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        formPanel.setBackground(BACKGROUND_COLOR);
        formPanel.setPreferredSize(new Dimension(720, 150));
        Dimension fieldSize = new Dimension(150, 25);

        // Product Name
        productNameComboBox = new JComboBox<>();
        productNameComboBox.setEditable(true);
        productNameComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        productNameComboBox.setForeground(TEXT_COLOR);
        productNameComboBox.setBackground(BACK_COLOR);
        productNameComboBox.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
        productNameComboBox.setPreferredSize(fieldSize);
        JLabel productNameLabel = new JLabel("Product Name:");
        productNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        productNameLabel.setForeground(TEXT_COLOR);
        formPanel.add(productNameLabel);
        formPanel.add(productNameComboBox);

        // Quantity
        quantityField = new JTextField(10);
        styleTextField(quantityField, fieldSize);
        JLabel quantityLabel = new JLabel("Quantity:");
        quantityLabel.setFont(new Font("Arial", Font.BOLD, 14));
        quantityLabel.setForeground(TEXT_COLOR);
        formPanel.add(quantityLabel);
        formPanel.add(quantityField);

        // Per Piece Rate
        perPieceRateField = new JTextField(10);
        styleTextField(perPieceRateField, fieldSize);
        JLabel perPieceRateLabel = new JLabel("Per Piece Rate:");
        perPieceRateLabel.setFont(new Font("Arial", Font.BOLD, 14));
        perPieceRateLabel.setForeground(TEXT_COLOR);
        formPanel.add(perPieceRateLabel);
        formPanel.add(perPieceRateField);

        // Expiry Date
        expiryDateChooser = new JDateChooser();
        expiryDateChooser.setFont(new Font("Arial", Font.PLAIN, 14));
        expiryDateChooser.setForeground(TEXT_COLOR);
        expiryDateChooser.setBackground(BACK_COLOR);
        expiryDateChooser.setPreferredSize(fieldSize);
        JLabel expiryDateLabel = new JLabel("Expiry Date:");
        expiryDateLabel.setFont(new Font("Arial", Font.BOLD, 14));
        expiryDateLabel.setForeground(TEXT_COLOR);
        formPanel.add(expiryDateLabel);
        formPanel.add(expiryDateChooser);

        // [1.3] Buttons (Add / Update)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        JButton addBtn = createStyledButton("Add Product");
        JButton updBtn = createStyledButton("Update");
        buttonPanel.add(addBtn);
        buttonPanel.add(updBtn);
        formPanel.add(buttonPanel);

        // [1.4] Table
        productTableModel = new DefaultTableModel(
                new String[]{"Product Name", "Quantity", "Per Piece Rate", "Expiry", "Total"}, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        productTable = new JTable(productTableModel);
        styleTable(productTable);
        JScrollPane scroll = new JScrollPane(productTable);
        scroll.setPreferredSize(new Dimension(720, 200));
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        formContainer.add(formPanel, BorderLayout.NORTH);
        formContainer.add(scroll, BorderLayout.CENTER);

        // [1.5] Bottom Buttons (Submit / Back)
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        southPanel.setBackground(BACKGROUND_COLOR);
        southPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        JButton submitBtn = createStyledButton("Submit");
        JButton backBtn = createStyledButton("Back");
        southPanel.add(submitBtn);
        southPanel.add(backBtn);

        add(formContainer, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        // [1.6] Setup Auto-Suggest & Event Listeners
        setupProductAutoSuggest();

        // [1.7] Table Row Selection
        productTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                selectedRow = productTable.getSelectedRow();
                if (selectedRow >= 0) {
                    productNameComboBox.setSelectedItem(productTableModel.getValueAt(selectedRow, 0));
                    quantityField.setText(String.valueOf(productTableModel.getValueAt(selectedRow, 1)));
                    perPieceRateField.setText(String.valueOf(productTableModel.getValueAt(selectedRow, 2)));
                    String exp = (String) productTableModel.getValueAt(selectedRow, 3);
                    expiryDateChooser.setDate(exp.equals("N/A") ? null
                            : java.util.Date.from(LocalDate.parse(exp).atStartOfDay(ZoneId.systemDefault()).toInstant()));
                }
            }
        });

        // [1.8] Button Actions
        addBtn.addActionListener(e -> addOrUpdateProduct(false));
        updBtn.addActionListener(e -> addOrUpdateProduct(true));
        submitBtn.addActionListener(e -> {
            try {
                if (products.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Add at least one product.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                BigDecimal ship = new BigDecimal(shippingCharges);
                BigDecimal amt = new BigDecimal(billAmount);
                Bill bill = new Bill(null, purchaseDate, ship, amt);
                DatabaseUtils.addBill(bill, wholesaler.getId(), products);
                JOptionPane.showMessageDialog(this, "Bill saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
                mainContentPanel.add(new WholesalerActionView(wholesaler, billProducts, cardLayout, mainContentPanel), "WHOLESALER_ACTION");
                cardLayout.show(mainContentPanel, "WHOLESALER_ACTION");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        backBtn.addActionListener(e -> {
            mainContentPanel.add(new NewBillForm(wholesaler, billProducts, cardLayout, mainContentPanel), "NEW_BILL");
            cardLayout.show(mainContentPanel, "NEW_BILL");
        });

        setVisible(true);
        revalidate();
        repaint();
    }

    // ===================================================================
    // [2] AUTO-SUGGEST SETUP (Fixed Cursor Jump)
    // ===================================================================
    private void setupProductAutoSuggest() {
        JTextField editor = (JTextField) productNameComboBox.getEditor().getEditorComponent();
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { scheduleUpdate(); }
            @Override public void removeUpdate(DocumentEvent e) { scheduleUpdate(); }
            @Override public void changedUpdate(DocumentEvent e) { scheduleUpdate(); }

            private void scheduleUpdate() {
                if (updating) return;
                SwingUtilities.invokeLater(() -> {
                    if (!updating) {
                        String typed = editor.getText();
                        String search = typed.trim().toLowerCase();
                        updating = true;
                        DatabaseUtils.updateProductComboBox(productNameComboBox, search);
                        productNameComboBox.getEditor().setItem(typed);
                        if (productNameComboBox.getItemCount() > 1) {
                            productNameComboBox.showPopup();
                        } else {
                            productNameComboBox.hidePopup();
                        }
                        for (int i = 0; i < productNameComboBox.getItemCount(); i++) {
                            String item = productNameComboBox.getItemAt(i);
                            if (item != null && item.trim().toLowerCase().equals(search)) {
                                productNameComboBox.setSelectedItem(item);
                                break;
                            }
                        }
                        SwingUtilities.invokeLater(() -> {
                            editor.setText(typed);
                            editor.setCaretPosition(typed.length());
                            editor.requestFocusInWindow();
                            updating = false;
                        });
                    }
                });
            }
        });
    }

    // ===================================================================
    // [3] ADD / UPDATE PRODUCT LOGIC
    // ===================================================================
    private void addOrUpdateProduct(boolean isUpdate) {
        try {
            String name = getComboBoxText();
            if (name == null || name.isBlank()) {
                JOptionPane.showMessageDialog(this, "Select a product name.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int qty = Integer.parseInt(quantityField.getText().trim());
            BigDecimal rate = new BigDecimal(perPieceRateField.getText().trim());
            LocalDate exp = expiryDateChooser.getDate() == null ? null
                    : expiryDateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            BigDecimal total = rate.multiply(BigDecimal.valueOf(qty));

            if (isUpdate) {
                if (selectedRow < 0) {
                    JOptionPane.showMessageDialog(this, "Select a row to update.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Product p = products.get(selectedRow);
                p.setProductName(name);
                p.setQuantity(qty);
                p.setPerPieceRate(rate);
                p.setExpiry(exp);
                p.setTotal(total);
                productTableModel.setValueAt(name, selectedRow, 0);
                productTableModel.setValueAt(qty, selectedRow, 1);
                productTableModel.setValueAt(rate, selectedRow, 2);
                productTableModel.setValueAt(exp != null ? exp.toString() : "N/A", selectedRow, 3);
                productTableModel.setValueAt(total, selectedRow, 4);
                selectedRow = -1;
                productTable.clearSelection();
            } else {
                Product p = new Product(null, name, qty, rate, exp, wholesaler.getId());
                p.setTotal(total);
                products.add(p);
                productTableModel.addRow(new Object[]{name, qty, rate, exp != null ? exp.toString() : "N/A", total});
            }
            clearFields();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================================================================
    // [4] UTILITY METHODS
    // ===================================================================
    private String getComboBoxText() {
        Object sel = productNameComboBox.getSelectedItem();
        return sel != null ? sel.toString().trim() : null;
    }

    private void clearFields() {
        productNameComboBox.setSelectedIndex(-1);
        quantityField.setText("");
        perPieceRateField.setText("");
        expiryDateChooser.setDate(null);
    }

    // ===================================================================
    // [5] STYLING METHODS
    // ===================================================================
    private void styleTextField(JTextField tf, Dimension size) {
        tf.setFont(new Font("Arial", Font.PLAIN, 14));
        tf.setForeground(TEXT_COLOR);
        tf.setBackground(BACK_COLOR);
        tf.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
        tf.setPreferredSize(size);
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setForeground(TEXT_COLOR);
        table.setBackground(DARK_GRAY);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 16));
        header.setForeground(TEXT_COLOR);
        header.setBackground(BACK_COLOR);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                comp.setForeground(TEXT_COLOR);
                comp.setBackground(s ? BACK_COLOR : (r % 2 == 0 ? LIGHT_GRAY : DARK_GRAY));
                return comp;
            }
        });
    }

    private JButton createStyledButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, 18));
        b.setForeground(TEXT_COLOR);
        b.setBackground(BACK_COLOR);
        b.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(160, 45));
        return b;
    }
}