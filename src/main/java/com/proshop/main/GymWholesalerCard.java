package com.proshop.main;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.proshop.model.GymBill;
import com.proshop.model.GymWholesaler;

public class GymWholesalerCard extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(GymWholesalerCard.class.getName());
    private final GymWholesaler wholesaler;
    private final GymWholesalerDAO dao;
    private final Map<GymBill, List<GymWholesaler>> transactions;
    private final JLabel amountPaidLabel;
    private final JLabel pendingAmountLabel;
    private final JLabel lastUpdatedLabel; // Label for earliest purchase date (card creation date)
    private static final Color BACKGROUND_COLOR = new Color(33, 33, 33);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color SUCCESS_COLOR = new Color(102, 187, 106);
    private static final Color PENDING_COLOR = new Color(239, 83, 80);
    private static final Color SHADOW_COLOR = new Color(224, 224, 224);
    private static final Color BUTTON_COLOR = new Color(50, 50, 50);

    public GymWholesalerCard(GymWholesaler wholesaler, CardLayout cardLayout, JPanel mainContentPanel,
            GymWholesalerDAO dao, Consumer<GymWholesaler> onCardClick) {
        this.wholesaler = wholesaler;
        this.dao = dao;
        this.transactions = new HashMap<>();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BACKGROUND_COLOR);
        setPreferredSize(new Dimension(250, 300)); // Increased height for new label
        setMaximumSize(new Dimension(250, 300));
        Border outerBorder = BorderFactory.createLineBorder(SHADOW_COLOR, 1);
        Border innerBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        setBorder(new CompoundBorder(outerBorder, innerBorder));

        add(Box.createVerticalGlue());

        JLabel nameLabel = new JLabel(
                "Name: " + (wholesaler.getWholesalerName() != null ? wholesaler.getWholesalerName() : "N/A"));
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        nameLabel.setForeground(TEXT_COLOR);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(nameLabel);
        add(Box.createVerticalStrut(6));

        JLabel mobileLabel = new JLabel(
                "Phone: " + (wholesaler.getMobileNo() != null ? wholesaler.getMobileNo() : "N/A"));
        mobileLabel.setFont(new Font("Arial", Font.BOLD, 16));
        mobileLabel.setForeground(TEXT_COLOR);
        mobileLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(mobileLabel);
        add(Box.createVerticalStrut(6));

        JLabel addressLabel = new JLabel(
                "Address: " + (wholesaler.getAddress() != null ? wholesaler.getAddress() : "N/A"));
        addressLabel.setFont(new Font("Arial", Font.BOLD, 16));
        addressLabel.setForeground(TEXT_COLOR);
        addressLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(addressLabel);
        add(Box.createVerticalStrut(6));

        amountPaidLabel = new JLabel("Paid: $0.00");
        amountPaidLabel.setFont(new Font("Arial", Font.BOLD, 16));
        amountPaidLabel.setForeground(SUCCESS_COLOR);
        amountPaidLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(amountPaidLabel);
        add(Box.createVerticalStrut(6));

        pendingAmountLabel = new JLabel("Pending: $0.00");
        pendingAmountLabel.setFont(new Font("Arial", Font.BOLD, 16));
        pendingAmountLabel.setForeground(PENDING_COLOR);
        pendingAmountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(pendingAmountLabel);
        add(Box.createVerticalStrut(6));

        // Last Updated Date Label (shows earliest purchase date as card creation date)
        lastUpdatedLabel = new JLabel("Created: N/A");
        lastUpdatedLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        lastUpdatedLabel.setForeground(TEXT_COLOR);
        lastUpdatedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(lastUpdatedLabel);
        add(Box.createVerticalStrut(10));

        JButton pdfButton = new JButton("Generate PDF");
        pdfButton.setFont(new Font("Arial", Font.BOLD, 14));
        pdfButton.setBackground(BUTTON_COLOR);
        pdfButton.setForeground(TEXT_COLOR);
        pdfButton.setFocusPainted(false);
        pdfButton.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
        pdfButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        pdfButton.setPreferredSize(new Dimension(120, 30));
        pdfButton.addActionListener(e -> generatePaymentPDF());
        add(pdfButton);
        add(Box.createVerticalGlue());

        setCursor(new Cursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() != pdfButton) {
                    onCardClick.accept(wholesaler);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setBorder(new CompoundBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 2), innerBorder));
                setBackground(BUTTON_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                setBorder(new CompoundBorder(outerBorder, innerBorder));
                setBackground(BACKGROUND_COLOR);
            }
        });

        try {
            updateFinancials();
        } catch (Exception e) {
            LOGGER.severe("Error initializing financials: " + e.getMessage());
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Failed to load financial data. Please try again. Error: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE));
        }
    }

    public GymWholesaler getWholesaler() {
        return wholesaler;
    }

    public void addTransaction(GymBill bill, GymWholesaler transaction) {
        transactions.computeIfAbsent(bill, k -> new ArrayList<>()).add(transaction);
        try {
            updateFinancials();
        } catch (Exception e) {
            LOGGER.severe("Error updating financials after transaction: " + e.getMessage());
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Failed to update financial data. Please try again. Error: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE));
        }
    }

    private void updateFinancials() {
        try {
            if (dao == null) {
                throw new IllegalStateException("GymWholesalerDAO is null");
            }
            Map<String, Object> paymentDetails = dao.fetchPaymentDetails(wholesaler);
            if (paymentDetails == null || paymentDetails.isEmpty()) {
                throw new IllegalStateException(
                        "No payment details retrieved for wholesaler: " + wholesaler.getWholesalerName());
            }

            Double totalBill = (Double) paymentDetails.get("totalBill");
            Double amountPaid = (Double) paymentDetails.get("totalPaid");
            if (totalBill == null || amountPaid == null) {
                throw new IllegalStateException("Missing totalBill or totalPaid in payment details");
            }

            double pendingAmount = totalBill - amountPaid;
            if (pendingAmount < 0) {
                pendingAmount = 0;
            }

            // Get the earliest purchase date as the card creation date
            String creationDate = "N/A";
            List<LocalDate> purchaseDates = dao.fetchPurchaseDates(wholesaler);
            if (!purchaseDates.isEmpty()) {
                LocalDate earliestDate = purchaseDates.stream()
                        .filter(date -> date != null)
                        .min(LocalDate::compareTo)
                        .orElse(null);
                if (earliestDate != null) {
                    creationDate = earliestDate.toString();
                }
            }

            amountPaidLabel.setText("Paid: $" + String.format("%.2f", amountPaid));
            pendingAmountLabel.setText("Pending: $" + String.format("%.2f", pendingAmount));
            lastUpdatedLabel.setText("Created: " + creationDate);
            revalidate();
            repaint();
        } catch (Exception e) {
            LOGGER.severe("Error fetching payment details for wholesaler " + wholesaler.getWholesalerName() + ": "
                    + e.getMessage());
            e.printStackTrace();
            amountPaidLabel.setText("Paid: $0.00");
            pendingAmountLabel.setText("Pending: $0.00");
            lastUpdatedLabel.setText("Created: N/A");
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Failed to load financial data for " + wholesaler.getWholesalerName()
                            + ". Error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE));
        }
    }

    private void generatePaymentPDF() {
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

            com.itextpdf.kernel.font.PdfFont timesRoman = PdfFontFactory.createFont("Times-Roman");

            document.add(new Paragraph("Gaurav Yadav Pure Protien")
                    .setFont(timesRoman)
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5));
            document.add(new Paragraph(
                    "Chatrapati Shivaji Maharaj Stadium, Near Venu Tai Chawhan Hall, Yashwantrao Park, Pawar Nagar, Karad, Maharashtra 415110")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Phone: 9822888876 | Email: gauravyadavpureprotien@gmail.com")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            document.add(new Paragraph("Payment Statement")
                    .setFont(timesRoman)
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginTop(10));
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
            infoTable.setWidth(UnitValue.createPercentValue(100));
            infoTable.setMarginTop(5);
            infoTable.setBackgroundColor(new DeviceRgb(200, 200, 200));

            Cell wholesalerCell = new Cell();
            wholesalerCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f));
            wholesalerCell.setPadding(10);
            wholesalerCell.add(new Paragraph("To: " + (wholesaler.getWholesalerName() != null ? wholesaler.getWholesalerName() : "Unknown"))
                    .setFontSize(12).setBold().setTextAlignment(TextAlignment.LEFT));
            wholesalerCell.add(new Paragraph(wholesaler.getAddress() != null ? wholesaler.getAddress() : "").setFontSize(10));
            wholesalerCell.add(new Paragraph("Phone: " + (wholesaler.getMobileNo() != null ? wholesaler.getMobileNo() : ""))
                    .setFontSize(10));
            infoTable.addCell(wholesalerCell);

            Cell dateCell = new Cell();
            dateCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f));
            dateCell.setPadding(10);
            dateCell.add(new Paragraph("Date: " + LocalDate.now()).setFontSize(10).setTextAlignment(TextAlignment.RIGHT));
            infoTable.addCell(dateCell);

            document.add(infoTable);
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Bill Details")
                    .setFont(timesRoman)
                    .setFontSize(14)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10)
                    .setBackgroundColor(new DeviceRgb(200, 200, 200))
                    .setFontColor(ColorConstants.BLACK)
                    .setPadding(5));

            float[] columnWidths = {3, 3, 3, 3, 3};
            Table statementTable = new Table(columnWidths);
            statementTable.setWidth(UnitValue.createPercentValue(100));
            statementTable.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 1));
            statementTable.setMarginTop(10);

            statementTable.addHeaderCell(new Cell().add(new Paragraph("Bill Date").setBold().setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(new DeviceRgb(200, 200, 200)).setFontColor(ColorConstants.BLACK).setPadding(8));
            statementTable.addHeaderCell(new Cell().add(new Paragraph("Main Bill").setBold().setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(new DeviceRgb(200, 200, 200)).setFontColor(ColorConstants.BLACK).setPadding(8));
            statementTable.addHeaderCell(new Cell().add(new Paragraph("Date Amount Paid").setBold().setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(new DeviceRgb(200, 200, 200)).setFontColor(ColorConstants.BLACK).setPadding(8));
            statementTable.addHeaderCell(new Cell().add(new Paragraph("Amount Paid").setBold().setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(new DeviceRgb(200, 200, 200)).setFontColor(ColorConstants.BLACK).setPadding(8));
            statementTable.addHeaderCell(new Cell().add(new Paragraph("Pending Amount").setBold().setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(new DeviceRgb(200, 200, 200)).setFontColor(ColorConstants.BLACK).setPadding(8));

            double totalBill = 0.0;
            double totalPaid = 0.0;
            Map<String, Object> paymentDetails = dao.fetchPaymentDetails(wholesaler);
            @SuppressWarnings("unchecked")
            List<Object[]> bills = (List<Object[]>) paymentDetails.get("bills");
            @SuppressWarnings("unchecked")
            List<Object[]> payments = (List<Object[]>) paymentDetails.get("payments");
            int maxRows = Math.max(bills != null ? bills.size() : 0, payments != null ? payments.size() : 0);

            for (int i = 0; i < maxRows; i++) {
                Cell billDateCell = new Cell();
                Cell billAmountCell = new Cell();
                Cell paidDateCell = new Cell();
                Cell amountPaidCell = new Cell();
                Cell pendingCell = new Cell();

                DeviceRgb rowColor = (i % 2 == 0) ? new DeviceRgb(220, 220, 220) : new DeviceRgb(180, 180, 180);
                billDateCell.setBackgroundColor(rowColor);
                billAmountCell.setBackgroundColor(rowColor);
                paidDateCell.setBackgroundColor(rowColor);
                amountPaidCell.setBackgroundColor(rowColor);
                pendingCell.setBackgroundColor(rowColor);

                billDateCell.setPadding(8);
                billAmountCell.setPadding(8);
                paidDateCell.setPadding(8);
                amountPaidCell.setPadding(8);
                pendingCell.setPadding(8);

                String billDate = "";
                double billValue = 0.0;
                double pendingValue = 0.0;
                if (bills != null && i < bills.size()) {
                    billDate = (String) bills.get(i)[0];
                    String billAmount = (String) bills.get(i)[1];
                    try {
                        if (billAmount != null && !billAmount.trim().isEmpty()) {
                            billValue = Double.parseDouble(billAmount);
                            totalBill += billValue;
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.warning("Invalid bill amount at row " + i + ": " + billAmount);
                    }
                    if (bills.get(i).length > 2 && bills.get(i)[2] != null) {
                        String pending = (String) bills.get(i)[2];
                        try {
                            if (pending != null && !pending.trim().isEmpty()) {
                                pendingValue = Double.parseDouble(pending);
                            }
                        } catch (NumberFormatException e) {
                            LOGGER.warning("Invalid pending amount at row " + i + ": " + pending);
                        }
                    }
                }

                String paidDate = "";
                double paidValue = 0.0;
                if (payments != null && i < payments.size()) {
                    paidDate = (String) payments.get(i)[0];
                    String amountPaid = (String) payments.get(i)[1];
                    try {
                        if (amountPaid != null && !amountPaid.trim().isEmpty()) {
                            paidValue = Double.parseDouble(amountPaid.replace("₹", "").trim());
                            totalPaid += paidValue;
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.warning("Invalid amount paid at row " + i + ": " + amountPaid);
                    }
                }

                if (pendingValue == 0.0 && billValue > 0.0) {
                    pendingValue = billValue - (i == 0 ? paidValue : 0.0);
                }

                billDateCell.add(new Paragraph(billDate != null ? billDate : "").setTextAlignment(TextAlignment.CENTER));
                billAmountCell.add(new Paragraph(billValue > 0 ? "₹" + String.format("%.2f", billValue) : "").setTextAlignment(TextAlignment.RIGHT));
                paidDateCell.add(new Paragraph(paidDate != null && !paidDate.equals(".") ? paidDate : "").setTextAlignment(TextAlignment.CENTER));
                amountPaidCell.add(new Paragraph(paidValue > 0 ? "₹" + String.format("%.2f", paidValue) : "").setTextAlignment(TextAlignment.RIGHT));
                pendingCell.add(new Paragraph(pendingValue != 0 ? "₹" + String.format("%.2f", pendingValue) : "").setTextAlignment(TextAlignment.RIGHT));

                billDateCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f));
                billAmountCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f));
                paidDateCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f));
                amountPaidCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f));
                pendingCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f));

                statementTable.addCell(billDateCell);
                statementTable.addCell(billAmountCell);
                statementTable.addCell(paidDateCell);
                statementTable.addCell(amountPaidCell);
                statementTable.addCell(pendingCell);
            }

            document.add(statementTable);

            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{80, 20}));
            summaryTable.setWidth(UnitValue.createPercentValue(100));
            summaryTable.setMarginTop(10);
            summaryTable.setBackgroundColor(new DeviceRgb(200, 200, 200));

            summaryTable.addCell(new Cell().add(new Paragraph("Total Bill").setBold().setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            summaryTable.addCell(new Cell().add(new Paragraph("₹" + String.format("%.2f", totalBill)).setBold()
                    .setTextAlignment(TextAlignment.RIGHT).setFontColor(ColorConstants.BLACK))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));

            summaryTable.addCell(new Cell().add(new Paragraph("Total Paid").setBold().setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            summaryTable.addCell(new Cell().add(new Paragraph("₹" + String.format("%.2f", totalPaid)).setBold()
                    .setTextAlignment(TextAlignment.RIGHT).setFontColor(ColorConstants.BLACK))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));

            summaryTable.addCell(new Cell().add(new Paragraph("Pending Amount").setBold().setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            summaryTable.addCell(new Cell().add(new Paragraph("₹" + String.format("%.2f", totalBill - totalPaid)).setBold()
                    .setTextAlignment(TextAlignment.RIGHT).setFontColor(ColorConstants.BLACK))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));

            document.add(summaryTable);

            document.add(new Paragraph("MAIN BILL IS ₹" + String.format("%.2f", totalBill) + 
                    " AND TOTAL AMOUNT PAID IS ₹" + String.format("%.2f", totalPaid) + 
                    " AND AMOUNT TO BE PAID IS ₹" + String.format("%.2f", totalBill - totalPaid))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginTop(10));

            document.add(new Paragraph("NOTE: THIS IS NOT FINAL IF ANY QUIRIES PLEASE CALL ON 9822888876")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginTop(10));

            document.add(new Paragraph("I declare all the information contained in this statement to be true and correct.")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginTop(10));

            document.add(new Paragraph("Thank You for Your Business!")
                    .setFontSize(14)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(new DeviceRgb(30, 144, 255))
                    .setMarginTop(10));
            document.add(new Paragraph("Contact us at: 9822888876 | Email: gauravyadavpureprotien@gmail.com")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(5));

            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
                PdfCanvas canvas = new PdfCanvas(((PdfDocumentEvent) event).getPage());
                canvas.setStrokeColor(ColorConstants.BLACK)
                        .setLineWidth(1)
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