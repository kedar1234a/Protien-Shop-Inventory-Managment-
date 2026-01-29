package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import com.toedter.calendar.JDateChooser;

public class CustomerFormUI {

    // ========================================================================
    // 1. UI COMPONENTS DECLARATION
    // ========================================================================
    private JTable customerTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField customerNameField, quantityField, mobileNoField, totalAmountField, finalBillField,
            amountPaidField;
    private JComboBox<String> productNameComboBox, paymentModeComboBox, statusComboBox;
    private JComboBox<Double> buyingPriceComboBox;
    private JTextField sellingPriceField, netProfitField, discountPercentageField;
    private JDateChooser dateChooser, startDateChooser, endDateChooser;
    private JTextField searchField;
    private JButton addButton, updateButton, refreshButton, backButton, pdfButton, individualBillButton,
            payPendingButton, viewPaymentsButton, monthlyReportButton, weeklyReportButton, minimizeButton,
            maximizeButton, prevDateButton, nextDateButton, viewAllDataButton, deleteButton;
    private JPopupMenu customerPopupMenu;
    private JLabel statusLabel, timestampLabel, stockQuantityLabel;
    private JLabel totalSalesLabel, netProfitLabel, totalPendingsLabel;
    private JDateChooser tableDateChooser;
    private final java.util.Map<String, java.util.List<Double>> productPriceMap = new java.util.HashMap<>();
    private final CustomerFormDB customerFormDB;

    // ========================================================================
    // 2. COLOR SCHEME (MATCHING SoldStockForm)
    // ========================================================================
    private static final Color PRIMARY_COLOR = new Color(33, 33, 33); // #212121
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color STAT_CARD_COLOR = new Color(50, 50, 50); // #323232
    private static final Color FAINT_ROW_COLOR = new Color(66, 66, 66); // #424242
    private static final Color SHADOW_COLOR = new Color(224, 224, 224); // #E0E0E0
    private static final Color BUTTON_GRAY_COLOR = new Color(50, 50, 50); // Button base
    private static final Color PENDING_HIGHLIGHT = Color.RED;
    private static final Color SUCCESS_COLOR = new Color(102, 187, 106); // #66BB6A
    private static final Color SKY_BLUE = new Color(135, 206, 250); // #87CEFA

    // ========================================================================
    // 3. CONSTRUCTOR
    // ========================================================================
    public CustomerFormUI(CustomerForm parentForm, CustomerFormDB customerFormDB) {
        this.customerFormDB = customerFormDB;
        initComponents();
    }

    // ========================================================================
    // 4. INITIALIZATION OF ALL COMPONENTS
    // ========================================================================
    private void initComponents() {
        // 4.1 Table Setup
        String[] columnNames = {
                "ID", "Customer Name", "Product Name", "Quantity", "Buying Price",
                "Selling Price", "Total Amount", "Final Bill", "Net Profit", "Payment",
                "Status", "Mobile No", "Amount Paid", "Payment Date", "Pending"
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        sorter = new TableRowSorter<>(tableModel);
        customerTable = new JTable(tableModel) {
            private static final long serialVersionUID = 1L;
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    double pendingAmount = 0.0;
                    double amountPaid = 0.0;
                    try {
                        pendingAmount = Double.parseDouble(tableModel.getValueAt(row, 14).toString());
                        amountPaid = Double.parseDouble(tableModel.getValueAt(row, 12).toString());
                    } catch (NumberFormatException e) {
                        // Ignore invalid values
                    }
                    if (column == 12 && amountPaid == 0.0) {
                        c.setBackground(SKY_BLUE);
                        c.setForeground(Color.BLACK);
                    } else if (column == 14 && pendingAmount > 0) {
                        c.setBackground(PENDING_HIGHLIGHT);
                        c.setForeground(Color.WHITE);
                    } else {
                        c.setBackground(row % 2 == 0 ? STAT_CARD_COLOR : FAINT_ROW_COLOR);
                        c.setForeground(TEXT_COLOR);
                    }
                } else {
                    c.setBackground(getSelectionBackground());
                    c.setForeground(getSelectionForeground());
                }
                return c;
            }
        };
        customerTable.setRowSorter(sorter);
        customerTable.setBackground(STAT_CARD_COLOR);
        customerTable.setForeground(TEXT_COLOR);
        customerTable.setFont(new Font("Arial", Font.PLAIN, 16));
        customerTable.setRowHeight(30);
        customerTable.getTableHeader().setBackground(PRIMARY_COLOR);
        customerTable.getTableHeader().setForeground(TEXT_COLOR);
        customerTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        setDynamicColumnWidths();

