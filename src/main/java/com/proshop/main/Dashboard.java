package com.proshop.main;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.proshop.connection.DBUtil;

public class Dashboard extends JFrame {

    private static final long serialVersionUID = 1L;

    // ====================== COLOR SCHEME ======================
    private static final Color PRIMARY_COLOR      = new Color(33, 33, 33);    // #212121
    private static final Color SECONDARY_COLOR    = new Color(33, 33, 33);
    private static final Color ACCENT_COLOR       = new Color(255, 215, 0);   // #FFD700
    private static final Color SUCCESS_COLOR      = new Color(102, 187, 106); // #66BB6A
    private static final Color HOVER_COLOR        = new Color(50, 50, 50);    // #323232
    private static final Color SIDEBAR_COLOR      = new Color(33, 33, 33);
    private static final Color BACKGROUND_COLOR   = new Color(33, 33, 33);
    private static final Color TEXT_COLOR         = Color.WHITE;
    private static final Color HOVER_TEXT_COLOR   = new Color(224, 224, 224);
    private static final Color STAT_CARD_COLOR    = new Color(50, 50, 50);
    private static final Color SHADOW_COLOR       = new Color(224, 224, 224);

    // ====================== UI COMPONENTS ======================
    private JPanel mainContentPanel;
    private CardLayout cardLayout;
    private JButton activeButton = null;
    private JPanel sidebarPanel;
    private boolean isSidebarVisible = true;
    private int sidebarWidth = 250;
    private Timer slideTimer;

    // ====================== CONSTRUCTOR ======================
    public Dashboard() {
        initializeFrame();
        setupLayout();
        setupKeyboardShortcuts();
        showWelcomePanel();
    }

