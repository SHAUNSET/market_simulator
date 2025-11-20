Market Simulator – Intelligent Virtual Stock Trading Engine



A complete end-to-end stock market simulation platform with live-like price movements, portfolio tracking, daily resets, and a full login system powered by SQLite.



------------------------------------------------------------------------------------

Overview

The Market Simulator is a desktop-based virtual trading platform designed to mimic a real stock market environment using:

• Dynamic price fluctuations

• Daily market cycles

• Portfolio tracking

• Buy/Sell execution system

• State persistence

• SQLite-backed authentication

• Clean, modular Java architecture

Users can log in, trade stocks, track profits, view momentum status, and watch how their portfolio evolves—everything inside a smooth, modern Java Swing UI.

Built fully in Java using Swing, OOP, serialization, and SQLite.



------------------------------------------------------------------------------------



Project Structure

src/

│

├── app/

│   └── Main.java – Application entry point

│

├── data/

│   └── sim_state.dat – Serialized market state

│

├── db/

│   └── DBHelper.java – SQLite connection and user validation

│

├── models/

│   ├── Stock.java – Represents stock with price, trend, etc.

│   ├── Portfolio.java – User holdings and valuation

│   ├── SimulatorState.java – Core persistent simulation state

│

├── services/

│   ├── MarketSimulator.java – Price engine, buy/sell logic

│   └── PriceEngine.java – Momentum-based price updates

│

└── ui/

    ├── LoginFrame.java – Login UI

    ├── MainSimulatorLauncher.java – Main dashboard

    ├── simulator/

    │       ├── SimulatorDashboard.java – Live data UI

    │       └── components...

------------------------------------------------------------------------------------



System Architecture

Application Layers:

1.       Presentation Layer → Java Swing UI  

2.      Service Layer → MarketSimulator + PriceEngine  

3.      Domain Layer → Stock, Portfolio, SimulatorState  

4.      Persistence Layer → SQLite (User Data) + Serialization (Market Data)



Flow Diagram (text-based):



[User Login]

        ↓  

[SQLite → Validate Credentials]

        ↓  

[Load sim_state.dat]

        ↓  

[Main Dashboard]

        ↓  

[Market Engine Updates Prices Each Tick]

        ↓  

[User Executes Buy/Sell]

        ↓  

[Portfolio Updates]

        ↓  

[State Saved Back to sim_state.dat]



------------------------------------------------------------------------------------



Simulation Logic (How Market Works)

✓ Prices fluctuate every tick

✓ Trend and volatility determine next price

✓ Randomness is injected via Gaussian noise

✓ Momentum badge updates (Bullish / Bearish / Neutral)



📌 Example price update formula:



newPrice = oldPrice + (momentum * volatility) + randomNoise



------------------------------------------------------------------------------------



Buy/Sell Execution

·        When user buys:

- Balance decreases

- Holdings increase

- Transaction logged

·        When user sells:

- Shares decrease

- Profit/loss realized

- Balance updated



Portfolio metrics include:

• Total value

• Daily P/L

• Unrealized P/L

• Cash available



------------------------------------------------------------------------------------



Data Persistence

SQLite handles:

• User accounts

• Passwords

• Login verification



Serialization handles:

• Market state

• Prices

• Positions

• Daily reset info



------------------------------------------------------------------------------------



How to Run on Any System

1. Install JDK 17 or newer

2. Install IntelliJ / VS Code

3. Clone the project:

   git clone https://github.com/SHAUNSET/market_simulator.git

4. Open project in IDE

5. Build using Gradle or default compiler

6. Run app/Main.java



------------------------------------------------------------------------------------



Math Used

1.       Price Momentum = (Current Price – Previous Price)  

2.       % Change = ((new - old) / old) × 100  

3.       Volatility = Random Gaussian value  

4.       Portfolio Value = Σ (shares × current price)  

5.       Daily P/L = today's value – yesterday's closing value  

These are intentionally lightweight so the simulation feels real without heavy computation.



------------------------------------------------------------------------------------



Features

✓ Realistic price engine

✓ Daily market reset

✓ Portfolio tracking

✓ SQLite login system

✓ Trend indicators

✓ Fully object-oriented

✓ Serializable state

✓ Java Swing modern UI



------------------------------------------------------------------------------------



Possible Future Extensions



• Live NSE/US stock market API

• Charting using JFreeChart

• Options & Futures simulation

• Multi-user leaderboard

• Strategy backtesting

• KPIs, Sharpe ratio, risk metrics

• Mobile app version



------------------------------------------------------------------------------------



Summary

This Market Simulator is a fully functional virtual stock market environment built for learning, experimenting, and building trading logic — all in Java. Clean architecture + real finance-style behavior makes it ideal for portfolios, resumes, and real-world Java practice.
