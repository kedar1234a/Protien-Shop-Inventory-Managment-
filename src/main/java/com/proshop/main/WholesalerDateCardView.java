package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import com.proshop.model.Bill;
import com.proshop.model.WholesalerPurchase;
import com.toedter.calendar.JDateChooser;

public class WholesalerDateCardView extends JPanel {
    private static final long serialVersionUID = 1L;
    private JPanel dateCardsPanel;
    private WholesalerPurchase wholesaler;
    private CardLayout cardLayout;
    private JPanel mainContentPanel;

    // Color scheme to match WholesalerCard
    private static final Color BACKGROUND_COLOR = new Color(33, 33, 33); // Dark gray (#212121)
    private static final Color TEXT_COLOR = Color.WHITE; // White
    private static final Color SUCCESS_COLOR = new Color(102, 187, 106); // Light Green (#66BB6A)
    private static final Color ACCENT_COLOR = new Color(255, 215, 0); // Yellow (#FFD700)
    private static final Color PENDING_COLOR = new Color(239, 83, 80); // Red (#EF5350)
    private static final Color SHADOW_COLOR = new Color(224, 224, 224); // Very light gray for border (#E0E0E0)
    private static final Color HOVER_COLOR = new Color(50, 50, 50); // Slightly lighter dark gray for hover (#323232)

    public WholesalerDateCardView(WholesalerPurchase wholesaler, CardLayout cardLayout, JPanel mainContentPanel) {
        this.wholesaler = wholesaler;
        this.cardLayout = cardLayout;
        this.mainContentPanel = mainContentPanel;
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);

