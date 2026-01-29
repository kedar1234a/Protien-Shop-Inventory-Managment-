package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.math.BigDecimal;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import com.proshop.model.Product;
import com.proshop.model.WholesalerPurchase;

public class WholesalerProductView extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// Color scheme to match WholesalerCard and WholesalerActionView
    private static final Color BACKGROUND_COLOR = new Color(33, 33, 33); // Dark gray (#212121)
    private static final Color TEXT_COLOR = Color.WHITE; // White
    private static final Color SUCCESS_COLOR = new Color(102, 187, 106); // Light Green (#66BB6A)
    private static final Color SHADOW_COLOR = new Color(224, 224, 224); // Very light gray for border (#E0E0E0)
    private static final Color STAT_CARD_COLOR = new Color(50, 50, 50); // Dark gray for odd columns (#323232)
    private static final Color FAINT_ROW_COLOR = new Color(66, 66, 66); // Light gray for even columns (#424242)

    public WholesalerProductView(WholesalerPurchase wholesaler, CardLayout cardLayout, JPanel mainContentPanel) {
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
        backButton.addActionListener(e -> {
            mainContentPanel.add(new WholesalerActionView(wholesaler, null, cardLayout, mainContentPanel), "WHOLESALER_ACTION");
            cardLayout.show(mainContentPanel, "WHOLESALER_ACTION");
        });
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(backButton);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Table
        List<Product> products = DatabaseUtils.fetchProductsForWholesaler(wholesaler.getId());
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Name", "Qty", "Rate", "Expiry", "Total"}, 0
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
        int totalQuantity = 0;
        BigDecimal finalTotal = BigDecimal.ZERO;

        for (Product product : products) {
            model.addRow(new Object[]{
                product.getProductName() != null ? product.getProductName() : "N/A",
                product.getQuantity(),
                product.getPerPieceRate() != null ? String.format("₹%.2f", product.getPerPieceRate()) : "₹0.00",
                product.getExpiry() != null ? product.getExpiry().toString() : "N/A",
                product.getTotal() != null ? String.format("₹%.2f", product.getTotal()) : "₹0.00"
            });
            totalQuantity += product.getQuantity() != null ? product.getQuantity() : 0;
            finalTotal = finalTotal.add(product.getTotal() != null ? product.getTotal() : BigDecimal.ZERO);
        }

        model.addRow(new Object[]{
            "",
            "Total Qty: " + totalQuantity,
            "",
            "",
            "Total: ₹" + String.format("%.2f", finalTotal)
        });

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

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);
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