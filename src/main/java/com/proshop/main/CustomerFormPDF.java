package com.proshop.main;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

public class CustomerFormPDF {
    private static final Logger LOGGER = Logger.getLogger(CustomerFormPDF.class.getName());
    private final CustomerForm customerForm;
    private final CustomerFormUI customerFormUI;

    public CustomerFormPDF(CustomerForm customerForm) {
        this.customerForm = customerForm;
        this.customerFormUI = customerForm.getFormUI();
    }

    public void generatePDF(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Customer Report PDF");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        if (fileChooser.showSaveDialog(customerForm) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".pdf")) {
                filePath += ".pdf";
            }
            try {
                PdfWriter writer = new PdfWriter(new FileOutputStream(filePath));
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document doc = new Document(pdfDoc);
                doc.setMargins(10, 10, 10, 10); // Tight margins to fit table
                PdfFont font = PdfFontFactory.createFont("Helvetica");
                PdfFont fontBold = PdfFontFactory.createFont("Helvetica-Bold");

                // Add heading
                Paragraph heading = new Paragraph("Customer Report")
                    .setFont(fontBold)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5);
                doc.add(heading);

                // Define very small column widths to fit 15 columns
                float[] columnWidths = {2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f}; // Uniform small widths
                Table table = new Table(UnitValue.createPercentArray(columnWidths));
                table.setWidth(UnitValue.createPercentValue(100));

                // Headers (non-bold, plain text)
                String[] headers = {
                    "ID", "Customer Name", "Product Name", "Quantity", "Buying Price", "Selling Price",
                    "Total Amount", "Final Bill", "Net Profit", "Payment Mode", "Status",
                    "Mobile No", "Amount Paid", "Payment Date", "Pending Amount"
                };
                for (String header : headers) {
                    Cell cell = new Cell()
                        .setFont(font)
                        .setFontSize(8)
                        .setTextAlignment(TextAlignment.CENTER)
                        .add(new Paragraph(header));
                    table.addHeaderCell(cell);
                }

                // Data rows
                for (int i = 0; i < customerFormUI.getCustomerTable().getRowCount(); i++) {
                    int modelRow = customerFormUI.getCustomerTable().convertRowIndexToModel(i);
                    for (int j = 0; j < customerFormUI.getCustomerTable().getColumnCount(); j++) {
                        Object value = customerFormUI.getTableModel().getValueAt(modelRow, j);
                        String text = value == null ? "" : value instanceof LocalDate ? ((LocalDate) value).format(DateTimeFormatter.ISO_LOCAL_DATE) : value.toString();
                        Cell cell = new Cell()
                            .setFont(font)
                            .setFontSize(8)
                            .setTextAlignment(TextAlignment.CENTER)
                            .add(new Paragraph(text));
                        table.addCell(cell);
                    }
                }

