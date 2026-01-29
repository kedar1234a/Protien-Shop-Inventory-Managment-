package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import com.proshop.model.BillDetails;
import com.toedter.calendar.JDateChooser;

public class BillDetailsForm extends JPanel {
	private static final long serialVersionUID = 1L;
	private JTextField shopAmountField, shopDescriptionField;
	private JDateChooser shopDateChooser;
	private JButton refreshButton, backButton;
	private CardLayout cardLayout;
	private JPanel mainContentPanel;
	private JPanel cardsPanel;
	private List<BillDetails> billDetailsList;
	private JLabel shopBalanceLabel;
	private BillDetailsDAO billDetailsDAO;

	// Color scheme
	private static final Color BACKGROUND_COLOR = new Color(33, 33, 33); // Dark gray (#212121)
	private static final Color TEXT_COLOR = Color.WHITE; // White (#FFFFFF)
	private static final Color SUCCESS_COLOR = new Color(76, 175, 80); // Green (#4CAF50)
	private static final Color ACCENT_COLOR = new Color(255, 215, 0); // Yellow (#FFD700)
	private static final Color SHADOW_COLOR = new Color(224, 224, 224); // Very light gray (#E0E0E0)
	private static final Color BACK_COLOR = new Color(120, 120, 120); // Gray (#787878)
	private static final Color LIGHT_GRAY = new Color(50, 50, 50); // Lighter gray for alternating rows
	private static final Color DARK_GRAY = new Color(33, 33, 33); // Darker gray for table background
	private static final Color RED_COLOR = new Color(255, 0, 0); // Red for Capital label

	private static final CardConfig[] CARD_CONFIGS = { new CardConfig("Rent", "rent", "othersDescription"),
			new CardConfig("Light Bill", "lightBill", "othersDescription"),
			new CardConfig("Maintenance", "maintenanceBill", "othersDescription"),
			new CardConfig("Salary", "salary", "othersDescription"),
			new CardConfig("Parcel Bill", "parcelBillAmount", "parcelBillDescription"),
			new CardConfig("Bank EMI", "bankEmi", "othersDescription"),
			new CardConfig("Others", "othersAmount", "othersDescription"),
			new CardConfig("Shop Amount", "shopAmount", "shop_description"), new CardConfig("Totals", "totals", null) };

	private static class CardConfig {
		String displayName, amountField, descriptionField;

		CardConfig(String displayName, String amountField, String descriptionField) {
			this.displayName = displayName;
			this.amountField = amountField;
			this.descriptionField = descriptionField;
		}
	}

