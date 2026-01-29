package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.proshop.model.GymWholesaler;
import com.toedter.calendar.JDateChooser;

public class GymWholesalerPaymentForm extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(GymWholesalerPaymentForm.class.getName());
	private static final Color SUCCESS_COLOR = new Color(102, 187, 106); // Light Green (#66BB6A)
	private static final Color STAT_CARD_COLOR = new Color(50, 50, 50); // Dark gray for even columns (#323232)
	private static final Color FAINT_ROW_COLOR = new Color(66, 66, 66); // Light gray for odd columns (#424242)
	private static final Color SHADOW_COLOR = new Color(224, 224, 224); // Very light gray (#E0E0E0)
	private final GymWholesaler wholesaler;
	private final GymWholesalerDAO dao;
	private final DefaultTableModel paymentTableModel;
	private final DefaultTableModel billTableModel;
	private final JTextField amountField;
	private final JComboBox<String> paymentModeCombo;
	private final JDateChooser dateChooser;
	private final JLabel totalBillLabel;
	private final JLabel totalPaidLabel;
	private final JLabel pendingAmountLabel;
	private JTable paymentTable;

	public GymWholesalerPaymentForm(GymWholesaler wholesaler, CardLayout cardLayout, JPanel mainContentPanel,
			GymWholesalerDAO dao) {
		this.wholesaler = wholesaler;
		this.dao = dao;
		setLayout(new BorderLayout());
		setBackground(UIUtils.BACKGROUND_COLOR);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1),
				BorderFactory.createEmptyBorder(10, 10, 10, 10)));

		// Form and Totals Panel (Top, centered side by side)
		JPanel topPanel = new JPanel(new GridBagLayout());
		topPanel.setBackground(UIUtils.BACKGROUND_COLOR);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.anchor = GridBagConstraints.CENTER;

		// Form Panel
		JPanel formPanel = new JPanel(new GridBagLayout());
		formPanel.setBackground(UIUtils.BACKGROUND_COLOR);
		GridBagConstraints formGbc = new GridBagConstraints();
		formGbc.insets = new Insets(10, 10, 10, 10);
		formGbc.anchor = GridBagConstraints.WEST;

		amountField = UIUtils.createStyledTextField(10);
		paymentModeCombo = new JComboBox<>(new String[] { "Cash", "Card", "UPI" });
		paymentModeCombo.setFont(UIUtils.TEXT_FONT);
		paymentModeCombo.setBackground(UIUtils.BACKGROUND_COLOR);
		paymentModeCombo.setForeground(UIUtils.TEXT_COLOR);
		dateChooser = UIUtils.createStyledDateChooser();
		dateChooser.setDate(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));

		JLabel amountLabel = new JLabel("Amount Paid:");
		amountLabel.setForeground(UIUtils.TEXT_COLOR); // White text
		amountLabel.setFont(UIUtils.LABEL_FONT);
		JLabel paymentModeLabel = new JLabel("Payment Mode:");
		paymentModeLabel.setForeground(UIUtils.TEXT_COLOR); // White text
		paymentModeLabel.setFont(UIUtils.LABEL_FONT);
		JLabel paymentDateLabel = new JLabel("Payment Date:");
		paymentDateLabel.setForeground(UIUtils.TEXT_COLOR); // White text
		paymentDateLabel.setFont(UIUtils.LABEL_FONT);

		int row = 0;
		formGbc.gridx = 0;
		formGbc.gridy = row++;
		formPanel.add(amountLabel, formGbc);
		formGbc.gridx = 1;
		formPanel.add(amountField, formGbc);

		formGbc.gridx = 0;
		formGbc.gridy = row++;
		formPanel.add(paymentModeLabel, formGbc);
		formGbc.gridx = 1;
		formPanel.add(paymentModeCombo, formGbc);

		formGbc.gridx = 0;
		formGbc.gridy = row++;
		formPanel.add(paymentDateLabel, formGbc);
		formGbc.gridx = 1;
		formPanel.add(dateChooser, formGbc);

		JPanel formButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		formButtonPanel.setBackground(UIUtils.BACKGROUND_COLOR);
		JButton addPaymentButton = UIUtils.createStyledButton("Add Payment");
		JButton refreshButton = UIUtils.createStyledButton("Refresh");
		formButtonPanel.add(addPaymentButton);
		formButtonPanel.add(refreshButton);
		formGbc.gridx = 1;
		formGbc.gridy = row;
		formPanel.add(formButtonPanel, formGbc);

		// Totals Panel
		JPanel totalsPanel = new JPanel(new GridBagLayout());
		totalsPanel.setBackground(UIUtils.BACKGROUND_COLOR);
		GridBagConstraints totalsGbc = new GridBagConstraints();
		totalsGbc.insets = new Insets(10, 10, 10, 10);
		totalsGbc.anchor = GridBagConstraints.WEST;

		totalBillLabel = new JLabel("Total Bill: ₹0.00");
		totalBillLabel.setForeground(Color.YELLOW);
		totalBillLabel.setFont(UIUtils.LABEL_FONT);
		totalPaidLabel = new JLabel("Total Paid: ₹0.00");
		totalPaidLabel.setForeground(SUCCESS_COLOR);
		totalPaidLabel.setFont(UIUtils.LABEL_FONT);
		pendingAmountLabel = new JLabel("Pending Amount: ₹0.00");
		pendingAmountLabel.setForeground(Color.RED);
		pendingAmountLabel.setFont(UIUtils.LABEL_FONT);

		totalsGbc.gridx = 0;
		totalsGbc.gridy = 0;
		totalsPanel.add(totalBillLabel, totalsGbc);
		totalsGbc.gridy = 1;
		totalsPanel.add(totalPaidLabel, totalsGbc);
		totalsGbc.gridy = 2;
		totalsPanel.add(pendingAmountLabel, totalsGbc);

		// Add form and totals panels side by side in top panel
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(10, 20, 10, 20);
		topPanel.add(formPanel, gbc);
		gbc.gridx = 1;
		topPanel.add(totalsPanel, gbc);

		// Table Panel (Center)
		billTableModel = new DefaultTableModel(new String[] { "Purchase Date", "Bill Amount" }, 0);
		paymentTableModel = new DefaultTableModel(new String[] { "Date", "Amount Paid", "Pending Amount" }, 0) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		paymentTable = new JTable(paymentTableModel) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component c = super.prepareRenderer(renderer, row, column);
				if (!isRowSelected(row)) {
					c.setBackground(column % 2 == 0 ? STAT_CARD_COLOR : FAINT_ROW_COLOR);
					if (row == getRowCount() - 1 && column == 2) {
						c.setForeground(SUCCESS_COLOR); // Highlight pending amount in last row
					} else {
						c.setForeground(UIUtils.TEXT_COLOR);
					}
				}
				return c;
			}
		};
		paymentTable.setFont(new Font("Arial", Font.PLAIN, 14));
		paymentTable.setRowHeight(30);
		paymentTable.setBackground(UIUtils.BACKGROUND_COLOR);
		paymentTable.setForeground(UIUtils.TEXT_COLOR);
		paymentTable.setGridColor(SHADOW_COLOR);
		paymentTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
		paymentTable.getTableHeader().setBackground(STAT_CARD_COLOR);
		paymentTable.getTableHeader().setForeground(UIUtils.TEXT_COLOR);

		// Adjust column widths
		TableColumnModel columnModel = paymentTable.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(150); // Date
		columnModel.getColumn(1).setPreferredWidth(100); // Amount Paid
		columnModel.getColumn(2).setPreferredWidth(100); // Pending Amount

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		for (int i = 0; i < paymentTable.getColumnCount(); i++) {
			paymentTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
		}

		JScrollPane tableScrollPane = new JScrollPane(paymentTable);
		tableScrollPane.setBackground(UIUtils.BACKGROUND_COLOR);
		tableScrollPane.getViewport().setBackground(UIUtils.BACKGROUND_COLOR);
		tableScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		tableScrollPane.setPreferredSize(new Dimension(600, 300));

		JPanel tablePanel = new JPanel(new BorderLayout());
		tablePanel.setBackground(UIUtils.BACKGROUND_COLOR);
		tablePanel.add(tableScrollPane, BorderLayout.CENTER);

		// Button Panel (Bottom)
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.setBackground(UIUtils.BACKGROUND_COLOR);
		JButton updateButton = UIUtils.createStyledButton("Update");
		JButton pdfButton = UIUtils.createStyledButton("Generate PDF");
		JButton backButton = UIUtils.createStyledButton("Back");
		buttonPanel.add(updateButton);
		buttonPanel.add(pdfButton);
		buttonPanel.add(backButton);

		add(topPanel, BorderLayout.NORTH);
		add(tablePanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);

		addPaymentButton.addActionListener(e -> addPayment());
		refreshButton.addActionListener(e -> loadPaymentDetails());
		updateButton.addActionListener(e -> updatePayment());
		pdfButton.addActionListener(e -> generatePaymentPDF(wholesaler, billTableModel, paymentTableModel,
				totalBillLabel.getText(), pendingAmountLabel.getText()));
		backButton.addActionListener(e -> cardLayout.show(mainContentPanel, "GYM_WHOLESALER"));

		loadPaymentDetails();
	}

	private void addPayment() {
		try {
			double amountPaid = Double.parseDouble(amountField.getText().trim());
			if (amountPaid <= 0) {
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Amount must be positive.",
						"Error", JOptionPane.ERROR_MESSAGE));
				return;
			}

			String paymentMode = (String) paymentModeCombo.getSelectedItem();
			LocalDate paymentDate = dateChooser.getDate() != null
					? dateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
					: LocalDate.now();

			dao.recordPayment(wholesaler, amountPaid, paymentMode, paymentDate, this);
			amountField.setText("");
			loadPaymentDetails();
		} catch (NumberFormatException ex) {
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Invalid amount format.", "Error",
					JOptionPane.ERROR_MESSAGE));
		} catch (Exception ex) {
			LOGGER.severe("Error adding payment: " + ex.getMessage());
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
					"Failed to record payment. Please try again.", "Error", JOptionPane.ERROR_MESSAGE));
		}
	}

	private void updatePayment() {
		int selectedRow = paymentTable.getSelectedRow();
		if (selectedRow < 0) {
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Please select a payment to update.",
					"Error", JOptionPane.ERROR_MESSAGE));
			return;
		}

		try {
			String currentDate = (String) paymentTableModel.getValueAt(selectedRow, 0);
			String currentAmount = (String) ((String) paymentTableModel.getValueAt(selectedRow, 1)).replace("₹", "");

			JTextField amountField = UIUtils.createStyledTextField(10);
			amountField.setText(currentAmount);
			JComboBox<String> paymentModeCombo = new JComboBox<>(new String[] { "Cash", "Card", "UPI" });
			paymentModeCombo.setFont(UIUtils.TEXT_FONT);
			paymentModeCombo.setBackground(UIUtils.BACKGROUND_COLOR);
			paymentModeCombo.setForeground(UIUtils.TEXT_COLOR);
			JDateChooser dateChooser = UIUtils.createStyledDateChooser();
			dateChooser
					.setDate(Date.from(LocalDate.parse(currentDate).atStartOfDay(ZoneId.systemDefault()).toInstant()));

			JPanel panel = new JPanel(new GridLayout(3, 2));
			panel.setBackground(UIUtils.BACKGROUND_COLOR);
			JLabel amountLabel = new JLabel("Amount Paid:");
			amountLabel.setForeground(UIUtils.TEXT_COLOR);
			amountLabel.setFont(UIUtils.LABEL_FONT);
			JLabel modeLabel = new JLabel("Payment Mode:");
			modeLabel.setForeground(UIUtils.TEXT_COLOR);
			modeLabel.setFont(UIUtils.LABEL_FONT);
			JLabel dateLabel = new JLabel("Payment Date:");
			dateLabel.setForeground(UIUtils.TEXT_COLOR);
			dateLabel.setFont(UIUtils.LABEL_FONT);

			panel.add(amountLabel);
			panel.add(amountField);
			panel.add(modeLabel);
			panel.add(paymentModeCombo);
			panel.add(dateLabel);
			panel.add(dateChooser);

			int result = JOptionPane.showConfirmDialog(this, panel, "Update Payment", JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				double newAmount = Double.parseDouble(amountField.getText().trim());
				if (newAmount <= 0) {
					SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Amount must be positive.",
							"Error", JOptionPane.ERROR_MESSAGE));
					return;
				}

				String newPaymentMode = (String) paymentModeCombo.getSelectedItem();
				LocalDate newPaymentDate = dateChooser.getDate() != null
						? dateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
						: LocalDate.now();

				dao.updatePayment(wholesaler, selectedRow, newAmount, newPaymentMode, newPaymentDate, this);
				loadPaymentDetails();
			}
		} catch (NumberFormatException ex) {
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Invalid amount format.", "Error",
					JOptionPane.ERROR_MESSAGE));
		} catch (Exception ex) {
			LOGGER.severe("Error updating payment: " + ex.getMessage());
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
					"Failed to update payment. Please try again.", "Error", JOptionPane.ERROR_MESSAGE));
		}
	}

	private void loadPaymentDetails() {
		try {
			Map<String, Object> paymentDetails = dao.fetchPaymentDetails(wholesaler);
			@SuppressWarnings("unchecked")
			List<Object[]> payments = (List<Object[]>) paymentDetails.get("payments");
			@SuppressWarnings("unchecked")
			List<Object[]> bills = (List<Object[]>) paymentDetails.get("bills");
			double totalBill = (Double) paymentDetails.get("totalBill");
			double totalPaid = (Double) paymentDetails.get("totalPaid");
			double pendingAmount = totalBill - totalPaid;

			paymentTableModel.setRowCount(0);
			for (Object[] payment : payments) {
				String date = (String) payment[0];
				String amount = (String) payment[1];
				paymentTableModel
						.addRow(new Object[] { date, "₹" + amount, "₹" + String.format("%.2f", pendingAmount) });
			}

			billTableModel.setRowCount(0);
			for (Object[] bill : bills) {
				billTableModel.addRow(bill);
			}

			// Add totals row
			paymentTableModel.addRow(new Object[] { "", "", "Total: ₹" + String.format("%.2f", pendingAmount) });

			totalBillLabel.setText("Total Bill: ₹" + String.format("%.2f", totalBill));
			totalPaidLabel.setText("Total Paid: ₹" + String.format("%.2f", totalPaid));
			pendingAmountLabel.setText("Pending Amount: ₹" + String.format("%.2f", pendingAmount));
		} catch (Exception ex) {
			LOGGER.severe("Error loading payment details: " + ex.getMessage());
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
					"Failed to load payment details. Please try again.", "Error", JOptionPane.ERROR_MESSAGE));
		}
	}

	private void generatePaymentPDF(GymWholesaler wholesaler, DefaultTableModel billTableModel,
			DefaultTableModel paymentTableModel, String totalBillText, String pendingAmountText) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save Payment Statement PDF");
		fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
		fileChooser.setSelectedFile(new File("payment_summary_" + (wholesaler.getWholesalerName() != null
				? wholesaler.getWholesalerName().replaceAll("[^a-zA-Z0-9]", "_")
				: "unknown") + "_" + LocalDate.now() + ".pdf"));

		int userSelection = fileChooser.showSaveDialog(this);
		if (userSelection != JFileChooser.APPROVE_OPTION) {
			return;
		}

		File fileToSave = fileChooser.getSelectedFile();
		String dest = fileToSave.getAbsolutePath();
		if (!dest.toLowerCase().endsWith(".pdf")) {
			dest += ".pdf";
		}

		try {
			PdfWriter writer = new PdfWriter(new FileOutputStream(dest));
			PdfDocument pdf = new PdfDocument(writer);
			Document document = new Document(pdf, com.itextpdf.kernel.geom.PageSize.A4);
			document.setMargins(30, 30, 30, 30);

// Times Roman font for headings
			com.itextpdf.kernel.font.PdfFont timesRoman = PdfFontFactory.createFont("Times-Roman");

// Header: Gaurav Yadav Pure Protien
			document.add(new Paragraph("Gaurav Yadav Pure Protien").setFont(timesRoman).setFontSize(24).setBold()
					.setTextAlignment(TextAlignment.CENTER).setMarginBottom(5));
			document.add(new Paragraph(
					"Chatrapati Shivaji Maharaj Stadium, Near Venu Tai Chawhan Hall, Yashwantrao Park, Pawar Nagar, Karad, Maharashtra 415110")
					.setFontSize(10).setTextAlignment(TextAlignment.CENTER));
			document.add(new Paragraph("Phone: 9822888876 | Email: gauravyadavpureprotien@gmail.com").setFontSize(10)
					.setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));

