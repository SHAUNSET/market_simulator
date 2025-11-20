package model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StockTransaction implements Serializable {
    private String stockName;
    private String type; // BUY or SELL
    private int quantity;
    private double price;
    private String timestamp;

    public StockTransaction(String stockName, String type, int quantity, double price) {
        this.stockName = stockName;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public String getStockName() { return stockName; }
    public String getType() { return type; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public String getTimestamp() { return timestamp; }
}