                doc.add(table);
                doc.close();
                JOptionPane.showMessageDialog(customerForm, "PDF generated successfully at " + filePath, "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                LOGGER.severe("Error generating PDF: " + ex.getMessage());
                JOptionPane.showMessageDialog(customerForm, "Error generating PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void generateIndividualBillPDF(ActionEvent e) {
        int[] selectedRows = customerFormUI.getCustomerTable().getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(customerForm, "Please select at least one customer to generate bill.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Customer Bills PDF");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        fileChooser.setSelectedFile(new File("invoices_" + LocalDate.now().toString() + ".pdf"));
        if (fileChooser.showSaveDialog(customerForm) != JFileChooser.APPROVE_OPTION) {
            return;
        }

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
            PdfFont helvetica = PdfFontFactory.createFont("Helvetica");

            // Validate table model column count
            int columnCount = customerFormUI.getTableModel().getColumnCount();
            if (columnCount != 15) {
                LOGGER.warning("Table model has " + columnCount + " columns, expected 15. Adjusting logic to use available columns.");
            }

            // Log column data for debugging
            LOGGER.info("Column headers: " + Arrays.asList(
                "ID", "Customer Name", "Product Name", "Quantity", "Buying Price", "Selling Price",
                "Total Amount", "Final Bill", "Net Profit", "Payment Mode", "Status",
                "Mobile No", "Amount Paid", "Payment Date", "Pending Amount"
            ));

            // Group selected rows by customerName
            Map<String, List<Integer>> customerToRows = new HashMap<>();
            for (int selectedRow : selectedRows) {
                int modelRow = customerFormUI.getCustomerTable().convertRowIndexToModel(selectedRow);
                String customerName = getSafeStringValue(modelRow, 1, "Customer");
                customerToRows.computeIfAbsent(customerName, k -> new ArrayList<>()).add(modelRow);
            }

            // Process each customer
            int customerIndex = 0;
            for (Map.Entry<String, List<Integer>> entry : customerToRows.entrySet()) {
                String customerName = entry.getKey();
                List<Integer> modelRows = entry.getValue();

                // Use the first row for customer info
                int firstRow = modelRows.get(0);
                LocalDate date = getSafeLocalDateValue(firstRow, 13, LocalDate.now()); // Payment Date
                String mobile = getSafeStringValue(firstRow, 11, ""); // Mobile No

                // Log data for debugging
                LOGGER.info("Processing customer: " + customerName + ", Mobile: " + mobile + ", Date: " + date);

                // Header
                doc.add(new Paragraph("Gaurav Yadav Pure Protien")
                        .setFont(timesRoman)
                        .setFontSize(24)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(5));
                doc.add(new Paragraph("Chatrapati Shivaji Maharaj Stadium, Near Venu Tai Chawhan Hall, Yashwantrao Park, Pawar Nagar, Karad, Maharashtra 415110")
                        .setFont(helvetica)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER));
                doc.add(new Paragraph("Phone: 8823858825 | Email: gauravyadavpureprotien@gmail.com")
                        .setFont(helvetica)
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

                // Customer Info Table
                Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
                infoTable.setWidth(UnitValue.createPercentValue(100));
                infoTable.setMarginTop(5);

                Cell customerCell = new Cell();
                customerCell.setPadding(10);
                customerCell.add(new Paragraph("To: " + customerName).setFont(helvetica).setFontSize(12).setBold());
                customerCell.add(new Paragraph("Mobile: " + mobile).setFont(helvetica).setFontSize(10));
                infoTable.addCell(customerCell);

                Cell invoiceCell = new Cell();
                invoiceCell.setPadding(10);
                invoiceCell.add(new Paragraph("Invoice Date: " + date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .setFont(helvetica).setFontSize(10).setTextAlignment(TextAlignment.RIGHT));
                infoTable.addCell(invoiceCell);

                doc.add(infoTable);
                doc.add(new Paragraph("\n"));

                // Products Table
                float[] columnWidths = {4, 2, 2, 2};
                Table table = new Table(columnWidths);
                table.setWidth(UnitValue.createPercentValue(100));
                table.setMarginTop(10);

                // Table Headers
                table.addHeaderCell(new Cell().add(new Paragraph("Product Name").setBold().setTextAlignment(TextAlignment.CENTER))
                        .setFont(helvetica));
                table.addHeaderCell(new Cell().add(new Paragraph("Quantity").setBold().setTextAlignment(TextAlignment.CENTER))
                        .setFont(helvetica));
                table.addHeaderCell(new Cell().add(new Paragraph("Unit Price").setBold().setTextAlignment(TextAlignment.CENTER))
                        .setFont(helvetica));
                table.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold().setTextAlignment(TextAlignment.CENTER))
                        .setFont(helvetica));

                // Aggregate totals
                int totalQuantity = 0;
                double totalAmount = 0.0;

                // Add products for this customer
                for (int modelRow : modelRows) {
                    String productName = getSafeStringValue(modelRow, 2, "");
                    int quantity = getSafeIntValue(modelRow, 3, 0);
                    double sellingPrice = getSafeDoubleValue(modelRow, 5, 0.0);
                    double amount = getSafeDoubleValue(modelRow, 7, 0.0);

                    // Log product data for debugging
                    LOGGER.info("Product: " + productName + ", Quantity: " + quantity + ", Selling Price: " + sellingPrice + ", Amount: " + amount);

                    table.addCell(new Cell().add(new Paragraph(productName).setTextAlignment(TextAlignment.LEFT))
                            .setFont(helvetica).setFontSize(10));
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(quantity)).setTextAlignment(TextAlignment.CENTER))
                            .setFont(helvetica).setFontSize(10));
                    table.addCell(new Cell().add(new Paragraph(String.format("%.2f", sellingPrice)).setTextAlignment(TextAlignment.RIGHT))
                            .setFont(helvetica).setFontSize(10));
                    table.addCell(new Cell().add(new Paragraph(String.format("%.2f", amount)).setTextAlignment(TextAlignment.RIGHT))
                            .setFont(helvetica).setFontSize(10));

