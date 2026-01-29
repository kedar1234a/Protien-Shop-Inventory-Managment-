package com.proshop.main;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.proshop.connection.DBUtil;
import com.proshop.model.Bill;
import com.proshop.model.Product;
import com.proshop.model.WholesalerPurchase;

public class WholesalerCard extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// Color scheme to match Dashboard and SoldStockForm
    private static final Color BACKGROUND_COLOR = new Color(33, 33, 33); // Dark gray (#212121)
    private static final Color TEXT_COLOR = Color.WHITE; // White
    private static final Color SUCCESS_COLOR = new Color(102, 187, 106); // Light Green (#66BB6A)
    private static final Color ACCENT_COLOR = new Color(255, 215, 0); // Yellow (#FFD700)
    private static final Color PENDING_COLOR = new Color(239, 83, 80); // Red (#EF5350)
    private static final Color SHADOW_COLOR = new Color(224, 224, 224); // Very light gray for border (#E0E0E0)

    public WholesalerCard(WholesalerPurchase wholesaler, Map<Bill, List<Product>> billProducts,
                         CardLayout cardLayout, JPanel mainContentPanel) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BACKGROUND_COLOR);
        setPreferredSize(new Dimension(250, 250));
        setMaximumSize(new Dimension(250, 250));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SHADOW_COLOR, 1),
            new EmptyBorder(10, 10, 10, 10)
        ));

        // Add vertical glue for centering
        add(Box.createVerticalGlue());

        // Name
        JLabel nameLabel = new JLabel("Name: " + (wholesaler.getWholesalerName() != null ? wholesaler.getWholesalerName() : "N/A"));
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        nameLabel.setForeground(TEXT_COLOR);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(nameLabel);
        add(Box.createVerticalStrut(6));

        // Phone
        JLabel phoneLabel = new JLabel("Phone: " + (wholesaler.getPhoneNo() != null ? wholesaler.getPhoneNo() : "N/A"));
        phoneLabel.setFont(new Font("Arial", Font.BOLD, 16));
        phoneLabel.setForeground(TEXT_COLOR);
        phoneLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(phoneLabel);
        add(Box.createVerticalStrut(6));

        // Address
        JLabel addressLabel = new JLabel("Address: " + (wholesaler.getAddress() != null ? wholesaler.getAddress() : "N/A"));
        addressLabel.setFont(new Font("Arial", Font.BOLD, 16));
        addressLabel.setForeground(TEXT_COLOR);
        addressLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(addressLabel);
        add(Box.createVerticalStrut(6));

        // Fetch totals from database
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        // Query payment table for paid amount
        if (!billProducts.isEmpty()) {
            String billIds = billProducts.keySet().stream()
                    .map(bill -> String.valueOf(bill.getId()))
                    .collect(Collectors.joining(","));
            String paymentSql = "SELECT SUM(paidAmount) FROM payment WHERE billId IN (" + billIds + ")";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(paymentSql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    paidAmount = rs.getBigDecimal(1) != null ? rs.getBigDecimal(1) : BigDecimal.ZERO;
                }
            } catch (SQLException ex) {
                System.err.println("Error fetching payment totals: " + ex.getMessage());
            }
        }

        // Query product table for total amount
        String productSql = "SELECT SUM(total) FROM product WHERE wholesalerId = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(productSql)) {
            pstmt.setLong(1, wholesaler.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalAmount = rs.getBigDecimal(1) != null ? rs.getBigDecimal(1) : BigDecimal.ZERO;
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error fetching product total: " + ex.getMessage());
        }

        // Calculate pending amount
        BigDecimal pendingAmount = totalAmount.subtract(paidAmount);
        if (pendingAmount.compareTo(BigDecimal.ZERO) < 0) {
            pendingAmount = BigDecimal.ZERO; // Avoid negative pending amount
        }

        // Paid Amount
        JLabel paidLabel = new JLabel("Paid: ₹" + String.format("%.2f", paidAmount));
        paidLabel.setFont(new Font("Arial", Font.BOLD, 16));
        paidLabel.setForeground(SUCCESS_COLOR);
        paidLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(paidLabel);
        add(Box.createVerticalStrut(6));

        // Total Amount
        JLabel totalAmountLabel = new JLabel("Total: ₹" + String.format("%.2f", totalAmount));
        totalAmountLabel.setFont(new Font("Arial", Font.BOLD, 16));
        totalAmountLabel.setForeground(ACCENT_COLOR);
        totalAmountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(totalAmountLabel);
        add(Box.createVerticalStrut(6));

        // Total Pending Amount
        JLabel pendingAmountLabel = new JLabel("Pending: ₹" + String.format("%.2f", pendingAmount));
        pendingAmountLabel.setFont(new Font("Arial", Font.BOLD, 16));
        pendingAmountLabel.setForeground(PENDING_COLOR);
        pendingAmountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(pendingAmountLabel);
        add(Box.createVerticalGlue());

        // Click behavior
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JPanel actionView = new WholesalerActionView(wholesaler, billProducts, cardLayout, mainContentPanel);
                mainContentPanel.add(actionView, "WHOLESALER_ACTION");
                cardLayout.show(mainContentPanel, "WHOLESALER_ACTION");
            }
        });
    }
}