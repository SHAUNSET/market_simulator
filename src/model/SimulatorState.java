package model;

import java.io.Serializable;
import java.util.*;

public class SimulatorState implements Serializable {
    private Map<String, Stock> stocks;
    private Map<String, Integer> portfolio;
    private List<StockTransaction> todayTransactions;
    private List<StockTransaction> allTransactions;
    private double balance;
    private double dailyPnL;
    private long lastTickTime;
    private int dayIndex;

    public SimulatorState() {
        this.stocks = new LinkedHashMap<>();
        this.portfolio = new HashMap<>();
        this.todayTransactions = new ArrayList<>();
        this.allTransactions = new ArrayList<>();
        this.balance = 100000;
        this.dailyPnL = 0.0;
        this.lastTickTime = System.currentTimeMillis();
        this.dayIndex = 0;
    }

    // ---------- Getters ----------
    public Map<String, Stock> getStocks() { return stocks; }
    public Map<String, Integer> getPortfolio() { return portfolio; }
    public List<StockTransaction> getTodayTransactions() { return todayTransactions; }
    public List<StockTransaction> getAllTransactions() { return allTransactions; }
    public double getBalance() { return balance; }
    public double getDailyPnL() { return dailyPnL; }
    public long getLastTickTime() { return lastTickTime; }
    public int getDayIndex() { return dayIndex; }

    // ---------- Setters ----------
    public void setStocks(Map<String, Stock> stocks) { this.stocks = stocks; }  // <-- add this
    public void setBalance(double balance) { this.balance = balance; }
    public void setDailyPnL(double dailyPnL) { this.dailyPnL = dailyPnL; }
    public void setLastTickTime(long lastTickTime) { this.lastTickTime = lastTickTime; }
    public void setDayIndex(int dayIndex) { this.dayIndex = dayIndex; }
}