    // ====================== INITIALIZATION ======================
    private void initializeFrame() {
        setTitle("Gaurav Yadav Pure Protein Shop - Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout(5, 5));
    }

    private void setupLayout() {
        JPanel headerPanel = createEnhancedHeader();
        sidebarPanel = createEnhancedSidebar();
        mainContentPanel = createMainContentPanel();

        add(headerPanel, BorderLayout.NORTH);
        add(sidebarPanel, BorderLayout.WEST);
        add(mainContentPanel, BorderLayout.CENTER);
    }

    private JPanel createMainContentPanel() {
        JPanel panel = new JPanel();
        cardLayout = new CardLayout();
        panel.setLayout(cardLayout);
        panel.setBackground(BACKGROUND_COLOR);

        panel.add(createWelcomePanel(), "WELCOME");
        panel.add(new CustomerForm(cardLayout, panel), "CUSTOMER");
        panel.add(new BillDetailsForm(cardLayout, panel), "BILL_DETAILS");
        panel.add(new GymWholesalerForm(cardLayout, panel), "GYM_WHOLESALER");
        panel.add(new StockForm(cardLayout, panel), "STOCK");
        panel.add(new WholesalerForm(cardLayout, panel), "WHOLESALER");
        panel.add(new SoldStockForm(cardLayout, panel), "SOLD_STOCK");

        return panel;
    }

    // ====================== KEYBOARD SHORTCUTS ======================
    private void setupKeyboardShortcuts() {
        InputMap inputMap = mainContentPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = mainContentPanel.getActionMap();

        String[][] shortcuts = {
            {"Alt+1", "WELCOME"}, {"Alt+2", "CUSTOMER"}, {"Alt+3", "BILL_DETAILS"},
            {"Alt+4", "GYM_WHOLESALER"}, {"Alt+5", "STOCK"}, {"Alt+6", "WHOLESALER"}, {"Alt+7", "SOLD_STOCK"}
        };

        for (int i = 0; i < shortcuts.length; i++) {
            final String key = shortcuts[i][0];
            final String panel = shortcuts[i][1];
            inputMap.put(KeyStroke.getKeyStroke(key), panel);
            actionMap.put(panel, new NavigationAction(panel));
        }

        KeyStroke sidebarKey = KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.ALT_DOWN_MASK);
        inputMap.put(sidebarKey, "TOGGLE_SIDEBAR");
        actionMap.put("TOGGLE_SIDEBAR", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleSidebar();
            }
        });
    }

    private void showWelcomePanel() {
        cardLayout.show(mainContentPanel, "WELCOME");
    }

    // ====================== HEADER ======================
    private JPanel createEnhancedHeader() {
        JPanel headerPanel = new GradientPanel(PRIMARY_COLOR, SECONDARY_COLOR);
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(0, 100));
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setOpaque(false);

        JButton hamburgerButton = createHamburgerButton();
        JLabel logoLabel = createLogoLabel();
        JPanel titlePanel = createTitlePanel();

        leftPanel.add(hamburgerButton);
        leftPanel.add(logoLabel);
        leftPanel.add(titlePanel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);

        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JButton createHamburgerButton() {
        JButton button = new JButton() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.WHITE);
                int y = 15;
                for (int i = 0; i < 3; i++) {
                    g2d.fillRect(5, y, 20, 3);
                    y += 8;
                }
            }
        };
        button.setPreferredSize(new Dimension(30, 30));
        button.setBackground(new Color(0, 0, 0, 0));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleSidebar();
            }
        });
        return button;
    }

    private JLabel createLogoLabel() {
        JLabel label = new JLabel();
        try {
            ImageIcon icon = new ImageIcon("logo.png");
            Image scaled = icon.getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            label.setText("Logo");
            label.setFont(new Font("Arial", Font.BOLD, 16));
            label.setForeground(Color.WHITE);
            System.err.println("Error loading logo: " + e.getMessage());
        }
        label.setPreferredSize(new Dimension(70, 70));
        return label;
    }

    private JPanel createTitlePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel mainTitle = new JLabel("GAURAV YADAV");
        mainTitle.setForeground(Color.WHITE);
        mainTitle.setFont(new Font("Arial", Font.BOLD, 28));

        JLabel subTitle = new JLabel("PURE PROTEIN SHOP");
        subTitle.setForeground(ACCENT_COLOR);
        subTitle.setFont(new Font("Arial", Font.BOLD, 20));

        panel.add(mainTitle);
        panel.add(subTitle);
        return panel;
    }

    // ====================== SIDEBAR ======================
    private JPanel createEnhancedSidebar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(SIDEBAR_COLOR);
        panel.setPreferredSize(new Dimension(sidebarWidth, 0));
        panel.setBorder(new EmptyBorder(20, 15, 20, 15));

        String[][] navItems = {
            {"", "DASHBOARD", "WELCOME"},
            {"", "CUSTOMERS", "CUSTOMER"},
            {"", "BILL DETAILS", "BILL_DETAILS"},
            {"", "GYM/SHOP CLIENT", "GYM_WHOLESALER"},
            {"", "INVENTORY", "STOCK"},
            {"", "WHOLESALE PURCHASE", "WHOLESALER"},
            {"", "SOLD STOCK", "SOLD_STOCK"}
        };

        for (int i = 0; i < navItems.length; i++) {
            JButton button = createEnhancedNavButton(navItems[i][0], navItems[i][1], navItems[i][2]);
            button.setActionCommand(navItems[i][2]);
            panel.add(button);
            panel.add(Box.createVerticalStrut(10));
        }

        panel.add(Box.createVerticalGlue());
        panel.add(createSidebarFooter());

        return panel;
    }

    private JPanel createSidebarFooter() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(20, 15, 10, 15));

        JButton logoutButton = createLogoutButton();
        JLabel versionLabel = new JLabel("Version 2.0");
        versionLabel.setForeground(Color.WHITE);
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.add(logoutButton);
        footer.add(Box.createVerticalStrut(10));
        footer.add(versionLabel);

        return footer;
    }

    private JButton createLogoutButton() {
        JButton button = new JButton("Logout");
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(Color.GRAY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 20, 10, 20));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(Color.DARK_GRAY);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.GRAY);
            }
        });

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                    Dashboard.this,
                    "Are you sure you want to logout?",
                    "Logout Confirmation",
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });

        return button;
    }

    private JButton createEnhancedNavButton(String icon, String text, String action) {
        JButton button = new JButton() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg;
                if (getModel().isPressed()) {
                    bg = SIDEBAR_COLOR.darker();
                } else if (getModel().isRollover() || this == activeButton) {
                    bg = HOVER_COLOR;
                } else {
                    bg = SIDEBAR_COLOR;
                }
                g2d.setColor(bg);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                Color textColor;
                if (this == activeButton) {
                    textColor = ACCENT_COLOR;
                } else if (getModel().isRollover()) {
                    textColor = HOVER_TEXT_COLOR;
                } else {
                    textColor = TEXT_COLOR;
                }
                g2d.setColor(textColor);

                g2d.setFont(new Font("Arial", Font.PLAIN, 20));
                g2d.drawString(icon, 20, 30);

                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.drawString(text, 50, 30);
            }
        };

        button.setPreferredSize(new Dimension(220, 50));
        button.setMaximumSize(new Dimension(220, 50));
        button.setMinimumSize(new Dimension(220, 50));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setBackground(SIDEBAR_COLOR);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 15, 10, 15));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBorder(BorderFactory.createLineBorder(SHADOW_COLOR, 2, true));
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBorder(new EmptyBorder(10, 15, 10, 15));
                button.repaint();
            }
        });

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainContentPanel, action);
                setActiveButton(button);
            }
        });

        return button;
    }

    // ====================== SIDEBAR TOGGLE ANIMATION ======================
    private void toggleSidebar() {
        if (slideTimer != null && slideTimer.isRunning()) {
            return;
        }

        final int targetWidth = isSidebarVisible ? 0 : sidebarWidth;
        final int startWidth = isSidebarVisible ? sidebarWidth : 0;
        final int steps = 10;
        final int stepDelay = 1;
        final int widthIncrement = (targetWidth - startWidth) / steps;

        slideTimer = new Timer(stepDelay, new ActionListener() {
            int currentStep = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                currentStep++;
                int newWidth = startWidth + (widthIncrement * currentStep);
                sidebarPanel.setPreferredSize(new Dimension(newWidth, sidebarPanel.getHeight()));
                sidebarPanel.revalidate();
                sidebarPanel.repaint();

                if (currentStep >= steps) {
                    slideTimer.stop();
                    isSidebarVisible = !isSidebarVisible;
                    sidebarPanel.setPreferredSize(new Dimension(targetWidth, 0));
                    sidebarPanel.revalidate();
                }
            }
        });
        slideTimer.start();
    }

    private void setActiveButton(JButton button) {
        if (activeButton != null) {
            activeButton.repaint();
        }
        activeButton = button;
        if (button != null) {
            button.repaint();
        }
    }

    // ====================== WELCOME PANEL ======================
    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(BACKGROUND_COLOR);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setLayout(new BorderLayout());
        panel.setOpaque(false);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(40, 40, 40, 40));
        contentPanel.setOpaque(false);

        JLabel welcomeLabel = new JLabel("Welcome to Pure Protein Shop");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 36));
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel("Your Premium Fitness Nutrition Management System");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        descLabel.setForeground(new Color(224, 224, 224));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel statsPanel = new JPanel(new GridLayout(2, 4, 20, 20));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(new EmptyBorder(40, 0, 0, 0));

        long activeCustomers = getActiveCustomers();
        long productsInStock = getProductsInStock();
        long totalWholesalers = getTotalWholesalers();
        double totalSales = getTotalSales();
        double totalProfit = getTotalProfit();
        long totalSoldQuantity = getTotalSoldQuantity();
        double totalSalesAmount = getTotalSalesAmount();
        double totalNetProfit = getTotalNetProfit();

        statsPanel.add(createStatCard("", "Customers Connected", String.valueOf(activeCustomers), SUCCESS_COLOR));
        statsPanel.add(createStatCard("", "Products in Stock", String.valueOf(productsInStock), SUCCESS_COLOR));
        statsPanel.add(createStatCard("", "Wholesalers", String.valueOf(totalWholesalers), SUCCESS_COLOR));
        statsPanel.add(createStatCard("", "Total Sales", String.format("₹%.2f", totalSales), SUCCESS_COLOR));
        statsPanel.add(createStatCard("", "Total Profit", String.format("₹%.2f", totalProfit), SUCCESS_COLOR));
        statsPanel.add(createStatCard("", "Total Sold Quantity", String.valueOf(totalSoldQuantity), SUCCESS_COLOR));
        statsPanel.add(createStatCard("", "Total Sales Amount", String.format("₹%.2f", totalSalesAmount), SUCCESS_COLOR));
        statsPanel.add(createStatCard("", "Total Net Profit", String.format("₹%.2f", totalNetProfit), SUCCESS_COLOR));

        contentPanel.add(welcomeLabel);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(descLabel);
        contentPanel.add(Box.createVerticalStrut(30));
        contentPanel.add(statsPanel);

        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    // ====================== STAT CARD ======================
    private JPanel createStatCard(String icon, String title, String value, Color valueColor) {
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
        card.setPreferredSize(new Dimension(220, 120));

        card.add(Box.createVerticalGlue());

        // Use text icons instead of emoji for Java 12 compatibility
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Arial", Font.BOLD, 28));
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(iconLabel);
        card.add(Box.createVerticalStrut(5));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(new Color(224, 224, 224));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));
        valueLabel.setForeground(valueColor);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(valueLabel);
        card.add(Box.createVerticalGlue());

        return card;
    }

    // ====================== DATABASE QUERIES ======================
    private long executeLongQuery(String sql, String errorMessage) {
        try {
            Connection conn = DBUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            rs.close();
            pstmt.close();
            conn.close();
        } catch (SQLException ex) {
            System.err.println(errorMessage + ": " + ex.getMessage());
        }
        return 0;
    }

    private double executeDoubleQuery(String sql, String errorMessage) {
        try {
            Connection conn = DBUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
            rs.close();
            pstmt.close();
            conn.close();
        } catch (SQLException ex) {
            System.err.println(errorMessage + ": " + ex.getMessage());
        }
        return 0.0;
    }

    private double sumDoubleQueries(List<String> queries, String errorMessage) {
        double sum = 0.0;
        for (String sql : queries) {
            sum += executeDoubleQuery(sql, errorMessage);
        }
        return sum;
    }

    private long sumLongQueries(List<String> queries, String errorMessage) {
        long sum = 0;
        for (String sql : queries) {
            sum += executeLongQuery(sql, errorMessage);
        }
        return sum;
    }

    private long getActiveCustomers() {
        return executeLongQuery("SELECT COUNT(*) FROM customer", "Error fetching active customers");
    }

    private long getProductsInStock() {
        return executeLongQuery("SELECT SUM(quantity) FROM stock", "Error fetching products in stock");
    }

    private long getTotalWholesalers() {
        return executeLongQuery("SELECT COUNT(*) FROM gym_wholesaler", "Error fetching wholesalers");
    }

    private double getTotalSales() {
        List<String> queries = Arrays.asList(
            "SELECT SUM(productSale) FROM billdetails",
            "SELECT SUM(finalBill) FROM customer",
            "SELECT SUM(totalBill + netProfit) FROM gym_wholesaler",
            "SELECT SUM(productQuantity * sellingPrice) FROM shop_wholesaler"
        );
        return sumDoubleQueries(queries, "Error fetching total sales");
    }

    private double getTotalProfit() {
        List<String> queries = Arrays.asList(
            "SELECT SUM(productSale - (rent + lightBill + maintenanceBill + salary + parcelBillAmount + bankEmi + othersAmount)) FROM billdetails",
            "SELECT SUM(netProfit) FROM customer",
            "SELECT SUM(netProfit) FROM gym_wholesaler",
            "SELECT SUM(productQuantity * (sellingPrice - buyingPrice)) FROM shop_wholesaler"
        );
        return sumDoubleQueries(queries, "Error fetching total profit");
    }

    private long getTotalSoldQuantity() {
        List<String> queries = Arrays.asList(
            "SELECT SUM(quantity) FROM customer",
            "SELECT SUM(quantity) FROM gym_wholesaler"
        );
        return sumLongQueries(queries, "Error fetching total sold quantity");
    }

    private double getTotalSalesAmount() {
        List<String> queries = Arrays.asList(
            "SELECT SUM(finalBill) FROM customer",
            "SELECT SUM(totalBill) FROM gym_wholesaler"
        );
        return sumDoubleQueries(queries, "Error fetching total sales amount");
    }

    private double getTotalNetProfit() {
        List<String> queries = Arrays.asList(
            "SELECT SUM(netProfit) FROM customer",
            "SELECT SUM(netProfit) FROM gym_wholesaler"
        );
        return sumDoubleQueries(queries, "Error fetching total net profit");
    }

    // ====================== INNER CLASSES ======================
    private class NavigationAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        private final String panelName;

        public NavigationAction(String panelName) {
            this.panelName = panelName;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            cardLayout.show(mainContentPanel, panelName);
            Component[] components = sidebarPanel.getComponents();
            for (int i = 0; i < components.length; i++) {
                if (components[i] instanceof JButton) {
                    JButton btn = (JButton) components[i];
                    if (panelName.equals(btn.getActionCommand())) {
                        setActiveButton(btn);
                        break;
                    }
                }
            }
        }
    }

    private static class GradientPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final Color startColor;
        private final Color endColor;

        public GradientPanel(Color startColor, Color endColor) {
            this.startColor = startColor;
            this.endColor = endColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            GradientPaint gp = new GradientPaint(0, 0, startColor, getWidth(), 0, endColor);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}