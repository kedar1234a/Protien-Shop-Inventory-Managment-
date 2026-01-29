package com.proshop.main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import com.toedter.calendar.JDateChooser;

public class UIUtils {
    public static final Color BACKGROUND_COLOR = new Color(33, 33, 33); // Dark gray background
    public static final Color BUTTON_COLOR = new Color(50, 50, 50); // Slightly lighter dark gray for buttons
    public static final Color BUTTON_HOVER_COLOR = new Color(80, 80, 80); // Lighter gray for hover effect
    public static final Color TEXT_COLOR = Color.WHITE; // White text for contrast
    public static final Font LABEL_FONT = new Font("Arial", Font.BOLD, 16); // Larger and bold for labels
    public static final Font TEXT_FONT = new Font("Arial", Font.BOLD, 14); // Larger and bold for text
    public static final Font TABLE_HEADER_FONT = new Font("Arial", Font.BOLD, 16); // Larger and bold for table headers

    public static JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(BUTTON_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFont(TEXT_FONT);
        button.setBorder(new RoundedButtonBorder(10));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(BUTTON_HOVER_COLOR);
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(BUTTON_COLOR);
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        return button;
    }

    public static JTextField createStyledTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(TEXT_FONT);
        Border outerBorder = BorderFactory.createLineBorder(new Color(224, 224, 224)); // Very light gray border
        Border innerBorder = BorderFactory.createEmptyBorder(8, 8, 8, 8);
        field.setBorder(new CompoundBorder(outerBorder, innerBorder));
        field.setBackground(BACKGROUND_COLOR); // Match background
        field.setForeground(TEXT_COLOR); // White text
        return field;
    }

    public static JDateChooser createStyledDateChooser() {
        JDateChooser dateChooser = new JDateChooser();
        dateChooser.setDateFormatString("yyyy-MM-dd");
        dateChooser.setFont(TEXT_FONT);
        dateChooser.setBackground(BACKGROUND_COLOR); // Match background
        dateChooser.setForeground(TEXT_COLOR); // White text
        return dateChooser;
    }

    public static JPanel createStyledFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND_COLOR);
        Border shadowBorder = BorderFactory.createLineBorder(new Color(224, 224, 224)); // Very light gray border
        Border paddingBorder = BorderFactory.createEmptyBorder(20, 20, 20, 20);
        panel.setBorder(new CompoundBorder(shadowBorder, paddingBorder));
        return panel;
    }

    public static void addFormField(JPanel panel, GridBagConstraints gbc, String label, JComponent field, int row, int col) {
        gbc.gridx = col;
        gbc.gridy = row;
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(LABEL_FONT);
        labelComponent.setForeground(TEXT_COLOR); // White text
        panel.add(labelComponent, gbc);
        gbc.gridx = col + 1;
        panel.add(field, gbc);
    }

    private static class RoundedButtonBorder implements Border {
        private final int radius;

        RoundedButtonBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(224, 224, 224)); // Very light gray border
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(10, 15, 10, 15);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}