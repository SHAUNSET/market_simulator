package ui;

import model.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Final MainSimulatorLauncher
 * - Integrates with model.User, model.DBHelper, model.Stock, model.StockTransaction, model.SimulatorState
 * - Preserves look/feel, timers, buy/sell flow, reset-with-undo, persistence
 */
public class MainSimulatorLauncher {

    private static final long DAY_MILLIS = 60 * 60 * 1000L; // 1 hour (adjust for testing)
    private static final File STATE_FILE = new File("sim_state.dat");

    // Icon paths - adjust if your icons live elsewhere
    private static final String ICON_MAIN = "icons8-trading-80.png";
    private static final String ICON_PORTFOLIO = "icons8-portfolio-48.png";
    private static final String ICON_TRANSACTIONS = "icons8-transaction-50.png";
    private static final String ICON_DAILY = "icons8-daily-50.png";
    private static final String ICON_INSIGHTS = "icons8-combo-chart-50.png";

    // runtime state
    private final User currentUser;
    private SimulatorState state;
    private final Map<String, Double> initialPrices = new HashMap<>();
    private final Map<String, JButton> priceButtons = new HashMap<>();
    private final Map<String, JLabel> momentumBadges = new HashMap<>();

    private JFrame mainFrame;
    private JLabel balanceLabel;

    private java.util.Timer dayTimer;
    private java.util.Timer liveFluctTimer;

    // serialized snapshot for undo
    private byte[] preResetSnapshotBytes = null;

    public MainSimulatorLauncher(User user) {
        this.currentUser = user;
        loadOrCreateState();
        buildUI();
        startDayTimer();
        startLiveFluctuations();
    }

    // =====================================================================
    // REQUIRED ICON HELPERS (FRAME + BUTTON VERSION)
    // =====================================================================

    // Safe icon setter for buttons (JButton, AbstractButton)
    // For JButton, JToggleButton, JMenuItem, etc.
    private void safeSetIcon(AbstractButton btn, String path) {
        try {
            // Try multiple possible locations
            File iconFile = new File(path);
            if (!iconFile.exists()) {
                // Try without src/ prefix
                String altPath = path.replace("src/", "");
                iconFile = new File(altPath);
                if (!iconFile.exists()) {
                    // Try in current directory
                    altPath = path.substring(path.lastIndexOf("/") + 1);
                    iconFile = new File(altPath);
                }
            }
            if (iconFile.exists()) {
                ImageIcon icon = new ImageIcon(iconFile.getAbsolutePath());
                btn.setIcon(icon);
            }
        } catch (Exception ignored) {
            System.out.println("Could not load icon: " + path);
        }
    }

    // For JFrame windows (your Portfolio, Transactions, etc.)
    private void safeSetIcon(JFrame frame, String path) {
        try {
            File iconFile = new File(path);
            if (!iconFile.exists()) {
                String altPath = path.replace("src/", "");
                iconFile = new File(altPath);
            }
            if (iconFile.exists()) {
                ImageIcon icon = new ImageIcon(iconFile.getAbsolutePath());
                frame.setIconImage(icon.getImage());
            }
        } catch (Exception ignored) {
            System.out.println("Could not load frame icon: " + path);
        }
    }

    // -------------------- State load/create/save --------------------

