package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.CardLayout;

/**
 * Main container for the Customer Form UI.
 * Uses JSplitPane to allow collapsing/hiding the input form.
 * Integrates UI, Actions, and DB logic.
 */
public class CustomerForm extends JPanel {

    // ========================================================================
    // 1. SERIAL VERSION & CONSTANTS
    // ========================================================================
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_DIVIDER_LOCATION = 450;

    // ========================================================================
    // 2. CORE COMPONENTS
    // ========================================================================
    private final CardLayout cardLayout;
    private final JPanel mainContentPanel;
    private final CustomerFormUI formUI;
    private final CustomerFormActions formActions;
    private final CustomerFormDB formDB;

    // ========================================================================
    // 3. LAYOUT & INTERACTION
    // ========================================================================
    private JSplitPane splitPane;
    private JButton toggleFormButton;
    private boolean isFormVisible = true;

    // ========================================================================
    // 4. CONSTRUCTOR
    // ========================================================================
    public CustomerForm(CardLayout cardLayout, JPanel mainContentPanel) {
        this.cardLayout = cardLayout;
        this.mainContentPanel = mainContentPanel;

        // Initialize dependencies in correct order
        this.formUI = new CustomerFormUI(this, null);
        this.formDB = new CustomerFormDB(this, formUI);
        this.formActions = new CustomerFormActions(this, formUI, formDB);

        // Initialize PDF generator
        new CustomerFormPDF(this);

        setLayout(new BorderLayout());
        initComponents();

        // Load initial data
        formDB.loadTableData();
        formDB.loadProductNames();
    }

    // ========================================================================
    // 5. UI INITIALIZATION
    // ========================================================================
    private void initComponents() {

        // 5.1 Toggle Button (Hide/Show Form)
        toggleFormButton = formUI.createGrayStyledButton("HIDE FORM");
        toggleFormButton.addActionListener(e -> toggleFormVisibility());

        // 5.2 Top Panel (Button + Timestamp)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(toggleFormButton, BorderLayout.WEST);
        topPanel.add(formUI.getTimestampLabel(), BorderLayout.EAST);

        // 5.3 Split Pane Setup
        JPanel formPanel = formUI.createFormPanel();
        JPanel tablePanel = formUI.createTablePanel();

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formPanel, tablePanel);
        splitPane.setDividerLocation(DEFAULT_DIVIDER_LOCATION);
        splitPane.setResizeWeight(0.5);
        splitPane.setEnabled(true); // Allow dragging divider

        // Minimum sizes to prevent collapse issues
        splitPane.setMinimumSize(new Dimension(300, 300));
        formPanel.setMinimumSize(new Dimension(100, 300));
        tablePanel.setMinimumSize(new Dimension(100, 300));

        // 5.4 Layout Assembly
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        // 5.5 Setup Action Listeners
        formActions.setupActionListeners();
    }

    // ========================================================================
    // 6. FORM VISIBILITY TOGGLE
    // ========================================================================
    public void toggleFormVisibility() {
        isFormVisible = !isFormVisible;

        if (isFormVisible) {
            // Show form panel
            splitPane.setLeftComponent(formUI.createFormPanel());
            splitPane.setDividerLocation(DEFAULT_DIVIDER_LOCATION);
            toggleFormButton.setText("HIDE FORM");
        } else {
            // Hide form panel
            splitPane.setLeftComponent(null);
            toggleFormButton.setText("SHOW FORM");
        }

        splitPane.resetToPreferredSizes();
        revalidate();
        repaint();
    }

    // ========================================================================
    // 7. GETTERS
    // ========================================================================
    public CardLayout getCardLayout() {
        return cardLayout;
    }

    public JPanel getMainContentPanel() {
        return mainContentPanel;
    }

    public CustomerFormUI getFormUI() {
        return formUI;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    public boolean isFormVisible() {
        return isFormVisible;
    }
}