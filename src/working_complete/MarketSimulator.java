package working_complete;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;

/* Market Simulator - Original Clean Version */
public class MarketSimulator {
    private static final long DAY_MILLIS = 60 * 60 * 1000L; // 1 hour = 1 day
    // private static final long DAY_MILLIS = 60_000L; // 1 minute for testing
    private static final File STATE_FILE = new File("sim_state.dat");

    // Use relative paths or resource loading for icons
    private static final String ICON_MAIN = "/icons/trading.png";
    private static final String ICON_PORTFOLIO = "/icons/portfolio.png";
    private static final String ICON_TRANSACTIONS = "/icons/transaction.png";
    private static final String ICON_DAILY = "/icons/daily.png";
    private static final String ICON_INSIGHTS = "/icons/insights.png";

    // --- Model classes ---
    static class Stock implements Serializable {
        String name;
        double price;
        double prevClose;
        LinkedList<Double> history = new LinkedList<>();

        Stock(String name, double price) {
            this.name = name;
            this.price = price;
            this.prevClose = price;
            history.add(price);
        }

        void pushPrice(double p) {
            history.add(p);
            if (history.size() > 30) history.removeFirst();
        }

        double shortMomentum() {
            int n = history.size();
            if (n < 6) return 0.0;
            int window = Math.min(5, n / 2);
            double sumNew = 0, sumOld = 0;
            for (int i = n - window; i < n; i++) sumNew += history.get(i);
            for (int i = n - 2*window; i < n - window; i++) sumOld += history.get(i);
            double avgNew = sumNew / window, avgOld = sumOld / window;
            return (avgNew - avgOld) / avgOld;
        }
    }

    static class Transaction implements Serializable {
        String stock;
        String type;
        int qty;
        double price;
        String time;

        Transaction(String stock, String type, int qty, double price) {
            this.stock = stock;
            this.type = type;
            this.qty = qty;
            this.price = price;
            this.time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        }
    }

    static class SimulatorState implements Serializable {
        Map<String, Stock> stocks = new LinkedHashMap<>();
        Map<String, Integer> portfolio = new HashMap<>();
        List<Transaction> todayTransactions = new ArrayList<>();
        List<Transaction> allTransactions = new ArrayList<>();
        double balance = 100000;
        double dailyPnL = 0.0;
        long lastTickTime = System.currentTimeMillis();
        int dayIndex = 0;
    }

    // Runtime
    private SimulatorState state;
    private Map<String, Double> initialPrices = new HashMap<>();
    private JFrame mainFrame;
    private JLabel balanceLabel;
    private Map<String, JButton> priceButtons = new HashMap<>();
    private Map<String, JLabel> momentumBadges = new HashMap<>();
    private java.util.Timer dayTimer;
    private java.util.Timer liveFluctTimer;
    private SimulatorState preResetSnapshot = null;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new MarketSimulator().start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void start() {
        loadOrCreateState();
        buildUI();
        startTimer();
        startLiveFluctuations();
    }