// Wholesaler Details
			document.add(new Paragraph("Payment Statement").setFont(timesRoman).setFontSize(18).setBold()
					.setTextAlignment(TextAlignment.LEFT).setMarginTop(10));
			Table infoTable = new Table(UnitValue.createPercentArray(new float[] { 50, 50 }));
			infoTable.setWidth(UnitValue.createPercentValue(100));
			infoTable.setMarginTop(5);
			infoTable.setBackgroundColor(new DeviceRgb(200, 200, 200)); // Darker grey

			Cell wholesalerCell = new Cell();
			wholesalerCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f));
			wholesalerCell.setPadding(10);
			wholesalerCell.add(new Paragraph(
					"To: " + (wholesaler.getWholesalerName() != null ? wholesaler.getWholesalerName() : "Unknown"))
					.setFontSize(12).setBold().setTextAlignment(TextAlignment.LEFT));
			wholesalerCell
					.add(new Paragraph(wholesaler.getAddress() != null ? wholesaler.getAddress() : "").setFontSize(10));
			wholesalerCell
					.add(new Paragraph("Phone: " + (wholesaler.getMobileNo() != null ? wholesaler.getMobileNo() : ""))
							.setFontSize(10));
			infoTable.addCell(wholesalerCell);

			Cell dateCell = new Cell();
			dateCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f));
			dateCell.setPadding(10);
			dateCell.add(
					new Paragraph("Date: " + LocalDate.now()).setFontSize(10).setTextAlignment(TextAlignment.RIGHT));
			infoTable.addCell(dateCell);

			document.add(infoTable);
			document.add(new Paragraph("\n"));

