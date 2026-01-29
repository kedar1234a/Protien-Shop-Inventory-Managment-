package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.proshop.model.WholesalerPurchase;

public class WholesalerUpdateForm extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField wholesalerNameField;
    private JTextField phoneNoField;
    private JTextField addressField;

    public WholesalerUpdateForm(WholesalerPurchase wholesaler, CardLayout cardLayout, JPanel mainContentPanel) {
        setLayout(new BorderLayout());
        setBackground(new Color(33, 33, 33)); // Dark gray background

        JLabel titleLabel = new JLabel("Update Wholesaler: " + wholesaler.getWholesalerName(), SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE); // White text
        titleLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
        add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = UIUtils.createStyledFormPanel();
        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.insets = new Insets(10, 10, 10, 10);
        formGbc.anchor = GridBagConstraints.WEST;

        wholesalerNameField = UIUtils.createStyledTextField(25);
        wholesalerNameField.setText(wholesaler.getWholesalerName());
        UIUtils.addFormField(formPanel, formGbc, "Wholesaler Name:", wholesalerNameField, 0, 0);

        phoneNoField = UIUtils.createStyledTextField(25);
        phoneNoField.setText(wholesaler.getPhoneNo() != null ? wholesaler.getPhoneNo() : "");
        UIUtils.addFormField(formPanel, formGbc, "Phone Number:", phoneNoField, 1, 0);

        addressField = UIUtils.createStyledTextField(25);
        addressField.setText(wholesaler.getAddress() != null ? wholesaler.getAddress() : "");
        UIUtils.addFormField(formPanel, formGbc, "Address:", addressField, 2, 0);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(new Color(33, 33, 33)); // Match background
        JButton updateButton = UIUtils.createStyledButton("Update");
        JButton backButton = UIUtils.createStyledButton("Back");

        updateButton.addActionListener(e -> {
            try {
                String wholesalerName = wholesalerNameField.getText().trim();
                String phoneNo = phoneNoField.getText().trim();
                String address = addressField.getText().trim();

                if (wholesalerName.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Wholesaler name is required.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                WholesalerPurchase updatedWholesaler = new WholesalerPurchase();
                updatedWholesaler.setId(wholesaler.getId());
                updatedWholesaler.setWholesalerName(wholesalerName);
                updatedWholesaler.setPhoneNo(phoneNo.isEmpty() ? null : phoneNo);
                updatedWholesaler.setAddress(address.isEmpty() ? null : address);

                DatabaseUtils.updateWholesaler(updatedWholesaler);
                JOptionPane.showMessageDialog(this, "Wholesaler updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

                WholesalerForm wholesalerForm = new WholesalerForm(cardLayout, mainContentPanel);
                mainContentPanel.add(wholesalerForm, "WHOLESALER");
                cardLayout.show(mainContentPanel, "WHOLESALER");
                wholesalerForm.loadBlocks();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        backButton.addActionListener(e -> {
            WholesalerForm wholesalerForm = new WholesalerForm(cardLayout, mainContentPanel);
            mainContentPanel.add(wholesalerForm, "WHOLESALER");
            cardLayout.show(mainContentPanel, "WHOLESALER");
            wholesalerForm.loadBlocks();
        });

        buttonPanel.add(updateButton);
        buttonPanel.add(backButton);
        formGbc.gridx = 0;
        formGbc.gridy = 3;
        formGbc.gridwidth = 2;
        formGbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(buttonPanel, formGbc);

        add(formPanel, BorderLayout.CENTER);
    }
}