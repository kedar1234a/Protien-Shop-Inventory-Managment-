package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.proshop.model.Bill;
import com.proshop.model.Product;
import com.proshop.model.WholesalerPurchase;

public class WholesalerForm extends JPanel {
    private static final long serialVersionUID = 1L;
    private JPanel blocksPanel;
    private final CardLayout cardLayout;
    private final JPanel mainContentPanel;
    private Map<WholesalerPurchase, Map<Bill, List<Product>>> wholesalerData;
    private JComboBox<String> searchComboBox;

    // Color scheme to match WholesalerDetailView and WholesalerDateCardView
    private static final Color BACKGROUND_COLOR = new Color(33, 33, 33); // Dark gray (#212121)
    private static final Color TEXT_COLOR = Color.WHITE; // White
    private static final Color SHADOW_COLOR = new Color(224, 224, 224); // Very light gray for border (#E0E0E0)
    private static final Color BACK_COLOR = new Color(120, 120, 120); // Gray (#787878)

    public WholesalerForm(CardLayout cardLayout, JPanel mainContentPanel) {
        this.cardLayout = cardLayout;
        this.mainContentPanel = mainContentPanel;
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SHADOW_COLOR, 1),
            new EmptyBorder(10, 10, 10, 10)
        ));

        // Button panel with styled buttons and search bar
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Search bar with JComboBox
        JLabel searchLabel = new JLabel("Search Wholesaler: ");
        searchLabel.setFont(new Font("Arial", Font.BOLD, 15));
        searchLabel.setForeground(TEXT_COLOR);
        searchComboBox = new JComboBox<>();
        searchComboBox.setEditable(true);
        searchComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        searchComboBox.setPreferredSize(new Dimension(200, 30));
        searchComboBox.setBackground(BACKGROUND_COLOR);
        searchComboBox.setForeground(TEXT_COLOR);
        loadWholesalerNames();
        
        // Handle typing in search bar
        JTextField editor = (JTextField) searchComboBox.getEditor().getEditorComponent();
        editor.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() != java.awt.event.KeyEvent.VK_UP &&
                    e.getKeyCode() != java.awt.event.KeyEvent.VK_DOWN &&
                    e.getKeyCode() != java.awt.event.KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(() -> {
                        String text = editor.getText().trim();
                        autoFillWholesalerNames(text);
                        filterCards(text);
                    });
                }
            }
        });

        // Handle selection from dropdown
        searchComboBox.addActionListener(e -> {
            String selectedName = (String) searchComboBox.getSelectedItem();
            if (selectedName != null && !selectedName.trim().isEmpty()) {
                filterCards(selectedName);
            }
        });

        JButton addNewWholesalerButton = createStyledButton("Add New Wholesaler");
        JButton updateWholesalerButton = createStyledButton("Update Wholesaler");
        JButton deleteWholesalerButton = createStyledButton("Delete Wholesaler");
        
        buttonPanel.add(searchLabel);
        buttonPanel.add(searchComboBox);
        buttonPanel.add(addNewWholesalerButton);
        buttonPanel.add(updateWholesalerButton);
        buttonPanel.add(deleteWholesalerButton);
        add(buttonPanel, BorderLayout.NORTH);

        // Blocks panel for wholesaler cards
        blocksPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        blocksPanel.setBackground(BACKGROUND_COLOR);
        blocksPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane scrollPane = new JScrollPane(blocksPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        add(scrollPane, BorderLayout.CENTER);

        addNewWholesalerButton.addActionListener(e -> {
            mainContentPanel.add(new WholesalerAddForm(cardLayout, mainContentPanel), "ADD_WHOLESALER");
            cardLayout.show(mainContentPanel, "ADD_WHOLESALER");
        });

        updateWholesalerButton.addActionListener(e -> selectWholesalerForUpdate());
        deleteWholesalerButton.addActionListener(e -> selectWholesalerForDelete());

        loadBlocks();
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 15));
        button.setForeground(TEXT_COLOR);
        button.setBackground(BACK_COLOR);
        button.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 1));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(160, 45));
        return button;
    }

    private void loadWholesalerNames() {
        List<WholesalerPurchase> wholesalers = DatabaseUtils.fetchWholesalers();
        List<String> wholesalerNames = wholesalers.stream()
                .map(WholesalerPurchase::getWholesalerName)
                .sorted()
                .collect(Collectors.toList());
        searchComboBox.removeAllItems();
        searchComboBox.addItem("");
        wholesalerNames.forEach(searchComboBox::addItem);
    }

    private void autoFillWholesalerNames(String searchText) {
        String queryText = searchText.trim().toLowerCase();
        List<WholesalerPurchase> wholesalers = DatabaseUtils.fetchWholesalers();
        List<String> suggestions = wholesalers.stream()
                .map(WholesalerPurchase::getWholesalerName)
                .filter(name -> name.toLowerCase().contains(queryText))
                .sorted()
                .collect(Collectors.toList());
        searchComboBox.removeAllItems();
        searchComboBox.addItem("");
        suggestions.forEach(searchComboBox::addItem);
        searchComboBox.setSelectedItem(searchText);
        if (!queryText.isEmpty() && !suggestions.isEmpty()) {
            searchComboBox.showPopup();
        }
    }

    private void filterCards(String searchText) {
        blocksPanel.removeAll();
        wholesalerData = DatabaseUtils.loadWholesalerData();
        String queryText = searchText != null ? searchText.trim().toLowerCase() : "";
        for (Map.Entry<WholesalerPurchase, Map<Bill, List<Product>>> entry : wholesalerData.entrySet()) {
            WholesalerPurchase wholesaler = entry.getKey();
            if (queryText.isEmpty() || 
                wholesaler.getWholesalerName().toLowerCase().contains(queryText)) {
                WholesalerCard card = new WholesalerCard(entry.getKey(), entry.getValue(), cardLayout, mainContentPanel);
                blocksPanel.add(card);
            }
        }
        blocksPanel.revalidate();
        blocksPanel.repaint();
    }

    private void selectWholesalerForUpdate() {
        List<WholesalerPurchase> wholesalers = DatabaseUtils.fetchWholesalers();
        if (wholesalers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No wholesalers available.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) mainContentPanel.getTopLevelAncestor(), "Select Wholesaler to Update", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setBackground(BACKGROUND_COLOR);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel titleLabel = new JLabel("Select Wholesaler:");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_COLOR);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(titleLabel, gbc);

        JCheckBox[] wholesalerCheckBoxes = new JCheckBox[wholesalers.size()];
        for (int i = 0; i < wholesalers.size(); i++) {
            wholesalerCheckBoxes[i] = new JCheckBox(wholesalers.get(i).getWholesalerName());
            wholesalerCheckBoxes[i].setFont(new Font("Arial", Font.BOLD, 16));
            wholesalerCheckBoxes[i].setForeground(TEXT_COLOR);
            wholesalerCheckBoxes[i].setBackground(BACKGROUND_COLOR);
            gbc.gridy = i + 1;
            panel.add(wholesalerCheckBoxes[i], gbc);
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR));
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        JButton okButton = createStyledButton("OK");
        JButton cancelButton = createStyledButton("Cancel");

        okButton.addActionListener(e -> {
            for (int i = 0; i < wholesalerCheckBoxes.length; i++) {
                if (wholesalerCheckBoxes[i].isSelected()) {
                    WholesalerPurchase selectedWholesaler = wholesalers.get(i);
                    mainContentPanel.add(new WholesalerUpdateForm(selectedWholesaler, cardLayout, mainContentPanel), "UPDATE_WHOLESALER");
                    cardLayout.show(mainContentPanel, "UPDATE_WHOLESALER");
                    dialog.dispose();
                    return;
                }
            }
            JOptionPane.showMessageDialog(dialog, "Please select a wholesaler.", "Error", JOptionPane.ERROR_MESSAGE);
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void selectWholesalerForDelete() {
        List<WholesalerPurchase> wholesalers = DatabaseUtils.fetchWholesalers();
        if (wholesalers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No wholesalers available.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) mainContentPanel.getTopLevelAncestor(), "Select Wholesaler to Delete", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setBackground(BACKGROUND_COLOR);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel titleLabel = new JLabel("Select Wholesaler:");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_COLOR);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(titleLabel, gbc);

        JCheckBox[] wholesalerCheckBoxes = new JCheckBox[wholesalers.size()];
        for (int i = 0; i < wholesalers.size(); i++) {
            wholesalerCheckBoxes[i] = new JCheckBox(wholesalers.get(i).getWholesalerName());
            wholesalerCheckBoxes[i].setFont(new Font("Arial", Font.BOLD, 16));
            wholesalerCheckBoxes[i].setForeground(TEXT_COLOR);
            wholesalerCheckBoxes[i].setBackground(BACKGROUND_COLOR);
            gbc.gridy = i + 1;
            panel.add(wholesalerCheckBoxes[i], gbc);
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR));
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        JButton deleteButton = createStyledButton("Delete");
        JButton cancelButton = createStyledButton("Cancel");

        deleteButton.addActionListener(e -> {
            WholesalerPurchase selectedWholesaler = null;
            for (int i = 0; i < wholesalerCheckBoxes.length; i++) {
                if (wholesalerCheckBoxes[i].isSelected()) {
                    selectedWholesaler = wholesalers.get(i);
                    break;
                }
            }
            if (selectedWholesaler == null) {
                JOptionPane.showMessageDialog(dialog, "Please select a wholesaler.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Are you sure you want to delete " + selectedWholesaler.getWholesalerName() + "?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    DatabaseUtils.deleteWholesaler(selectedWholesaler);
                    loadBlocks();
                    loadWholesalerNames(); // Update search bar after deletion
                    JOptionPane.showMessageDialog(this, "Wholesaler deleted successfully!", "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                } catch (RuntimeException ex) {
                    JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(deleteButton);
        buttonPanel.add(cancelButton);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    public void loadBlocks() {
        blocksPanel.removeAll();
        wholesalerData = DatabaseUtils.loadWholesalerData();
        for (Map.Entry<WholesalerPurchase, Map<Bill, List<Product>>> entry : wholesalerData.entrySet()) {
            WholesalerCard card = new WholesalerCard(entry.getKey(), entry.getValue(), cardLayout, mainContentPanel);
            blocksPanel.add(card);
        }
        blocksPanel.revalidate();
        blocksPanel.repaint();
        loadWholesalerNames(); // Update search bar when blocks are loaded
    }
}