                    totalQuantity += quantity;
                    totalAmount += amount;
                }

                doc.add(table);

                // Payment Details Table
                Table paymentTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
                paymentTable.setWidth(UnitValue.createPercentValue(100));
                paymentTable.setMarginTop(10);

                // Aggregate payment details
                String paymentMode = getSafeStringValue(firstRow, 9, "");
                double amountPaid = modelRows.stream().mapToDouble(row -> getSafeDoubleValue(row, 12, 0.0)).sum();
                double pendingAmount = modelRows.stream().mapToDouble(row -> getSafeDoubleValue(row, 14, 0.0)).sum();
                String status = getSafeStringValue(firstRow, 10, "");
                LocalDate paymentDate = getSafeLocalDateValue(firstRow, 13, null);

                // Log payment details for debugging
                LOGGER.info("Payment Mode: " + paymentMode + ", Amount Paid: " + amountPaid + ", Pending Amount: " + pendingAmount + ", Status: " + status + ", Payment Date: " + (paymentDate != null ? paymentDate : "null"));

                paymentTable.addCell(new Cell().add(new Paragraph("Payment Mode: " + paymentMode).setFont(helvetica).setFontSize(10))
                        .setPadding(5));
                paymentTable.addCell(new Cell().add(new Paragraph("Amount Paid: " + String.format("%.2f", amountPaid)).setFont(helvetica).setFontSize(10))
                        .setPadding(5));
                paymentTable.addCell(new Cell().add(new Paragraph("Status: " + status).setFont(helvetica).setFontSize(10))
                        .setPadding(5));
                paymentTable.addCell(new Cell().add(new Paragraph("Pending Amount: " + String.format("%.2f", pendingAmount)).setFont(helvetica).setFontSize(10))
                        .setPadding(5));
                if (paymentDate != null) {
                    paymentTable.addCell(new Cell().add(new Paragraph("Payment Date: " + paymentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)).setFont(helvetica).setFontSize(10))
                            .setPadding(5));
                    paymentTable.addCell(new Cell().add(new Paragraph("")).setPadding(5));
                }

                doc.add(paymentTable);

                // Totals Table
                Table totalTable = new Table(UnitValue.createPercentArray(new float[]{80, 20}));
                totalTable.setWidth(UnitValue.createPercentValue(100));
                totalTable.setMarginTop(10);

                totalTable.addCell(new Cell().add(new Paragraph("Total Quantity: " + totalQuantity).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setFontSize(10)));
                totalTable.addCell(new Cell().add(new Paragraph("")).setTextAlignment(TextAlignment.RIGHT));
                totalTable.addCell(new Cell().add(new Paragraph("Total Amount").setBold().setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setFontSize(10)));
                totalTable.addCell(new Cell().add(new Paragraph(String.format("₹%.2f", totalAmount)).setBold().setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setFontSize(10)));

                doc.add(totalTable);

                // Declaration
                doc.add(new Paragraph("I declare all the information contained in this invoice to be true and correct.")
                        .setFont(helvetica).setFontSize(10).setTextAlignment(TextAlignment.LEFT).setMarginTop(10));

                // Footer
                doc.add(new Paragraph("Thank You for Your Business!").setFont(helvetica).setFontSize(14).setBold()
                        .setTextAlignment(TextAlignment.CENTER).setMarginTop(10));
                doc.add(new Paragraph("Contact us at: 8823858825 | Email: gauravyadavpureprotien@gmail.com")
                        .setFont(helvetica).setFontSize(10).setTextAlignment(TextAlignment.CENTER).setMarginTop(5));

                // Add new page for next customer, except for the last one
                if (customerIndex < customerToRows.size() - 1) {
                    doc.add(new Paragraph("").setFixedLeading(0).setMarginBottom(0));
                    pdfDoc.addNewPage();
                }
                customerIndex++;
            }

            doc.close();
            JOptionPane.showMessageDialog(customerForm, "Bill PDF for selected customers generated successfully at " + filePath, "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            LOGGER.severe("Error generating bill PDF: " + ex.getMessage());
            JOptionPane.showMessageDialog(customerForm, "Error generating bill PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Helper methods for safe data retrieval
    private String getSafeStringValue(int row, int column, String defaultValue) {
        if (column >= customerFormUI.getTableModel().getColumnCount()) {
            LOGGER.warning("Column " + column + " is out of bounds. Returning default value: " + defaultValue);
            return defaultValue;
        }
        Object value = customerFormUI.getTableModel().getValueAt(row, column);
        String result = value != null ? value.toString() : defaultValue;
        if (column == 10 && !Arrays.asList("Paid", "Pending", "").contains(result)) {
            LOGGER.warning("Invalid status value at row " + row + ", column " + column + ": " + result + ". Using default: " + defaultValue);
            return defaultValue;
        }
        return result;
    }

    private int getSafeIntValue(int row, int column, int defaultValue) {
        if (column >= customerFormUI.getTableModel().getColumnCount()) {
            LOGGER.warning("Column " + column + " is out of bounds. Returning default value: " + defaultValue);
            return defaultValue;
        }
        Object value = customerFormUI.getTableModel().getValueAt(row, column);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value != null ? value.toString() : String.valueOf(defaultValue));
        } catch (NumberFormatException ex) {
            LOGGER.warning("Invalid integer value at row " + row + ", column " + column + ": " + value);
            return defaultValue;
        }
    }

    private double getSafeDoubleValue(int row, int column, double defaultValue) {
        if (column >= customerFormUI.getTableModel().getColumnCount()) {
            LOGGER.warning("Column " + column + " is out of bounds. Returning default value: " + defaultValue);
            return defaultValue;
        }
        Object value = customerFormUI.getTableModel().getValueAt(row, column);
        if (value instanceof Number) {
            double result = ((Number) value).doubleValue();
            if (column == 12 && result == 0.0) {
                LOGGER.warning("Amount Paid is zero at row " + row + ", column " + column + ". Verify data correctness.");
            }
            return result;
        }
        try {
            double result = Double.parseDouble(value != null ? value.toString() : String.valueOf(defaultValue));
            if (column == 12 && result == 0.0) {
                LOGGER.warning("Amount Paid is zero at row " + row + ", column " + column + ". Verify data correctness.");
            }
            return result;
        } catch (NumberFormatException ex) {
            LOGGER.warning("Invalid double value at row " + row + ", column " + column + ": " + value);
            return defaultValue;
        }
    }

    private LocalDate getSafeLocalDateValue(int row, int column, LocalDate defaultValue) {
        if (column >= customerFormUI.getTableModel().getColumnCount()) {
            LOGGER.warning("Column " + column + " is out of bounds. Returning default value: " + defaultValue);
            return defaultValue;
        }
        Object value = customerFormUI.getTableModel().getValueAt(row, column);
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        try {
            return LocalDate.parse(value.toString(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ex) {
            LOGGER.warning("Invalid date value at row " + row + ", column " + column + ": " + value);
            return defaultValue;
        }
    }
}