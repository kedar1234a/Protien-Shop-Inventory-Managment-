package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.proshop.model.Bill;
import com.proshop.model.Product;
import com.proshop.model.WholesalerPurchase;

public class WholesalerActionView extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public WholesalerActionView(WholesalerPurchase wholesaler, Map<Bill, List<Product>> billProducts,
                                CardLayout cardLayout, JPanel mainContentPanel) {
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);

        JLabel titleLabel = new JLabel(wholesaler.getWholesalerName().toUpperCase(), SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(40, 167, 69));
        titleLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
        add(titleLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        buttonPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JButton viewProductsButton = UIUtils.createStyledButton("View Products by Date");
        viewProductsButton.setMaximumSize(new Dimension(300, 50));
        JButton viewAllProductsButton = UIUtils.createStyledButton("View All Products");
        viewAllProductsButton.setMaximumSize(new Dimension(300, 50));
        JButton newBillButton = UIUtils.createStyledButton("New Bill");
        newBillButton.setMaximumSize(new Dimension(300, 50));
        JButton backButton = UIUtils.createStyledButton("Back");
        backButton.setMaximumSize(new Dimension(300, 50));

        buttonPanel.add(viewProductsButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(viewAllProductsButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(newBillButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(backButton);

        add(buttonPanel, BorderLayout.CENTER);

        viewProductsButton.addActionListener(e -> {
            mainContentPanel.add(new WholesalerDateCardView(wholesaler, cardLayout, mainContentPanel), "WHOLESALER_DATE_CARD");
            cardLayout.show(mainContentPanel, "WHOLESALER_DATE_CARD");
        });

        viewAllProductsButton.addActionListener(e -> {
            mainContentPanel.add(new WholesalerProductView(wholesaler, cardLayout, mainContentPanel), "WHOLESALER_PRODUCTS");
            cardLayout.show(mainContentPanel, "WHOLESALER_PRODUCTS");
        });

        newBillButton.addActionListener(e -> {
            mainContentPanel.add(new NewBillForm(wholesaler, billProducts, cardLayout, mainContentPanel), "NEW_BILL");
            cardLayout.show(mainContentPanel, "NEW_BILL");
        });

        backButton.addActionListener(e -> {
            mainContentPanel.add(new WholesalerForm(cardLayout, mainContentPanel), "WHOLESALER");
            cardLayout.show(mainContentPanel, "WHOLESALER");
        });
    }
}