        // Header panel with title and Filter by Date button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Bills by Date for " + wholesaler.getWholesalerName(), SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(TEXT_COLOR);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        JButton filterButton = UIUtils.createStyledButton("Filter by Date");
        filterButton.addActionListener(e -> showDateFilterDialog());
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonWrapper.setBackground(BACKGROUND_COLOR);
        buttonWrapper.add(filterButton);
        headerPanel.add(buttonWrapper, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Date cards panel with FlowLayout to respect card sizes
        dateCardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        dateCardsPanel.setBackground(BACKGROUND_COLOR);
        dateCardsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        JScrollPane scrollPane = new JScrollPane(dateCardsPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Load all bills initially
        loadBills(null);

        // Back button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        JButton backButton = UIUtils.createStyledButton("Back");
        backButton.addActionListener(e -> {
            mainContentPanel.add(new WholesalerActionView(wholesaler, null, cardLayout, mainContentPanel),
                    "WHOLESALER_ACTION");
            cardLayout.show(mainContentPanel, "WHOLESALER_ACTION");
        });
        buttonPanel.add(backButton);
        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
        revalidate();
        repaint();
    }

    private void showDateFilterDialog() {
        JDialog filterDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Filter by Date", true);
        filterDialog.setLayout(new BorderLayout());
        filterDialog.setSize(300, 150);
        filterDialog.setLocationRelativeTo(this);

        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBackground(BACKGROUND_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel dateLabel = new JLabel("Select Date:");
        dateLabel.setFont(UIUtils.LABEL_FONT);
        dateLabel.setForeground(TEXT_COLOR);
        gbc.gridx = 0;
        gbc.gridy = 0;
        filterPanel.add(dateLabel, gbc);

        JDateChooser dateChooser = UIUtils.createStyledDateChooser();
        dateChooser.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1;
        filterPanel.add(dateChooser, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        JButton applyButton = UIUtils.createStyledButton("Apply");
        JButton clearButton = UIUtils.createStyledButton("Clear Filter");
        buttonPanel.add(applyButton);
        buttonPanel.add(clearButton);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        filterPanel.add(buttonPanel, gbc);

        applyButton.addActionListener(e -> {
            LocalDate selectedDate = dateChooser.getDate() != null
                    ? dateChooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    : null;
            loadBills(selectedDate);
            filterDialog.dispose();
        });

        clearButton.addActionListener(e -> {
            loadBills(null);
            filterDialog.dispose();
        });

        filterDialog.add(filterPanel, BorderLayout.CENTER);
        filterDialog.setVisible(true);
    }

    private void loadBills(LocalDate filterDate) {
        dateCardsPanel.removeAll();
        List<LocalDate> billDates = filterDate == null ? DatabaseUtils.fetchBillDatesForWholesaler(wholesaler.getId())
                : List.of(filterDate);

        for (LocalDate date : billDates) {
            List<Bill> bills = DatabaseUtils.fetchBillsForDate(wholesaler.getId(), date);
            for (Bill bill : bills) {
                JPanel dateCard = createDateCard(wholesaler, bill, cardLayout, mainContentPanel);
                dateCardsPanel.add(dateCard);
            }
        }

        dateCardsPanel.revalidate();
        dateCardsPanel.repaint();
    }

    private JPanel createDateCard(WholesalerPurchase wholesaler2, Bill bill, CardLayout cardLayout,
            JPanel mainContentPanel) {
        JPanel dateCard = new JPanel(new BorderLayout());
        dateCard.setPreferredSize(new Dimension(250, 280)); // Increased height to accommodate new button
        dateCard.setMaximumSize(new Dimension(250, 280));
        dateCard.setMinimumSize(new Dimension(250, 280));
        dateCard.setBackground(BACKGROUND_COLOR);
        Border outerBorder = BorderFactory.createLineBorder(SHADOW_COLOR, 1);
        Border innerBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        Border compoundBorder = new CompoundBorder(outerBorder, innerBorder);
        dateCard.setBorder(compoundBorder);

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBackground(BACKGROUND_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;

        JLabel dateLabel = new JLabel("Date: " + bill.getDate().toString());
        dateLabel.setFont(new Font("Arial", Font.BOLD, 16));
        dateLabel.setForeground(TEXT_COLOR);
        gbc.gridy = 0;
        infoPanel.add(dateLabel, gbc);

        BigDecimal totalAmount = DatabaseUtils.fetchTotalBillAmountForBill(bill.getId());
        JLabel totalLabel = new JLabel("Total: ₹" + String.format("%.2f", totalAmount));
        totalLabel.setFont(new Font("Arial", Font.BOLD, 16));
        totalLabel.setForeground(ACCENT_COLOR);
        gbc.gridy = 1;
        infoPanel.add(totalLabel, gbc);

        DatabaseUtils.PaymentSummary paymentSummary = DatabaseUtils.fetchPaymentsForBill(bill.getId());
        BigDecimal paidAmount = paymentSummary.getTotalPaidAmount() != null ? paymentSummary.getTotalPaidAmount()
                : BigDecimal.ZERO;
        JLabel paidLabel = new JLabel("Paid: ₹" + String.format("%.2f", paidAmount));
        paidLabel.setFont(new Font("Arial", Font.BOLD, 16));
        paidLabel.setForeground(SUCCESS_COLOR);
        gbc.gridy = 2;
        infoPanel.add(paidLabel, gbc);

        BigDecimal pendingAmount = totalAmount.subtract(paidAmount);
        JLabel pendingLabel = new JLabel("Pending: ₹" + String.format("%.2f", pendingAmount));
        pendingLabel.setFont(new Font("Arial", Font.BOLD, 16));
        pendingLabel.setForeground(PENDING_COLOR);
        gbc.gridy = 3;
        infoPanel.add(pendingLabel, gbc);

        BigDecimal shippingCharges = bill.getShippingCharges() != null ? bill.getShippingCharges() : BigDecimal.ZERO;
        JLabel shippingLabel = new JLabel("Shipping: ₹" + String.format("%.2f", shippingCharges));
        shippingLabel.setFont(new Font("Arial", Font.BOLD, 16));
        shippingLabel.setForeground(TEXT_COLOR);
        gbc.gridy = 4;
        infoPanel.add(shippingLabel, gbc);

        // Add button panel with Delete and Generate PDF buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        
        JButton deleteButton = UIUtils.createStyledButton("Delete");
        deleteButton.setBackground(PENDING_COLOR);
        deleteButton.setForeground(TEXT_COLOR);
        deleteButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete the bill dated " + bill.getDate().toString() + "?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                DatabaseUtils.deleteBill(bill.getId());
                loadBills(null); // Refresh the view
            }
        });
        buttonPanel.add(deleteButton);

        JButton pdfButton = UIUtils.createStyledButton("Generate PDF");
        pdfButton.setBackground(SUCCESS_COLOR);
        pdfButton.setForeground(TEXT_COLOR);
        pdfButton.addActionListener(e -> {
            WholesalerBillPDF pdfGenerator = new WholesalerBillPDF(wholesaler2, bill);
            pdfGenerator.generateBillPDF(e);
        });
        buttonPanel.add(pdfButton);

        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.CENTER;
        infoPanel.add(buttonPanel, gbc);

        dateCard.add(infoPanel, BorderLayout.CENTER);

        dateCard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mainContentPanel.add(new WholesalerDetailView(wholesaler2, bill, cardLayout, mainContentPanel),
                        "WHOLESALER_DETAIL");
                cardLayout.show(mainContentPanel, "WHOLESALER_DETAIL");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                dateCard.setCursor(new Cursor(Cursor.HAND_CURSOR));
                dateCard.setBorder(new CompoundBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 2), innerBorder));
                dateCard.setBackground(HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                dateCard.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                dateCard.setBorder(compoundBorder);
                dateCard.setBackground(BACKGROUND_COLOR);
            }
        });

        return dateCard;
    }
}