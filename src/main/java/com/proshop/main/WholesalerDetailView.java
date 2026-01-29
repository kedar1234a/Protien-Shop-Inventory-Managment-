package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.proshop.model.Bill;
import com.proshop.model.Payment;
import com.proshop.model.Product;
import com.proshop.model.WholesalerPurchase;
import com.toedter.calendar.JDateChooser;

public class WholesalerDetailView extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final WholesalerPurchase wholesaler;
	private final Bill bill;
	private final CardLayout cardLayout;
	private final JPanel mainContentPanel;
	private DefaultTableModel productModel;
	private DefaultTableModel paymentModel;
	private JLabel paidAmountLabel;
	private JLabel totalBillLabel;
	private JLabel pendingAmountLabel;
	private JTable productTable;
	private JTable paymentTable;

	// Color scheme to match WholesalerProductView
	private static final Color BACKGROUND_COLOR = new Color(33, 33, 33); // Dark gray (#212121)
	private static final Color TEXT_COLOR = Color.WHITE; // White
	private static final Color SUCCESS_COLOR = new Color(102, 187, 106); // Light Green (#66BB6A)
	private static final Color ACCENT_COLOR = new Color(255, 215, 0); // Yellow (#FFD700)
	private static final Color PENDING_COLOR = new Color(239, 83, 80); // Red (#EF5350)
	private static final Color SHADOW_COLOR = new Color(224, 224, 224); // Very light gray for border (#E0E0E0)
	private static final Color STAT_CARD_COLOR = new Color(50, 50, 50); // Dark gray for odd columns (#323232)
	private static final Color FAINT_ROW_COLOR = new Color(66, 66, 66); // Light gray for even columns (#424242)
	private static final Color UPDATE_COLOR = new Color(33, 150, 243); // Blue (#2196F3)
	private static final Color BACK_COLOR = new Color(120, 120, 120); // Gray (#787878)
	private static final Color CANCEL_COLOR = new Color(239, 83, 80); // Red (#EF5350)

	public WholesalerDetailView(WholesalerPurchase wholesaler, Bill bill, CardLayout cardLayout,
			JPanel mainContentPanel) {
		this.wholesaler = wholesaler;
		this.bill = bill;
		this.cardLayout = cardLayout;
		this.mainContentPanel = mainContentPanel;
		setLayout(new BorderLayout());
		setBackground(BACKGROUND_COLOR);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1),
				new EmptyBorder(10, 10, 10, 10)));

		// Info panel with two-column layout
		JPanel infoPanel = new JPanel(new GridBagLayout());
		infoPanel.setBackground(BACKGROUND_COLOR);
		infoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.anchor = GridBagConstraints.WEST;

		// Left half: Wholesaler Name, Phone, Address
		JLabel wholesalerNameLabel = new JLabel(
				"Wholesaler: " + (wholesaler.getWholesalerName() != null ? wholesaler.getWholesalerName() : "N/A"));
		wholesalerNameLabel.setFont(new Font("Arial", Font.BOLD, 16));
		wholesalerNameLabel.setForeground(TEXT_COLOR);
		gbc.gridx = 0;
		gbc.gridy = 0;
		infoPanel.add(wholesalerNameLabel, gbc);

		JLabel phoneLabel = new JLabel("Phone: " + (wholesaler.getPhoneNo() != null ? wholesaler.getPhoneNo() : "N/A"));
		phoneLabel.setFont(new Font("Arial", Font.BOLD, 16));
		phoneLabel.setForeground(TEXT_COLOR);
		gbc.gridy = 1;
		infoPanel.add(phoneLabel, gbc);

		JLabel addressLabel = new JLabel(
				"Address: " + (wholesaler.getAddress() != null ? wholesaler.getAddress() : "N/A"));
		addressLabel.setFont(new Font("Arial", Font.BOLD, 16));
		addressLabel.setForeground(TEXT_COLOR);
		gbc.gridy = 2;
		infoPanel.add(addressLabel, gbc);

		// Right half: Total Bill Amount, Paid Amount, Pending Amount
		BigDecimal totalBillAmount = DatabaseUtils.fetchTotalBillAmountForBill(bill.getId());
		totalBillLabel = new JLabel("Total Bill: $" + String.format("%.2f", totalBillAmount));
		totalBillLabel.setFont(new Font("Arial", Font.BOLD, 16));
		totalBillLabel.setForeground(ACCENT_COLOR);
		gbc.gridx = 1;
		gbc.gridy = 0;
		infoPanel.add(totalBillLabel, gbc);

		DatabaseUtils.PaymentSummary paymentSummary = DatabaseUtils.fetchPaymentsForBill(bill.getId());
		BigDecimal totalPaidAmount = paymentSummary.getTotalPaidAmount() != null ? paymentSummary.getTotalPaidAmount()
				: BigDecimal.ZERO;
		paidAmountLabel = new JLabel("Paid: $" + String.format("%.2f", totalPaidAmount));
		paidAmountLabel.setFont(new Font("Arial", Font.BOLD, 16));
		paidAmountLabel.setForeground(SUCCESS_COLOR);
		gbc.gridy = 1;
		infoPanel.add(paidAmountLabel, gbc);

		BigDecimal pendingAmount = totalBillAmount.subtract(totalPaidAmount);
		pendingAmountLabel = new JLabel("Pending: $" + String.format("%.2f", pendingAmount));
		pendingAmountLabel.setFont(new Font("Arial", Font.BOLD, 16));
		pendingAmountLabel.setForeground(PENDING_COLOR);
		gbc.gridy = 2;
		infoPanel.add(pendingAmountLabel, gbc);

		add(infoPanel, BorderLayout.NORTH);

		// Tables panel
		JPanel tablesPanel = new JPanel(new GridLayout(2, 1, 0, 20));
		tablesPanel.setBackground(BACKGROUND_COLOR);
		tablesPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

		productModel = new DefaultTableModel(
				new String[] { "PRODUCT NAME", "QUANTITY", "EXPIRY", "PER PIECE RATE", "TOTAL" }, 0) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		List<Product> products = DatabaseUtils.fetchProductsForBill(bill.getId(), wholesaler.getId());
		for (Product product : products) {
			productModel.addRow(new Object[] { product.getProductName() != null ? product.getProductName() : "N/A",
					product.getQuantity(), product.getExpiry() != null ? product.getExpiry().toString() : "N/A",
					product.getPerPieceRate() != null ? String.format("$%.2f", product.getPerPieceRate()) : "$0.00",
					product.getTotal() != null ? String.format("$%.2f", product.getTotal()) : "$0.00" });
		}
		productTable = new JTable(productModel) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component c = super.prepareRenderer(renderer, row, column);
				if (!isRowSelected(row)) {
					c.setBackground(column % 2 == 0 ? STAT_CARD_COLOR : FAINT_ROW_COLOR);
					c.setForeground(TEXT_COLOR);
				}
				return c;
			}
		};
		productTable.setFont(new Font("Arial", Font.PLAIN, 14));
		productTable.setRowHeight(30);
		productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		productTable.setBackground(BACKGROUND_COLOR);
		productTable.setForeground(TEXT_COLOR);
		productTable.setGridColor(SHADOW_COLOR);
		productTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
		productTable.getTableHeader().setBackground(STAT_CARD_COLOR);
		productTable.getTableHeader().setForeground(TEXT_COLOR);
		JScrollPane productScrollPane = new JScrollPane(productTable);
		productScrollPane.setBackground(BACKGROUND_COLOR);
		productScrollPane.getViewport().setBackground(BACKGROUND_COLOR);
		productScrollPane
				.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(SHADOW_COLOR), "Products"));
		tablesPanel.add(productScrollPane);

		paymentModel = new DefaultTableModel(new String[] { "PAID DATE", "PAID AMOUNT", "PENDING AMOUNT" }, 0) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		List<Payment> payments = paymentSummary.getPayments();
		for (Payment payment : payments) {
			paymentModel.addRow(new Object[] { payment.getPaidDate() != null ? payment.getPaidDate().toString() : "N/A",
					payment.getPaidAmount() != null ? String.format("$%.2f", payment.getPaidAmount()) : "$0.00",
					payment.getPendingAmount() != null ? String.format("$%.2f", payment.getPendingAmount())
							: "$0.00" });
		}
		paymentTable = new JTable(paymentModel) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component c = super.prepareRenderer(renderer, row, column);
				if (!isRowSelected(row)) {
					c.setBackground(column % 2 == 0 ? STAT_CARD_COLOR : FAINT_ROW_COLOR);
					c.setForeground(TEXT_COLOR);
				}
				return c;
			}
		};
		paymentTable.setFont(new Font("Arial", Font.PLAIN, 14));
		paymentTable.setRowHeight(30);
		paymentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		paymentTable.setBackground(BACKGROUND_COLOR);
		paymentTable.setForeground(TEXT_COLOR);
		paymentTable.setGridColor(SHADOW_COLOR);
		paymentTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
		paymentTable.getTableHeader().setBackground(STAT_CARD_COLOR);
		paymentTable.getTableHeader().setForeground(TEXT_COLOR);
		JScrollPane paymentScrollPane = new JScrollPane(paymentTable);
		paymentScrollPane.setBackground(BACKGROUND_COLOR);
		paymentScrollPane.getViewport().setBackground(BACKGROUND_COLOR);
		paymentScrollPane
				.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(SHADOW_COLOR), "Payments"));
		tablesPanel.add(paymentScrollPane);

		add(tablesPanel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.setBackground(BACKGROUND_COLOR);
		JButton addPaymentButton = createStyledButton("Add Payment", SUCCESS_COLOR);
		JButton addProductButton = createStyledButton("Add Product", SUCCESS_COLOR);
		JButton updateButton = createStyledButton("Update", UPDATE_COLOR);
		JButton deleteButton = createStyledButton("Delete Product", CANCEL_COLOR);
		JButton backButton = createStyledButton("Back", BACK_COLOR);

		addPaymentButton.addActionListener(e -> showAddPaymentDialog());
		addProductButton.addActionListener(e -> showAddProductDialog());
		updateButton.addActionListener(e -> showUpdateDialog());
		deleteButton.addActionListener(e -> deleteProduct());
		backButton.addActionListener(e -> {
			mainContentPanel.add(new WholesalerDateCardView(wholesaler, cardLayout, mainContentPanel),
					"WHOLESALER_DATE_CARD");
			cardLayout.show(mainContentPanel, "WHOLESALER_DATE_CARD");
		});

		buttonPanel.add(addPaymentButton);
		buttonPanel.add(addProductButton);
		buttonPanel.add(updateButton);
		buttonPanel.add(deleteButton);
		buttonPanel.add(backButton);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JButton createStyledButton(String text, Color bgColor) {
		JButton button = new JButton(text);
		button.setFont(new Font("Arial", Font.BOLD, 16));
		button.setForeground(TEXT_COLOR);
		button.setBackground(bgColor);
		button.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
		button.setFocusPainted(false);
		return button;
	}

	private void deleteProduct() {
	    int selectedProductRow = productTable.getSelectedRow();
	    if (selectedProductRow == -1) {
	        JOptionPane.showMessageDialog(this, "Please select a product to delete.", "Error",
	                JOptionPane.ERROR_MESSAGE);
	        return;
	    }

	    int confirm = JOptionPane.showConfirmDialog(this,
	            "Are you sure you want to delete this product?\n" +
	            "This will remove the product and all its stock units permanently.",
	            "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

	    if (confirm == JOptionPane.YES_OPTION) {
	        try {
	            // Get the product from the current list
	            Product product = DatabaseUtils.fetchProductsForBill(bill.getId(), wholesaler.getId())
	                    .get(selectedProductRow);

	            // DELETE: Product + Stock + BillProduct link
	            DatabaseUtils.deleteProduct(product.getId(), bill.getId());

	            // Update bill amount
	            BigDecimal newBillAmount = DatabaseUtils.fetchTotalBillAmountForBill(bill.getId())
	                    .add(bill.getShippingCharges() != null ? bill.getShippingCharges() : BigDecimal.ZERO);
	            bill.setBillAmount(newBillAmount);
	            DatabaseUtils.updateBill(bill);

	            // Refresh UI
	            refreshProductData();
	            refreshWholesalerData();

	            JOptionPane.showMessageDialog(this, "Product and all its units deleted successfully.", "Success",
	                    JOptionPane.INFORMATION_MESSAGE);

	        } catch (RuntimeException ex) {
	            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error",
	                    JOptionPane.ERROR_MESSAGE);
	        }
	    }
	}
	private void showAddPaymentDialog() {
		JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Add Payment",
				Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setLayout(new BorderLayout());
		dialog.setSize(500, 300);
		dialog.setLocationRelativeTo(this);
		dialog.setBackground(BACKGROUND_COLOR);

		JPanel formPanel = new JPanel(new GridBagLayout());
		formPanel.setBackground(BACKGROUND_COLOR);
		formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;

		JTextField paidAmountField = UIUtils.createStyledTextField(30);
		paidAmountField.setPreferredSize(new Dimension(200, 30));
		UIUtils.addFormField(formPanel, gbc, "Paid Amount:", paidAmountField, 0, 0);

		JDateChooser paidDateField = UIUtils.createStyledDateChooser();
		paidDateField.setDate(java.sql.Date.valueOf(LocalDate.now()));
		paidDateField.setPreferredSize(new Dimension(200, 30));
		UIUtils.addFormField(formPanel, gbc, "Paid Date:", paidDateField, 1, 0);

		BigDecimal currentPendingAmount = DatabaseUtils.getCurrentPendingAmount(bill.getId());
		JLabel pendingAmountLabel = new JLabel(String.format("$%.2f", currentPendingAmount));
		pendingAmountLabel.setFont(new Font("Arial", Font.BOLD, 16));
		pendingAmountLabel.setForeground(TEXT_COLOR);
		UIUtils.addFormField(formPanel, gbc, "Pending Amount:", pendingAmountLabel, 2, 0);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.setBackground(BACKGROUND_COLOR);
		JButton submitButton = createStyledButton("Submit", SUCCESS_COLOR);
		JButton cancelButton = createStyledButton("Cancel", CANCEL_COLOR);

		submitButton.addActionListener(e -> {
			try {
				BigDecimal paidAmount = new BigDecimal(paidAmountField.getText().trim());
				if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
					JOptionPane.showMessageDialog(dialog, "Paid amount must be greater than zero.", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (paidAmount.compareTo(currentPendingAmount) > 0) {
					JOptionPane.showMessageDialog(dialog, "Paid amount cannot exceed pending amount.", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				java.util.Date selectedDate = paidDateField.getDate();
				if (selectedDate == null) {
					JOptionPane.showMessageDialog(dialog, "Please select a valid date.", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				LocalDate paidDate = selectedDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

				DatabaseUtils.addPayment(bill.getId(), paidAmount, paidDate, currentPendingAmount);
				refreshPaymentData();
				dialog.dispose();
				JOptionPane.showMessageDialog(this, "Payment added successfully.", "Success",
						JOptionPane.INFORMATION_MESSAGE);
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(dialog, "Invalid amount format.", "Error", JOptionPane.ERROR_MESSAGE);
			} catch (RuntimeException ex) {
				JOptionPane.showMessageDialog(dialog, "Error adding payment: " + ex.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			} catch (SQLException e1) {
				JOptionPane.showMessageDialog(dialog, "Insufficient Shop Balance: " + e1.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				e1.printStackTrace();
			}
		});

		cancelButton.addActionListener(e -> dialog.dispose());

		buttonPanel.add(submitButton);
		buttonPanel.add(cancelButton);

		dialog.add(formPanel, BorderLayout.CENTER);
		dialog.add(buttonPanel, BorderLayout.SOUTH);
		dialog.setVisible(true);
	}

	private void showUpdateDialog() {
		int selectedProductRow = productTable.getSelectedRow();
		int selectedPaymentRow = paymentTable.getSelectedRow();

		if (selectedProductRow == -1 && selectedPaymentRow == -1) {
			JOptionPane.showMessageDialog(this, "Please select a product or payment to update.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Update",
				Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setLayout(new BorderLayout());
		dialog.setSize(500, 400);
		dialog.setLocationRelativeTo(this);
		dialog.setBackground(BACKGROUND_COLOR);

		JPanel formPanel = new JPanel(new GridBagLayout());
		formPanel.setBackground(BACKGROUND_COLOR);
		formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;

		if (selectedProductRow != -1) {
			String productName = (String) productModel.getValueAt(selectedProductRow, 0);
			Integer quantity = (Integer) productModel.getValueAt(selectedProductRow, 1);
			String expiryStr = (String) productModel.getValueAt(selectedProductRow, 2);
			String perPieceRateStr = (String) productModel.getValueAt(selectedProductRow, 3);
			BigDecimal perPieceRate = perPieceRateStr != null ? new BigDecimal(perPieceRateStr.replace("$", ""))
					: BigDecimal.ZERO;
			JTextField nameField = UIUtils.createStyledTextField(30);
			nameField.setText(productName);
			nameField.setPreferredSize(new Dimension(200, 30));
			UIUtils.addFormField(formPanel, gbc, "Product Name:", nameField, 0, 0);

			JTextField quantityField = UIUtils.createStyledTextField(30);
			quantityField.setText(quantity.toString());
			quantityField.setPreferredSize(new Dimension(200, 30));
			UIUtils.addFormField(formPanel, gbc, "Quantity:", quantityField, 1, 0);

			JDateChooser expiryField = UIUtils.createStyledDateChooser();
			if (!expiryStr.equals("N/A")) {
				expiryField.setDate(java.sql.Date.valueOf(LocalDate.parse(expiryStr)));
			}
			expiryField.setPreferredSize(new Dimension(200, 30));
			UIUtils.addFormField(formPanel, gbc, "Expiry Date:", expiryField, 2, 0);

			JTextField rateField = UIUtils.createStyledTextField(30);
			rateField.setText(perPieceRate.toString());
			rateField.setPreferredSize(new Dimension(200, 30));
			UIUtils.addFormField(formPanel, gbc, "Per Piece Rate:", rateField, 3, 0);

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			buttonPanel.setBackground(BACKGROUND_COLOR);
			JButton submitButton = createStyledButton("Submit", SUCCESS_COLOR);
			JButton cancelButton = createStyledButton("Cancel", CANCEL_COLOR);

			submitButton.addActionListener(e -> {
				try {
					String newName = nameField.getText().trim();
					if (newName.isEmpty()) {
						JOptionPane.showMessageDialog(dialog, "Product name cannot be empty.", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					int newQuantity = Integer.parseInt(quantityField.getText().trim());
					if (newQuantity <= 0) {
						JOptionPane.showMessageDialog(dialog, "Quantity must be greater than zero.", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					BigDecimal newRate = new BigDecimal(rateField.getText().trim());
					if (newRate.compareTo(BigDecimal.ZERO) <= 0) {
						JOptionPane.showMessageDialog(dialog, "Per piece rate must be greater than zero.", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					LocalDate newExpiry = null;
					if (expiryField.getDate() != null) {
						newExpiry = expiryField.getDate().toInstant().atZone(java.time.ZoneId.systemDefault())
								.toLocalDate();
					}
					BigDecimal newTotal = newRate.multiply(BigDecimal.valueOf(newQuantity));

					Product product = DatabaseUtils.fetchProductsForBill(bill.getId(), wholesaler.getId())
							.get(selectedProductRow);
					product.setProductName(newName);
					product.setQuantity(newQuantity);
					product.setPerPieceRate(newRate);
					product.setExpiry(newExpiry);
					product.setTotal(newTotal);

					DatabaseUtils.updateProduct(product, bill);
					refreshProductData();
					dialog.dispose();
					JOptionPane.showMessageDialog(this, "Product updated successfully.", "Success",
							JOptionPane.INFORMATION_MESSAGE);
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(dialog, "Invalid number format.", "Error", JOptionPane.ERROR_MESSAGE);
				} catch (RuntimeException ex) {
					JOptionPane.showMessageDialog(dialog, "Error updating product: " + ex.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			});

			cancelButton.addActionListener(e -> dialog.dispose());

			buttonPanel.add(submitButton);
			buttonPanel.add(cancelButton);

			dialog.add(formPanel, BorderLayout.CENTER);
			dialog.add(buttonPanel, BorderLayout.SOUTH);
			dialog.setVisible(true);
		} else {
			String paidDateStr = (String) paymentModel.getValueAt(selectedPaymentRow, 0);
			String paidAmountStr = (String) paymentModel.getValueAt(selectedPaymentRow, 1);
			BigDecimal paidAmount = paidAmountStr != null ? new BigDecimal(paidAmountStr.replace("$", ""))
					: BigDecimal.ZERO;
			JTextField paidAmountField = UIUtils.createStyledTextField(30);
			paidAmountField.setText(paidAmount.toString());
			paidAmountField.setPreferredSize(new Dimension(200, 30));
			UIUtils.addFormField(formPanel, gbc, "Paid Amount:", paidAmountField, 0, 0);

			JDateChooser paidDateField = UIUtils.createStyledDateChooser();
			if (!paidDateStr.equals("N/A")) {
				paidDateField.setDate(java.sql.Date.valueOf(LocalDate.parse(paidDateStr)));
			}
			paidDateField.setPreferredSize(new Dimension(200, 30));
			UIUtils.addFormField(formPanel, gbc, "Paid Date:", paidDateField, 1, 0);

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			buttonPanel.setBackground(BACKGROUND_COLOR);
			JButton submitButton = createStyledButton("Submit", SUCCESS_COLOR);
			JButton cancelButton = createStyledButton("Cancel", CANCEL_COLOR);

			submitButton.addActionListener(e -> {
				try {
					BigDecimal newPaidAmount = new BigDecimal(paidAmountField.getText().trim());
					if (newPaidAmount.compareTo(BigDecimal.ZERO) <= 0) {
						JOptionPane.showMessageDialog(dialog, "Paid amount must be greater than zero.", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					java.util.Date selectedDate = paidDateField.getDate();
					if (selectedDate == null) {
						JOptionPane.showMessageDialog(dialog, "Please select a valid date.", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					LocalDate newPaidDate = selectedDate.toInstant().atZone(java.time.ZoneId.systemDefault())
							.toLocalDate();

					Payment payment = DatabaseUtils.fetchPaymentsForBill(bill.getId()).getPayments()
							.get(selectedPaymentRow);
					BigDecimal currentPendingAmount = DatabaseUtils.getCurrentPendingAmount(bill.getId());
					DatabaseUtils.updatePayment(payment.getId(), newPaidAmount, newPaidDate, currentPendingAmount);
					refreshPaymentData();
					dialog.dispose();
					JOptionPane.showMessageDialog(this, "Payment updated successfully.", "Success",
							JOptionPane.INFORMATION_MESSAGE);
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(dialog, "Invalid amount format.", "Error", JOptionPane.ERROR_MESSAGE);
				} catch (RuntimeException ex) {
					JOptionPane.showMessageDialog(dialog, "Error updating payment: " + ex.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			});

			cancelButton.addActionListener(e -> dialog.dispose());

			buttonPanel.add(submitButton);
			buttonPanel.add(cancelButton);

			dialog.add(formPanel, BorderLayout.CENTER);
			dialog.add(buttonPanel, BorderLayout.SOUTH);
			dialog.setVisible(true);
		}
	}

	private void showAddProductDialog() {
	    JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Add Product",
	            Dialog.ModalityType.APPLICATION_MODAL);
	    dialog.setLayout(new BorderLayout());
	    dialog.setSize(500, 400);
	    dialog.setLocationRelativeTo(this);
	    dialog.setBackground(BACKGROUND_COLOR);

	    JPanel formPanel = new JPanel(new GridBagLayout());
	    formPanel.setBackground(BACKGROUND_COLOR);
	    formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

	    GridBagConstraints gbc = new GridBagConstraints();
	    gbc.insets = new Insets(10, 10, 10, 10);
	    gbc.anchor = GridBagConstraints.WEST;
	    gbc.fill = GridBagConstraints.HORIZONTAL;
	    gbc.weightx = 1.0;

	    // === 1. PRODUCT NAME: JComboBox with Auto-Suggestion ===
	    JComboBox<String> nameComboBox = new JComboBox<>();
	    nameComboBox.setEditable(true);
	    nameComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
	    nameComboBox.setForeground(TEXT_COLOR);
	    nameComboBox.setBackground(BACK_COLOR);
	    nameComboBox.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
	    nameComboBox.setPreferredSize(new Dimension(200, 30));

	    // Auto-suggestion setup
	    JTextField editor = (JTextField) nameComboBox.getEditor().getEditorComponent();
	    editor.addKeyListener(new KeyAdapter() {
	        @Override
	        public void keyReleased(KeyEvent e) {
	            int code = e.getKeyCode();
	            if (code != KeyEvent.VK_UP && code != KeyEvent.VK_DOWN && code != KeyEvent.VK_ENTER) {
	                SwingUtilities.invokeLater(() -> {
	                    String text = editor.getText().trim();
	                    DatabaseUtils.updateProductComboBox(nameComboBox, text);
	                });
	            }
	        }
	    });

	    UIUtils.addFormField(formPanel, gbc, "Product Name:", nameComboBox, 0, 0);

	    // === 2. QUANTITY ===
	    JTextField quantityField = UIUtils.createStyledTextField(30);
	    quantityField.setPreferredSize(new Dimension(200, 30));
	    UIUtils.addFormField(formPanel, gbc, "Quantity:", quantityField, 1, 0);

	    // === 3. EXPIRY DATE ===
	    JDateChooser expiryField = UIUtils.createStyledDateChooser();
	    expiryField.setPreferredSize(new Dimension(200, 30));
	    UIUtils.addFormField(formPanel, gbc, "Expiry Date:", expiryField, 2, 0);

	    // === 4. PER PIECE RATE ===
	    JTextField rateField = UIUtils.createStyledTextField(30);
	    rateField.setPreferredSize(new Dimension(200, 30));
	    UIUtils.addFormField(formPanel, gbc, "Per Piece Rate:", rateField, 3, 0);

	    // === BUTTONS ===
	    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
	    buttonPanel.setBackground(BACKGROUND_COLOR);
	    JButton submitButton = createStyledButton("Submit", SUCCESS_COLOR);
	    JButton cancelButton = createStyledButton("Cancel", CANCEL_COLOR);

	    submitButton.addActionListener(e -> {
	        try {
	            // Get selected or typed product name
	            String productName = nameComboBox.getEditor().getItem().toString().trim();
	            if (productName.isEmpty()) {
	                JOptionPane.showMessageDialog(dialog, "Product name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
	                return;
	            }

	            int quantity;
	            try {
	                quantity = Integer.parseInt(quantityField.getText().trim());
	                if (quantity <= 0) {
	                    JOptionPane.showMessageDialog(dialog, "Quantity must be > 0.", "Error", JOptionPane.ERROR_MESSAGE);
	                    return;
	                }
	            } catch (NumberFormatException ex) {
	                JOptionPane.showMessageDialog(dialog, "Invalid quantity.", "Error", JOptionPane.ERROR_MESSAGE);
	                return;
	            }

	            BigDecimal perPieceRate;
	            try {
	                perPieceRate = new BigDecimal(rateField.getText().trim());
	                if (perPieceRate.compareTo(BigDecimal.ZERO) <= 0) {
	                    JOptionPane.showMessageDialog(dialog, "Rate must be > 0.", "Error", JOptionPane.ERROR_MESSAGE);
	                    return;
	                }
	            } catch (NumberFormatException ex) {
	                JOptionPane.showMessageDialog(dialog, "Invalid rate.", "Error", JOptionPane.ERROR_MESSAGE);
	                return;
	            }

	            LocalDate expiry = null;
	            if (expiryField.getDate() != null) {
	                expiry = expiryField.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
	            }

	            BigDecimal total = perPieceRate.multiply(BigDecimal.valueOf(quantity));
	            Product product = new Product(null, productName, quantity, perPieceRate, expiry, wholesaler.getId());
	            product.setTotal(total);

	            // === CALL UPDATED METHOD WITH BILL DATE ===
	            DatabaseUtils.addProductToBill(bill.getId(), wholesaler.getId(), product, bill.getDate());

	            // Refresh UI
	            refreshProductData();
	            BigDecimal newBillAmount = DatabaseUtils.fetchTotalBillAmountForBill(bill.getId())
	                    .add(bill.getShippingCharges() != null ? bill.getShippingCharges() : BigDecimal.ZERO);
	            bill.setBillAmount(newBillAmount);
	            DatabaseUtils.updateBill(bill);
	            refreshWholesalerData();

	            dialog.dispose();
	            JOptionPane.showMessageDialog(this, "Product added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);

	        } catch (RuntimeException ex) {
	            JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
	        }
	    });

	    cancelButton.addActionListener(e -> dialog.dispose());
	    buttonPanel.add(submitButton);
	    buttonPanel.add(cancelButton);

	    dialog.add(formPanel, BorderLayout.CENTER);
	    dialog.add(buttonPanel, BorderLayout.SOUTH);
	    dialog.setVisible(true);
	}

	public void refreshPaymentData() {
		DatabaseUtils.PaymentSummary paymentSummary = DatabaseUtils.fetchPaymentsForBill(bill.getId());
		BigDecimal totalPaidAmount = paymentSummary.getTotalPaidAmount() != null ? paymentSummary.getTotalPaidAmount()
				: BigDecimal.ZERO;
		paidAmountLabel.setText("Paid: $" + String.format("%.2f", totalPaidAmount));

		BigDecimal totalBillAmount = DatabaseUtils.fetchTotalBillAmountForBill(bill.getId());
		totalBillLabel.setText("Total Bill: $" + String.format("%.2f", totalBillAmount));

		BigDecimal pendingAmount = totalBillAmount.subtract(totalPaidAmount);
		pendingAmountLabel.setText("Pending: $" + String.format("%.2f", pendingAmount));

		paymentModel.setRowCount(0);
		List<Payment> payments = paymentSummary.getPayments();
		for (Payment payment : payments) {
			paymentModel.addRow(new Object[] { payment.getPaidDate() != null ? payment.getPaidDate().toString() : "N/A",
					payment.getPaidAmount() != null ? String.format("$%.2f", payment.getPaidAmount()) : "$0.00",
					payment.getPendingAmount() != null ? String.format("$%.2f", payment.getPendingAmount())
							: "$0.00" });
		}
	}

	public void refreshProductData() {
		productModel.setRowCount(0);
		List<Product> products = DatabaseUtils.fetchProductsForBill(bill.getId(), wholesaler.getId());
		for (Product product : products) {
			productModel.addRow(new Object[] { product.getProductName() != null ? product.getProductName() : "N/A",
					product.getQuantity(), product.getExpiry() != null ? product.getExpiry().toString() : "N/A",
					product.getPerPieceRate() != null ? String.format("$%.2f", product.getPerPieceRate()) : "$0.00",
					product.getTotal() != null ? String.format("$%.2f", product.getTotal()) : "$0.00" });
		}

		BigDecimal totalBillAmount = DatabaseUtils.fetchTotalBillAmountForBill(bill.getId());
		totalBillLabel.setText("Total Bill: $" + String.format("%.2f", totalBillAmount));

		DatabaseUtils.PaymentSummary paymentSummary = DatabaseUtils.fetchPaymentsForBill(bill.getId());
		BigDecimal totalPaidAmount = paymentSummary.getTotalPaidAmount() != null ? paymentSummary.getTotalPaidAmount()
				: BigDecimal.ZERO;
		BigDecimal pendingAmount = totalBillAmount.subtract(totalPaidAmount);
		pendingAmountLabel.setText("Pending: $" + String.format("%.2f", pendingAmount));
	}

	public void refreshWholesalerData() {
		mainContentPanel.add(new WholesalerDetailView(wholesaler, bill, cardLayout, mainContentPanel),
				"WHOLESALER_DETAIL");
		cardLayout.show(mainContentPanel, "WHOLESALER_DETAIL");
	}
}