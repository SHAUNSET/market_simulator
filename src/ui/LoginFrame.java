package ui;

import model.DBHelper;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("Login - Market Simulator");
        setSize(400, 250);
        setLayout(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel lblTitle = new JLabel("Market Simulator Login", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial Black", Font.BOLD, 16));
        lblTitle.setBounds(50, 10, 300, 30);
        add(lblTitle);

        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setBounds(50, 60, 100, 25);
        add(lblUsername);

        usernameField = new JTextField();
        usernameField.setBounds(150, 60, 180, 25);
        add(usernameField);

        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setBounds(50, 100, 100, 25);
        add(lblPassword);

        passwordField = new JPasswordField();
        passwordField.setBounds(150, 100, 180, 25);
        add(passwordField);

        JButton loginBtn = new JButton("Login");
        loginBtn.setBounds(50, 150, 120, 30);
        loginBtn.setBackground(new Color(34, 139, 34));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        add(loginBtn);

        JButton signupBtn = new JButton("Signup");
        signupBtn.setBounds(210, 150, 120, 30);
        signupBtn.setBackground(new Color(30, 144, 255));
        signupBtn.setForeground(Color.WHITE);
        signupBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        add(signupBtn);

        loginBtn.addActionListener(e -> onLogin());

        signupBtn.addActionListener(e -> {
            dispose();
            new SignupFrame().setVisible(true);
        });

        passwordField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ENTER) onLogin();
            }
        });

        setVisible(true);
    }

    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.");
            return;
        }

        try {
            User user = DBHelper.authenticateUser(username, password);
            if (user != null) {
                JOptionPane.showMessageDialog(this, "Login successful! Welcome " + user.getUsername());
                dispose();
                new MainSimulatorLauncher(user); // <-- works now
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}
