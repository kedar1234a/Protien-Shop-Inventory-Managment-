package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import com.proshop.model.GymWholesaler;

public class GymWholesalerViewAllProducts extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(GymWholesalerViewAllProducts.class.getName());
    private static final Color BACKGROUND_COLOR = new Color(33, 33, 33); // Dark gray (#212121)
    private static final Color TEXT_COLOR = Color.WHITE; // White
    private static final Color SUCCESS_COLOR = new Color(102, 187, 106); // Light Green (#66BB6A)
    private static final Color SHADOW_COLOR = new Color(224, 224, 224); // Very light gray (#E0E0E0)
    private static final Color STAT_CARD_COLOR = new Color(50, 50, 50); // Dark gray for odd columns (#323232)
    private static final Color FAINT_ROW_COLOR = new Color(66, 66, 66); // Light gray for even columns (#424242)

    private final GymWholesaler wholesaler;
    private final GymWholesalerDAO dao;

    public GymWholesalerViewAllProducts(GymWholesaler wholesaler, CardLayout cardLayout, JPanel mainContentPanel,
            GymWholesalerDAO dao) {
        this.wholesaler = wholesaler;
        this.dao = dao;
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SHADOW_COLOR, 1),
            new EmptyBorder(10, 10, 10, 10)
        ));

        // Top panel containing title and back button
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(BACKGROUND_COLOR);
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(BACKGROUND_COLOR);
        JLabel titleLabel = new JLabel("All Products for " + (wholesaler.getWholesalerName() != null ? wholesaler.getWholesalerName().toUpperCase() : "N/A"), SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(Box.createVerticalGlue());
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalGlue());
        topPanel.add(titlePanel, BorderLayout.CENTER);

        // Back button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        JButton backButton = createStyledButton("Back");
        backButton.setMaximumSize(new Dimension(150, 40));
        backButton.addActionListener(e -> cardLayout.show(mainContentPanel, "GYM_WHOLESALER"));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(backButton);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Table
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Product Name", "Quantity", "Buying Price", "Selling Price", "Total Amount", "Net Profit", "Date"}, 0
        ) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(column % 2 == 0 ? STAT_CARD_COLOR : FAINT_ROW_COLOR);
                    if (row == getRowCount() - 1 && (column == 1 || column == 4)) {
                        c.setForeground(SUCCESS_COLOR); // Highlight totals
                    } else {
                        c.setForeground(TEXT_COLOR);
                    }
                }
                return c;
            }
        };
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(30);
        table.setBackground(BACKGROUND_COLOR);
        table.setForeground(TEXT_COLOR);
        table.setGridColor(SHADOW_COLOR);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        table.getTableHeader().setBackground(STAT_CARD_COLOR);
        table.getTableHeader().setForeground(TEXT_COLOR);

        // Adjust column widths
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(150);
        columnModel.getColumn(1).setPreferredWidth(80);
        columnModel.getColumn(2).setPreferredWidth(100);
        columnModel.getColumn(3).setPreferredWidth(100);
        columnModel.getColumn(4).setPreferredWidth(100);
        columnModel.getColumn(5).setPreferredWidth(100);
        columnModel.getColumn(6).setPreferredWidth(100);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Load Data
        loadProducts(model);
    }

    private void loadProducts(DefaultTableModel model) {
        try {
            List<GymWholesaler> products = dao.fetchAllProducts(wholesaler);
            model.setRowCount(0);
            int totalQuantity = 0;
            double totalBill = 0.0;

            for (GymWholesaler product : products) {
                model.addRow(new Object[]{
                    product.getProductName() != null ? product.getProductName() : "N/A",
                    product.getProductQuantity(),
                    String.format("₹%.2f", product.getBuyingPrice()),
                    String.format("₹%.2f", product.getSellingPrice()),
                    String.format("₹%.2f", product.getTotalBill()),
                    String.format("₹%.2f", product.getNetProfit()),
                    product.getDateOfPurchase() != null ? product.getDateOfPurchase().toString() : "N/A"
                });
                totalQuantity += product.getProductQuantity();
                totalBill += product.getTotalBill();
            }

            // Add totals row
            model.addRow(new Object[]{
                "",
                "Total Qty: " + totalQuantity,
                "",
                "",
                "Total: ₹" + String.format("%.2f", totalBill),
                "",
                ""
            });

        } catch (Exception ex) {
            LOGGER.severe("Error loading products: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Failed to load products. Please try again.", "Error", JOptionPane.ERROR_MESSAGE));
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setForeground(TEXT_COLOR);
        button.setBackground(SUCCESS_COLOR);
        button.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
        button.setFocusPainted(false);
        button.setAlignmentX(Component.RIGHT_ALIGNMENT);
        return button;
    }
}