        // 4.2 Input Fields
        customerNameField = UIUtils.createStyledTextField(12);
        customerNameField.setBackground(BUTTON_GRAY_COLOR);
        mobileNoField = UIUtils.createStyledTextField(10);
        mobileNoField.setBackground(BUTTON_GRAY_COLOR);

        // === PRODUCT NAME COMBOBOX WITH SPACE SUPPORT ===
        productNameComboBox = new JComboBox<>();
        productNameComboBox.setEditable(true);
        productNameComboBox.setFont(UIUtils.TEXT_FONT);
        productNameComboBox.setBackground(UIUtils.BACKGROUND_COLOR);
        productNameComboBox.setForeground(UIUtils.TEXT_COLOR);

        // FIX: Allow space typing + prevent popup
        JTextField productEditor = (JTextField) productNameComboBox.getEditor().getEditorComponent();
        productEditor.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                    String text = productEditor.getText();
                    int caret = productEditor.getCaretPosition();
                    productEditor.setText(text.substring(0, caret) + " " + text.substring(caret));
                    productEditor.setCaretPosition(caret + 1);
                    e.consume();
                }
            }
        });
        productNameComboBox.setKeySelectionManager((aKey, aModel) -> -1); // Disable space popup
        // === END FIX ===

        quantityField = UIUtils.createStyledTextField(6);
        quantityField.setBackground(BUTTON_GRAY_COLOR);

        buyingPriceComboBox = new JComboBox<>();
        buyingPriceComboBox.setEditable(true);
        buyingPriceComboBox.setFont(UIUtils.TEXT_FONT);
        buyingPriceComboBox.setBackground(UIUtils.BACKGROUND_COLOR);
        buyingPriceComboBox.setForeground(UIUtils.TEXT_COLOR);

        sellingPriceField = UIUtils.createStyledTextField(8);
        sellingPriceField.setBackground(BUTTON_GRAY_COLOR);
        netProfitField = UIUtils.createStyledTextField(8);
        netProfitField.setBackground(BUTTON_GRAY_COLOR);
        netProfitField.setEditable(false);

        paymentModeComboBox = new JComboBox<>(new String[] { "", "Online", "Cash" });
        paymentModeComboBox.setFont(UIUtils.TEXT_FONT);
        paymentModeComboBox.setBackground(BUTTON_GRAY_COLOR);
        paymentModeComboBox.setForeground(UIUtils.TEXT_COLOR);

        discountPercentageField = UIUtils.createStyledTextField(6);
        discountPercentageField.setBackground(BUTTON_GRAY_COLOR);

        statusComboBox = new JComboBox<>(new String[] { "", "Paid", "Pending" });
        statusComboBox.setFont(UIUtils.TEXT_FONT);
        statusComboBox.setBackground(BUTTON_GRAY_COLOR);
        statusComboBox.setForeground(UIUtils.TEXT_COLOR);

        totalAmountField = UIUtils.createStyledTextField(8);
        totalAmountField.setEditable(false);
        totalAmountField.setBackground(BUTTON_GRAY_COLOR);
        finalBillField = UIUtils.createStyledTextField(8);
        finalBillField.setEditable(false);
        finalBillField.setBackground(BUTTON_GRAY_COLOR);
        amountPaidField = UIUtils.createStyledTextField(8);
        amountPaidField.setBackground(BUTTON_GRAY_COLOR);

        dateChooser = UIUtils.createStyledDateChooser();
        dateChooser.setDate(java.sql.Date.valueOf(java.time.LocalDate.now()));
        startDateChooser = UIUtils.createStyledDateChooser();
        endDateChooser = UIUtils.createStyledDateChooser();
        searchField = UIUtils.createStyledTextField(12);
        searchField.setBackground(BUTTON_GRAY_COLOR);

        tableDateChooser = UIUtils.createStyledDateChooser();
        tableDateChooser.setDate(java.sql.Date.valueOf(java.time.LocalDate.now()));
        tableDateChooser.getDateEditor().addPropertyChangeListener("date", evt -> {
            if (evt.getNewValue() != null) {
                customerFormDB.loadTableData();
                updateSummaryLabels();
            }
        });

        // 4.3 Buttons
        addButton = createGrayStyledButton("ADD");
        updateButton = createGrayStyledButton("UPDATE");
        refreshButton = createGrayStyledButton("REFRESH");
        backButton = createGrayStyledButton("BACK");
        pdfButton = createGrayStyledButton("PDF");
        individualBillButton = createGrayStyledButton("BILL PDF");
        payPendingButton = createGrayStyledButton("PAY PENDING");
        viewPaymentsButton = createGrayStyledButton("VIEW PAYMENTS");
        monthlyReportButton = createGrayStyledButton("MONTHLY REPORT");
        weeklyReportButton = createGrayStyledButton("WEEKLY REPORT");
        viewAllDataButton = createGrayStyledButton("VIEW ALL DATA");
        deleteButton = createGrayStyledButton("DELETE");
        minimizeButton = createGrayStyledButton("LEFT TRIANGLE");
        maximizeButton = createGrayStyledButton("RIGHT TRIANGLE");
        prevDateButton = createGrayStyledButton("<");
        nextDateButton = createGrayStyledButton(">");

        // 4.4 Labels
        statusLabel = new JLabel("");
        statusLabel.setFont(UIUtils.LABEL_FONT);
        statusLabel.setForeground(TEXT_COLOR);
        timestampLabel = new JLabel("Logged in: " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timestampLabel.setFont(UIUtils.LABEL_FONT);
        timestampLabel.setForeground(Color.BLACK);
        stockQuantityLabel = new JLabel("AVAILABLE STOCK: 0");
        stockQuantityLabel.setFont(UIUtils.LABEL_FONT);
        stockQuantityLabel.setForeground(TEXT_COLOR);
        totalSalesLabel = new JLabel("TOTAL SALES: ₹0.00");
        totalSalesLabel.setFont(UIUtils.LABEL_FONT);
        totalSalesLabel.setForeground(SUCCESS_COLOR);
        netProfitLabel = new JLabel("TOTAL NET PROFIT: ₹0.00");
        netProfitLabel.setFont(UIUtils.LABEL_FONT);
        netProfitLabel.setForeground(SUCCESS_COLOR);
        totalPendingsLabel = new JLabel("TOTAL PENDINGS: ₹0.00");
        totalPendingsLabel.setFont(UIUtils.LABEL_FONT);
        totalPendingsLabel.setForeground(SUCCESS_COLOR);

        customerPopupMenu = new JPopupMenu();
    }

    // ========================================================================
    // 5. HELPER: BUTTON CREATION
    // ========================================================================
    public JButton createGrayStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(BUTTON_GRAY_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
        button.setFocusPainted(false);
        button.setRolloverEnabled(false);
        button.setPreferredSize(new Dimension(90, 30));
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        return button;
    }

    // ========================================================================
    // 6. PANEL LAYOUTS
    // ========================================================================
    public JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(PRIMARY_COLOR);
        mainPanel.add(createFormPanel(), BorderLayout.WEST);
        mainPanel.add(createTablePanel(), BorderLayout.CENTER);
        return mainPanel;
    }

    public JPanel createFormPanel() {
        JPanel formPanel = UIUtils.createStyledFormPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        int fieldWidth = 150;
        int fieldHeight = 30;
        Dimension fieldSize = new Dimension(fieldWidth, fieldHeight);

        applyFieldSize(customerNameField, fieldSize);
        applyFieldSize(mobileNoField, fieldSize);
        applyFieldSize(productNameComboBox, fieldSize);
        applyFieldSize(quantityField, fieldSize);
        applyFieldSize(buyingPriceComboBox, fieldSize);
        applyFieldSize(sellingPriceField, fieldSize);
        applyFieldSize(totalAmountField, fieldSize);
        applyFieldSize(finalBillField, fieldSize);
        applyFieldSize(netProfitField, fieldSize);
        applyFieldSize(paymentModeComboBox, fieldSize);
        applyFieldSize(statusComboBox, fieldSize);
        applyFieldSize(amountPaidField, fieldSize);
        applyFieldSize(dateChooser, fieldSize);

        UIUtils.addFormField(formPanel, gbc, "CUSTOMER NAME:", customerNameField, 0, 0);
        UIUtils.addFormField(formPanel, gbc, "MOBILE NO:", mobileNoField, 0, 2);
        UIUtils.addFormField(formPanel, gbc, "PRODUCT:", productNameComboBox, 1, 0);
        UIUtils.addFormField(formPanel, gbc, "", stockQuantityLabel, 1, 2);
        UIUtils.addFormField(formPanel, gbc, "QUANTITY:", quantityField, 2, 0);
        UIUtils.addFormField(formPanel, gbc, "BUYING PRICE:", buyingPriceComboBox, 2, 2);
        UIUtils.addFormField(formPanel, gbc, "SELLING PRICE:", sellingPriceField, 3, 0);
        UIUtils.addFormField(formPanel, gbc, "TOTAL AMOUNT:", totalAmountField, 3, 2);
        UIUtils.addFormField(formPanel, gbc, "FINAL BILL:", finalBillField, 4, 0);
        UIUtils.addFormField(formPanel, gbc, "NET PROFIT:", netProfitField, 4, 2);
        UIUtils.addFormField(formPanel, gbc, "PAYMENT MODE:", paymentModeComboBox, 5, 0);
        UIUtils.addFormField(formPanel, gbc, "STATUS:", statusComboBox, 5, 2);
        UIUtils.addFormField(formPanel, gbc, "AMOUNT PAID:", amountPaidField, 6, 0);
        UIUtils.addFormField(formPanel, gbc, "DATE:", dateChooser, 6, 2);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(PRIMARY_COLOR);
        contentPanel.add(formPanel, BorderLayout.NORTH);
        contentPanel.add(createButtonPanel(), BorderLayout.CENTER);
        return contentPanel;
    }

    private void applyFieldSize(Component comp, Dimension size) {
        comp.setPreferredSize(size);
        comp.setMaximumSize(size);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setBackground(PRIMARY_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;

        gbc.gridx = 0; gbc.gridy = 0; buttonPanel.add(addButton, gbc);
        gbc.gridx = 1; buttonPanel.add(updateButton, gbc);
        gbc.gridx = 0; gbc.gridy = 1; buttonPanel.add(pdfButton, gbc);
        gbc.gridx = 1; buttonPanel.add(individualBillButton, gbc);
        gbc.gridx = 0; gbc.gridy = 2; buttonPanel.add(payPendingButton, gbc);
        gbc.gridx = 1; buttonPanel.add(viewPaymentsButton, gbc);
        gbc.gridx = 0; gbc.gridy = 3; buttonPanel.add(weeklyReportButton, gbc);
        gbc.gridx = 1; buttonPanel.add(monthlyReportButton, gbc);
        gbc.gridx = 0; gbc.gridy = 4; buttonPanel.add(viewAllDataButton, gbc);
        gbc.gridx = 1; buttonPanel.add(deleteButton, gbc);
        gbc.gridx = 0; gbc.gridy = 5; buttonPanel.add(backButton, gbc);
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; buttonPanel.add(statusLabel, gbc);

        return buttonPanel;
    }

    public JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(PRIMARY_COLOR);

        JPanel dateShifterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        dateShifterPanel.setBackground(PRIMARY_COLOR);
        tableDateChooser.setPreferredSize(new Dimension(120, 25));
        searchField.setPreferredSize(new Dimension(120, 25));
        dateShifterPanel.add(prevDateButton);
        dateShifterPanel.add(tableDateChooser);
        dateShifterPanel.add(nextDateButton);
        dateShifterPanel.add(refreshButton);
        dateShifterPanel.add(searchField);
        tablePanel.add(dateShifterPanel, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(customerTable);
        tableScrollPane.setBackground(PRIMARY_COLOR);
        tableScrollPane.getViewport().setBackground(PRIMARY_COLOR);
        tableScrollPane.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));

        JPanel cornerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cornerPanel.setBackground(PRIMARY_COLOR);
        cornerPanel.add(minimizeButton);
        cornerPanel.add(maximizeButton);
        tableScrollPane.setCorner(JScrollPane.LOWER_RIGHT_CORNER, cornerPanel);

        tablePanel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        summaryPanel.setBackground(PRIMARY_COLOR);
        summaryPanel.setBorder(new EmptyBorder(10, 40, 10, 40));
        summaryPanel.add(totalSalesLabel);
        summaryPanel.add(Box.createHorizontalStrut(20));
        summaryPanel.add(netProfitLabel);
        summaryPanel.add(Box.createHorizontalStrut(20));
        summaryPanel.add(totalPendingsLabel);
        tablePanel.add(summaryPanel, BorderLayout.SOUTH);

        return tablePanel;
    }

    public JScrollPane createTableScrollPane() {
        JScrollPane scrollPane = new JScrollPane(customerTable);
        scrollPane.setBackground(PRIMARY_COLOR);
        scrollPane.getViewport().setBackground(PRIMARY_COLOR);
        scrollPane.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
        return scrollPane;
    }

    // ========================================================================
    // 7. TABLE COLUMN WIDTHS
    // ========================================================================
    void setDynamicColumnWidths() {
        int[] widths = { 60, 180, 180, 80, 100, 100, 100, 100, 100, 100, 80, 120, 100, 120, 100 };
        for (int i = 0; i < customerTable.getColumnCount(); i++) {
            customerTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    // ========================================================================
    // 8. SUMMARY LABEL UPDATER
    // ========================================================================
    public void updateSummaryLabels() {
        double totalSales = 0.0, totalNetProfit = 0.0, totalPendings = 0.0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                totalSales += Double.parseDouble(tableModel.getValueAt(i, 7).toString());
                totalNetProfit += Double.parseDouble(tableModel.getValueAt(i, 8).toString());
                totalPendings += Double.parseDouble(tableModel.getValueAt(i, 14).toString());
            } catch (NumberFormatException e) {
                // Skip malformed data
            }
        }
        totalSalesLabel.setText("TOTAL SALES: ₹" + String.format("%.2f", totalSales));
        netProfitLabel.setText("TOTAL NET PROFIT: ₹" + String.format("%.2f", totalNetProfit));
        totalPendingsLabel.setText("TOTAL PENDINGS: ₹" + String.format("%.2f", totalPendings));
    }

    // ========================================================================
    // 9. GETTERS
    // ========================================================================
    public JTable getCustomerTable() { return customerTable; }
    public DefaultTableModel getTableModel() { return tableModel; }
    public TableRowSorter<DefaultTableModel> getSorter() { return sorter; }

    public JTextField getCustomerNameField() { return customerNameField; }
    public JTextField getQuantityField() { return quantityField; }
    public JTextField getMobileNoField() { return mobileNoField; }
    public JTextField getTotalAmountField() { return totalAmountField; }
    public JTextField getFinalBillField() { return finalBillField; }
    public JTextField getAmountPaidField() { return amountPaidField; }
    public JTextField getSellingPriceField() { return sellingPriceField; }
    public JTextField getNetProfitField() { return netProfitField; }
    public JTextField getDiscountPercentageField() { return discountPercentageField; }
    public JTextField getSearchField() { return searchField; }

    public JComboBox<String> getProductNameComboBox() { return productNameComboBox; }
    public JComboBox<String> getPaymentModeComboBox() { return paymentModeComboBox; }
    public JComboBox<String> getStatusComboBox() { return statusComboBox; }
    public JComboBox<Double> getBuyingPriceComboBox() { return buyingPriceComboBox; }

    public JDateChooser getDateChooser() { return dateChooser; }
    public JDateChooser getStartDateChooser() { return startDateChooser; }
    public JDateChooser getEndDateChooser() { return endDateChooser; }
    public JDateChooser getTableDateChooser() { return tableDateChooser; }

    public JButton getAddButton() { return addButton; }
    public JButton getUpdateButton() { return updateButton; }
    public JButton getRefreshButton() { return refreshButton; }
    public JButton getBackButton() { return backButton; }
    public JButton getPdfButton() { return pdfButton; }
    public JButton getIndividualBillButton() { return individualBillButton; }
    public JButton getPayPendingButton() { return payPendingButton; }
    public JButton getViewPaymentsButton() { return viewPaymentsButton; }
    public JButton getMonthlyReportButton() { return monthlyReportButton; }
    public JButton getWeeklyReportButton() { return weeklyReportButton; }
    public JButton getViewAllDataButton() { return viewAllDataButton; }
    public JButton getDeleteButton() { return deleteButton; }
    public JButton getMinimizeButton() { return minimizeButton; }
    public JButton getMaximizeButton() { return maximizeButton; }
    public JButton getPrevDateButton() { return prevDateButton; }
    public JButton getNextDateButton() { return nextDateButton; }

    public JPopupMenu getCustomerPopupMenu() { return customerPopupMenu; }
    public JLabel getStatusLabel() { return statusLabel; }
    public JLabel getTimestampLabel() { return timestampLabel; }
    public JLabel getStockQuantityLabel() { return stockQuantityLabel; }
    public JLabel getTotalSalesLabel() { return totalSalesLabel; }
    public JLabel getNetProfitLabel() { return netProfitLabel; }
    public JLabel getTotalPendingsLabel() { return totalPendingsLabel; }

    public java.util.Map<String, java.util.List<Double>> getProductPriceMap() {
        return productPriceMap;
    }
}