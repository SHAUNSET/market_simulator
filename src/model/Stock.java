package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Stock implements Serializable {
    private String name;
    private String symbol;
    private double price;
    private double prevClose;
    private List<Double> history; // ← add this

    public Stock(String name, String symbol, double price) {
        this.name = name;
        this.symbol = symbol;
        this.price = price;
        this.prevClose = price;
        this.history = new ArrayList<>();
        this.history.add(price); // initial price
    }

    public String getName() { return name; }
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public void setPrice(double price) {
        this.price = price;
        this.history.add(price); // add to history whenever price changes
    }

    public double getPrevClose() { return prevClose; }
    public void setPrevClose(double prevClose) { this.prevClose = prevClose; }

    public List<Double> getHistory() { return history; } // ← getter for MainSimulatorLauncher

    // Short-term momentum (last 3 prices)
    public double shortMomentum() {
        if(history.size()<2) return 0;
        double last = history.get(history.size()-1);
        double prev = history.get(history.size()-2);
        return (last-prev)/prev;
    }
}
