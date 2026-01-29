package com.proshop.main;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;

public class CustomerFormActions {
    private final CustomerForm form;
    private final CustomerFormUI formUI;
    private final CustomerFormDB formDB;
    private static final int DEFAULT_DIVIDER_LOCATION = 450;

    public CustomerFormActions(CustomerForm form, CustomerFormUI formUI, CustomerFormDB formDB) {
        this.form = form;
        this.formUI = formUI;
        this.formDB = formDB;
    }

    // ========================================================================
    // 1. MAIN ACTION LISTENERS (BUTTONS)
    // ========================================================================
    public void setupActionListeners() {

        // 1.1 Add Customer
        formUI.getAddButton().addActionListener(e -> formDB.addCustomer(e));

        // 1.2 Update Customer
        formUI.getUpdateButton().addActionListener(e -> formDB.updateCustomer(e));

        // 1.3 Refresh Table & Fields
        formUI.getRefreshButton().addActionListener(e -> {
            formDB.refreshTableDate();
            formDB.loadProductNames();
            formDB.clearFields();
        });

        // 1.4 Delete Customer
        formUI.getDeleteButton().addActionListener(e -> formDB.deleteCustomer(e));

        // 1.5 Generate Full PDF Report
        formUI.getPdfButton().addActionListener(e -> formDB.generatePDF(e));

        // 1.6 Generate Individual Bill PDF
        formUI.getIndividualBillButton().addActionListener(e -> formDB.generateIndividualBillPDF(e));

        // 1.7 Pay Pending Amount
        formUI.getPayPendingButton().addActionListener(e -> formDB.payPendingAmount(e));

        // 1.8 View Payment History
        formUI.getViewPaymentsButton().addActionListener(e -> formDB.viewPaymentHistory(e));

        // 1.9 Monthly Report
        formUI.getMonthlyReportButton().addActionListener(e -> formDB.showMonthlyReport(e));

        // 1.10 Weekly Report
        formUI.getWeeklyReportButton().addActionListener(e -> formDB.showWeeklyReport(e));

        // 1.11 View All Data
        formUI.getViewAllDataButton().addActionListener(e -> formDB.viewAllData(e));

        // 1.12 Back to Main Menu
        formUI.getBackButton().addActionListener(e -> {
            form.getCardLayout().show(form.getMainContentPanel(), "MainMenu");
        });

        // 1.13 Previous Date Navigation
        formUI.getPrevDateButton().addActionListener(e -> formDB.shiftTableDate(false));

        // 1.14 Next Date Navigation
        formUI.getNextDateButton().addActionListener(e -> formDB.shiftTableDate(true));

        // 1.15 Minimize Split Pane
        formUI.getMinimizeButton().addActionListener(e -> {
            if (form.isFormVisible()) {
                form.getSplitPane().setDividerLocation(0);
            }
        });

        // 1.16 Maximize Split Pane
        formUI.getMaximizeButton().addActionListener(e -> {
            if (form.isFormVisible()) {
                form.getSplitPane().setDividerLocation(DEFAULT_DIVIDER_LOCATION);
            }
        });

        // ========================================================================
        // 2. COMBOBOX & INPUT FIELD LISTENERS (REAL-TIME CALCULATIONS)
        // ========================================================================

        // 2.1 Product Name Selection → Update Buying Price
        formUI.getProductNameComboBox().addActionListener(e -> {
            if ("comboBoxChanged".equals(e.getActionCommand())) {
                formDB.updateBuyingPriceComboBox();
            }
        });

        // 2.2 Buying Price Selection → Recalculate Profit
        formUI.getBuyingPriceComboBox().addActionListener(e -> {
            if ("comboBoxChanged".equals(e.getActionCommand())) {
                formDB.calculateNetProfit();
            }
        });

        // 2.3 Quantity Field → Recalculate Profit
        formUI.getQuantityField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) {
                formDB.calculateNetProfit();
            }
        });

        // 2.4 Selling Price Field → Recalculate Profit
        formUI.getSellingPriceField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) {
                formDB.calculateNetProfit();
            }
        });

        // 2.5 Discount % Field → Recalculate Profit
        formUI.getDiscountPercentageField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) {
                formDB.calculateNetProfit();
            }
        });

        // 2.6 Amount Paid Field → Recalculate Profit
        formUI.getAmountPaidField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) {
                formDB.calculateNetProfit();
            }
        });

        // ========================================================================
        // 3. TABLE INTERACTION LISTENERS
        // ========================================================================

        // 3.1 Single Click on Table Row → Populate Input Fields
        formUI.getCustomerTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 1) {
                    int row = formUI.getCustomerTable().rowAtPoint(evt.getPoint());
                    if (row >= 0) {
                        formDB.populateFields(row);
                    }
                }
            }
        });

        // 3.2 Search Field → Live Table Filtering
        formUI.getSearchField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) {
                formDB.filterTable();
            }
        });

        // ========================================================================
        // 4. AUTO-SUGGEST FIELDS (Customer Name & Mobile)
        // ========================================================================

        // 4.1 Customer Name Field (Auto-suggest placeholder)
        formUI.getCustomerNameField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() != KeyEvent.VK_UP &&
                    evt.getKeyCode() != KeyEvent.VK_DOWN &&
                    evt.getKeyCode() != KeyEvent.VK_ENTER) {
                    // TODO: Implement auto-suggest for customer name
                }
            }
        });

        // 4.2 Mobile Number Field (Auto-suggest placeholder)
        formUI.getMobileNoField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() != KeyEvent.VK_UP &&
                    evt.getKeyCode() != KeyEvent.VK_DOWN &&
                    evt.getKeyCode() != KeyEvent.VK_ENTER) {
                    // TODO: Implement auto-suggest for mobile number
                }
            }
        });

        // ========================================================================
        // 5. DATE FILTER LISTENERS (Start & End Date Choosers)
        // ========================================================================

        // 5.1 Shared Date Change Listener for Filtering
        PropertyChangeListener dateFilterListener = evt -> {
            if ("date".equals(evt.getPropertyName())) {
                formDB.filterTable();
            }
        };

        // 5.2 Start Date Chooser
        formUI.getStartDateChooser().getDateEditor().addPropertyChangeListener(dateFilterListener);

        // 5.3 End Date Chooser
        formUI.getEndDateChooser().getDateEditor().addPropertyChangeListener(dateFilterListener);
    }
}