package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.proshop.connection.DBUtil;
import com.toedter.calendar.JDateChooser;

public class SoldStockForm extends JPanel {
    private static final long serialVersionUID = 1L;
    private JTable soldStockTable;
    private DefaultTableModel tableModel;
    private JLabel totalQuantityLabel;
    private JLabel totalAmountLabel;
    private JLabel totalNetProfitLabel;
    private JTextField searchField;
    private JDateChooser startDateChooser;
    private JDateChooser endDateChooser;
    private Timer debounceTimer;

    // Color scheme to match Dashboard
    private static final Color PRIMARY_COLOR = new Color(33, 33, 33);
    private static final Color SUCCESS_COLOR = new Color(102, 187, 106);
    private static final Color BACKGROUND_COLOR = new Color(33, 33, 33);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color STAT_CARD_COLOR = new Color(50, 50, 50);
    private static final Color SHADOW_COLOR = new Color(224, 224, 224);
    private static final Color FAINT_ROW_COLOR = new Color(66, 66, 66);

    public SoldStockForm(CardLayout cardLayout, JPanel mainContentPanel) {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);

        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(BACKGROUND_COLOR);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(20, 40, 20, 40));

        // Center panel for title and description
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Sold Stock Details");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel("Overview of All Sold Stock Transactions");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        descLabel.setForeground(new Color(224, 224, 224));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(descLabel);
        centerPanel.add(Box.createVerticalGlue());

        // Refresh button in upper right corner with smaller size
        JButton refreshButton = UIUtils.createStyledButton("Refresh");
        refreshButton.setPreferredSize(new Dimension(100, 20));
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            startDateChooser.setDate(null);
            endDateChooser.setDate(null);
            loadSoldStockData("", null, null);
        });

        titlePanel.add(centerPanel, BorderLayout.CENTER);
        titlePanel.add(refreshButton, BorderLayout.EAST);

        // Totals Panel (with stat card styling)
        JPanel totalsPanel = new JPanel(new GridLayout(1, 3, 20, 20));
        totalsPanel.setOpaque(false);
        totalsPanel.setBorder(new EmptyBorder(20, 40, 20, 40));

        totalQuantityLabel = createStatLabel("Total Quantity: 0", SUCCESS_COLOR);
        totalAmountLabel = createStatLabel("Total Amount: ₹0.00", SUCCESS_COLOR);
        totalNetProfitLabel = createStatLabel("Total Net Profit: ₹0.00", SUCCESS_COLOR);

        totalsPanel.add(totalQuantityLabel);
        totalsPanel.add(totalAmountLabel);
        totalsPanel.add(totalNetProfitLabel);

        // Search and Filter Panel
        JPanel searchFilterPanel = new JPanel();
        searchFilterPanel.setOpaque(false);
        searchFilterPanel.setBorder(new EmptyBorder(10, 40, 10, 40));
        searchFilterPanel.setLayout(new BoxLayout(searchFilterPanel, BoxLayout.X_AXIS));

        // Search by product name
        JLabel searchLabel = new JLabel("Search Product: ");
        searchLabel.setForeground(TEXT_COLOR);
        searchLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        searchField = new JTextField(20);
        searchField.setFont(new Font("Arial", Font.PLAIN, 16));
        searchField.setBackground(STAT_CARD_COLOR);
        searchField.setForeground(TEXT_COLOR);
        searchField.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (debounceTimer != null) {
                    debounceTimer.cancel();
                }
                debounceTimer = new Timer();
                debounceTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        filterData();
                    }
                }, 300); // Delay of 300ms to debounce
            }
        });

        // Date range filter
        JLabel dateRangeLabel = new JLabel("Date Range: ");
        dateRangeLabel.setForeground(TEXT_COLOR);
        dateRangeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        startDateChooser = new JDateChooser();
        startDateChooser.setFont(new Font("Arial", Font.PLAIN, 16));
        startDateChooser.setPreferredSize(new Dimension(150, 30));
        endDateChooser = new JDateChooser();
        endDateChooser.setFont(new Font("Arial", Font.PLAIN, 16));
        endDateChooser.setPreferredSize(new Dimension(150, 30));

        JButton applyFilterButton = UIUtils.createStyledButton("Apply Filter");
        applyFilterButton.setPreferredSize(new Dimension(120, 30));
        applyFilterButton.addActionListener(e -> filterData());

        searchFilterPanel.add(searchLabel);
        searchFilterPanel.add(Box.createHorizontalStrut(10));
        searchFilterPanel.add(searchField);
        searchFilterPanel.add(Box.createHorizontalStrut(20));
        searchFilterPanel.add(dateRangeLabel);
        searchFilterPanel.add(Box.createHorizontalStrut(10));
        searchFilterPanel.add(startDateChooser);
        searchFilterPanel.add(Box.createHorizontalStrut(10));
        searchFilterPanel.add(endDateChooser);
        searchFilterPanel.add(Box.createHorizontalStrut(10));
        searchFilterPanel.add(applyFilterButton);
        searchFilterPanel.add(Box.createHorizontalGlue());

        // North Panel (title + totals + search/filter)
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setOpaque(false);
        northPanel.add(titlePanel, BorderLayout.NORTH);
        northPanel.add(totalsPanel, BorderLayout.CENTER);
        northPanel.add(searchFilterPanel, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);

        // Table
        String[] columnNames = {"Date", "Product Name", "Quantity", "Selling Price", "Total Amount", "Net Profit"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        soldStockTable = new JTable(tableModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? STAT_CARD_COLOR : FAINT_ROW_COLOR);
                }
                return c;
            }
        };
        soldStockTable.setBackground(STAT_CARD_COLOR);
        soldStockTable.setForeground(TEXT_COLOR);
        soldStockTable.setFont(new Font("Arial", Font.PLAIN, 16));
        soldStockTable.setRowHeight(30);
        soldStockTable.getTableHeader().setBackground(PRIMARY_COLOR);
        soldStockTable.getTableHeader().setForeground(TEXT_COLOR);
        soldStockTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));

        JScrollPane scrollPane = new JScrollPane(soldStockTable);
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        scrollPane.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
        add(scrollPane, BorderLayout.CENTER);

        // Load data
        loadSoldStockData("", null, null);
    }

    private JLabel createStatLabel(String text, Color textColor) {
        JPanel card = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(STAT_CARD_COLOR);
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2d.setColor(SHADOW_COLOR);
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(220, 100));

        card.add(Box.createVerticalGlue());
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        label.setForeground(textColor);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(label);
        card.add(Box.createVerticalGlue());

        return label;
    }

    private void filterData() {
        String searchText = searchField.getText().trim();
        Date startDate = startDateChooser.getDate();
        Date endDate = endDateChooser.getDate();
        loadSoldStockData(searchText, startDate, endDate);
    }

    private void loadSoldStockData(String searchText, Date startDate, Date endDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT date AS sale_date, COALESCE(productName, '') AS productName, quantity, sellingPrice, finalBill AS total_amount, netProfit, id " +
            "FROM customer " +
            "WHERE 1=1 "
        );
        if (!searchText.isEmpty()) {
            sql.append("AND UPPER(COALESCE(productName, '')) LIKE ? ");
        }
        if (startDate != null) {
            sql.append("AND date >= ? ");
        }
        if (endDate != null) {
            sql.append("AND date <= ? ");
        }
        sql.append(
            "UNION " +
            "SELECT dateOfPurchase AS sale_date, COALESCE(productName, '') AS productName, quantity, sellingPrice, totalBill AS total_amount, netProfit, id " +
            "FROM gym_wholesaler " +
            "WHERE 1=1 "
        );
        if (!searchText.isEmpty()) {
            sql.append("AND UPPER(COALESCE(productName, '')) LIKE ? ");
        }
        if (startDate != null) {
            sql.append("AND dateOfPurchase >= ? ");
        }
        if (endDate != null) {
            sql.append("AND dateOfPurchase <= ? ");
        }
        sql.append("ORDER BY sale_date DESC");

        List<Object[]> rows = new ArrayList<>();
        double[] totals = new double[3]; // [totalQuantity, totalAmount, totalNetProfit]

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            // Set parameters for customer table
            if (!searchText.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + searchText.toUpperCase().replaceAll("\\s+", "%") + "%");
            }
            if (startDate != null) {
                pstmt.setDate(paramIndex++, new java.sql.Date(startDate.getTime()));
            }
            if (endDate != null) {
                pstmt.setDate(paramIndex++, new java.sql.Date(endDate.getTime()));
            }
            // Set parameters for gym_wholesaler table
            if (!searchText.isEmpty()) {
                pstmt.setString(paramIndex++, "%" + searchText.toUpperCase().replaceAll("\\s+", "%") + "%");
            }
            if (startDate != null) {
                pstmt.setDate(paramIndex++, new java.sql.Date(startDate.getTime()));
            }
            if (endDate != null) {
                pstmt.setDate(paramIndex++, new java.sql.Date(endDate.getTime()));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String productName = rs.getString("productName") != null ? rs.getString("productName") : "";
                    double sellingPrice = rs.getDouble("sellingPrice");
                    double totalAmount = rs.getDouble("total_amount");
                    double netProfit = rs.getDouble("netProfit");
                    int quantity = rs.getInt("quantity");
                    Date saleDate = rs.getDate("sale_date");
                    rows.add(new Object[]{
                        saleDate,
                        productName,
                        quantity,
                        String.format("%.2f", sellingPrice),
                        String.format("%.2f", totalAmount),
                        String.format("%.2f", netProfit)
                    });
                    totals[0] += quantity;
                    totals[1] += totalAmount;
                    totals[2] += netProfit;
                }
            }

            // Update UI atomically
            javax.swing.SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                rows.forEach(tableModel::addRow);
                totalQuantityLabel.setText("Total Quantity: " + (int) totals[0]);
                totalAmountLabel.setText("Total Amount: ₹" + String.format("%.2f", totals[1]));
                totalNetProfitLabel.setText("Total Net Profit: ₹" + String.format("%.2f", totals[2]));
            });

            // Debug specific dates for missing rows
            debugDateRows(conn, "2025-07-31");
            debugDateRows(conn, "2025-08-03");

        } catch (SQLException ex) {
            System.err.println("Error fetching sold stock data: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching sold stock data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void debugDateRows(Connection conn, String date) throws SQLException {
        String debugSql = "SELECT 'customer' AS source, id, date AS sale_date, COALESCE(productName, '') AS productName, quantity, sellingPrice, finalBill AS total_amount, netProfit " +
                          "FROM customer " +
                          "WHERE date = ? " +
                          "UNION " +
                          "SELECT 'gym_wholesaler' AS source, id, dateOfPurchase AS sale_date, COALESCE(productName, '') AS productName, quantity, sellingPrice, totalBill AS total_amount, netProfit " +
                          "FROM gym_wholesaler " +
                          "WHERE dateOfPurchase = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(debugSql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(date));
            pstmt.setDate(2, java.sql.Date.valueOf(date));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Process rows without printing
                }
            }
        }
    }
}