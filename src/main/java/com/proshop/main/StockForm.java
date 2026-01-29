package com.proshop.main;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.proshop.connection.DBUtil;
import com.toedter.calendar.JDateChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockForm extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(StockForm.class);

    private JTable stockTable;
    private DefaultTableModel stockTableModel;
    private TableRowSorter<DefaultTableModel> stockSorter;
    private JTextField searchField;
    private JDateChooser startDateChooser, endDateChooser;
    private JButton refreshButton, backButton, filterButton, pdfButton, deleteButton, updateExpiryButton;
    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private List<Long> stockIds;
    private JLabel totalQuantityLabel, totalAmountLabel;

    // Colors
    private static final Color PRIMARY_COLOR = new Color(33, 33, 33);
    private static final Color ACCENT_COLOR = new Color(255, 215, 0);
    private static final Color SUCCESS_COLOR = new Color(102, 187, 106);
    private static final Color BACKGROUND_COLOR = new Color(33, 33, 33);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color STAT_CARD_COLOR = new Color(50, 50, 50);
    private static final Color SHADOW_COLOR = new Color(224, 224, 224);
    private static final Color FAINT_ROW_COLOR = new Color(66, 66, 66);
    private static final Color EXPIRY_HIGHLIGHT_COLOR = new Color(255, 82, 82);

    public StockForm(CardLayout cardLayout, JPanel mainContentPanel) {
        this.cardLayout = cardLayout;
        this.mainContentPanel = mainContentPanel;
        this.stockIds = new ArrayList<>();
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        setMinimumSize(new Dimension(1000, 600));
        initComponents();
        loadStockTableData();
    }

    private void initComponents() {
        // Title Panel
        JPanel titlePanel = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(BACKGROUND_COLOR);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(20, 40, 20, 40));

        JLabel titleLabel = new JLabel("Stock Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel("Manage and Track Inventory Stock");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        descLabel.setForeground(SHADOW_COLOR);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        titlePanel.add(Box.createVerticalGlue());
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(10));
        titlePanel.add(descLabel);
        titlePanel.add(Box.createVerticalGlue());

        // Totals Panel
        JPanel totalsPanel = new JPanel(new GridBagLayout());
        totalsPanel.setOpaque(false);
        totalsPanel.setBorder(new EmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        totalQuantityLabel = createStatLabel("Total Quantity: 0", SUCCESS_COLOR);
        totalAmountLabel = createStatLabel("Total Amount: ₹0.00", SUCCESS_COLOR);
        gbc.gridx = 0; gbc.gridy = 0; totalsPanel.add(totalQuantityLabel, gbc);
        gbc.gridx = 1; totalsPanel.add(totalAmountLabel, gbc);

        // Search & Filter Panel
        JPanel searchFilterPanel = new JPanel(new GridBagLayout());
        searchFilterPanel.setOpaque(false);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        searchField = new JTextField(20);
        searchField.setBackground(STAT_CARD_COLOR);
        searchField.setForeground(TEXT_COLOR);
        searchField.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
        searchField.setPreferredSize(new Dimension(250, 40));
        searchField.setFont(new Font("Arial", Font.PLAIN, 18));

        startDateChooser = new JDateChooser();
        startDateChooser.setDateFormatString("yyyy-MM-dd");
        startDateChooser.setBackground(STAT_CARD_COLOR);
        startDateChooser.setForeground(TEXT_COLOR);

        endDateChooser = new JDateChooser();
        endDateChooser.setDateFormatString("yyyy-MM-dd");
        endDateChooser.setBackground(STAT_CARD_COLOR);
        endDateChooser.setForeground(TEXT_COLOR);

        filterButton = createStyledButton("Filter", ACCENT_COLOR, PRIMARY_COLOR);
        refreshButton = createStyledButton("Refresh", ACCENT_COLOR, PRIMARY_COLOR);
        pdfButton = createStyledButton("PDF", ACCENT_COLOR, PRIMARY_COLOR);
        deleteButton = createStyledButton("Delete", ACCENT_COLOR, PRIMARY_COLOR);
        updateExpiryButton = createStyledButton("Update Expiry", ACCENT_COLOR, PRIMARY_COLOR);
        backButton = createStyledButton("Back", ACCENT_COLOR, PRIMARY_COLOR);

        addFormField(searchFilterPanel, gbc, "Search (Name / Price):", searchField, 0, 0, 2);
        addFormField(searchFilterPanel, gbc, "From:", startDateChooser, 1, 0, 1);
        addFormField(searchFilterPanel, gbc, "To:", endDateChooser, 2, 0, 1);

        gbc.gridx = 2; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0;
        searchFilterPanel.add(filterButton, gbc);
        gbc.gridx = 3; searchFilterPanel.add(refreshButton, gbc);
        gbc.gridx = 4; searchFilterPanel.add(pdfButton, gbc);
        gbc.gridx = 5; searchFilterPanel.add(deleteButton, gbc);
        gbc.gridx = 6; searchFilterPanel.add(updateExpiryButton, gbc);
        gbc.gridx = 7; searchFilterPanel.add(backButton, gbc);
        gbc.gridx = 8; gbc.weightx = 1.0;
        searchFilterPanel.add(new JLabel(), gbc);

        // North Panel
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setOpaque(false);
        northPanel.add(titlePanel, BorderLayout.NORTH);
        northPanel.add(totalsPanel, BorderLayout.CENTER);
        northPanel.add(searchFilterPanel, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);

        // Table
        String[] columns = { "Product Name", "Quantity", "Per Piece Rate", "Total Amount", "Expiry Date", "Purchase Date" };
        stockTableModel = new DefaultTableModel(columns, 0) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override public boolean isCellEditable(int r, int c) { return false; }
        };
        stockSorter = new TableRowSorter<>(stockTableModel);
        stockTable = new JTable(stockTableModel) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    Object exp = getValueAt(row, 4);
                    if (exp instanceof LocalDate && exp != null) {
                        LocalDate expiry = (LocalDate) exp;
                        if (!expiry.isAfter(LocalDate.now().plusMonths(1))) {
                            c.setBackground(EXPIRY_HIGHLIGHT_COLOR);
                        } else {
                            c.setBackground(row % 2 == 0 ? STAT_CARD_COLOR : FAINT_ROW_COLOR);
                        }
                    } else {
                        c.setBackground(row % 2 == 0 ? STAT_CARD_COLOR : FAINT_ROW_COLOR);
                    }
                }
                return c;
            }
        };
        stockTable.setRowSorter(stockSorter);
        stockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stockTable.setFont(new Font("Arial", Font.PLAIN, 16));
        stockTable.setRowHeight(30);
        stockTable.setForeground(TEXT_COLOR);
        stockTable.getTableHeader().setBackground(PRIMARY_COLOR);
        stockTable.getTableHeader().setForeground(TEXT_COLOR);
        stockTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));

        JScrollPane scroll = new JScrollPane(stockTable);
        scroll.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
        add(scroll, BorderLayout.CENTER);

        // === LIVE SEARCH WITH PRICE SUPPORT ===
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applySearchAndFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applySearchAndFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applySearchAndFilter(); }
        });

        // Action Listeners
        refreshButton.addActionListener(e -> { clearFields(); loadStockTableData(); });
        backButton.addActionListener(e -> cardLayout.show(mainContentPanel, "WELCOME"));
        filterButton.addActionListener(e -> applySearchAndFilter());
        pdfButton.addActionListener(this::generatePDF);
        deleteButton.addActionListener(this::deleteStock);
        updateExpiryButton.addActionListener(this::updateExpiryDate);
    }

    // === NEW: SEARCH BY NAME OR PRICE ===
    private void applySearchAndFilter() {
        String text = searchField.getText().trim();
        Date start = startDateChooser.getDate();
        Date end = endDateChooser.getDate();

        List<RowFilter<DefaultTableModel, Object>> filters = new ArrayList<>();

        // Product Name Search
        if (!text.isEmpty()) {
            if (text.matches("(?i)>(\\d+(\\.\\d+)?)")) {
                double val = Double.parseDouble(text.substring(1).trim());
                filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, val, 2));
            } else if (text.matches("(?i)<(\\d+(\\.\\d+)?)")) {
                double val = Double.parseDouble(text.substring(1).trim());
                filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, val, 2));
            } else if (text.matches("(?i)(\\d+(\\.\\d+)?)-(\\d+(\\.\\d+)?)")) {
                String[] parts = text.replaceAll("(?i)", "").split("-");
                double min = Double.parseDouble(parts[0].trim());
                double max = Double.parseDouble(parts[1].trim());
                filters.add(RowFilter.andFilter(Arrays.asList(
                    RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, min - 0.001, 2),
                    RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, max + 0.001, 2)
                )));
            } else if (text.matches("\\d+(\\.\\d+)?")) {
                double val = Double.parseDouble(text);
                filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 2));
            } else {
                filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0));
            }
        }

        // Date Filter
        if (start != null && end != null) {
            LocalDate s = start.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate e = end.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (s.isAfter(e)) {
                JOptionPane.showMessageDialog(this, "Start date cannot be after end date.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            filters.add(new RowFilter<DefaultTableModel, Object>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                    Object d = entry.getValue(5);
                    if (d instanceof LocalDate) {
                        LocalDate date = (LocalDate) d;
                        return !date.isBefore(s) && !date.isAfter(e);
                    }
                    return false;
                }
            });
        }

        stockSorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        updateTotalsFromFilteredRows();
    }

    private void loadStockTableData() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement("DELETE FROM stock WHERE quantity = 0");
            pstmt.executeUpdate();
            pstmt.close();

            String sql = "SELECT id, productName, quantity, perPieceRate, totalAmount, expiryDate, purchaseDate FROM stock ORDER BY productName";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            stockTableModel.setRowCount(0);
            stockIds.clear();
            int totalQty = 0;
            BigDecimal totalAmt = BigDecimal.ZERO;

            while (rs.next()) {
                Long id = rs.getLong("id");
                String name = rs.getString("productName");
                int qty = rs.getInt("quantity");
                BigDecimal rate = rs.getBigDecimal("perPieceRate");
                BigDecimal amt = rs.getBigDecimal("totalAmount");
                LocalDate exp = rs.getObject("expiryDate", LocalDate.class);
                LocalDate pur = rs.getObject("purchaseDate", LocalDate.class);

                stockTableModel.addRow(new Object[]{name, qty, rate, amt, exp, pur});
                stockIds.add(id);
                totalQty += qty;
                totalAmt = totalAmt.add(amt != null ? amt : BigDecimal.ZERO);
            }

            stockTableModel.addRow(new Object[]{"Total", totalQty, "", String.format("%.2f", totalAmt), null, null});
            totalQuantityLabel.setText("Total Quantity: " + totalQty);
            totalAmountLabel.setText("Total Amount: ₹" + String.format("%.2f", totalAmt));

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            logger.error("Load error", ex);
        } finally {
            closeResources(rs, pstmt, conn);
        }
    }

    private void updateTotalsFromFilteredRows() {
        int qty = 0;
        BigDecimal amt = BigDecimal.ZERO;
        for (int i = 0; i < stockTable.getRowCount(); i++) {
            int modelRow = stockTable.convertRowIndexToModel(i);
            if (modelRow < stockTableModel.getRowCount() - 1) {
                qty += (Integer) stockTableModel.getValueAt(modelRow, 1);
                BigDecimal a = (BigDecimal) stockTableModel.getValueAt(modelRow, 3);
                if (a != null) amt = amt.add(a);
            }
        }
        totalQuantityLabel.setText("Total Quantity: " + qty);
        totalAmountLabel.setText("Total Amount: ₹" + String.format("%.2f", amt));
        int last = stockTableModel.getRowCount() - 1;
        if (last >= 0) {
            stockTableModel.setValueAt(qty, last, 1);
            stockTableModel.setValueAt(String.format("%.2f", amt), last, 3);
        }
    }

    
    //METHOD UPDATED
    private void deleteStock(ActionEvent e) {
        int selectedRow = stockTable.getSelectedRow();
        
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a stock record to delete.", "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Convert the selected view row to model row (important when table is sorted/filtered)
        int modelRow = stockTable.convertRowIndexToModel(selectedRow);

        // Check if the selected row corresponds to a real stock (not the total row)
        // Assuming stockIds list contains only the IDs of actual stock records (not the total row)
        if (modelRow >= stockIds.size()) {
            JOptionPane.showMessageDialog(this, "Cannot delete the total summary row.", "Invalid Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirm deletion 
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete this stock record?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DBUtil.getConnection();
            
            // Safely get the stock ID using the model row index
            Long stockId = stockIds.get(modelRow);
            
            String sql = "DELETE FROM stock WHERE id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, stockId);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Refresh the table data and stockIds list
                loadStockTableData();
                
                JOptionPane.showMessageDialog(this, "Stock deleted successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete stock. Record may no longer exist.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            logger.error("Database error in deleteStock", ex);
        } finally {
            closeResources(null, pstmt, conn);
        }
    }

    private void updateExpiryDate(ActionEvent e) {
        int viewRow = stockTable.getSelectedRow();
        if (viewRow < 0 || viewRow >= stockTable.getRowCount() - 1) {
            JOptionPane.showMessageDialog(this, "Select a valid row.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JDateChooser chooser = new JDateChooser();
        chooser.setDateFormatString("yyyy-MM-dd");
        int modelRow = stockTable.convertRowIndexToModel(viewRow);
        LocalDate current = (LocalDate) stockTableModel.getValueAt(modelRow, 4);
        if (current != null) {
            chooser.setDate(Date.from(current.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        if (JOptionPane.showConfirmDialog(this, chooser, "New Expiry", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION && chooser.getDate() != null) {
            LocalDate newExp = chooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            Long id = stockIds.get(modelRow);
            try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE stock SET expiryDate = ? WHERE id = ?")) {
                ps.setObject(1, newExp);
                ps.setLong(2, id);
                ps.executeUpdate();
                loadStockTableData();
                JOptionPane.showMessageDialog(this, "Expiry updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void generatePDF(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("StockReport.pdf"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileOutputStream fos = new FileOutputStream(fc.getSelectedFile())) {
                PdfWriter writer = new PdfWriter(fos);
                PdfDocument pdf = new PdfDocument(writer);
                Document doc = new Document(pdf);
                doc.add(new Paragraph("Stock Report").setFontSize(20).setBold().setTextAlignment(TextAlignment.CENTER));
                Table table = new Table(new float[]{300, 100});
                table.addHeaderCell(new Cell().add(new Paragraph("Product Name").setBold()));
                table.addHeaderCell(new Cell().add(new Paragraph("Quantity").setBold()));
                for (int i = 0; i < stockTable.getRowCount() - 1; i++) {
                    int m = stockTable.convertRowIndexToModel(i);
                    table.addCell(stockTableModel.getValueAt(m, 0).toString());
                    table.addCell(stockTableModel.getValueAt(m, 1).toString());
                }
                doc.add(table);
                doc.close();
                JOptionPane.showMessageDialog(this, "PDF Saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "PDF Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearFields() {
        searchField.setText("");
        startDateChooser.setDate(null);
        endDateChooser.setDate(null);
        stockSorter.setRowFilter(null);
        loadStockTableData();
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font("Arial", Font.BOLD, 16));
        return b;
    }

    private void addFormField(JPanel p, GridBagConstraints gbc, String label, JComponent field, int row, int col, int width) {
        gbc.gridx = col; gbc.gridy = row; gbc.gridwidth = 1;
        JLabel l = new JLabel(label); l.setForeground(TEXT_COLOR);
        p.add(l, gbc);
        gbc.gridx = col + 1; gbc.gridwidth = width;
        p.add(field, gbc);
    }

    private void closeResources(ResultSet rs, PreparedStatement ps, Connection c) {
        try { if (rs != null) rs.close(); } catch (Exception ignored) {}
        try { if (ps != null) ps.close(); } catch (Exception ignored) {}
        DBUtil.closeConnection(c);
    }

    private JLabel createStatLabel(String text, Color color) {
        JPanel card = new JPanel() {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(STAT_CARD_COLOR);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.setColor(SHADOW_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(220, 100));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.BOLD, 24));
        lbl.setForeground(color);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(Box.createVerticalGlue());
        card.add(lbl);
        card.add(Box.createVerticalGlue());
        return lbl;
    }
}