    private void loadOrCreateState() {
        if (STATE_FILE.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(STATE_FILE))) {
                state = (SimulatorState) in.readObject();
                System.out.println("Loaded state. Day index: " + state.dayIndex + " Balance: " + state.balance);
            } catch (Exception e) {
                e.printStackTrace();
                createFreshState();
            }
        } else {
            createFreshState();
        }
        for (Map.Entry<String, Stock> e : state.stocks.entrySet())
            initialPrices.put(e.getKey(), e.getValue().price);
    }

    private void createFreshState() {
        state = new SimulatorState();
        state.stocks.put("Reliance", new Stock("Reliance", 2500));
        state.stocks.put("TCS", new Stock("TCS", 3500));
        state.stocks.put("Infosys", new Stock("Infosys", 1450));
        state.stocks.put("HDFC Bank", new Stock("HDFC Bank", 1600));
        state.stocks.put("ICICI Bank", new Stock("ICICI Bank", 970));
        state.stocks.put("Adani Ports", new Stock("Adani Ports", 1200));
        state.stocks.put("Bajaj Finance", new Stock("Bajaj Finance", 7800));
        state.stocks.put("Wipro", new Stock("Wipro", 400));
        state.stocks.put("ONGC", new Stock("ONGC", 210));
        state.stocks.put("Coal India", new Stock("Coal India", 285));
        state.stocks.put("Maruti", new Stock("Maruti", 11000));
        state.stocks.put("Tata Motors", new Stock("Tata Motors", 875));
        state.stocks.put("NTPC", new Stock("NTPC", 310));
        state.stocks.put("Tech Mahindra", new Stock("Tech Mahindra", 1300));
        state.stocks.put("Sun Pharma", new Stock("Sun Pharma", 1250));
    }

    private void buildUI() {
        mainFrame = new JFrame("Market Simulator");
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.setSize(900, 600);
        mainFrame.setLayout(new BorderLayout());
        mainFrame.setLocationRelativeTo(null);

        // Set icon using classpath resource
        try {
            java.net.URL iconUrl = getClass().getResource(ICON_MAIN);
            if (iconUrl != null) {
                ImageIcon ic = new ImageIcon(iconUrl);
                mainFrame.setIconImage(ic.getImage());
            }
        } catch (Exception ignored) {}

        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        // MENU BAR
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(18,18,18));

        JMenuItem miMainChart = new JMenuItem("MAIN CHART");
        JMenuItem miPortfolio = new JMenuItem("PORTFOLIO");
        JMenuItem miTransactions = new JMenuItem("TRANSACTIONS");
        JMenuItem miDailyPnL = new JMenuItem("DAILY PNL");
        JMenuItem miInsights = new JMenuItem("INSIGHTS");

        // Set menu icons from resources
        setMenuItemIcon(miMainChart, ICON_MAIN);
        setMenuItemIcon(miPortfolio, ICON_PORTFOLIO);
        setMenuItemIcon(miTransactions, ICON_TRANSACTIONS);
        setMenuItemIcon(miDailyPnL, ICON_DAILY);
        setMenuItemIcon(miInsights, ICON_INSIGHTS);

        miMainChart.addActionListener(e -> openMainChartWindow());
        miPortfolio.addActionListener(e -> openPortfolioWindow());
        miTransactions.addActionListener(e -> openTransactionsWindow());
        miDailyPnL.addActionListener(e -> openDailyPnLWindow());
        miInsights.addActionListener(e -> openInsightsWindow(null));

        menuBar.add(miMainChart);
        menuBar.add(miPortfolio);
        menuBar.add(miTransactions);
        menuBar.add(miDailyPnL);
        menuBar.add(miInsights);

        mainFrame.setJMenuBar(menuBar);

        // TOP: balance + reset
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(12, 12, 12));
        topBar.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

        JButton resetBtn = new JButton("RESET");
        resetBtn.setBackground(new Color(44,44,44));
        resetBtn.setForeground(Color.WHITE);
        resetBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        resetBtn.setToolTipText("Reset market (Ctrl+R).");
        resetBtn.addActionListener(e -> {
            int ans = JOptionPane.showConfirmDialog(mainFrame,
                    "Reset will clear portfolio, transactions and set prices back to initial values. Continue?",
                    "Confirm Reset", JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.YES_OPTION) doResetWithUndo();
        });

        // Ctrl+R shortcut
        mainFrame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("control R"), "resetNow");
        mainFrame.getRootPane().getActionMap().put("resetNow", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { doResetWithUndo(); }
        });

        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftTop.setOpaque(false);
        leftTop.add(resetBtn);

        balanceLabel = new JLabel("Balance = ₹" + (long) state.balance);
        balanceLabel.setForeground(new Color(0, 220, 120));
        balanceLabel.setFont(new Font("Consolas", Font.BOLD, 20));

        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightTop.setOpaque(false);
        rightTop.add(balanceLabel);

        topBar.add(leftTop, BorderLayout.WEST);
        topBar.add(rightTop, BorderLayout.EAST);
        mainFrame.add(topBar, BorderLayout.NORTH);

        // CENTER: stock list with header
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(new Color(24,24,24));
        center.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        JPanel header = new JPanel(null);
        header.setPreferredSize(new Dimension(800, 44));
        header.setBackground(new Color(15,15,15));
        header.setBorder(BorderFactory.createMatteBorder(0,0,2,0, Color.DARK_GRAY));

        JLabel Lstock = new JLabel("STOCK");
        Lstock.setBounds(20, 8, 150, 28);
        Lstock.setForeground(new Color(255, 200, 0));
        Lstock.setFont(new Font("Arial Black", Font.BOLD, 14));

        JLabel Lprice = new JLabel("PRICE");
        Lprice.setBounds(220, 8, 100, 28);
        Lprice.setForeground(new Color(0,200,255));
        Lprice.setFont(new Font("Arial Black", Font.BOLD, 14));

        JLabel Lbuy = new JLabel("BUY");
        Lbuy.setBounds(380, 8, 80, 28);
        Lbuy.setForeground(new Color(100,255,100));
        Lbuy.setFont(new Font("Arial Black", Font.BOLD, 14));

        JLabel Lsell = new JLabel("SELL");
        Lsell.setBounds(480, 8, 80, 28);
        Lsell.setForeground(new Color(255,120,120));
        Lsell.setFont(new Font("Arial Black", Font.BOLD, 14));

        header.add(Lstock);
        header.add(Lprice);
        header.add(Lbuy);
        header.add(Lsell);

        center.add(header, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new GridLayout(0,1,0,6));
        listPanel.setBackground(new Color(28,28,28));

        boolean light = true;
        for (Stock s : state.stocks.values()) {
            listPanel.add(makeStockRowUI(s, light));
            light = !light;
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        center.add(scroll, BorderLayout.CENTER);

        mainFrame.add(center, BorderLayout.CENTER);
        mainFrame.setVisible(true);
    }

    private void setMenuItemIcon(JMenuItem menuItem, String iconPath) {
        try {
            java.net.URL iconUrl = getClass().getResource(iconPath);
            if (iconUrl != null) {
                ImageIcon icon = new ImageIcon(iconUrl);
                Image scaledImage = icon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
                menuItem.setIcon(new ImageIcon(scaledImage));
            }
        } catch (Exception ignored) {}
    }

    private JPanel makeStockRowUI(Stock s, boolean lightRow) {
        JPanel row = new JPanel(null);
        row.setPreferredSize(new Dimension(780, 44));
        row.setBackground(lightRow ? new Color(44,44,44) : new Color(36,36,36));
        row.setBorder(BorderFactory.createLineBorder(new Color(60,60,60)));

        JButton nameBtn = new JButton(s.name);
        nameBtn.setBounds(12, 6, 170, 32);
        nameBtn.setBackground(randomNiceColorFor(s.name));
        nameBtn.setForeground(Color.BLACK);
        nameBtn.setFont(new Font("Verdana", Font.BOLD, 13));
        nameBtn.addActionListener(e -> openInsightsWindow(s.name));
        row.add(nameBtn);

        JButton priceBtn = new JButton(String.format("₹%.0f", s.price));
        priceBtn.setBounds(210, 6, 120, 32);
        priceBtn.setBackground(new Color(240,240,240));
        priceBtn.setForeground(Color.BLACK);
        priceBtn.setFont(new Font("Verdana", Font.PLAIN, 13));
        updatePriceButtonColor(priceBtn, s);
        priceButtons.put(s.name, priceBtn);
        row.add(priceBtn);

        JButton buyBtn = new JButton("Buy");
        buyBtn.setBounds(360, 6, 80, 32);
        buyBtn.setBackground(new Color(34,139,34));
        buyBtn.setForeground(Color.WHITE);
        buyBtn.setFont(new Font("Verdana", Font.BOLD, 12));
        buyBtn.addActionListener(e -> onBuySell(s.name, "BUY"));
        row.add(buyBtn);

        JButton sellBtn = new JButton("Sell");
        sellBtn.setBounds(460, 6, 80, 32);
        sellBtn.setBackground(new Color(178,34,34));
        sellBtn.setForeground(Color.WHITE);
        sellBtn.setFont(new Font("Verdana", Font.BOLD, 12));
        sellBtn.addActionListener(e -> onBuySell(s.name, "SELL"));
        row.add(sellBtn);

        // momentum badge
        JLabel mom = new JLabel();
        mom.setBounds(560, 10, 120, 24);
        mom.setFont(new Font("Consolas", Font.PLAIN, 12));
        momentumBadges.put(s.name, mom);
        updateMomentumBadge(s, mom);
        row.add(mom);

        // tooltip
        row.setToolTipText(makeTooltipFromHistory(s));

        // right-click menu
        JPopupMenu popup = new JPopupMenu();
        JMenuItem miBuy = new JMenuItem("Quick Buy");
        JMenuItem miSell = new JMenuItem("Quick Sell");
        JMenuItem miInsights = new JMenuItem("Show Insights");

        miBuy.addActionListener(e -> onBuySell(s.name, "BUY"));
        miSell.addActionListener(e -> onBuySell(s.name, "SELL"));
        miInsights.addActionListener(e -> openInsightsWindow(s.name));

        popup.add(miBuy);
        popup.add(miSell);
        popup.addSeparator();
        popup.add(miInsights);

        row.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e))
                    popup.show(row, e.getX(), e.getY());
            }
            public void mouseEntered(MouseEvent e) {
                row.setBackground(new Color(70,70,70));
            }
            public void mouseExited(MouseEvent e) {
                row.setBackground(lightRow ? new Color(44,44,44) : new Color(36,36,36));
            }
        });

        return row;
    }

    private String makeTooltipFromHistory(Stock s) {
        StringBuilder sb = new StringBuilder("<html>Last prices:<br>");
        int start = Math.max(0, s.history.size()-6);
        for (int i = start; i < s.history.size(); i++) {
            sb.append("₹").append(String.format("%.0f", s.history.get(i)));
            if (i < s.history.size()-1) sb.append(", ");
        }
        sb.append("</html>");
        return sb.toString();
    }

    private void updateMomentumBadge(Stock s, JLabel badge) {
        double m = s.shortMomentum();
        if (m > 0.01) {
            badge.setText("↑ Momentum +" + String.format("%.2f%%", m*100));
            badge.setForeground(new Color(34,139,34));
        } else if (m < -0.01) {
            badge.setText("↓ Momentum " + String.format("%.2f%%", m*100));
            badge.setForeground(new Color(178,34,34));
        } else {
            badge.setText("Momentum ~0");
            badge.setForeground(new Color(200,200,200));
        }
    }

    private void onBuySell(String stockName, String type) {
        Stock s = state.stocks.get(stockName);
        if (s == null) return;

        String qtyStr = JOptionPane.showInputDialog(mainFrame,
                "Enter quantity to " + type + " of " + stockName + ":",
                "Quantity", JOptionPane.PLAIN_MESSAGE);
        if (qtyStr == null) return;

        int qty;
        try {
            qty = Integer.parseInt(qtyStr.trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame, "Please enter a valid integer quantity.");
            return;
        }

        if (qty <= 0) {
            JOptionPane.showMessageDialog(mainFrame, "Quantity must be > 0.");
            return;
        }

        double total = qty * s.price;

        if ("BUY".equals(type)) {
            if (state.balance < total) {
                JOptionPane.showMessageDialog(mainFrame, "Insufficient balance.");
                return;
            }
            state.balance -= total;
            state.portfolio.put(stockName, state.portfolio.getOrDefault(stockName, 0) + qty);
        } else {
            int have = state.portfolio.getOrDefault(stockName, 0);
            if (have < qty) {
                JOptionPane.showMessageDialog(mainFrame, "Not enough shares to sell.");
                return;
            }
            state.portfolio.put(stockName, have - qty);
            double pnl = (s.price - s.prevClose) * qty;
            state.dailyPnL += pnl;
            state.balance += total;
        }

        Transaction t = new Transaction(stockName, type, qty, s.price);
        state.todayTransactions.add(t);
        state.allTransactions.add(t);

        balanceLabel.setText("Balance = ₹" + (long) state.balance);
        saveState();
    }

    private void startTimer() {
        long now = System.currentTimeMillis();
        long delay = Math.max(0, DAY_MILLIS - (now - state.lastTickTime));
        dayTimer = new java.util.Timer();
        dayTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> tickDay());
            }
        }, delay, DAY_MILLIS);
    }

    private void tickDay() {
        state.dayIndex++;
        state.lastTickTime = System.currentTimeMillis();
        Random rnd = new Random();

        for (Stock s : state.stocks.values()) {
            s.prevClose = s.price;
            double changePct = (rnd.nextDouble() * 20.0) - 10.0; // ±10%
            double newPrice = Math.max(1.0, Math.round(s.price * (1 + changePct/100.0)));
            s.price = newPrice;
            s.pushPrice(s.price);

            JButton pb = priceButtons.get(s.name);
            if (pb != null) {
                pb.setText(String.format("₹%.0f", s.price));
                updatePriceButtonColor(pb, s);
            }

            JLabel mb = momentumBadges.get(s.name);
            if (mb != null) updateMomentumBadge(s, mb);
        }

        state.todayTransactions.clear();
        state.dailyPnL = 0.0;
        saveState();
    }

    private void startLiveFluctuations() {
        liveFluctTimer = new java.util.Timer();
        liveFluctTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    Random rnd = new Random();
                    double changeProb = 0.4;
                    for (Stock s : state.stocks.values()) {
                        if (rnd.nextDouble() <= changeProb) {
                            double changePct = (rnd.nextDouble() * 2.0) - 1.0;
                            double newPrice = Math.max(1.0, Math.round(s.price * (1 + changePct/100.0)));
                            s.price = newPrice;
                            s.pushPrice(s.price);

                            JButton pb = priceButtons.get(s.name);
                            if (pb != null) {
                                pb.setText(String.format("₹%.0f", s.price));
                                updatePriceButtonColor(pb, s);
                            }

                            JLabel mb = momentumBadges.get(s.name);
                            if (mb != null) updateMomentumBadge(s, mb);
                        }
                    }
                });
            }
        }, 0, 1000);
    }

    private void updatePriceButtonColor(JButton btn, Stock s) {
        if (s.price > s.prevClose) {
            btn.setForeground(Color.WHITE);
            btn.setBackground(new Color(34,139,34));
        } else if (s.price < s.prevClose) {
            btn.setForeground(Color.WHITE);
            btn.setBackground(new Color(178,34,34));
        } else {
            btn.setForeground(Color.BLACK);
            btn.setBackground(new Color(200,200,200));
        }
    }

    private void openPortfolioWindow() {
        JFrame f = new JFrame("Portfolio");
        setFrameIcon(f, ICON_PORTFOLIO);
        f.setSize(520, 360);
        f.setLocationRelativeTo(mainFrame);

        String[] cols = {"Stock", "Quantity"};
        DefaultTableModel tm = new DefaultTableModel(cols,0);
        for (Map.Entry<String,Integer> e : state.portfolio.entrySet())
            tm.addRow(new Object[]{e.getKey(), e.getValue()});

        JTable table = new JTable(tm);
        f.add(new JScrollPane(table));
        f.setVisible(true);
    }

    private void openTransactionsWindow() {
        JFrame f = new JFrame("Transactions");
        setFrameIcon(f, ICON_TRANSACTIONS);
        f.setSize(700, 420);
        f.setLocationRelativeTo(mainFrame);

        String[] cols = {"Time","Stock","Type","Qty","Price"};
        DefaultTableModel tm = new DefaultTableModel(cols, 0);
        for (Transaction t : state.allTransactions)
            tm.addRow(new Object[]{t.time, t.stock, t.type, t.qty, (long)t.price});

        JTable table = new JTable(tm);
        f.add(new JScrollPane(table));
        f.setVisible(true);
    }

    private void openDailyPnLWindow() {
        JFrame f = new JFrame("Daily PnL");
        setFrameIcon(f, ICON_DAILY);
        f.setSize(360,200);
        f.setLocationRelativeTo(mainFrame);

        JLabel lbl = new JLabel("Daily PnL: ₹" + String.format("%.2f", state.dailyPnL), SwingConstants.CENTER);
        lbl.setFont(new Font("Consolas", Font.BOLD, 18));
        f.add(lbl);
        f.setVisible(true);
    }

    private void openMainChartWindow() {
        JFrame f = new JFrame("Main Chart");
        setFrameIcon(f, ICON_MAIN);
        f.setSize(800,400);
        f.setLocationRelativeTo(mainFrame);

        JPanel p = new JPanel(new GridLayout(2,3,6,6));
        p.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        int i=0;
        for (Stock s : state.stocks.values()) {
            if (i++>5) break;
            p.add(makeSparklinePanel(s));
        }

        f.add(new JScrollPane(p));
        f.setVisible(true);
    }

    private void openInsightsWindow(String stockName) {
        JFrame f = new JFrame("Insights" + (stockName != null ? " - "+stockName : ""));
        setFrameIcon(f, ICON_INSIGHTS);
        f.setSize(420, 360);
        f.setLocationRelativeTo(mainFrame);

        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setFont(new Font("SansSerif", Font.PLAIN, 13));

        if (stockName == null) {
            ta.setText("Insights: Click a stock button for details. \n\nThis window also provides a small price sparkline and short momentum indicator.");
            f.add(new JScrollPane(ta));
            f.setVisible(true);
            return;
        }

        Stock s = state.stocks.get(stockName);
        StringBuilder sb = new StringBuilder();
        sb.append(stockName).append("\n");
        sb.append("Price: ₹").append(String.format("%.0f", s.price)).append("\n");
        sb.append("Prev Close: ₹").append(String.format("%.0f", s.prevClose)).append("\n");
        sb.append("Sector: ").append(guessSector(stockName)).append("\n\n");
        sb.append("Fundamentals (placeholder):\n- Revenue trend: positive\n- Debt: manageable\n- Notes: Example static data.\n\n");
        sb.append("Recent prices: ").append(s.history.toString()).append("\n");

        ta.setText(sb.toString());

        JPanel container = new JPanel(new BorderLayout(6,6));
        container.add(new JScrollPane(ta), BorderLayout.CENTER);
        container.add(makeSparklinePanel(s), BorderLayout.SOUTH);

        f.add(container);
        f.setVisible(true);
    }

    private JPanel makeSparklinePanel(Stock s) {
        JPanel p = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                List<Double> h = s.history;
                if (h.size() < 2) return;

                int w = getWidth()-10;
                int hgt = getHeight()-10;
                double min = Collections.min(h), max = Collections.max(h);
                if (max == min) max = min + 1;

                int n = h.size();
                int[] xs = new int[n], ys = new int[n];
                for (int i=0;i<n;i++) {
                    xs[i] = 5 + (int)((double)i/(n-1) * w);
                    ys[i] = 5 + (int)((1 - (h.get(i)-min)/(max-min)) * hgt);
                }

                Graphics2D g2 = (Graphics2D)g;
                g2.setStroke(new BasicStroke(2));
                g2.setColor(new Color(0,200,255));
                for (int i=0;i<n-1;i++)
                    g2.drawLine(xs[i], ys[i], xs[i+1], ys[i+1]);

                g2.setColor(new Color(120,120,120));
                g2.drawLine(5, hgt+5, w+5, hgt+5);
            }
            public Dimension getPreferredSize() {
                return new Dimension(380, 80);
            }
        };
        p.setBackground(new Color(20,20,20));
        p.setBorder(BorderFactory.createTitledBorder("Price Sparkline"));
        return p;
    }

    private String guessSector(String name) {
        name = name.toLowerCase();
        if (name.contains("bank") || name.contains("icici") || name.contains("hdfc")) return "Banking & Finance";
        if (name.contains("tech") || name.contains("tcs") || name.contains("infosys")) return "IT Services";
        if (name.contains("reliance") || name.contains("ongc") || name.contains("adani")) return "Energy / Infrastructure";
        if (name.contains("pharma") || name.contains("sun")) return "Healthcare / Pharma";
        return "Conglomerate";
    }

    private void setFrameIcon(JFrame f, String iconPath) {
        try {
            java.net.URL iconUrl = getClass().getResource(iconPath);
            if (iconUrl != null) {
                ImageIcon ic = new ImageIcon(iconUrl);
                f.setIconImage(ic.getImage());
            }
        } catch (Exception ignored) {}
    }

    private void saveState() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(STATE_FILE))) {
            out.writeObject(state);
            System.out.println("Saved state at day " + state.dayIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doResetWithUndo() {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(bout)) {
                oos.writeObject(state);
            }
            ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
            try (ObjectInputStream ois = new ObjectInputStream(bin)) {
                preResetSnapshot = (SimulatorState) ois.readObject();
            }
        } catch (Exception e) {
            preResetSnapshot = null;
        }

        performReset();

        final JDialog dlg = new JDialog(mainFrame, "Reset done", false);
        dlg.setLayout(new BorderLayout());
        JLabel msg = new JLabel("Market reset. Click UNDO within 6s to revert.", SwingConstants.CENTER);
        dlg.add(msg, BorderLayout.CENTER);

        JButton undo = new JButton("UNDO");
        dlg.add(undo, BorderLayout.SOUTH);
        undo.addActionListener(e -> {
            if (preResetSnapshot != null) {
                state = preResetSnapshot;
                saveState();
                preResetSnapshot = null;
                dlg.dispose();
                JOptionPane.showMessageDialog(mainFrame, "Reset undone.");
            }
        });

        dlg.setSize(360,120);
        dlg.setLocationRelativeTo(mainFrame);
        dlg.setVisible(true);

        new Timer().schedule(new TimerTask() {
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (dlg.isVisible()) dlg.dispose();
                });
            }
        }, 6000);
    }

    private void performReset() {
        if (dayTimer != null) dayTimer.cancel();
        if (liveFluctTimer != null) liveFluctTimer.cancel();

        for (Map.Entry<String, Stock> e : state.stocks.entrySet()) {
            String n = e.getKey();
            Stock s = e.getValue();
            Double ip = initialPrices.get(n);
            if (ip != null) {
                s.price = ip;
                s.prevClose = ip;
                s.history = new LinkedList<>();
                s.history.add(ip);
            }

            JButton pb = priceButtons.get(n);
            if (pb != null) {
                pb.setText(String.format("₹%.0f", s.price));
                pb.setBackground(new Color(200,200,200));
                pb.setForeground(Color.BLACK);
            }

            JLabel mb = momentumBadges.get(n);
            if (mb != null) updateMomentumBadge(s, mb);
        }

        state.portfolio.clear();
        state.todayTransactions.clear();
        state.allTransactions.clear();
        state.dailyPnL = 0.0;
        state.balance = 100000;
        state.dayIndex = 0;
        state.lastTickTime = System.currentTimeMillis();

        balanceLabel.setText("Balance = ₹" + (long)state.balance);
        saveState();
        startTimer();
        startLiveFluctuations();
    }

    private void shutdown() {
        if (dayTimer != null) dayTimer.cancel();
        if (liveFluctTimer != null) liveFluctTimer.cancel();
        saveState();
        System.exit(0);
    }

    private Color randomNiceColorFor(String name) {
        int h = Math.abs(name.hashCode());
        int r = 100 + (h % 120);
        int g = 60 + ((h / 3) % 120);
        int b = 80 + ((h / 7) % 120);
        return new Color(r,g,b);
    }
}