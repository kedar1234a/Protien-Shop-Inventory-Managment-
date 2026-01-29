package com.proshop.main;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
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
import com.proshop.model.Bill;
import com.proshop.model.Payment;
import com.proshop.model.Product;
import com.proshop.model.WholesalerPurchase;

public class WholesalerBillPDF {
    private static final Logger LOGGER = Logger.getLogger(WholesalerBillPDF.class.getName());
    private final WholesalerPurchase wholesaler;
    private final Bill bill;

    public WholesalerBillPDF(WholesalerPurchase wholesaler, Bill bill) {
        this.wholesaler = wholesaler;
        this.bill = bill;
    }

    public void generateBillPDF(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Bill PDF");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        fileChooser.setSelectedFile(new File("bill_" + bill.getDate().toString() + ".pdf"));
        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".pdf")) {
                filePath += ".pdf";
            }
            try {
                PdfWriter writer = new PdfWriter(new FileOutputStream(filePath));
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document doc = new Document(pdfDoc, com.itextpdf.kernel.geom.PageSize.A4);
                doc.setMargins(30, 30, 30, 30);

                PdfFont timesRoman = PdfFontFactory.createFont("Times-Roman");

                // Header
                doc.add(new Paragraph("Gaurav Yadav Pure Protien")
                        .setFont(timesRoman)
                        .setFontSize(24)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(5));
                doc.add(new Paragraph(
                        "Chatrapati Shivaji Maharaj Stadium, Near Venu Tai Chawhan Hall, Yashwantrao Park, Pawar Nagar, Karad, Maharashtra 415110")
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER));
                doc.add(new Paragraph("Phone: 9822888876 | Email: gauravyadavpureprotien@gmail.com")
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(10));

                // Invoice Title
                doc.add(new Paragraph("Invoice")
                        .setFont(timesRoman)
                        .setFontSize(18)
                        .setBold()
                        .setTextAlignment(TextAlignment.LEFT)
                        .setMarginTop(10));

                // Wholesaler Info Table
                Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
                infoTable.setWidth(UnitValue.createPercentValue(100));
                infoTable.setMarginTop(5);
                infoTable.setBackgroundColor(new DeviceRgb(200, 200, 200)); // Light grey background

                Cell wholesalerCell = new Cell();
                wholesalerCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f)); // Darker grey border
                wholesalerCell.setPadding(10);
                wholesalerCell.add(new Paragraph("To: " + (wholesaler.getWholesalerName() != null ? wholesaler.getWholesalerName() : "N/A"))
                        .setFontSize(12).setBold()
                        .setTextAlignment(TextAlignment.LEFT));
                wholesalerCell.add(new Paragraph(wholesaler.getAddress() != null ? wholesaler.getAddress() : "N/A").setFontSize(10));
                wholesalerCell.add(new Paragraph("Phone: " + (wholesaler.getPhoneNo() != null ? wholesaler.getPhoneNo() : "N/A")).setFontSize(10));
                infoTable.addCell(wholesalerCell);

                Cell invoiceCell = new Cell();
                invoiceCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f)); // Darker grey border
                invoiceCell.setPadding(10);
                invoiceCell.add(new Paragraph("Invoice Date: " + bill.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.RIGHT));
                infoTable.addCell(invoiceCell);

                doc.add(infoTable);
                doc.add(new Paragraph("\n"));

                // Products Table
                float[] columnWidths = {4, 2, 2, 2};
                Table table = new Table(columnWidths);
                table.setWidth(UnitValue.createPercentValue(100));
                table.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 1)); // Darker grey border
                table.setMarginTop(10);

                // Table Headers
                table.addHeaderCell(
                        new Cell().add(new Paragraph("Product Name").setBold().setTextAlignment(TextAlignment.CENTER))
                                .setBackgroundColor(new DeviceRgb(200, 200, 200)) // Light grey background
                                .setFontColor(ColorConstants.BLACK));
                table.addHeaderCell(
                        new Cell().add(new Paragraph("Quantity").setBold().setTextAlignment(TextAlignment.CENTER))
                                .setBackgroundColor(new DeviceRgb(200, 200, 200)) // Light grey background
                                .setFontColor(ColorConstants.BLACK));
                table.addHeaderCell(
                        new Cell().add(new Paragraph("Unit Price").setBold().setTextAlignment(TextAlignment.CENTER))
                                .setBackgroundColor(new DeviceRgb(200, 200, 200)) // Light grey background
                                .setFontColor(ColorConstants.BLACK));
                table.addHeaderCell(
                        new Cell().add(new Paragraph("Amount").setBold().setTextAlignment(TextAlignment.CENTER))
                                .setBackgroundColor(new DeviceRgb(200, 200, 200)) // Light grey background
                                .setFontColor(ColorConstants.BLACK));

                // Fetch products
                List<Product> products = DatabaseUtils.fetchProductsForBill(bill.getId(), wholesaler.getId());
                double totalAmount = 0.0;
                int totalQuantity = 0;

                for (Product product : products) {
                    String productName = product.getProductName() != null ? product.getProductName() : "N/A";
                    int quantity = product.getQuantity();
                    double unitPrice = product.getPerPieceRate() != null ? product.getPerPieceRate().doubleValue() : 0.0;
                    double amount = product.getTotal() != null ? product.getTotal().doubleValue() : 0.0;

                    table.addCell(new Cell().add(new Paragraph(productName).setTextAlignment(TextAlignment.LEFT))
                            .setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f))); // Darker grey border
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(quantity)).setTextAlignment(TextAlignment.CENTER))
                            .setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f))); // Darker grey border
                    table.addCell(new Cell().add(new Paragraph(String.format("%.2f", unitPrice)).setTextAlignment(TextAlignment.RIGHT))
                            .setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f))); // Darker grey border
                    table.addCell(new Cell().add(new Paragraph("₹" + String.format("%.2f", amount)).setTextAlignment(TextAlignment.RIGHT))
                            .setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f))); // Darker grey border

                    totalQuantity += quantity;
                    totalAmount += amount;
                }

                doc.add(table);

                // Payment Details Table
                Table paymentTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
                paymentTable.setWidth(UnitValue.createPercentValue(100));
                paymentTable.setMarginTop(10);
                paymentTable.setBackgroundColor(new DeviceRgb(200, 200, 200)); // Light grey background

                // Fetch payment details
                DatabaseUtils.PaymentSummary paymentSummary = DatabaseUtils.fetchPaymentsForBill(bill.getId());
                double totalPaidAmount = paymentSummary.getTotalPaidAmount() != null ? paymentSummary.getTotalPaidAmount().doubleValue() : 0.0;
                double pendingAmount = totalAmount - totalPaidAmount;
                double shippingCharges = bill.getShippingCharges() != null ? bill.getShippingCharges().doubleValue() : 0.0;

                paymentTable.addCell(new Cell().add(new Paragraph("Total Bill: ₹" + String.format("%.2f", totalAmount)).setFontSize(10))
                        .setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f)).setPadding(5));
                paymentTable.addCell(new Cell().add(new Paragraph("Parcel: ₹" + String.format("%.2f", shippingCharges)).setFontSize(10))
                        .setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f)).setPadding(5));

                // Add payment rows
                List<Payment> payments = paymentSummary.getPayments();
                for (Payment payment : payments) {
                    String paidDate = payment.getPaidDate() != null ? payment.getPaidDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "N/A";
                    double paidAmount = payment.getPaidAmount() != null ? payment.getPaidAmount().doubleValue() : 0.0;
                    paymentTable.addCell(new Cell().add(new Paragraph("Paid on " + paidDate + ": ₹" + String.format("%.2f", paidAmount))
                            .setFontSize(10)).setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f)).setPadding(5));
                    paymentTable.addCell(new Cell().add(new Paragraph("")).setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f)).setPadding(5));
                }

                paymentTable.addCell(new Cell().add(new Paragraph("Pending Amount: ₹" + String.format("%.2f", pendingAmount))
                        .setFontSize(10)).setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f)).setPadding(5));
                paymentTable.addCell(new Cell().add(new Paragraph("")).setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f)).setPadding(5));

                doc.add(paymentTable);

                // Totals Table
                Table totalTable = new Table(UnitValue.createPercentArray(new float[]{80, 20}));
                totalTable.setWidth(UnitValue.createPercentValue(100));
                totalTable.setMarginTop(10);
                totalTable.setBackgroundColor(new DeviceRgb(200, 200, 200)); // Light grey background

                totalTable.addCell(new Cell().add(new Paragraph("Total Quantity: " + totalQuantity).setTextAlignment(TextAlignment.RIGHT))
                        .setBorder(Border.NO_BORDER));
                totalTable.addCell(new Cell().add(new Paragraph("")).setBorder(Border.NO_BORDER));
                totalTable.addCell(new Cell().add(new Paragraph("Total Amount").setBold().setTextAlignment(TextAlignment.RIGHT))
                        .setBorder(Border.NO_BORDER));
                totalTable.addCell(new Cell().add(new Paragraph("₹" + String.format("%.2f", totalAmount + shippingCharges)).setBold()
                        .setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));

                doc.add(totalTable);

                // Declaration
                doc.add(new Paragraph("I declare all the information contained in this invoice to be true and correct.")
                        .setFontSize(10).setTextAlignment(TextAlignment.LEFT).setMarginTop(10));

                // Footer
                doc.add(new Paragraph("Thank You for Your Business!").setFontSize(14).setBold()
                        .setTextAlignment(TextAlignment.CENTER).setFontColor(new DeviceRgb(40, 144, 350))
                        .setMarginTop(10));
                doc.add(new Paragraph("Contact us at: 9822888876 | Email: gauravyadavpureprotien@gmail.com")
                        .setFontSize(10).setTextAlignment(TextAlignment.CENTER).setMarginTop(5));

                // Page Border
                pdfDoc.addEventHandler(com.itextpdf.kernel.events.PdfDocumentEvent.END_PAGE, event -> {
                    PdfCanvas canvas = new PdfCanvas(((com.itextpdf.kernel.events.PdfDocumentEvent) event).getPage());
                    canvas.setStrokeColor(ColorConstants.BLACK).setLineWidth(1)
                            .rectangle(20, 20, com.itextpdf.kernel.geom.PageSize.A4.getWidth() - 40, com.itextpdf.kernel.geom.PageSize.A4.getHeight() - 40).stroke();
                });

                doc.close();
                JOptionPane.showMessageDialog(null, "Bill PDF generated successfully at " + filePath, "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                LOGGER.severe("Error generating bill PDF: " + ex.getMessage());
                JOptionPane.showMessageDialog(null, "Error generating bill PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}