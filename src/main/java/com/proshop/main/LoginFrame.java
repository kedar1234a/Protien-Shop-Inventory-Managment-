// LoginFrame.java
package com.proshop.main;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

public class LoginFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JLabel statusLabel;

	// Hardcoded credentials
	private static final String USERNAME = "admin";
	private static final String PASSWORD = "admin";

	public LoginFrame() {
		setTitle("Login - Pure Protein Shop");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(450, 550);
		setLocationRelativeTo(null);
		setResizable(false);

		// Main panel with gradient background
		JPanel mainPanel = new JPanel() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				GradientPaint gp = new GradientPaint(0, 0, new Color(33, 33, 33), 0, getHeight(),
						new Color(50, 50, 50));
				g2d.setPaint(gp);
				g2d.fillRect(0, 0, getWidth(), getHeight());
			}
		};
		mainPanel.setLayout(new GridBagLayout());
		mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(10, 0, 10, 0);

		// Title
		JLabel titleLabel = new JLabel("PURE PROTEIN SHOP", JLabel.CENTER);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
		titleLabel.setForeground(new Color(255, 215, 0)); // Gold
		mainPanel.add(titleLabel, gbc);

		JLabel subtitleLabel = new JLabel("Management System Login", JLabel.CENTER);
		subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 16));
		subtitleLabel.setForeground(Color.LIGHT_GRAY);
		mainPanel.add(subtitleLabel, gbc);

		// Logo
		JLabel logoLabel = new JLabel();
		try {
			ImageIcon logoIcon = new ImageIcon("logo.png");
			Image scaledImage = logoIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
			logoLabel.setIcon(new ImageIcon(scaledImage));
		} catch (Exception e) {
			logoLabel.setText("LOGO");
			logoLabel.setFont(new Font("Arial", Font.BOLD, 20));
			logoLabel.setForeground(Color.WHITE);
		}
		logoLabel.setHorizontalAlignment(JLabel.CENTER);
		mainPanel.add(logoLabel, gbc);

		// Username field
		JLabel userLabel = new JLabel("Username");
		userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
		userLabel.setForeground(Color.WHITE);
		mainPanel.add(userLabel, gbc);

		usernameField = new JTextField(20);
		usernameField.setFont(new Font("Arial", Font.PLAIN, 16));
		usernameField.setPreferredSize(new Dimension(300, 45));
		usernameField.setBackground(new Color(70, 70, 70));
		usernameField.setForeground(Color.WHITE);
		usernameField.setBorder(
				BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
						BorderFactory.createEmptyBorder(5, 10, 5, 10)));
		mainPanel.add(usernameField, gbc);

		// Password field
		JLabel passLabel = new JLabel("Password");
		passLabel.setFont(new Font("Arial", Font.PLAIN, 14));
		passLabel.setForeground(Color.WHITE);
		mainPanel.add(passLabel, gbc);

		passwordField = new JPasswordField(20);
		passwordField.setFont(new Font("Arial", Font.PLAIN, 16));
		passwordField.setPreferredSize(new Dimension(300, 45));
		passwordField.setBackground(new Color(70, 70, 70));
		passwordField.setForeground(Color.WHITE);
		passwordField.setBorder(
				BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
						BorderFactory.createEmptyBorder(5, 10, 5, 10)));
		mainPanel.add(passwordField, gbc);

		// Status label
		statusLabel = new JLabel(" ", JLabel.CENTER);
		statusLabel.setFont(new Font("Arial", Font.PLAIN, 13));
		statusLabel.setForeground(Color.RED);
		mainPanel.add(statusLabel, gbc);

		// Login button
		JButton loginButton = new JButton("LOGIN");
		loginButton.setFont(new Font("Arial", Font.BOLD, 16));
		loginButton.setBackground(new Color(255, 215, 0));
		loginButton.setForeground(Color.BLACK);
		loginButton.setFocusPainted(false);
		loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		loginButton.setPreferredSize(new Dimension(300, 50));
		loginButton.addActionListener(new LoginAction());
		mainPanel.add(loginButton, gbc);

		// Footer
		JLabel footerLabel = new JLabel("Version 2.0", JLabel.CENTER);
		footerLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		footerLabel.setForeground(Color.GRAY);
		mainPanel.add(footerLabel, gbc);

		// Enter key support
		passwordField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					loginButton.doClick();
				}
			}
		});

		add(mainPanel);
	}

	private class LoginAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String username = usernameField.getText().trim();
			String password = new String(passwordField.getPassword()).trim();

			if (username.isEmpty() || password.isEmpty()) {
				statusLabel.setText("Please enter both username and password.");
				return;
			}

			if (USERNAME.equals(username) && PASSWORD.equals(password)) {
				statusLabel.setForeground(new Color(102, 187, 106));
				statusLabel.setText("Login successful! Opening dashboard...");
				Timer timer = new Timer(800, evt -> {
					dispose();
					EventQueue.invokeLater(() -> new Dashboard().setVisible(true));
				});
				timer.setRepeats(false);
				timer.start();
			} else {
				statusLabel.setText("Invalid username or password.");
				passwordField.setText("");
				usernameField.requestFocus();
			}
		}
	}

	// Main method now here
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getLookAndFeel());
			} catch (Exception e) {
				e.printStackTrace();
			}
			new LoginFrame().setVisible(true);
		});
	}
}