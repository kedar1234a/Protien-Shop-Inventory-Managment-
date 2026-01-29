package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
import javax.swing.table.JTableHeader;

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
import com.proshop.connection.DBUtil;
import com.proshop.model.GymWholesaler;
import com.toedter.calendar.JDateChooser;

public class GymWholesalerViewByDate extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(GymWholesalerViewByDate.class.getName());
    private static final Color STAT_CARD_COLOR = new Color(50, 50, 50); // Dark gray for card background (#323232)
    private static final Color FAINT_ROW_COLOR = new Color(66, 66, 66); // Light gray for hover (#424242)
    private static final Color SHADOW_COLOR = new Color(224, 224, 224); // Very light gray for border (#E0E0E0)
    private final GymWholesaler wholesaler;
    private final GymWholesalerDAO dao;
    private final JPanel productsPanel;
    private final JDateChooser dateChooser;
    private JTable productTable;

    public GymWholesalerViewByDate(GymWholesaler wholesaler, CardLayout cardLayout, JPanel mainContentPanel, GymWholesalerDAO dao) {
        this.wholesaler = wholesaler;
        this.dao = dao;
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);

        // Wholesaler Name Heading
        JPanel headingPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headingPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        JLabel wholesalerNameLabel = new JLabel(wholesaler.getWholesalerName());
        wholesalerNameLabel.setFont(UIUtils.LABEL_FONT.deriveFont(18f));
        wholesalerNameLabel.setForeground(UIUtils.TEXT_COLOR);
        headingPanel.add(wholesalerNameLabel);

        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filterPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        dateChooser = UIUtils.createStyledDateChooser();
        dateChooser.setDateFormatString("yyyy-MM-dd");
        JLabel dateLabel = new JLabel("Select Date:");
        dateLabel.setForeground(UIUtils.TEXT_COLOR);
        dateLabel.setFont(UIUtils.LABEL_FONT);
        JButton filterButton = UIUtils.createStyledButton("Filter");
        filterPanel.add(dateLabel);
        filterPanel.add(dateChooser);
        filterPanel.add(filterButton);

        // Combine heading and filter panels
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        topPanel.add(headingPanel, BorderLayout.NORTH);
        topPanel.add(filterPanel, BorderLayout.CENTER);

        productsPanel = new JPanel();
        productsPanel.setLayout(new BoxLayout(productsPanel, BoxLayout.Y_AXIS));
        productsPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        JScrollPane scrollPane = new JScrollPane(productsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scrollPane.getViewport().setBackground(UIUtils.BACKGROUND_COLOR);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        JButton backButton = UIUtils.createStyledButton("Back");
        buttonPanel.add(backButton);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        filterButton.addActionListener(e -> loadProductsByDate());
        backButton.addActionListener(e -> cardLayout.show(mainContentPanel, "GYM_WHOLESALER"));

        loadProductsByDate();
    }

    private void loadProductsByDate() {
        try {
            LocalDate selectedDate = dateChooser.getDate() != null
                    ? dateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    : null;

            productsPanel.removeAll();
            List<LocalDate> dates = selectedDate == null ? dao.fetchPurchaseDates(wholesaler) : List.of(selectedDate);

            for (LocalDate date : dates) {
                List<GymWholesaler> products = dao.fetchProductsByDate(wholesaler, date);
                if (!products.isEmpty()) {
                    JLabel dateLabel = createDateLabel(date, products);
                    productsPanel.add(dateLabel);
                    productsPanel.add(Box.createVerticalStrut(10));
                }
            }

            productsPanel.revalidate();
            productsPanel.repaint();
        } catch (Exception ex) {
            LOGGER.severe("Error loading products by date: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Failed to load products. Please try again.", "Error", JOptionPane.ERROR_MESSAGE));
        }
    }

    private JLabel createDateLabel(LocalDate date, List<GymWholesaler> products) {
        double totalBill = 0.0;
        int totalQuantity = 0;
        double totalProfit = 0.0;
        for (GymWholesaler product : products) {
            totalBill += product.getTotalBill();
            totalQuantity += product.getProductQuantity();
            totalProfit += product.getNetProfit();
        }

        String labelText = String.format(
                "<html>" +
                "<div style='text-align: center;'>" +
                "<b>Date:</b> %s<br>" +
                "<b style='color: #FFFF00;'>Total Amount:</b> ₹%.2f<br>" +
                "<b style='color: #00FFFF;'>Total Quantity:</b> %d<br>" +
                "<b style='color: #66BB6A;'>Total Net Profit:</b> ₹%.2f" +
                "</div>" +
                "</html>",
                date.toString(), totalBill, totalQuantity, totalProfit);

        JLabel label = new JLabel(labelText);
        label.setFont(UIUtils.LABEL_FONT.deriveFont(16f));
        label.setForeground(UIUtils.TEXT_COLOR);
        label.setBackground(STAT_CARD_COLOR);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(10, SHADOW_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        label.setPreferredSize(new Dimension(200, 150));
        label.setMaximumSize(new Dimension(200, 150));
        label.setHorizontalAlignment(JLabel.CENTER);

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showProductPopup(date, products);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                label.setBackground(FAINT_ROW_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                label.setBackground(STAT_CARD_COLOR);
            }
        });

        return label;
    }

    private void showProductPopup(LocalDate date, List<GymWholesaler> products) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(600, 400));

        DefaultTableModel tableModel = new DefaultTableModel(new String[]{
                "Product Name", "Quantity", "Buying Price", "Selling Price", "Total Amount", "Net Profit"
        }, 0);
        productTable = new JTable(tableModel);
        JTableHeader tableHeader = productTable.getTableHeader();
        tableHeader.setFont(UIUtils.TABLE_HEADER_FONT);
        tableHeader.setBackground(new Color(230, 230, 230));
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        productTable.setDefaultRenderer(Object.class, centerRenderer);
        productTable.setGridColor(new Color(200, 200, 200));
        productTable.setShowGrid(true);

        for (GymWholesaler product : products) {
            tableModel.addRow(new Object[]{
                    product.getProductName(),
                    product.getProductQuantity(),
                    String.format("%.2f", product.getBuyingPrice()),
                    String.format("%.2f", product.getSellingPrice()),
                    String.format("%.2f", product.getTotalBill()),
                    String.format("%.2f", product.getNetProfit())
            });
        }

        JScrollPane tableScrollPane = new JScrollPane(productTable);
        panel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        JButton editButton = UIUtils.createStyledButton("Edit");
        JButton deleteButton = UIUtils.createStyledButton("Delete");
        JButton pdfButton = UIUtils.createStyledButton("Generate PDF");
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(pdfButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        editButton.addActionListener(e -> editProduct(products));
        deleteButton.addActionListener(e -> deleteProduct(products));
        pdfButton.addActionListener(e -> generateProductListPDF(wholesaler, date, tableModel, null));

        JOptionPane.showMessageDialog(this, panel, "Products for " + date, JOptionPane.PLAIN_MESSAGE);
    }

    private void editProduct(List<GymWholesaler> products) {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a product to edit.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            GymWholesaler selectedProduct = products.get(selectedRow);
            JTextField quantityField = UIUtils.createStyledTextField(5);
            quantityField.setText(String.valueOf(selectedProduct.getProductQuantity()));
            JTextField sellingPriceField = UIUtils.createStyledTextField(10);
            sellingPriceField.setText(String.format("%.2f", selectedProduct.getSellingPrice()));

            JPanel panel = new JPanel(new GridLayout(2, 2));
            panel.setBackground(UIUtils.BACKGROUND_COLOR);
            JLabel quantityLabel = new JLabel("New Quantity:");
            quantityLabel.setForeground(UIUtils.TEXT_COLOR);
            quantityLabel.setFont(UIUtils.LABEL_FONT);
            JLabel priceLabel = new JLabel("New Selling Price:");
            priceLabel.setForeground(UIUtils.TEXT_COLOR);
            priceLabel.setFont(UIUtils.LABEL_FONT);
            panel.add(quantityLabel);
            panel.add(quantityField);
            panel.add(priceLabel);
            panel.add(sellingPriceField);

            int result = JOptionPane.showConfirmDialog(this, panel, "Edit Product", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                int newQuantity = Integer.parseInt(quantityField.getText().trim());
                double newSellingPrice = Double.parseDouble(sellingPriceField.getText().trim());
                if (newQuantity <= 0) {
                    JOptionPane.showMessageDialog(this, "Quantity must be positive.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                dao.updateProduct(selectedProduct.getId(), newQuantity, newSellingPrice, selectedProduct.getBuyingPrice(),
                        selectedProduct.getProductName(), this);
                loadProductsByDate();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input format.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            LOGGER.severe("Error editing product: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to edit product. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteProduct(List<GymWholesaler> products) {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a product to delete.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            GymWholesaler selectedProduct = products.get(selectedRow);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete " + selectedProduct.getProductName() + "?", "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                dao.deleteProduct(selectedProduct.getId(), selectedProduct.getProductName(), this);
                loadProductsByDate();
            }
        } catch (Exception ex) {
            LOGGER.severe("Error deleting product: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to delete product. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generateProductListPDF(GymWholesaler wholesaler, LocalDate date, DefaultTableModel tableModel,
                                       Map<Integer, String> descriptions) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Invoice PDF");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        fileChooser.setSelectedFile(
                new File("invoice_" + wholesaler.getWholesalerName() + "_" + date.toString() + ".pdf"));

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

            // Wholesaler Details from Database
            String wholesalerName = "";
            String wholesalerAddress = "";
            String wholesalerPhone = "";
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn = DBUtil.getConnection();
                String sql = "SELECT wholesalerName, address, mobileNo FROM gym_wholesaler WHERE wholesalerName = ? AND mobileNo = ? LIMIT 1";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, wholesaler.getWholesalerName());
                pstmt.setString(2, wholesaler.getMobileNo());
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    wholesalerName = rs.getString("wholesalerName");
                    wholesalerAddress = rs.getString("address");
                    wholesalerPhone = rs.getString("mobileNo");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error fetching wholesaler details: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (pstmt != null) pstmt.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    LOGGER.severe("Error closing resources: " + e.getMessage());
                }
            }

            // Wholesaler Info
            document.add(new Paragraph("Invoice")
                    .setFont(timesRoman)
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginTop(10));
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
            infoTable.setWidth(UnitValue.createPercentValue(100));
            infoTable.setMarginTop(5);
            infoTable.setBackgroundColor(new DeviceRgb(200, 200, 200)); // Light grey background

            Cell wholesalerCell = new Cell();
            wholesalerCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f)); // Darker grey border
            wholesalerCell.setPadding(10);
            wholesalerCell.add(new Paragraph("To: " + wholesalerName).setFontSize(12).setBold()
                    .setTextAlignment(TextAlignment.LEFT));
            wholesalerCell.add(new Paragraph(wholesalerAddress).setFontSize(10));
            wholesalerCell.add(new Paragraph("Phone: " + wholesalerPhone).setFontSize(10));
            infoTable.addCell(wholesalerCell);

            Cell invoiceCell = new Cell();
            invoiceCell.setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f)); // Darker grey border
            invoiceCell.setPadding(10);
            invoiceCell.add(new Paragraph("Invoice Date: " + date.toString()).setFontSize(10)
                    .setTextAlignment(TextAlignment.RIGHT));
            infoTable.addCell(invoiceCell);

            document.add(infoTable);
            document.add(new Paragraph("\n"));

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

            int totalQuantity = 0;
            double totalAmount = 0.0;
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String productName = (String) tableModel.getValueAt(i, 0);
                int quantity = Integer.parseInt(tableModel.getValueAt(i, 1).toString());
                double sellingPrice = Double.parseDouble(tableModel.getValueAt(i, 3).toString());
                double amount = quantity * sellingPrice;

                table.addCell(new Cell().add(new Paragraph(productName).setTextAlignment(TextAlignment.LEFT))
                        .setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f))); // Darker grey border
                table.addCell(new Cell().add(new Paragraph(String.valueOf(quantity)).setTextAlignment(TextAlignment.CENTER))
                        .setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f))); // Darker grey border
                table.addCell(new Cell().add(new Paragraph(String.format("%.2f", sellingPrice)).setTextAlignment(TextAlignment.RIGHT))
                        .setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f))); // Darker grey border
                table.addCell(new Cell().add(new Paragraph("₹" + String.format("%.2f", amount)).setTextAlignment(TextAlignment.RIGHT))
                        .setBorder(new SolidBorder(new DeviceRgb(33, 33, 33), 0.5f))); // Darker grey border

                totalQuantity += quantity;
                totalAmount += amount;
            }

            document.add(table);

            // Totals
            Table totalTable = new Table(UnitValue.createPercentArray(new float[]{80, 20}));
            totalTable.setWidth(UnitValue.createPercentValue(100));
            totalTable.setMarginTop(10);
            totalTable.setBackgroundColor(new DeviceRgb(200, 200, 200)); // Light grey background

            totalTable.addCell(new Cell().add(new Paragraph("Total Quantity: " + totalQuantity).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(Border.NO_BORDER));
            totalTable.addCell(new Cell().add(new Paragraph("").setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(Border.NO_BORDER));

            totalTable.addCell(new Cell().add(new Paragraph("Total Amount").setBold().setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(Border.NO_BORDER));
            totalTable.addCell(new Cell().add(new Paragraph("₹" + String.format("%.2f", totalAmount)).setBold()
                    .setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));

            document.add(totalTable);

            // Declaration
            document.add(new Paragraph("I declare all the information contained in this invoice to be true and correct.")
                    .setFontSize(10).setTextAlignment(TextAlignment.LEFT).setMarginTop(10));

            // Footer
            document.add(new Paragraph("Thank You for Your Business!").setFontSize(14).setBold()
                    .setTextAlignment(TextAlignment.CENTER).setFontColor(new DeviceRgb(40, 144, 350))
                    .setMarginTop(10));
            document.add(new Paragraph("Contact us at: 9822888876 | Email: gauravyadavpureprotien@gmail.com")
                    .setFontSize(10).setTextAlignment(TextAlignment.CENTER).setMarginTop(5));

            // Page Border
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, event -> {
                PdfCanvas canvas = new PdfCanvas(((PdfDocumentEvent) event).getPage());
                canvas.setStrokeColor(ColorConstants.BLACK).setLineWidth(1)
                        .rectangle(20, 20, com.itextpdf.kernel.geom.PageSize.A4.getWidth() - 40, com.itextpdf.kernel.geom.PageSize.A4.getHeight() - 40).stroke();
            });

            document.close();
            JOptionPane.showMessageDialog(this, "PDF generated successfully at: " + dest, "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            LOGGER.severe("Error generating PDF: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Error generating PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        
    }
    
}