// Payment Statement Heading
			document.add(new Paragraph("Payment Statement").setFont(timesRoman).setFontSize(14).setBold()
					.setTextAlignment(TextAlignment.CENTER).setMarginTop(10)
					.setBackgroundColor(new DeviceRgb(200, 200, 200)) // Darker grey
					.setFontColor(ColorConstants.BLACK).setPadding(5));

// Combined Statement Table
			float[] columnWidths = { 3, 2, 3, 2 }; // Adjusted for wider date columns
			Table statementTable = new Table(columnWidths);
			statementTable.setWidth(UnitValue.createPercentValue(100));
			statementTable.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 1));
			statementTable.setMarginTop(10);

// Table Headers
			statementTable.addHeaderCell(
					new Cell().add(new Paragraph("Purchase Date").setBold().setTextAlignment(TextAlignment.CENTER))
							.setBackgroundColor(new DeviceRgb(200, 200, 200)) // Darker grey
							.setFontColor(ColorConstants.BLACK).setPadding(8));
			statementTable.addHeaderCell(
					new Cell().add(new Paragraph("Bill Amount").setBold().setTextAlignment(TextAlignment.CENTER))
							.setBackgroundColor(new DeviceRgb(200, 200, 200)) // Darker grey
							.setFontColor(ColorConstants.BLACK).setPadding(8));
			statementTable.addHeaderCell(
					new Cell().add(new Paragraph("Amount Paid Date").setBold().setTextAlignment(TextAlignment.CENTER))
							.setBackgroundColor(new DeviceRgb(200, 200, 200)) // Darker grey
							.setFontColor(ColorConstants.BLACK).setPadding(8));
			statementTable.addHeaderCell(
					new Cell().add(new Paragraph("Amount Paid").setBold().setTextAlignment(TextAlignment.CENTER))
							.setBackgroundColor(new DeviceRgb(200, 200, 200)) // Darker grey
							.setFontColor(ColorConstants.BLACK).setPadding(8));

