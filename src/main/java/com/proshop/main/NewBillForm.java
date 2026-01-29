package com.proshop.main;

import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.proshop.model.Bill;
import com.proshop.model.Product;
import com.proshop.model.WholesalerPurchase;
import com.toedter.calendar.JDateChooser;

public class NewBillForm extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(NewBillForm.class.getName());
    private JDateChooser purchaseDateChooser;
    private JTextField shippingChargesField;
    private JTextField billAmountField;
	// Color scheme
    private static final Color BACKGROUND_COLOR = new Color(33, 33, 33);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color SHADOW_COLOR = new Color(224, 224, 224);
    private static final Color BACK_COLOR = new Color(120, 120, 120);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);

    public NewBillForm(WholesalerPurchase wholesaler, Map<Bill, java.util.List<Product>> billProducts,
                       CardLayout cardLayout, JPanel mainContentPanel) {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SHADOW_COLOR, 1),
            new EmptyBorder(10, 10, 10, 10)
        ));

        // Title label
        JLabel titleLabel = new JLabel("New Bill for " + wholesaler.getWholesalerName(), SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(SUCCESS_COLOR);
        titleLabel.setBackground(BACKGROUND_COLOR);
        titleLabel.setOpaque(true);
        titleLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
        add(titleLabel, BorderLayout.NORTH);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(BACKGROUND_COLOR);
        formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.insets = new Insets(10, 10, 10, 10);
        formGbc.anchor = GridBagConstraints.WEST;

        // Purchase Date
        purchaseDateChooser = new JDateChooser();
        purchaseDateChooser.setFont(new Font("Arial", Font.PLAIN, 16));
        purchaseDateChooser.setForeground(TEXT_COLOR);
        purchaseDateChooser.setBackground(BACK_COLOR);
        purchaseDateChooser.setPreferredSize(new Dimension(200, 30));
        purchaseDateChooser.setDate(java.util.Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        JLabel purchaseDateLabel = new JLabel("Purchase Date:");
        purchaseDateLabel.setFont(new Font("Arial", Font.BOLD, 16));
        purchaseDateLabel.setForeground(TEXT_COLOR);
        formGbc.gridx = 0;
        formGbc.gridy = 0;
        formPanel.add(purchaseDateLabel, formGbc);
        formGbc.gridx = 1;
        formPanel.add(purchaseDateChooser, formGbc);

        // Shipping Charges
        shippingChargesField = new JTextField(15);
        shippingChargesField.setFont(new Font("Arial", Font.PLAIN, 16));
        shippingChargesField.setForeground(TEXT_COLOR);
        shippingChargesField.setBackground(BACK_COLOR);
        shippingChargesField.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
        JLabel shippingChargesLabel = new JLabel("Shipping Charges:");
        shippingChargesLabel.setFont(new Font("Arial", Font.BOLD, 16));
        shippingChargesLabel.setForeground(TEXT_COLOR);
        formGbc.gridx = 0;
        formGbc.gridy = 1;
        formPanel.add(shippingChargesLabel, formGbc);
        formGbc.gridx = 1;
        formPanel.add(shippingChargesField, formGbc);

        // Bill Amount (non-editable)
        billAmountField = new JTextField(15);
        billAmountField.setFont(new Font("Arial", Font.PLAIN, 16));
        billAmountField.setForeground(TEXT_COLOR);
        billAmountField.setBackground(BACK_COLOR);
        billAmountField.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
        billAmountField.setEditable(false);
        billAmountField.setText("Calculated after adding products");
        JLabel billAmountLabel = new JLabel("Bill Amount:");
        billAmountLabel.setFont(new Font("Arial", Font.BOLD, 16));
        billAmountLabel.setForeground(TEXT_COLOR);
        formGbc.gridx = 0;
        formGbc.gridy = 2;
        formPanel.add(billAmountLabel, formGbc);
        formGbc.gridx = 1;
        formPanel.add(billAmountField, formGbc);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JButton submitButton = createStyledButton("Next");
        JButton backButton = createStyledButton("Back");

        submitButton.addActionListener(e -> {
            try {
                LocalDate purchaseDate = purchaseDateChooser.getDate() != null
                    ? purchaseDateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    : LocalDate.now();
                String shippingCharges = shippingChargesField.getText().trim();

                if (shippingCharges.isEmpty()) {
                    shippingCharges = "0";
                } else {
                    try {
                        BigDecimal shippingValue = new BigDecimal(shippingCharges);
                        if (shippingValue.compareTo(BigDecimal.ZERO) < 0) {
                            JOptionPane.showMessageDialog(this, "Shipping charges cannot be negative.", "Error", JOptionPane.ERROR_MESSAGE);
                            LOGGER.warning("Negative shipping charges entered: " + shippingCharges);
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Invalid shipping charges.", "Error", JOptionPane.ERROR_MESSAGE);
                        LOGGER.log(Level.WARNING, "Invalid shipping charges: " + shippingCharges, ex);
                        return;
                    }
                }

                LOGGER.info("Navigating to AddProductForm with purchaseDate: " + purchaseDate + ", shippingCharges: " + shippingCharges);
                mainContentPanel.add(new AddProductForm(wholesaler, billProducts, purchaseDate, shippingCharges, shippingCharges, cardLayout, mainContentPanel), "ADD_PRODUCT");
                cardLayout.show(mainContentPanel, "ADD_PRODUCT");
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error navigating to AddProductForm: ", ex);
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        backButton.addActionListener(e -> {
            LOGGER.info("Navigating back to WholesalerActionView");
            mainContentPanel.add(new WholesalerActionView(wholesaler, billProducts, cardLayout, mainContentPanel), "WHOLESALER_ACTION");
            cardLayout.show(mainContentPanel, "WHOLESALER_ACTION");
        });

        buttonPanel.add(submitButton);
        buttonPanel.add(backButton);
        formGbc.gridx = 0;
        formGbc.gridy = 3;
        formGbc.gridwidth = 2;
        formGbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(buttonPanel, formGbc);

        add(formPanel, BorderLayout.CENTER);
        setVisible(true);
        revalidate();
        repaint();
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setForeground(TEXT_COLOR);
        button.setBackground(BACK_COLOR);
        button.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(160, 45));
        return button;
    }
}