package ui;

import model.DBHelper;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class SignupFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;

    public SignupFrame() {
        setTitle("Signup - Market Simulator");
        setSize(400, 300);
        setLayout(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel lblTitle = new JLabel("Market Simulator Signup", SwingConstants.CENTER);
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

        JLabel lblConfirm = new JLabel("Confirm:");
        lblConfirm.setBounds(50, 140, 100, 25);
        add(lblConfirm);

        confirmField = new JPasswordField();
        confirmField.setBounds(150, 140, 180, 25);
        add(confirmField);

        JButton signupBtn = new JButton("Signup");
        signupBtn.setBounds(50, 200, 120, 30);
        signupBtn.setBackground(new Color(30, 144, 255));
        signupBtn.setForeground(Color.WHITE);
        signupBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        add(signupBtn);

        JButton loginBtn = new JButton("Back to Login");
        loginBtn.setBounds(210, 200, 120, 30);
        loginBtn.setBackground(new Color(34, 139, 34));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        add(loginBtn);

        signupBtn.addActionListener(e -> onSignup());
        loginBtn.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });

        setVisible(true);
    }

    private void onSignup() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String confirm = new String(confirmField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.");
            return;
        }

        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.");
            return;
        }

        try {
            if (DBHelper.userExists(username)) {
                JOptionPane.showMessageDialog(this, "Username already exists.");
                return;
            }

            DBHelper.insertUser(new User(username, password)); // balance defaults to 100000
            JOptionPane.showMessageDialog(this, "Signup successful! Please login.");
            dispose();
            new LoginFrame();

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }
}