// Populate Table
			double totalBill = 0.0;
			double totalPaid = 0.0;
			int maxRows = Math.max(billTableModel.getRowCount(), paymentTableModel.getRowCount());
			for (int i = 0; i < maxRows; i++) {
				Cell purchaseDateCell = new Cell();
				Cell billAmountCell = new Cell();
				Cell paidDateCell = new Cell();
				Cell amountPaidCell = new Cell();

// Set alternating background colors
				DeviceRgb rowColor = (i % 2 == 0) ? new DeviceRgb(220, 220, 220) : new DeviceRgb(180, 180, 180);
				purchaseDateCell.setBackgroundColor(rowColor);
				billAmountCell.setBackgroundColor(rowColor);
				paidDateCell.setBackgroundColor(rowColor);
				amountPaidCell.setBackgroundColor(rowColor);

// Set consistent padding
				purchaseDateCell.setPadding(8);
				billAmountCell.setPadding(8);
				paidDateCell.setPadding(8);
				amountPaidCell.setPadding(8);

// Purchase Date and Bill Amount
				if (i < billTableModel.getRowCount()) {
					String purchaseDate = (String) billTableModel.getValueAt(i, 0);
					String billAmount = (String) billTableModel.getValueAt(i, 1);
					double billValue = 0.0;
					try {
						if (billAmount != null && !billAmount.trim().isEmpty()) {
							billValue = Double.parseDouble(billAmount);
							totalBill += billValue;
						}
					} catch (NumberFormatException e) {
						LOGGER.warning("Invalid bill amount at row " + i + ": " + billAmount);
					}
					purchaseDateCell.add(new Paragraph(purchaseDate != null ? purchaseDate : "")
							.setTextAlignment(TextAlignment.CENTER));
					billAmountCell.add(new Paragraph(billValue > 0 ? "₹" + String.format("%.2f", billValue) : "")
							.setTextAlignment(TextAlignment.RIGHT));
				} else {
					purchaseDateCell.add(new Paragraph("").setTextAlignment(TextAlignment.CENTER));
					billAmountCell.add(new Paragraph("").setTextAlignment(TextAlignment.RIGHT));
				}

// Amount Paid Date and Amount Paid
				if (i < paymentTableModel.getRowCount()) {
					String paidDate = (String) paymentTableModel.getValueAt(i, 0);
					String amountPaidRaw = (String) paymentTableModel.getValueAt(i, 1);
					String amountPaid = (amountPaidRaw != null && amountPaidRaw.startsWith("₹"))
							? amountPaidRaw.replace("₹", "").trim()
							: (amountPaidRaw != null ? amountPaidRaw.trim() : "");
					double paidValue = 0.0;
					try {
						if (amountPaid != null && !amountPaid.isEmpty()) {
							paidValue = Double.parseDouble(amountPaid);
							totalPaid += paidValue;
						}
					} catch (NumberFormatException e) {
						LOGGER.warning("Invalid amount paid at row " + i + ": " + amountPaid);
					}
					paidDateCell.add(
							new Paragraph(paidDate != null ? paidDate : "").setTextAlignment(TextAlignment.CENTER));
					amountPaidCell.add(new Paragraph(paidValue > 0 ? "₹" + String.format("%.2f", paidValue) : "")
							.setTextAlignment(TextAlignment.RIGHT));
				} else {
					paidDateCell.add(new Paragraph("").setTextAlignment(TextAlignment.CENTER));
					amountPaidCell.add(new Paragraph("").setTextAlignment(TextAlignment.RIGHT));
				}

// Add borders
				purchaseDateCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f));
				billAmountCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f));
				paidDateCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f));
				amountPaidCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f));

				statementTable.addCell(purchaseDateCell);
				statementTable.addCell(billAmountCell);
				statementTable.addCell(paidDateCell);
				statementTable.addCell(amountPaidCell);
			}

			document.add(statementTable);