    private void loadOrCreateState() {
        if (STATE_FILE.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(STATE_FILE))) {
                Object obj = in.readObject();
                if (obj instanceof SimulatorState) {
                    state = (SimulatorState) obj;
                    // Validate loaded state
                    if (state.getStocks() == null) {
                        createFreshState();
                    }
                } else {
                    createFreshState();
                }
            } catch (Exception e) {
                System.err.println("Error loading state: " + e.getMessage());
                createFreshState();
            }
        } else {
            createFreshState();
        }

        // remember initial prices
        if (state.getStocks() != null) {
            for (Stock s : state.getStocks().values()) {
                if (s != null) {
                    initialPrices.put(s.getSymbol(), s.getPrice());
                }
            }
        }
    }

    private void createFreshState() {
        state = new SimulatorState();
        // initialize balance from user if available
        try {
            state.setBalance(currentUser.getBalance());
        } catch (Exception ignored) { /* ignore if user has no balance */ }

        Map<String, Stock> stocks = new LinkedHashMap<>();
        stocks.put("Reliance", new Stock("Reliance", "RELIANCE", 2500));
        stocks.put("TCS", new Stock("TCS", "TCS", 3500));
        stocks.put("Infosys", new Stock("Infosys", "INFY", 1450));
        stocks.put("HDFC Bank", new Stock("HDFC Bank", "HDFCB", 1600));
        stocks.put("ICICI Bank", new Stock("ICICI Bank", "ICICIB", 970));
        stocks.put("Adani Ports", new Stock("Adani Ports", "ADANIP", 1200));
        stocks.put("Bajaj Finance", new Stock("Bajaj Finance", "BAJFIN", 7800));
        stocks.put("Wipro", new Stock("Wipro", "WIPRO", 400));
        stocks.put("ONGC", new Stock("ONGC", "ONGC", 210));
        stocks.put("Coal India", new Stock("Coal India", "COALIND", 285));
        stocks.put("Maruti", new Stock("Maruti", "MARUTI", 11000));
        stocks.put("Tata Motors", new Stock("Tata Motors", "TATAM", 875));
        stocks.put("NTPC", new Stock("NTPC", "NTPC", 310));
        stocks.put("Tech Mahindra", new Stock("Tech Mahindra", "TECHM", 1300));
        stocks.put("Sun Pharma", new Stock("Sun Pharma", "SUNP", 1250));
        state.setStocks(stocks);
    }

    private void saveState() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(STATE_FILE))) {
            out.writeObject(state);
            System.out.println("Saved state at day " + state.getDayIndex());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // -------------------- UI build --------------------

    private void buildUI() {
        mainFrame = new JFrame("Market Simulator - " + currentUser.getUsername());
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.setSize(900, 600);
        mainFrame.setLayout(new BorderLayout());
        mainFrame.setLocationRelativeTo(null);

        // main icon
        safeSetIcon(mainFrame, ICON_MAIN);

        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // stop timers, save, persist balance to DB, then exit
                stopTimers();
                saveState();
                if (currentUser != null) {
                    currentUser.setBalance(state.getBalance());
                    try {
                        DBHelper.updateUserBalance(currentUser);
                    } catch (Exception ex) {
                        // Log but don't show error to user on exit
                        System.err.println("Error updating user balance: " + ex.getMessage());
                    }
                }
                System.exit(0);
            }
        });

        // Menu bar
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(18,18,18));
        JMenuItem miMainChart = new JMenuItem("MAIN CHART");
        JMenuItem miPortfolio = new JMenuItem("PORTFOLIO");
        JMenuItem miTransactions = new JMenuItem("TRANSACTIONS");
        JMenuItem miDailyPnL = new JMenuItem("DAILY PNL");
        JMenuItem miInsights = new JMenuItem("INSIGHTS");

        safeSetIcon(miMainChart, ICON_MAIN);
        safeSetIcon(miPortfolio, ICON_PORTFOLIO);
        safeSetIcon(miTransactions, ICON_TRANSACTIONS);
        safeSetIcon(miDailyPnL, ICON_DAILY);
        safeSetIcon(miInsights, ICON_INSIGHTS);

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
        topBar.setBackground(new Color(12,12,12));
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

        // ctrl+r shortcut
        mainFrame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control R"), "resetNow");
        mainFrame.getRootPane().getActionMap().put("resetNow", new AbstractAction() {
            public void actionPerformed(ActionEvent e){ doResetWithUndo(); }
        });

        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftTop.setOpaque(false);
        leftTop.add(resetBtn);

        balanceLabel = new JLabel(String.format("Balance = ₹%.0f", state.getBalance()));
        balanceLabel.setForeground(new Color(0,220,120));
        balanceLabel.setFont(new Font("Consolas", Font.BOLD, 20));
        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightTop.setOpaque(false);
        rightTop.add(balanceLabel);

        topBar.add(leftTop, BorderLayout.WEST);
        topBar.add(rightTop, BorderLayout.EAST);
        mainFrame.add(topBar, BorderLayout.NORTH);

        // CENTER: stock list
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(new Color(24,24,24));
        center.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        JPanel header = new JPanel(null);
        header.setPreferredSize(new Dimension(800,44));
        header.setBackground(new Color(15,15,15));
        header.setBorder(BorderFactory.createMatteBorder(0,0,2,0, Color.DARK_GRAY));

        JLabel Lstock = new JLabel("STOCK"); Lstock.setBounds(20,8,150,28);
        Lstock.setForeground(new Color(255,200,0)); Lstock.setFont(new Font("Arial Black", Font.BOLD,14));
        JLabel Lprice = new JLabel("PRICE"); Lprice.setBounds(220,8,100,28);
        Lprice.setForeground(new Color(0,200,255)); Lprice.setFont(new Font("Arial Black", Font.BOLD,14));
        JLabel Lbuy = new JLabel("BUY"); Lbuy.setBounds(380,8,80,28);
        Lbuy.setForeground(new Color(100,255,100)); Lbuy.setFont(new Font("Arial Black", Font.BOLD,14));
        JLabel Lsell = new JLabel("SELL"); Lsell.setBounds(480,8,80,28);
        Lsell.setForeground(new Color(255,120,120)); Lsell.setFont(new Font("Arial Black", Font.BOLD,14));

        header.add(Lstock); header.add(Lprice); header.add(Lbuy); header.add(Lsell);
        center.add(header, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new GridLayout(0,1,0,6));
        listPanel.setBackground(new Color(28,28,28));
        boolean light = true;
        if (state.getStocks() != null) {
            for (Stock s : state.getStocks().values()) {
                if (s != null) {
                    listPanel.add(makeStockRowUI(s, light));
                    light = !light;
                }
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        center.add(scroll, BorderLayout.CENTER);

        mainFrame.add(center, BorderLayout.CENTER);
        mainFrame.setVisible(true);
    }

    // Creates the stock row UI (keeps original look/logic)
    private JPanel makeStockRowUI(Stock s, boolean lightRow) {
        JPanel row = new JPanel(null);
        row.setPreferredSize(new Dimension(780, 44));
        row.setBackground(lightRow ? new Color(44,44,44) : new Color(36,36,36));
        row.setBorder(BorderFactory.createLineBorder(new Color(60,60,60)));

        JButton nameBtn = new JButton(s.getName());
        nameBtn.setBounds(12, 6, 170, 32);
        nameBtn.setBackground(randomNiceColorFor(s.getSymbol()));
        nameBtn.setForeground(Color.BLACK);
        nameBtn.setFont(new Font("Verdana", Font.BOLD, 13));
        nameBtn.addActionListener(e -> openInsightsWindow(s.getSymbol()));
        row.add(nameBtn);

        JButton priceBtn = new JButton(String.format("₹%.0f", s.getPrice()));
        priceBtn.setBounds(210, 6, 120, 32);
        priceBtn.setBackground(new Color(240,240,240));
        priceBtn.setForeground(Color.BLACK);
        priceBtn.setFont(new Font("Verdana", Font.PLAIN, 13));
        updatePriceButtonColor(priceBtn, s);
        priceButtons.put(s.getSymbol(), priceBtn);
        row.add(priceBtn);

        JButton buyBtn = new JButton("Buy");
        buyBtn.setBounds(360, 6, 80, 32);
        buyBtn.setBackground(new Color(34,139,34));
        buyBtn.setForeground(Color.WHITE);
        buyBtn.setFont(new Font("Verdana", Font.BOLD, 12));
        buyBtn.addActionListener(e -> onBuySell(s.getSymbol(), "BUY"));
        row.add(buyBtn);

        JButton sellBtn = new JButton("Sell");
        sellBtn.setBounds(460, 6, 80, 32);
        sellBtn.setBackground(new Color(178,34,34));
        sellBtn.setForeground(Color.WHITE);
        sellBtn.setFont(new Font("Verdana", Font.BOLD, 12));
        sellBtn.addActionListener(e -> onBuySell(s.getSymbol(), "SELL"));
        row.add(sellBtn);

        JLabel mom = new JLabel();
        mom.setBounds(560, 10, 120, 24);
        mom.setFont(new Font("Consolas", Font.PLAIN, 12));
        momentumBadges.put(s.getSymbol(), mom);
        updateMomentumBadge(s, mom);
        row.add(mom);

        // tooltip: last N price points
        row.setToolTipText(makeTooltipFromHistory(s));

        // right-click quick menu
        JPopupMenu popup = new JPopupMenu();
        JMenuItem miBuy = new JMenuItem("Quick Buy");
        JMenuItem miSell = new JMenuItem("Quick Sell");
        JMenuItem miInsights = new JMenuItem("Show Insights");
        miBuy.addActionListener(e -> onBuySell(s.getSymbol(), "BUY"));
        miSell.addActionListener(e -> onBuySell(s.getSymbol(), "SELL"));
        miInsights.addActionListener(e -> openInsightsWindow(s.getSymbol()));
        popup.add(miBuy); popup.add(miSell); popup.addSeparator(); popup.add(miInsights);

        row.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) popup.show(row, e.getX(), e.getY());
            }
            public void mouseEntered(MouseEvent e) { row.setBackground(new Color(70,70,70)); }
            public void mouseExited(MouseEvent e) { row.setBackground(lightRow ? new Color(44,44,44) : new Color(36,36,36)); }
        });

        return row;
    }

    private void updatePriceButtonColor(JButton btn, Stock s) {
        if (btn == null || s == null) return;

        if (s.getPrice() > s.getPrevClose()) {
            btn.setForeground(Color.WHITE);
            btn.setBackground(new Color(34,139,34));
        } else if (s.getPrice() < s.getPrevClose()) {
            btn.setForeground(Color.WHITE);
            btn.setBackground(new Color(178,34,34));
        } else {
            btn.setForeground(Color.BLACK);
            btn.setBackground(new Color(200,200,200));
        }
    }

    // format tooltip from history
    private String makeTooltipFromHistory(Stock s) {
        if (s == null) return "<html>No data</html>";

        StringBuilder sb = new StringBuilder("<html>Last prices:<br>");
        List<Double> h = s.getHistory();
        if (h == null || h.isEmpty()) {
            sb.append("No history</html>");
            return sb.toString();
        }

        int start = Math.max(0, h.size() - 6);
        for (int i = start; i < h.size(); i++) {
            sb.append(String.format("₹%.0f", h.get(i)));
            if (i < h.size() - 1) sb.append(", ");
        }
        sb.append("</html>");
        return sb.toString();
    }

    private void updateMomentumBadge(Stock s, JLabel badge) {
        if (s == null || badge == null) return;

        double m = s.shortMomentum();
        if (m > 0.01) {
            badge.setText("\u2191 Momentum +" + String.format("%.2f%%", m*100));
            badge.setForeground(new Color(34,139,34));
        } else if (m < -0.01) {
            badge.setText("\u2193 Momentum " + String.format("%.2f%%", m*100));
            badge.setForeground(new Color(178,34,34));
        } else {
            badge.setText("Momentum ~0");
            badge.setForeground(new Color(200,200,200));
        }
    }

    private Color randomNiceColorFor(String name) {
        int h = Math.abs(name.hashCode());
        int r = 100 + (h % 120);
        int g = 60 + ((h / 3) % 120);
        int b = 80 + ((h / 7) % 120);
        return new Color(r,g,b);
    }

    // ---------------- Buy/Sell flow ----------------

    private void onBuySell(String symbol, String type) {
        Stock s = state.getStocks().get(symbol);
        if (s == null) {
            JOptionPane.showMessageDialog(mainFrame, "Stock not found: " + symbol);
            return;
        }

        String qtyStr = JOptionPane.showInputDialog(mainFrame, "Enter quantity to " + type + " of " + symbol + ":", "Quantity", JOptionPane.PLAIN_MESSAGE);
        if (qtyStr == null || qtyStr.trim().isEmpty()) return;

        int qty;
        try {
            qty = Integer.parseInt(qtyStr.trim());
            if (qty <= 0) {
                JOptionPane.showMessageDialog(mainFrame, "Quantity must be > 0.");
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(mainFrame, "Please enter a valid integer quantity.");
            return;
        }

        double total = qty * s.getPrice();
        if ("BUY".equals(type)) {
            if (state.getBalance() < total) {
                JOptionPane.showMessageDialog(mainFrame, "Insufficient balance.");
                return;
            }
            state.setBalance(state.getBalance() - total);
            state.getPortfolio().put(symbol, state.getPortfolio().getOrDefault(symbol, 0) + qty);
            StockTransaction t = new StockTransaction(symbol, "BUY", qty, s.getPrice());
            state.getTodayTransactions().add(t);
            state.getAllTransactions().add(t);
        } else {
            int have = state.getPortfolio().getOrDefault(symbol, 0);
            if (have < qty) {
                JOptionPane.showMessageDialog(mainFrame, "Not enough shares to sell.");
                return;
            }
            state.getPortfolio().put(symbol, have - qty);
            double pnl = (s.getPrice() - s.getPrevClose()) * qty;
            state.setDailyPnL(state.getDailyPnL() + pnl);
            state.setBalance(state.getBalance() + total);
            StockTransaction t = new StockTransaction(symbol, "SELL", qty, s.getPrice());
            state.getTodayTransactions().add(t);
            state.getAllTransactions().add(t);
        }

        balanceLabel.setText(String.format("Balance = ₹%.0f", state.getBalance()));
        JButton pb = priceButtons.get(symbol);
        if (pb != null) {
            pb.setText(String.format("₹%.0f", s.getPrice()));
            updatePriceButtonColor(pb, s);
        }
        JLabel mb = momentumBadges.get(symbol);
        if (mb != null) updateMomentumBadge(s, mb);

        // persist
        saveState();
    }

    // ---------------- Timers ----------------

    private void startDayTimer() {
        long now = System.currentTimeMillis();
        long delay = Math.max(0, DAY_MILLIS - (now - state.getLastTickTime()));
        dayTimer = new java.util.Timer();
        dayTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override public void run() { SwingUtilities.invokeLater(() -> tickDay()); }
        }, delay, DAY_MILLIS);
    }

    private void tickDay() {
        state.setDayIndex(state.getDayIndex() + 1);
        state.setLastTickTime(System.currentTimeMillis());
        Random rnd = new Random();
        for (Stock s : state.getStocks().values()) {
            if (s != null) {
                s.setPrevClose(s.getPrice());
                double changePct = (rnd.nextDouble() * 20.0) - 10.0; // ±10%
                double newPrice = Math.max(1.0, Math.round(s.getPrice() * (1 + changePct / 100.0)));
                s.setPrice(newPrice);
                JButton pb = priceButtons.get(s.getSymbol());
                if (pb != null) {
                    pb.setText(String.format("₹%.0f", s.getPrice()));
                    updatePriceButtonColor(pb, s);
                }
                JLabel mb = momentumBadges.get(s.getSymbol());
                if (mb != null) updateMomentumBadge(s, mb);
            }
        }
        // end of day housekeeping
        state.getTodayTransactions().clear();
        state.setDailyPnL(0.0);
        saveState();
    }

    private void startLiveFluctuations() {
        liveFluctTimer = new java.util.Timer();
        liveFluctTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override public void run() { SwingUtilities.invokeLater(() -> {
                Random rnd = new Random();
                double changeProb = 0.4; // 40% chance per tick
                for (Stock s : state.getStocks().values()) {
                    if (s != null && rnd.nextDouble() <= changeProb) {
                        double changePct = (rnd.nextDouble() * 2.0) - 1.0; // ±1%
                        double newPrice = Math.max(1.0, Math.round(s.getPrice() * (1 + changePct / 100.0)));
                        s.setPrice(newPrice);
                        JButton pb = priceButtons.get(s.getSymbol());
                        if (pb != null) {
                            pb.setText(String.format("₹%.0f", s.getPrice()));
                            updatePriceButtonColor(pb, s);
                        }
                        JLabel mb = momentumBadges.get(s.getSymbol());
                        if (mb != null) updateMomentumBadge(s, mb);
                    }
                }
            }); }
        }, 0, 1000); // every 1s
    }

    // ---------------- Reset with undo ----------------

    private void doResetWithUndo() {
        // snapshot via serialization
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(bout)) {
                oos.writeObject(state);
            }
            preResetSnapshotBytes = bout.toByteArray();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Could not create reset snapshot: " + e.getMessage());
            preResetSnapshotBytes = null;
        }

        // perform reset
        performReset();

        // show undo dialog for 6 seconds
        final JDialog dlg = new JDialog(mainFrame, "Reset done", false);
        dlg.setLayout(new BorderLayout());
        JLabel msg = new JLabel("Market reset. Click UNDO within 6s to revert.", SwingConstants.CENTER);
        dlg.add(msg, BorderLayout.CENTER);
        JButton undo = new JButton("UNDO");
        dlg.add(undo, BorderLayout.SOUTH);

        undo.addActionListener(e -> {
            if (preResetSnapshotBytes != null) {
                try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(preResetSnapshotBytes))) {
                    state = (SimulatorState) ois.readObject();
                    // refresh UI labels/buttons
                    balanceLabel.setText(String.format("Balance = ₹%.0f", state.getBalance()));
                    for (Stock s : state.getStocks().values()) {
                        if (s != null) {
                            JButton pb = priceButtons.get(s.getSymbol());
                            if (pb != null) pb.setText(String.format("₹%.0f", s.getPrice()));
                            JLabel mb = momentumBadges.get(s.getSymbol());
                            if (mb != null) updateMomentumBadge(s, mb);
                        }
                    }
                    saveState();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    preResetSnapshotBytes = null;
                    dlg.dispose();
                    JOptionPane.showMessageDialog(mainFrame, "Reset undone.");
                }
            }
        });

        dlg.setSize(360,120);
        dlg.setLocationRelativeTo(mainFrame);
        dlg.setVisible(true);

        // auto close after 6s
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (dlg.isVisible()) dlg.dispose();
                });
            }
        }, 6000);
    }

    private void performReset() {
        stopTimers();

        // reset prices to initialPrices if available, else keep current
        for (Map.Entry<String, Stock> e : state.getStocks().entrySet()) {
            String key = e.getKey();
            Stock s = e.getValue();
            if (s != null) {
                Double ip = initialPrices.get(key);
                if (ip != null) {
                    s.setPrice(ip);
                    s.setPrevClose(ip);
                    // clear history and add ip
                    List<Double> hist = s.getHistory();
                    if (hist != null) {
                        hist.clear();
                        hist.add(ip);
                    }
                }
                JButton pb = priceButtons.get(key);
                if (pb != null) {
                    pb.setText(String.format("₹%.0f", s.getPrice()));
                    pb.setBackground(new Color(200,200,200));
                    pb.setForeground(Color.BLACK);
                }
                JLabel mb = momentumBadges.get(key);
                if (mb != null) updateMomentumBadge(s, mb);
            }
        }

        state.getPortfolio().clear();
        state.getTodayTransactions().clear();
        state.getAllTransactions().clear();
        state.setDailyPnL(0.0);
        state.setBalance(100000);
        state.setDayIndex(0);
        state.setLastTickTime(System.currentTimeMillis());
        balanceLabel.setText(String.format("Balance = ₹%.0f", state.getBalance()));
        saveState();

        startDayTimer();
        startLiveFluctuations();
    }

    private void stopTimers() {
        if (dayTimer != null) {
            dayTimer.cancel();
            dayTimer.purge();
            dayTimer = null;
        }
        if (liveFluctTimer != null) {
            liveFluctTimer.cancel();
            liveFluctTimer.purge();
            liveFluctTimer = null;
        }
    }

    // ----------------- Placeholder windows (real ones included) -----------------

    private void openPortfolioWindow() {
        JFrame f = new JFrame("Portfolio");
        safeSetIcon(f, ICON_PORTFOLIO);
        f.setSize(520, 360);
        f.setLocationRelativeTo(mainFrame);
        String[] cols = {"Stock", "Quantity"};
        DefaultTableModel tm = new DefaultTableModel(cols,0);
        if (state.getPortfolio() != null) {
            for (Map.Entry<String,Integer> e : state.getPortfolio().entrySet()) {
                tm.addRow(new Object[]{e.getKey(), e.getValue()});
            }
        }
        JTable table = new JTable(tm);
        f.add(new JScrollPane(table));
        f.setVisible(true);
    }

    private void openTransactionsWindow() {
        JFrame f = new JFrame("Transactions");
        safeSetIcon(f, ICON_TRANSACTIONS);
        f.setSize(700, 420);
        f.setLocationRelativeTo(mainFrame);
        String[] cols = {"Time","Stock","Type","Qty","Price"};
        DefaultTableModel tm = new DefaultTableModel(cols, 0);
        if (state.getAllTransactions() != null) {
            for (StockTransaction t : state.getAllTransactions()) {
                if (t != null) {
                    tm.addRow(new Object[]{
                            t.getTimestamp(),
                            t.getStockName(),
                            t.getType(),
                            t.getQuantity(),
                            String.format("₹%.0f", t.getPrice())
                    });
                }
            }
        }
        JTable table = new JTable(tm);
        f.add(new JScrollPane(table));
        f.setVisible(true);
    }

    private void openDailyPnLWindow() {
        JFrame f = new JFrame("Daily PnL");
        safeSetIcon(f, ICON_DAILY);
        f.setSize(360,200);
        f.setLocationRelativeTo(mainFrame);
        JLabel lbl = new JLabel("Daily PnL: ₹" + String.format("%.2f", state.getDailyPnL()), SwingConstants.CENTER);
        lbl.setFont(new Font("Consolas", Font.BOLD, 18));
        f.add(lbl);
        f.setVisible(true);
    }

    private void openMainChartWindow() {
        JFrame f = new JFrame("Main Chart");
        safeSetIcon(f, ICON_MAIN);
        f.setSize(800,400);
        f.setLocationRelativeTo(mainFrame);
        JPanel p = new JPanel(new GridLayout(2,3,6,6));
        p.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        int i=0;
        for (Stock s : state.getStocks().values()) {
            if (s != null && i++>5) break;
            p.add(makeSparklinePanel(s));
        }
        f.add(new JScrollPane(p));
        f.setVisible(true);
    }

    private void openInsightsWindow(String stockSymbol) {
        JFrame f = new JFrame("Insights" + (stockSymbol!=null ? " - " + stockSymbol : ""));
        safeSetIcon(f, ICON_INSIGHTS);
        f.setSize(420,360);
        f.setLocationRelativeTo(mainFrame);

        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setFont(new Font("SansSerif", Font.PLAIN, 13));

        if (stockSymbol == null) {
            ta.setText("Insights: Click a stock button for details.\n\nThis window also provides a small price sparkline and short momentum indicator.");
            f.add(new JScrollPane(ta));
            f.setVisible(true);
            return;
        }

        Stock s = state.getStocks().get(stockSymbol);
        if (s == null) {
            ta.setText("Stock not found.");
            f.add(new JScrollPane(ta));
            f.setVisible(true);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(stockSymbol).append("\n");
        sb.append("Price: ").append(String.format("₹%.0f", s.getPrice())).append("\n");
        sb.append("Prev Close: ").append(String.format("₹%.0f", s.getPrevClose())).append("\n");
        sb.append("Sector: ").append(guessSector(stockSymbol)).append("\n\n");
        sb.append("Fundamentals (placeholder):\n- Revenue trend: positive\n- Debt: manageable\n- Notes: Example static data.\n\n");
        sb.append("Recent prices: ").append(s.getHistory() != null ? s.getHistory().toString() : "No history").append("\n");
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
                if (s == null) return;
                List<Double> h = s.getHistory();
                if (h == null || h.size() < 2) return;
                int w = getWidth()-10;
                int hgt = getHeight()-10;
                double min = Collections.min(h);
                double max = Collections.max(h);
                if (max == min) max = min + 1;
                int n = h.size();
                int[] xs = new int[n], ys = new int[n];
                for (int i=0;i<n;i++) {
                    xs[i] = 5 + (int)((double)i/(n-1) * w);
                    ys[i] = 5 + (int)((1 - (h.get(i)-min)/(max-min)) * hgt);
                }
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(2));
                g2.setColor(new Color(0,200,255));
                for (int i=0;i<n-1;i++) g2.drawLine(xs[i], ys[i], xs[i+1], ys[i+1]);
                g2.setColor(new Color(120,120,120));
                g2.drawLine(5, hgt+5, w+5, hgt+5);
            }
            public Dimension getPreferredSize() { return new Dimension(380,80); }
        };
        p.setBackground(new Color(20,20,20));
        p.setBorder(BorderFactory.createTitledBorder("Price Sparkline"));
        return p;
    }

    private String guessSector(String name) {
        if (name == null) return "Unknown";
        name = name.toLowerCase();
        if (name.contains("bank") || name.contains("icici") || name.contains("hdfc")) return "Banking & Finance";
        if (name.contains("tech") || name.contains("tcs") || name.contains("infosys")) return "IT Services";
        if (name.contains("reliance") || name.contains("ongc") || name.contains("adani")) return "Energy / Infrastructure";
        if (name.contains("pharma") || name.contains("sun")) return "Healthcare / Pharma";
        return "Conglomerate";
    }

} // end MainSimulatorLauncher