	public BillDetailsForm(CardLayout cardLayout, JPanel mainContentPanel) {
		this.cardLayout = cardLayout;
		this.mainContentPanel = mainContentPanel;
		this.billDetailsList = new ArrayList<>();
		this.billDetailsDAO = new BillDetailsDAO();
		setLayout(new BorderLayout(3, 3));
		setBackground(BACKGROUND_COLOR);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1),
				new EmptyBorder(3, 3, 3, 3)));
		setPreferredSize(new Dimension(1366, 768));
		initComponents();
		loadData();
	}

	private void initComponents() {
		// Top panel
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBackground(BACKGROUND_COLOR);
		topPanel.setBorder(new EmptyBorder(3, 3, 3, 3));
		topPanel.setPreferredSize(new Dimension(0, 45));

		JLabel titleLabel = new JLabel("Bill Details Management", SwingConstants.CENTER);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
		titleLabel.setForeground(ACCENT_COLOR);
		topPanel.add(titleLabel, BorderLayout.CENTER);

		shopBalanceLabel = new JLabel("Shop Balance: 0.00");
		shopBalanceLabel.setFont(new Font("Arial", Font.BOLD, 14));
		shopBalanceLabel.setForeground(SUCCESS_COLOR);
		shopBalanceLabel.setBorder(new EmptyBorder(0, 5, 5, 0));
		topPanel.add(shopBalanceLabel, BorderLayout.WEST);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 3));
		buttonPanel.setBackground(BACKGROUND_COLOR);
		refreshButton = createStyledButton("Refresh");
		backButton = createStyledButton("Back");
		buttonPanel.add(refreshButton);
		buttonPanel.add(backButton);
		topPanel.add(buttonPanel, BorderLayout.EAST);
		add(topPanel, BorderLayout.NORTH);

		// Cards panel with scroll pane
		cardsPanel = new JPanel(new GridBagLayout());
		cardsPanel.setBackground(BACKGROUND_COLOR);
		JScrollPane cardsScrollPane = new JScrollPane(cardsPanel);
		cardsScrollPane.setBackground(DARK_GRAY);
		cardsScrollPane.getViewport().setBackground(DARK_GRAY);
		cardsScrollPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		cardsScrollPane.setPreferredSize(new Dimension(1366, 710));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;

		// First row: Rent, Light Bill, Maintenance
		gbc.gridy = 0;
		gbc.gridx = 0;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[0].displayName, CARD_CONFIGS[0].amountField,
				CARD_CONFIGS[0].descriptionField), gbc);
		gbc.gridx = 1;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[1].displayName, CARD_CONFIGS[1].amountField,
				CARD_CONFIGS[1].descriptionField), gbc);
		gbc.gridx = 2;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[2].displayName, CARD_CONFIGS[2].amountField,
				CARD_CONFIGS[2].descriptionField), gbc);

		// Second row: Salary, Parcel Bill, Bank EMI
		gbc.gridy = 1;
		gbc.gridx = 0;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[3].displayName, CARD_CONFIGS[3].amountField,
				CARD_CONFIGS[3].descriptionField), gbc);
		gbc.gridx = 1;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[4].displayName, CARD_CONFIGS[4].amountField,
				CARD_CONFIGS[4].descriptionField), gbc);
		gbc.gridx = 2;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[5].displayName, CARD_CONFIGS[5].amountField,
				CARD_CONFIGS[5].descriptionField), gbc);

		// Third row: Others, Shop Amount, Totals
		gbc.gridy = 2;
		gbc.gridx = 0;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[6].displayName, CARD_CONFIGS[6].amountField,
				CARD_CONFIGS[6].descriptionField), gbc);
		gbc.gridx = 1;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[7].displayName, CARD_CONFIGS[7].amountField,
				CARD_CONFIGS[7].descriptionField), gbc);
		gbc.gridx = 2;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[8].displayName, CARD_CONFIGS[8].amountField,
				CARD_CONFIGS[8].descriptionField), gbc);

		add(cardsScrollPane, BorderLayout.CENTER);

		refreshButton.addActionListener(e -> {
			clearFields();
			loadData();
		});
		backButton.addActionListener(e -> cardLayout.show(mainContentPanel, "WELCOME"));
	}

	private void addShopAmount(ActionEvent e) {
		String shopAmountText = shopAmountField.getText().trim();
		String shopDescription = shopDescriptionField.getText().trim();
		Date shopDate = shopDateChooser.getDate();

		if (shopAmountText.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please enter a shop amount.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (shopDescription.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Please enter a shop amount description.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (shopDate == null) {
			JOptionPane.showMessageDialog(this, "Please select a date.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			double amount = Double.parseDouble(shopAmountText);
			if (amount < 0) {
				JOptionPane.showMessageDialog(this, "Shop amount cannot be negative.", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			billDetailsDAO.addShopAmount(amount, shopDescription, shopDate);
			JOptionPane.showMessageDialog(this, "Shop amount added successfully!", "Success",
					JOptionPane.INFORMATION_MESSAGE);
			shopAmountField.setText("");
			shopDescriptionField.setText("");
			shopDateChooser.setDate(null);
			loadData();
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(this, "Invalid shop amount format.", "Error", JOptionPane.ERROR_MESSAGE);
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private class CardPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private JTextField amountField, descriptionField;
		private JDateChooser dateChooser;
		private JButton addButton;
		private String displayName;

		public CardPanel(String displayName, String amountFieldName, String descriptionFieldName) {
			this.displayName = displayName;
			setLayout(new GridBagLayout());
			setBackground(BACKGROUND_COLOR);
			setPreferredSize(new Dimension(400, 150));
			setMaximumSize(new Dimension(400, 150));
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1),
					new EmptyBorder(3, 3, 3, 3)));
			setCursor(new Cursor(Cursor.HAND_CURSOR));

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(3, 3, 3, 3);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.WEST;

			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 2;
			JLabel label = new JLabel(displayName, SwingConstants.CENTER);
			label.setFont(new Font("Arial", Font.BOLD, 14));
			label.setForeground(ACCENT_COLOR);
			add(label, gbc);

			if (!amountFieldName.equals("totals")) {
				// Amount field
				gbc.gridwidth = 1;
				gbc.gridy = 1;
				gbc.gridx = 0;
				JLabel amountLabel = new JLabel("Amount:");
				amountLabel.setFont(new Font("Arial", Font.BOLD, 12));
				amountLabel.setForeground(TEXT_COLOR);
				add(amountLabel, gbc);

				gbc.gridx = 1;
				if (amountFieldName.equals("shopAmount")) {
					shopAmountField = createStyledTextField(12, "Enter shop amount");
					add(shopAmountField, gbc);
				} else {
					amountField = createStyledTextField(12, "Enter " + displayName.toLowerCase() + " amount");
					add(amountField, gbc);
				}

				// Description field
				gbc.gridy = 2;
				gbc.gridx = 0;
				JLabel descLabel = new JLabel("Description:");
				descLabel.setFont(new Font("Arial", Font.BOLD, 12));
				descLabel.setForeground(TEXT_COLOR);
				add(descLabel, gbc);

				gbc.gridx = 1;
				if (amountFieldName.equals("shopAmount")) {
					shopDescriptionField = createStyledTextField(12, "Enter shop description");
					add(shopDescriptionField, gbc);
				} else {
					descriptionField = createStyledTextField(12, "Enter " + displayName.toLowerCase() + " description");
					add(descriptionField, gbc);
				}

				// Date field
				gbc.gridy = 3;
				gbc.gridx = 0;
				JLabel dateLabel = new JLabel("Date:");
				dateLabel.setFont(new Font("Arial", Font.BOLD, 12));
				dateLabel.setForeground(TEXT_COLOR);
				add(dateLabel, gbc);

				gbc.gridx = 1;
				if (amountFieldName.equals("shopAmount")) {
					shopDateChooser = createStyledDateChooser();
					add(shopDateChooser, gbc);
				} else {
					dateChooser = createStyledDateChooser();
					add(dateChooser, gbc);
				}

				// Add button
				gbc.gridy = 4;
				gbc.gridx = 0;
				gbc.gridwidth = 2;
				gbc.anchor = GridBagConstraints.CENTER;
				addButton = createStyledButton("Add");
				if (amountFieldName.equals("shopAmount")) {
					addButton.addActionListener(e -> BillDetailsForm.this.addShopAmount(e));
				} else {
					addButton.addActionListener(e -> addBillDetails(amountFieldName, descriptionFieldName));
				}
				add(addButton, gbc);
			} else {
				gbc.gridy = 1;
				gbc.gridwidth = 2;
				JLabel placeholder = new JLabel("Click to view totals", SwingConstants.CENTER);
				placeholder.setFont(new Font("Arial", Font.PLAIN, 12));
				placeholder.setForeground(TEXT_COLOR);
				add(placeholder, gbc);
			}

			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (amountFieldName.equals("totals")) {
						TotalsPanel totalsPanel = new TotalsPanel();
						mainContentPanel.add(totalsPanel, "TOTALS_DETAILS");
						cardLayout.show(mainContentPanel, "TOTALS_DETAILS");
					} else {
						ColumnDetailPanel detailPanel = new ColumnDetailPanel(amountFieldName, descriptionFieldName,
								displayName);
						mainContentPanel.add(detailPanel, amountFieldName.toUpperCase() + "_DETAILS");
						cardLayout.show(mainContentPanel, amountFieldName.toUpperCase() + "_DETAILS");
					}
				}
			});
		}

		private void addBillDetails(String amountFieldName, String descriptionFieldName) {
			try {
				String amountText = amountField.getText().trim();
				String descriptionText = descriptionField.getText().trim();
				Date billDate = dateChooser.getDate();

				if (billDate == null) {
					JOptionPane.showMessageDialog(CardPanel.this, "Bill date is required.", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				double amount = amountText.isEmpty() ? 0.0 : Double.parseDouble(amountText);
				if (amount <= 0) {
					JOptionPane.showMessageDialog(CardPanel.this, "Please enter a valid amount.", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				if (amount > 0 && descriptionText.isEmpty()) {
					JOptionPane.showMessageDialog(CardPanel.this,
							displayName + " description is required when amount is provided.", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				billDetailsDAO.addBillDetails(amountFieldName, descriptionFieldName, amount, descriptionText, billDate);
				JOptionPane.showMessageDialog(CardPanel.this, displayName + " added successfully!", "Success",
						JOptionPane.INFORMATION_MESSAGE);
				amountField.setText("");
				descriptionField.setText("");
				dateChooser.setDate(null);
				loadData();
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(CardPanel.this, "Invalid amount format.", "Error",
						JOptionPane.ERROR_MESSAGE);
			} catch (SQLException ex) {
				JOptionPane.showMessageDialog(CardPanel.this, "Database error: " + ex.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private class UpdateBillDialog extends JDialog {
		private static final long serialVersionUID = 1L;
		private JTextField amountField;
		private JTextField descriptionField;
		private JDateChooser dateChooser;
		private JButton saveButton, cancelButton;

		public UpdateBillDialog(Frame parent, BillDetails bill, String amountFieldName, String descriptionFieldName,
				String displayName, double oldAmount) {
			super(parent, "Update " + displayName, true);
			setLayout(new GridBagLayout());
			setBackground(BACKGROUND_COLOR);
			setSize(340, 200);
			setLocationRelativeTo(parent);

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(3, 3, 3, 3);
			gbc.fill = GridBagConstraints.HORIZONTAL;

			// Title
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 2;
			JLabel titleLabel = new JLabel("Update " + displayName, SwingConstants.CENTER);
			titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
			titleLabel.setForeground(ACCENT_COLOR);
			add(titleLabel, gbc);

			// Amount
			gbc.gridwidth = 1;
			gbc.gridy = 1;
			gbc.gridx = 0;
			JLabel amountLabel = new JLabel("Amount:");
			amountLabel.setFont(new Font("Arial", Font.BOLD, 12));
			amountLabel.setForeground(TEXT_COLOR);
			add(amountLabel, gbc);

			gbc.gridx = 1;
			amountField = createStyledTextField(12, "Enter amount");
			amountField.setText(String.format("%.2f", getAmountByField(bill, amountFieldName)));
			add(amountField, gbc);

			// Description
			gbc.gridy = 2;
			gbc.gridx = 0;
			JLabel descLabel = new JLabel("Description:");
			descLabel.setFont(new Font("Arial", Font.BOLD, 12));
			descLabel.setForeground(TEXT_COLOR);
			add(descLabel, gbc);

			gbc.gridx = 1;
			descriptionField = createStyledTextField(12, "Enter description");
			descriptionField.setText(getDescriptionByField(bill, descriptionFieldName));
			add(descriptionField, gbc);

			// Date
			gbc.gridy = 3;
			gbc.gridx = 0;
			JLabel dateLabel = new JLabel("Date:");
			dateLabel.setFont(new Font("Arial", Font.BOLD, 12));
			dateLabel.setForeground(TEXT_COLOR);
			add(dateLabel, gbc);

			gbc.gridx = 1;
			dateChooser = createStyledDateChooser();
			dateChooser.setDate(bill.getBillDate() != null
					? Date.from(bill.getBillDate().atStartOfDay(ZoneId.systemDefault()).toInstant())
					: null);
			add(dateChooser, gbc);

			// Buttons
			gbc.gridy = 4;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			buttonPanel.setBackground(BACKGROUND_COLOR);
			saveButton = createStyledButton("Save");
			cancelButton = createStyledButton("Cancel");
			buttonPanel.add(saveButton);
			buttonPanel.add(cancelButton);
			add(buttonPanel, gbc);

			saveButton.addActionListener(e -> {
				try {
					String amountText = amountField.getText().trim();
					String descriptionText = descriptionField.getText().trim();
					Date billDate = dateChooser.getDate();

					if (billDate == null) {
						JOptionPane.showMessageDialog(this, "Bill date is required.", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}

					double amount = amountText.isEmpty() ? 0.0 : Double.parseDouble(amountText);
					if (amount < 0) {
						JOptionPane.showMessageDialog(this, "Amount cannot be negative.", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}

					if (amount > 0 && descriptionText.isEmpty()) {
						JOptionPane.showMessageDialog(this,
								displayName + " description is required when amount is provided.", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}

					billDetailsDAO.updateBillDetails(bill.getBillId(), amountFieldName, descriptionFieldName, amount,
							descriptionText, billDate, oldAmount);
					JOptionPane.showMessageDialog(this, displayName + " updated successfully!", "Success",
							JOptionPane.INFORMATION_MESSAGE);
					loadData();
					dispose();
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this, "Invalid amount format.", "Error", JOptionPane.ERROR_MESSAGE);
				} catch (SQLException ex) {
					JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			});

			cancelButton.addActionListener(e -> dispose());
		}
	}

	private class ColumnDetailPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private JTable table;
		private DefaultTableModel tableModel;

		public ColumnDetailPanel(String amountField, String descriptionField, String displayName) {
			setLayout(new BorderLayout(3, 3));
			setBackground(BACKGROUND_COLOR);
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1),
					new EmptyBorder(3, 3, 3, 3)));

			JPanel topPanel = new JPanel(new BorderLayout());
			topPanel.setBackground(BACKGROUND_COLOR);
			topPanel.setBorder(new EmptyBorder(3, 3, 3, 3));
			topPanel.setPreferredSize(new Dimension(0, 40));

			JLabel titleLabel = new JLabel(displayName + " Details", SwingConstants.CENTER);
			titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
			titleLabel.setForeground(ACCENT_COLOR);
			topPanel.add(titleLabel, BorderLayout.CENTER);

			JButton backButton = createStyledButton("Back");
			backButton.setPreferredSize(new Dimension(80, 20));
			backButton.addActionListener(e -> cardLayout.show(mainContentPanel, "BILL_DETAILS"));
			JPanel buttonPanelButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			buttonPanelButtons.setBackground(BACKGROUND_COLOR);
			buttonPanelButtons.add(backButton);
			topPanel.add(buttonPanelButtons, BorderLayout.EAST);
			add(topPanel, BorderLayout.NORTH);

			String[] columnNames = new String[] { "Amount", "Description", "Date" };
			tableModel = new DefaultTableModel(columnNames, 0) {
				private static final long serialVersionUID = 1L;

				@Override
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			};
			table = new JTable(tableModel);
			table.setFont(new Font("Arial", Font.PLAIN, 14));
			table.setForeground(TEXT_COLOR);
			table.setBackground(DARK_GRAY);
			table.setOpaque(true);
			table.setRowHeight(24);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

			JTableHeader header = table.getTableHeader();
			header.setFont(new Font("Arial", Font.BOLD, 16));
			header.setForeground(TEXT_COLOR);
			header.setBackground(BACK_COLOR);

			table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
				private static final long serialVersionUID = 1L;

				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
						boolean hasFocus, int row, int column) {
					Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					c.setForeground(TEXT_COLOR);
					if (isSelected) {
						c.setBackground(BACK_COLOR);
					} else {
						c.setBackground(row % 2 == 0 ? LIGHT_GRAY : DARK_GRAY);
					}
					return c;
				}
			});

			table.getColumnModel().getColumn(0).setPreferredWidth(100);
			table.getColumnModel().getColumn(1).setPreferredWidth(160);
			table.getColumnModel().getColumn(2).setPreferredWidth(100);

			JScrollPane tableScrollPane = new JScrollPane(table);
			tableScrollPane.setBackground(DARK_GRAY);
			tableScrollPane.getViewport().setBackground(DARK_GRAY);
			tableScrollPane.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
			add(tableScrollPane, BorderLayout.CENTER);

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 3));
			buttonPanel.setBackground(BACKGROUND_COLOR);
			JButton updateDetailButton = createStyledButton("Update");
			updateDetailButton.setPreferredSize(new Dimension(80, 20));
			JButton deleteDetailButton = createStyledButton("Delete");
			deleteDetailButton.setPreferredSize(new Dimension(80, 20));
			buttonPanel.add(updateDetailButton);
			buttonPanel.add(deleteDetailButton);
			add(buttonPanel, BorderLayout.SOUTH);

			loadDetailTableData(amountField, descriptionField);

			updateDetailButton.addActionListener(e -> {
				int selectedRow = table.getSelectedRow();
				if (selectedRow >= 0) {
					if (!amountField.equals("shopAmount")) {
						BillDetails bill = findBillByDetailRow(amountField, selectedRow);
						if (bill != null) {
							bill.getBillId();
							double oldAmount = getAmountByField(bill, amountField);
							new UpdateBillDialog((Frame) SwingUtilities.getWindowAncestor(this), bill, amountField,
									descriptionField, displayName, oldAmount).setVisible(true);
						}
					} else {
						JOptionPane.showMessageDialog(this, "Shop amount entries cannot be updated.", "Info",
								JOptionPane.INFORMATION_MESSAGE);
					}
				} else {
					JOptionPane.showMessageDialog(this, "Please select a bill to update.", "Error",
							JOptionPane.WARNING_MESSAGE);
				}
			});

			deleteDetailButton.addActionListener(e -> {
				int selectedRow = table.getSelectedRow();
				if (selectedRow >= 0) {
					if (!amountField.equals("shopAmount")) {
						BillDetails bill = findBillByDetailRow(amountField, selectedRow);
						if (bill != null) {
							int confirm = JOptionPane.showConfirmDialog(this,
									"Are you sure you want to delete this bill?", "Confirm Delete",
									JOptionPane.YES_NO_OPTION);
							if (confirm == JOptionPane.YES_OPTION) {
								try {
									billDetailsDAO.deleteBillDetails(bill.getBillId());
									loadDetailTableData(amountField, descriptionField);
									JOptionPane.showMessageDialog(this, "Bill deleted successfully!", "Success",
											JOptionPane.INFORMATION_MESSAGE);
								} catch (SQLException ex) {
									JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error",
											JOptionPane.ERROR_MESSAGE);
								}
							}
						}
						
					} else {
						JOptionPane.showMessageDialog(this, "Shop amount entries cannot be deleted.", "Info",
								JOptionPane.INFORMATION_MESSAGE);
					}
				} else {
					JOptionPane.showMessageDialog(this, "Please select a bill to delete.", "Error",
							JOptionPane.WARNING_MESSAGE);
				}
			});
		}

		private void loadDetailTableData(String amountField, String descriptionField) {
			tableModel.setRowCount(0);
			if (amountField.equals("shopAmount")) {
				try {
					List<Object[]> shopAmounts = billDetailsDAO.loadShopAmountData();
					for (Object[] row : shopAmounts) {
						tableModel.addRow(row);
					}
				} catch (SQLException ex) {
					JOptionPane.showMessageDialog(BillDetailsForm.this,
							"Error loading shop amount data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			} else {
				for (BillDetails bill : billDetailsList) {
					double amount = getAmountByField(bill, amountField);
					if (amount > 0) {
						String description = getDescriptionByField(bill, descriptionField);
						tableModel.addRow(
								new Object[] { String.format("%.2f", amount), description, bill.getBillDate() });
					}
				}
			}
		}
	}

	private class TotalsPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private JLabel shopBalanceLabel, totalProfitLabel, capitalLabel;
		private JPanel cardsPanel;

		public TotalsPanel() {
			setLayout(new BorderLayout(3, 3));
			setBackground(BACKGROUND_COLOR);
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1),
					new EmptyBorder(3, 3, 3, 3)));

			JPanel topPanel = new JPanel(new BorderLayout());
			topPanel.setBackground(BACKGROUND_COLOR);
			topPanel.setBorder(new EmptyBorder(3, 3, 3, 3));
			topPanel.setPreferredSize(new Dimension(0, 50));

			JLabel titleLabel = new JLabel("Financial Totals", SwingConstants.CENTER);
			titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
			titleLabel.setForeground(ACCENT_COLOR);
			topPanel.add(titleLabel, BorderLayout.NORTH);

			JPanel totalsDisplayPanel = new JPanel(new GridLayout(1, 3, 3, 3));
			totalsDisplayPanel.setBackground(BACKGROUND_COLOR);
			shopBalanceLabel = new JLabel("Shop Balance: 0.00");
			totalProfitLabel = new JLabel("Total Net Profit: 0.00");
			capitalLabel = new JLabel("Capital: 0.00");
			shopBalanceLabel.setFont(new Font("Arial", Font.BOLD, 14));
			totalProfitLabel.setFont(new Font("Arial", Font.BOLD, 14));
			capitalLabel.setFont(new Font("Arial", Font.BOLD, 14));
			shopBalanceLabel.setForeground(SUCCESS_COLOR);
			totalProfitLabel.setForeground(ACCENT_COLOR);
			capitalLabel.setForeground(RED_COLOR);
			totalsDisplayPanel.add(shopBalanceLabel);
			totalsDisplayPanel.add(totalProfitLabel);
			totalsDisplayPanel.add(capitalLabel);
			topPanel.add(totalsDisplayPanel, BorderLayout.CENTER);

			JButton backButton = createStyledButton("Back");
			backButton.setPreferredSize(new Dimension(80, 20));
			backButton.addActionListener(e -> cardLayout.show(mainContentPanel, "BILL_DETAILS"));
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			buttonPanel.setBackground(BACKGROUND_COLOR);
			buttonPanel.add(backButton);
			topPanel.add(buttonPanel, BorderLayout.EAST);
			add(topPanel, BorderLayout.NORTH);

			cardsPanel = new JPanel(new GridLayout(0, 2, 3, 3));
			cardsPanel.setBackground(BACKGROUND_COLOR);
			JScrollPane cardsScrollPane = new JScrollPane(cardsPanel);
			cardsScrollPane.setBackground(DARK_GRAY);
			cardsScrollPane.getViewport().setBackground(DARK_GRAY);
			cardsScrollPane.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
			add(cardsScrollPane, BorderLayout.CENTER);

			loadTotalsData();
		}

		private void loadTotalsData() {
			cardsPanel.removeAll();
			try {
				billDetailsDAO.loadTotalsData(cardsPanel, shopBalanceLabel, totalProfitLabel, capitalLabel);
			} catch (SQLException ex) {
				JOptionPane.showMessageDialog(this, "Error loading totals: " + ex.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
			cardsPanel.revalidate();
			cardsPanel.repaint();
		}
	}

	private double getAmountByField(BillDetails bill, String amountField) {
		switch (amountField) {
		case "rent":
			return bill.getRent();
		case "lightBill":
			return bill.getLightBill();
		case "maintenanceBill":
			return bill.getMaintenanceBill();
		case "salary":
			return bill.getSalary();
		case "parcelBillAmount":
			return bill.getParcelBillAmount();
		case "bankEmi":
			return bill.getBankEmi();
		case "othersAmount":
			return bill.getOthersAmount();
		default:
			return 0.0;
		}
	}

	private String getDescriptionByField(BillDetails bill, String descriptionField) {
		if (descriptionField == null)
			return "";
		if (descriptionField.equals("parcelBillDescription")) {
			return bill.getParcelBillDescription() != null ? bill.getParcelBillDescription() : "";
		}
		return bill.getOthersDescription() != null ? bill.getOthersDescription() : "";
	}

	private BillDetails findBillByDetailRow(String amountField, int row) {
		int currentRow = 0;
		for (BillDetails bill : billDetailsList) {
			if (getAmountByField(bill, amountField) > 0) {
				if (currentRow == row)
					return bill;
				currentRow++;
			}
		}
		return null;
	}

	private void loadData() {
		try {
			billDetailsList = billDetailsDAO.loadBillDetails();
			double shopBalance = billDetailsDAO.getShopBalance();
			shopBalanceLabel.setText(String.format("Shop Balance: %.2f", shopBalance));
			updateCards();
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(this, "Error loading data: " + ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void updateCards() {
		cardsPanel.removeAll();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;

		// First row: Rent, Light Bill, Maintenance
		gbc.gridy = 0;
		gbc.gridx = 0;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[0].displayName, CARD_CONFIGS[0].amountField,
				CARD_CONFIGS[0].descriptionField), gbc);
		gbc.gridx = 1;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[1].displayName, CARD_CONFIGS[1].amountField,
				CARD_CONFIGS[1].descriptionField), gbc);
		gbc.gridx = 2;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[2].displayName, CARD_CONFIGS[2].amountField,
				CARD_CONFIGS[2].descriptionField), gbc);

		// Second row: Salary, Parcel Bill, Bank EMI
		gbc.gridy = 1;
		gbc.gridx = 0;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[3].displayName, CARD_CONFIGS[3].amountField,
				CARD_CONFIGS[3].descriptionField), gbc);
		gbc.gridx = 1;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[4].displayName, CARD_CONFIGS[4].amountField,
				CARD_CONFIGS[4].descriptionField), gbc);
		gbc.gridx = 2;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[5].displayName, CARD_CONFIGS[5].amountField,
				CARD_CONFIGS[5].descriptionField), gbc);

		// Third row: Others, Shop Amount, Totals
		gbc.gridy = 2;
		gbc.gridx = 0;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[6].displayName, CARD_CONFIGS[6].amountField,
				CARD_CONFIGS[6].descriptionField), gbc);
		gbc.gridx = 1;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[7].displayName, CARD_CONFIGS[7].amountField,
				CARD_CONFIGS[7].descriptionField), gbc);
		gbc.gridx = 2;
		cardsPanel.add(new CardPanel(CARD_CONFIGS[8].displayName, CARD_CONFIGS[8].amountField,
				CARD_CONFIGS[8].descriptionField), gbc);

		cardsPanel.revalidate();
		cardsPanel.repaint();
		mainContentPanel.revalidate();
		mainContentPanel.repaint();
	}

	private JButton createStyledButton(String text) {
		JButton button = new JButton(text);
		button.setFont(new Font("Arial", Font.BOLD, 12));
		button.setForeground(TEXT_COLOR);
		button.setBackground(LIGHT_GRAY);
		button.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
		button.setFocusPainted(false);
		button.setPreferredSize(new Dimension(80, 20));
		return button;
	}

	private JTextField createStyledTextField(int columns, String tooltip) {
		JTextField field = new JTextField(columns);
		field.setFont(new Font("Arial", Font.PLAIN, 12));
		field.setForeground(TEXT_COLOR);
		field.setBackground(LIGHT_GRAY);
		field.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
		field.setPreferredSize(new Dimension(200, 32));
		field.setMinimumSize(new Dimension(200, 32));
		field.setToolTipText(tooltip);
		field.setEditable(true);
		field.setFocusable(true);
		return field;
	}

	private JDateChooser createStyledDateChooser() {
		JDateChooser dateChooser = new JDateChooser();
		dateChooser.setDateFormatString("yyyy-MM-dd");
		dateChooser.setFont(new Font("Arial", Font.PLAIN, 12));
		dateChooser.setForeground(TEXT_COLOR);
		dateChooser.setBackground(SUCCESS_COLOR);
		dateChooser.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
		dateChooser.setPreferredSize(new Dimension(200, 32));
		dateChooser.setMinimumSize(new Dimension(200, 32));
		dateChooser.setEnabled(true);
		dateChooser.setFocusable(true);
		return dateChooser;
	}

	private void clearFields() {
		for (Component c : cardsPanel.getComponents()) {
			if (c instanceof CardPanel) {
				CardPanel card = (CardPanel) c;
				if (card.amountField != null)
					card.amountField.setText("");
				if (card.descriptionField != null)
					card.descriptionField.setText("");
				if (card.dateChooser != null)
					card.dateChooser.setDate(null);
			}
		}
		shopAmountField.setText("");
		shopDescriptionField.setText("");
		shopDateChooser.setDate(null);
	}
}