// Summary
			Table summaryTable = new Table(UnitValue.createPercentArray(new float[] { 80, 20 }));
			summaryTable.setWidth(UnitValue.createPercentValue(100));
			summaryTable.setMarginTop(10);
			summaryTable.setBackgroundColor(new DeviceRgb(200, 200, 200)); // Darker grey

			summaryTable
					.addCell(new Cell().add(new Paragraph("Total Bill").setBold().setTextAlignment(TextAlignment.RIGHT))
							.setBorder(Border.NO_BORDER));
			summaryTable.addCell(new Cell()
					.add(new Paragraph("₹" + String.format("%.2f", totalBill)).setBold()
							.setTextAlignment(TextAlignment.RIGHT).setFontColor(ColorConstants.BLACK))
					.setBorder(Border.NO_BORDER));

			summaryTable
					.addCell(new Cell().add(new Paragraph("Total Paid").setBold().setTextAlignment(TextAlignment.RIGHT))
							.setBorder(Border.NO_BORDER));
			summaryTable.addCell(new Cell()
					.add(new Paragraph("₹" + String.format("%.2f", totalPaid)).setBold()
							.setTextAlignment(TextAlignment.RIGHT).setFontColor(ColorConstants.BLACK))
					.setBorder(Border.NO_BORDER));

			summaryTable.addCell(
					new Cell().add(new Paragraph("Pending Amount").setBold().setTextAlignment(TextAlignment.RIGHT))
							.setBorder(Border.NO_BORDER));
			summaryTable.addCell(new Cell()
					.add(new Paragraph("₹" + String.format("%.2f", totalBill - totalPaid)).setBold()
							.setTextAlignment(TextAlignment.RIGHT).setFontColor(ColorConstants.BLACK))
					.setBorder(Border.NO_BORDER));

			document.add(summaryTable);

