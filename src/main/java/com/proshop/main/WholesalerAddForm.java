package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.proshop.model.WholesalerPurchase;

public class WholesalerAddForm extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField wholesalerNameField;
    private JTextField phoneNoField;
    private JTextField addressField;

    public WholesalerAddForm(CardLayout cardLayout, JPanel mainContentPanel) {
        setLayout(new BorderLayout());
        setBackground(new Color(33, 33, 33)); // Dark gray background

        JLabel titleLabel = new JLabel("Add New Wholesaler", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE); // White text
        titleLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
        add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = UIUtils.createStyledFormPanel();
        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.insets = new Insets(10, 10, 10, 10);
        formGbc.anchor = GridBagConstraints.WEST;

        wholesalerNameField = UIUtils.createStyledTextField(25);
        UIUtils.addFormField(formPanel, formGbc, "Wholesaler Name:", wholesalerNameField, 0, 0);

        phoneNoField = UIUtils.createStyledTextField(25);
        UIUtils.addFormField(formPanel, formGbc, "Phone Number:", phoneNoField, 1, 0);

        addressField = UIUtils.createStyledTextField(25);
        UIUtils.addFormField(formPanel, formGbc, "Address:", addressField, 2, 0);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(new Color(33, 33, 33)); // Match background
        JButton submitButton = UIUtils.createStyledButton("Submit");
        JButton backButton = UIUtils.createStyledButton("Back");

        submitButton.addActionListener(e -> {
            try {
                String wholesalerName = wholesalerNameField.getText().trim();
                String phoneNo = phoneNoField.getText().trim();
                String address = addressField.getText().trim();

                if (wholesalerName.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Wholesaler name is required.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                WholesalerPurchase wholesaler = new WholesalerPurchase();
                wholesaler.setWholesalerName(wholesalerName);
                wholesaler.setPhoneNo(phoneNo.isEmpty() ? null : phoneNo);
                wholesaler.setAddress(address.isEmpty() ? null : address);

                DatabaseUtils.addWholesaler(wholesaler);
                JOptionPane.showMessageDialog(this, "Wholesaler added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

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

        buttonPanel.add(submitButton);
        buttonPanel.add(backButton);
        formGbc.gridx = 0;
        formGbc.gridy = 3;
        formGbc.gridwidth = 2;
        formGbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(buttonPanel, formGbc);

        add(formPanel, BorderLayout.CENTER);
    }
}

class UpdateWholesalerForm extends JDialog {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final WholesalerPurchase wholesaler;
    private final WholesalerDetailView parentView;

    public UpdateWholesalerForm(WholesalerPurchase wholesaler, WholesalerDetailView parentView) {
        super((Frame) SwingUtilities.getWindowAncestor(parentView), "Update Wholesaler", true);
        this.wholesaler = wholesaler;
        this.parentView = parentView;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setSize(400, 300);
        setLocationRelativeTo(parentView);
        setBackground(new Color(33, 33, 33)); // Match background

        JPanel formPanel = UIUtils.createStyledFormPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameField = UIUtils.createStyledTextField(15);
        nameField.setText(wholesaler.getWholesalerName());
        UIUtils.addFormField(formPanel, gbc, "Wholesaler Name:", nameField, 0, 0);

        JTextField phoneField = UIUtils.createStyledTextField(15);
        phoneField.setText(wholesaler.getPhoneNo() != null ? wholesaler.getPhoneNo() : "");
        UIUtils.addFormField(formPanel, gbc, "Phone Number:", phoneField, 1, 0);

        JTextField addressField = UIUtils.createStyledTextField(15);
        addressField.setText(wholesaler.getAddress() != null ? wholesaler.getAddress() : "");
        UIUtils.addFormField(formPanel, gbc, "Address:", addressField, 2, 0);

        add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(new Color(33, 33, 33)); // Match background
        JButton saveButton = UIUtils.createStyledButton("Save");
        JButton cancelButton = UIUtils.createStyledButton("Cancel");

        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Wholesaler name is required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String phone = phoneField.getText().trim();
            String address = addressField.getText().trim();
            wholesaler.setWholesalerName(name);
            wholesaler.setPhoneNo(phone.isEmpty() ? null : phone);
            wholesaler.setAddress(address.isEmpty() ? null : address);
            try {
                DatabaseUtils.updateWholesaler(wholesaler);
                JOptionPane.showMessageDialog(this, "Wholesaler updated successfully.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                parentView.refreshWholesalerData();
                dispose();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "Error updating wholesaler: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
}