package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.proshop.model.GymWholesaler;

public class GymWholesalerActionView extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(GymWholesalerActionView.class.getName());

	public GymWholesalerActionView(GymWholesaler wholesaler, CardLayout cardLayout, JPanel mainContentPanel,
			GymWholesalerDAO dao, Runnable hideSidebar) {
		setLayout(new BorderLayout());
		setBackground(UIUtils.BACKGROUND_COLOR);

		// Button Panel (Vertical)
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		buttonPanel.setBackground(UIUtils.BACKGROUND_COLOR);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		JButton addProductButton = UIUtils.createStyledButton("Add Products");
		addProductButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
		addProductButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		JButton viewAllProductsButton = UIUtils.createStyledButton("View All Products");
		viewAllProductsButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
		viewAllProductsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		JButton viewByDateButton = UIUtils.createStyledButton("View Products By Date");
		viewByDateButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
		viewByDateButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		JButton paymentButton = UIUtils.createStyledButton("Payment");
		paymentButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
		paymentButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		JButton backButton = UIUtils.createStyledButton("Back");
		backButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
		backButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		buttonPanel.add(addProductButton);
		buttonPanel.add(Box.createVerticalStrut(15));
		buttonPanel.add(viewAllProductsButton);
		buttonPanel.add(Box.createVerticalStrut(15));
		buttonPanel.add(viewByDateButton);
		buttonPanel.add(Box.createVerticalStrut(15));
		buttonPanel.add(paymentButton);
		buttonPanel.add(Box.createVerticalStrut(15));
		buttonPanel.add(backButton);

		add(buttonPanel, BorderLayout.CENTER);

		addProductButton.addActionListener(e -> {
			try {
				hideSidebar.run();
				mainContentPanel.add(new GymWholesalerAddProductForm(wholesaler, cardLayout, mainContentPanel, dao),
						"ADD_PRODUCT");
				cardLayout.show(mainContentPanel, "ADD_PRODUCT");
			} catch (Exception ex) {
				LOGGER.severe("Error loading add product form: " + ex.getMessage());
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
						"Failed to load add product form. Please try again.", "Error", JOptionPane.ERROR_MESSAGE));
			}
		});

		viewAllProductsButton.addActionListener(e -> {
			try {
				hideSidebar.run();
				mainContentPanel.add(new GymWholesalerViewAllProducts(wholesaler, cardLayout, mainContentPanel, dao),
						"VIEW_ALL_PRODUCTS");
				cardLayout.show(mainContentPanel, "VIEW_ALL_PRODUCTS");
			} catch (Exception ex) {
				LOGGER.severe("Error loading view all products: " + ex.getMessage());
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
						"Failed to load product list. Please try again.", "Error", JOptionPane.ERROR_MESSAGE));
			}
		});

		viewByDateButton.addActionListener(e -> {
			try {
				hideSidebar.run();
				mainContentPanel.add(new GymWholesalerViewByDate(wholesaler, cardLayout, mainContentPanel, dao),
						"VIEW_PRODUCTS_BY_DATE");
				cardLayout.show(mainContentPanel, "VIEW_PRODUCTS_BY_DATE");
			} catch (Exception ex) {
				LOGGER.severe("Error loading view by date: " + ex.getMessage());
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
						"Failed to load products by date. Please try again.", "Error", JOptionPane.ERROR_MESSAGE));
			}
		});

		paymentButton.addActionListener(e -> {
			try {
				hideSidebar.run();
				mainContentPanel.add(new GymWholesalerPaymentForm(wholesaler, cardLayout, mainContentPanel, dao),
						"PAYMENT_FORM");
				cardLayout.show(mainContentPanel, "PAYMENT_FORM");
			} catch (Exception ex) {
				LOGGER.severe("Error loading payment form: " + ex.getMessage());
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
						"Failed to load payment form. Please try again.", "Error", JOptionPane.ERROR_MESSAGE));
			}
		});

		backButton.addActionListener(e -> {
			// Declare panelNames outside try block to ensure it's accessible in catch
			ArrayList<String> panelNames = new ArrayList<>();
			try {
				// Debug: Log CardLayout state and available panels
				Component[] components = mainContentPanel.getComponents();
				for (Component comp : components) {
					String name = comp.getName();
					if (name != null && !name.isEmpty()) {
						panelNames.add(name + " (" + comp.getClass().getSimpleName() + ")");
					}
				}
				LOGGER.info("Available panels in mainContentPanel: " + panelNames);
				LOGGER.info("Current CardLayout: " + cardLayout.toString());
				LOGGER.info("Target panel: GYM_WHOLESALER");
				LOGGER.info("Is this panel in layered pane? " + (getParent() instanceof JLayeredPane));

				// Hide the sidebar
				hideSidebar.run();

				// Ensure GYM_WHOLESALER panel exists
				boolean panelExists = false;
				for (Component comp : mainContentPanel.getComponents()) {
					if ("GYM_WHOLESALER".equals(comp.getName())) {
						panelExists = true;
						break;
					}
				}

				if (panelExists) {
					cardLayout.show(mainContentPanel, "GYM_WHOLESALER");
					LOGGER.info("Successfully navigated to GYM_WHOLESALER panel");
				} else {
					// Fallback to MainMenu or show error
					LOGGER.warning("GYM_WHOLESALER panel not found. Attempting fallback to MainMenu");
					boolean mainMenuExists = panelNames.stream().anyMatch(name -> name.startsWith("MainMenu"));
					if (mainMenuExists) {
						cardLayout.show(mainContentPanel, "MainMenu");
						LOGGER.info("Successfully navigated to MainMenu as fallback");
					} else {
//                        throw new IllegalStateException("Neither GYM_WHOLESALER nor MainMenu found. Available panels: " + panelNames);
					}
				}
			} catch (Exception ex) {
				LOGGER.severe("Error navigating back: " + ex.getMessage());
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
						"Failed to navigate back: " + ex.getMessage() + ". Available panels: " + panelNames,
						"Navigation Error", JOptionPane.ERROR_MESSAGE));
			}
		});
	}
}