// Declaration
			document.add(
					new Paragraph("I declare all the information contained in this statement to be true and correct.")
							.setFontSize(10).setTextAlignment(TextAlignment.LEFT).setMarginTop(20));

// Footer
			document.add(new Paragraph("Thank You for Your Business!").setFontSize(14).setBold()
					.setTextAlignment(TextAlignment.CENTER).setFontColor(new DeviceRgb(30, 144, 255)).setMarginTop(10));
			document.add(new Paragraph("Contact us at: 9822888876 | Email: gauravyadavpureprotien@gmail.com")
					.setFontSize(10).setTextAlignment(TextAlignment.CENTER).setMarginTop(5));

// Page Border
			pdf.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
				PdfCanvas canvas = new PdfCanvas(((PdfDocumentEvent) event).getPage());
				canvas.setStrokeColor(ColorConstants.BLACK).setLineWidth(1)
						.rectangle(20, 20, com.itextpdf.kernel.geom.PageSize.A4.getWidth() - 40,
								com.itextpdf.kernel.geom.PageSize.A4.getHeight() - 40)
						.stroke();
			});

			document.close();
			JOptionPane.showMessageDialog(this, "Payment PDF generated successfully at: " + dest, "Success",
					JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception ex) {
			LOGGER.severe("Error generating payment PDF: " + ex.getMessage());
			JOptionPane.showMessageDialog(this, "Error generating payment PDF: " + ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}
}