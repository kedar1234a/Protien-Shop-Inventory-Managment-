package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.proshop.model.GymWholesaler;

public class GymWholesalerAddForm extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(GymWholesalerAddForm.class.getName());
    private final CardLayout cardLayout;
    private final JPanel mainContentPanel;
    private final GymWholesalerDAO dao;
    private final JTextField wholesalerNameField;
    private final JTextField mobileNoField;
    private final JTextField addressField;

    public GymWholesalerAddForm(CardLayout cardLayout, JPanel mainContentPanel, GymWholesalerDAO dao) {
        this.cardLayout = cardLayout;
        this.mainContentPanel = mainContentPanel;
        this.dao = dao;
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        wholesalerNameField = UIUtils.createStyledTextField(20);
        mobileNoField = UIUtils.createStyledTextField(10);
        addressField = UIUtils.createStyledTextField(20);

        JLabel nameLabel = new JLabel("Wholesaler Name:");
        nameLabel.setForeground(UIUtils.TEXT_COLOR); // White text
        nameLabel.setFont(UIUtils.LABEL_FONT);
        JLabel mobileLabel = new JLabel("Mobile No:");
        mobileLabel.setForeground(UIUtils.TEXT_COLOR); // White text
        mobileLabel.setFont(UIUtils.LABEL_FONT);
        JLabel addressLabel = new JLabel("Address:");
        addressLabel.setForeground(UIUtils.TEXT_COLOR); // White text
        addressLabel.setFont(UIUtils.LABEL_FONT);

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row++;
        formPanel.add(nameLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(wholesalerNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        formPanel.add(mobileLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(mobileNoField, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        formPanel.add(addressLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(addressField, gbc);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        JButton addButton = UIUtils.createStyledButton("Add");
        JButton backButton = UIUtils.createStyledButton("Back");
        buttonPanel.add(addButton);
        buttonPanel.add(backButton);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addWholesaler());
        backButton.addActionListener(e -> cardLayout.show(mainContentPanel, "GYM_WHOLESALER"));
    }

    private void addWholesaler() {
        String wholesalerName = wholesalerNameField.getText().trim();
        String mobileNo = mobileNoField.getText().trim();
        String address = addressField.getText().trim();

        if (wholesalerName.isEmpty() || address.isEmpty()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Wholesaler Name and Address are required.", "Error", JOptionPane.ERROR_MESSAGE));
            return;
        }

        if (!mobileNo.matches("\\d{10}")) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Mobile No must be a 10-digit number.", "Error", JOptionPane.ERROR_MESSAGE));
            return;
        }

        GymWholesaler wholesaler = new GymWholesaler(null, wholesalerName, mobileNo, null, 0, 0.0, 0.0, 0.0, null, null, address);
        try {
            dao.addWholesaler(wholesaler, this);
            wholesalerNameField.setText("");
            mobileNoField.setText("");
            addressField.setText("");
            cardLayout.show(mainContentPanel, "GYM_WHOLESALER");
        } catch (Exception ex) {
            LOGGER.severe("Error adding wholesaler: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Failed to add wholesaler. Please try again.", "Error", JOptionPane.ERROR_MESSAGE));
        }
    }
}