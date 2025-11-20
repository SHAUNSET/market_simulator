package model;

public class User {
    private int id;
    private String username;
    private String password;
    private double balance;

    // Constructor for new signup (balance defaults to 100000)
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.balance = 100000;
    }

    // Constructor for fetching from DB
    public User(int id, String username, String password, double balance) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.balance = balance;
    }

    // Getters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public double getBalance() { return balance; }

    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setBalance(double balance) { this.balance = balance; }
}
