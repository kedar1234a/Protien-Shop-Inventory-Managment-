package com.proshop.main;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.proshop.model.GymWholesaler;

public class GymWholesalerForm extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(GymWholesalerForm.class.getName());
    private final JPanel blocksPanel;
    private final CardLayout cardLayout;
    private final JPanel mainContentPanel;
    private final GymWholesalerDAO dao;
    private final JLayeredPane layeredPane;
    private JPanel actionSidebar;
    private JComboBox<String> searchComboBox;

    // Color scheme consistent with the application's theme
    private static final Color TEXT_COLOR = Color.WHITE; // White

    public GymWholesalerForm(CardLayout cardLayout, JPanel mainContentPanel) {
        this.cardLayout = cardLayout;
        this.mainContentPanel = mainContentPanel;
        this.dao = new GymWholesalerDAO();
        setLayout(new BorderLayout());
        setBackground(UIUtils.BACKGROUND_COLOR);

        // Layered Pane for Overlay
        layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);
        add(layeredPane, BorderLayout.CENTER);

        // Button Panel (Top)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        buttonPanel.setBounds(0, 0, getWidth(), 50);
        
        // Search bar with custom-styled JLabel
        JLabel searchLabel = new JLabel("Search Client: ");
        searchLabel.setFont(new Font("Arial", Font.BOLD, 15));
        searchLabel.setForeground(TEXT_COLOR);
        searchComboBox = new JComboBox<>();
        searchComboBox.setEditable(true);
        searchComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        searchComboBox.setPreferredSize(new Dimension(200, 30));
        searchComboBox.setBackground(UIUtils.BACKGROUND_COLOR);
        searchComboBox.setForeground(TEXT_COLOR);
        try {
            loadWholesalerNames();
        } catch (SQLException ex) {
            LOGGER.severe("Error initializing search bar: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Error initializing search bar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
        }

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
                        try {
                            autoFillWholesalerNames(text);
                            filterCards(text);
                        } catch (SQLException ex) {
                            LOGGER.severe("Error auto-filling client names: " + ex.getMessage());
                            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GymWholesalerForm.this,
                                    "Error filtering clients: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                        }
                    });
                }
            }
        });

        // Handle selection from dropdown
        searchComboBox.addActionListener(e -> {
            String selectedName = (String) searchComboBox.getSelectedItem();
            if (selectedName != null && !selectedName.trim().isEmpty()) {
                try {
                    filterCards(selectedName);
                } catch (SQLException ex) {
                    LOGGER.severe("Error filtering cards on selection: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GymWholesalerForm.this,
                            "Error filtering cards: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                }
            }
        });

        JButton addNewWholesalerButton = UIUtils.createStyledButton("Add New Client");
        JButton updateWholesalerButton = UIUtils.createStyledButton("Update Client");
        JButton deleteWholesalerButton = UIUtils.createStyledButton("Delete Client");
        JButton refreshButton = UIUtils.createStyledButton("Refresh");
        
        buttonPanel.add(searchLabel);
        buttonPanel.add(searchComboBox);
        buttonPanel.add(addNewWholesalerButton);
        buttonPanel.add(updateWholesalerButton);
        buttonPanel.add(deleteWholesalerButton);
        buttonPanel.add(refreshButton);
        layeredPane.add(buttonPanel, JLayeredPane.DEFAULT_LAYER);

        // Blocks Panel (Cards)
        blocksPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 65));
        blocksPanel.setBackground(UIUtils.BACKGROUND_COLOR);
        blocksPanel.setBounds(0, 50, getWidth(), getHeight() - 50);
        JScrollPane scrollPane = new JScrollPane(blocksPanel);
        scrollPane.setBorder(null);
        scrollPane.setBounds(0, 50, getWidth(), getHeight() - 50);
        layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);

        // Resize listener
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                buttonPanel.setBounds(0, 0, getWidth(), 50);
                scrollPane.setBounds(0, 50, getWidth(), getHeight() - 50);
                if (actionSidebar != null) {
                    actionSidebar.setBounds(0, 50, 250, getHeight() - 50);
                }
            }
        });

        // Event Listeners
        addNewWholesalerButton.addActionListener(e -> {
            hideActionSidebar();
            mainContentPanel.add(new GymWholesalerAddForm(cardLayout, mainContentPanel, dao), "ADD_GYM_WHOLESALER");
            cardLayout.show(mainContentPanel, "ADD_GYM_WHOLESALER");
        });

        updateWholesalerButton.addActionListener(e -> {
            hideActionSidebar();
            try {
                selectWholesalerForUpdate();
            } catch (SQLException ex) {
                LOGGER.severe("Error selecting client for update: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GymWholesalerForm.this,
                        "Error selecting client for update: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        });

        deleteWholesalerButton.addActionListener(e -> {
            hideActionSidebar();
            try {
                selectWholesalerForDelete();
            } catch (SQLException ex) {
                LOGGER.severe("Error selecting client for delete: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GymWholesalerForm.this,
                        "Error selecting client for delete: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        });

        refreshButton.addActionListener(e -> {
            try {
                loadBlocks();
                loadWholesalerNames();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GymWholesalerForm.this,
                        "Client data refreshed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE));
            } catch (SQLException ex) {
                LOGGER.severe("Error refreshing client data: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GymWholesalerForm.this,
                        "Error refreshing client data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        });

        // Load initial data
        try {
            loadBlocks();
        } catch (SQLException ex) {
            LOGGER.severe("Error loading blocks: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(GymWholesalerForm.this,
                    "Error loading blocks: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
        }
    }

    private void loadWholesalerNames() throws SQLException {
        List<GymWholesaler> wholesalers = dao.fetchWholesalers();
        List<String> wholesalerNames = wholesalers.stream()
                .map(w -> w.getWholesalerName() + " (" + w.getMobileNo() + ")")
                .sorted()
                .collect(Collectors.toList());
        searchComboBox.removeAllItems();
        searchComboBox.addItem("");
        wholesalerNames.forEach(searchComboBox::addItem);
    }

    private void autoFillWholesalerNames(String searchText) throws SQLException {
        String queryText = searchText.trim().toLowerCase();
        List<GymWholesaler> wholesalers = dao.fetchWholesalers();
        List<String> suggestions = wholesalers.stream()
                .map(w -> w.getWholesalerName() + " (" + w.getMobileNo() + ")")
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

    private void filterCards(String searchText) throws SQLException {
        blocksPanel.removeAll();
        List<GymWholesaler> wholesalers = dao.fetchWholesalers();
        String queryText = searchText.trim().toLowerCase();
        for (GymWholesaler wholesaler : wholesalers) {
            String displayName = wholesaler.getWholesalerName() + " (" + wholesaler.getMobileNo() + ")";
            if (queryText.isEmpty() || displayName.toLowerCase().contains(queryText)) {
                try {
                    GymWholesalerCard card = new GymWholesalerCard(wholesaler, cardLayout, mainContentPanel, dao, this::showActionSidebar);
                    blocksPanel.add(card);
                } catch (Exception e) {
                    LOGGER.severe("Error creating client card: " + e.getMessage());
                }
            }
        }
        blocksPanel.revalidate();
        blocksPanel.repaint();
    }

    private void selectWholesalerForUpdate() throws SQLException {
        List<GymWholesaler> wholesalers = dao.fetchWholesalers();
        if (wholesalers.isEmpty()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "No client available.", "Info", JOptionPane.INFORMATION_MESSAGE));
            return;
        }

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JCheckBox[] wholesalerCheckBoxes = new JCheckBox[wholesalers.size()];
        for (int i = 0; i < wholesalers.size(); i++) {
            wholesalerCheckBoxes[i] = new JCheckBox(
                    wholesalers.get(i).getWholesalerName() + " (" + wholesalers.get(i).getMobileNo() + ")");
            gbc.gridy = i;
            panel.add(wholesalerCheckBoxes[i], gbc);
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(300, 200));

        int result = JOptionPane.showConfirmDialog(this, scrollPane, "Select client to Update",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            for (int i = 0; i < wholesalerCheckBoxes.length; i++) {
                if (wholesalerCheckBoxes[i].isSelected()) {
                    GymWholesaler selectedWholesaler = wholesalers.get(i);
                    mainContentPanel.add(new GymWholesalerUpdateForm(selectedWholesaler, cardLayout, mainContentPanel, dao),
                            "UPDATE_GYM_WHOLESALER");
                    cardLayout.show(mainContentPanel, "UPDATE_GYM_WHOLESALER");
                    return;
                }
            }
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Please select a client.", "Error", JOptionPane.ERROR_MESSAGE));
        }
    }

    private void selectWholesalerForDelete() throws SQLException {
        List<GymWholesaler> wholesalers = dao.fetchWholesalers();
        if (wholesalers.isEmpty()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "No client available.", "Info", JOptionPane.INFORMATION_MESSAGE));
            return;
        }

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JCheckBox[] wholesalerCheckBoxes = new JCheckBox[wholesalers.size()];
        for (int i = 0; i < wholesalers.size(); i++) {
            wholesalerCheckBoxes[i] = new JCheckBox(
                    wholesalers.get(i).getWholesalerName() + " (" + wholesalers.get(i).getMobileNo() + ")");
            gbc.gridy = i;
            panel.add(wholesalerCheckBoxes[i], gbc);
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(300, 200));

        int result = JOptionPane.showConfirmDialog(this, scrollPane, "Select client to Delete",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            List<GymWholesaler> selectedWholesalers = new ArrayList<>();
            StringBuilder selectedNames = new StringBuilder();
            for (int i = 0; i < wholesalerCheckBoxes.length; i++) {
                if (wholesalerCheckBoxes[i].isSelected()) {
                    selectedWholesalers.add(wholesalers.get(i));
                    if (selectedNames.length() > 0) {
                        selectedNames.append(", ");
                    }
                    selectedNames.append(wholesalers.get(i).getWholesalerName());
                }
            }

            if (selectedWholesalers.isEmpty()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Please select at least one client.", "Error", JOptionPane.ERROR_MESSAGE));
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete the following client?\n" + selectedNames,
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                for (GymWholesaler wholesaler : selectedWholesalers) {
                    dao.deleteWholesaler(wholesaler, this);
                }
                loadBlocks();
                loadWholesalerNames();
            }
        }
    }

    public void loadBlocks() throws SQLException {
        blocksPanel.removeAll();
        java.util.List<GymWholesaler> wholesalers = dao.fetchWholesalers();
        for (GymWholesaler wholesaler : wholesalers) {
            try {
                GymWholesalerCard card = new GymWholesalerCard(wholesaler, cardLayout, mainContentPanel, dao, this::showActionSidebar);
                blocksPanel.add(card);
            } catch (Exception e) {
                LOGGER.severe("Error creating client card: " + e.getMessage());
            }
        }
        blocksPanel.revalidate();
        blocksPanel.repaint();
        loadWholesalerNames();
    }

    private void showActionSidebar(GymWholesaler wholesaler) {
        hideActionSidebar();
        try {
            actionSidebar = new GymWholesalerActionView(wholesaler, cardLayout, mainContentPanel, dao, this::hideActionSidebar);
            actionSidebar.setBounds(0, 50, 250, getHeight() - 50);
            layeredPane.add(actionSidebar, JLayeredPane.POPUP_LAYER);
            layeredPane.revalidate();
            layeredPane.repaint();
        } catch (Exception e) {
            LOGGER.severe("Error showing action sidebar: " + e.getMessage());
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Failed to load action sidebar. Please try again.", "Error", JOptionPane.ERROR_MESSAGE));
        }
    }

    private void hideActionSidebar() {
        if (actionSidebar != null) {
            layeredPane.remove(actionSidebar);
            actionSidebar = null;
            layeredPane.revalidate();
            layeredPane.repaint();
        }
    }
}