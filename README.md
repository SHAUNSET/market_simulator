Market Simulator – Intelligent Virtual Stock Trading Engine

A complete, end-to-end stock market simulation platform with live-like price movements, momentum-driven market behavior, portfolio tracking, daily resets, and a secure SQLite-powered login system.

Built entirely in Java (Swing + OOP + Serialization + SQLite) to give a real trading experience inside a clean desktop interface.




📌 Overview

Market Simulator is a desktop-based virtual trading environment designed to behave like a simplified real stock market.
It features:

Dynamic, momentum-based price fluctuations

Daily market cycles & resets

Portfolio valuation with P/L tracking

Buy/Sell execution engine

Persistent market state storage

SQLite-backed user authentication

Modular & scalable Java architecture

Users can log in, trade, track profits, view market momentum, and watch their portfolio evolve in real-time—all inside a smooth Java Swing UI.





📁 Project Structure

src/
│
├── app/
│   └── Main.java                       # Application entry point
│
├── data/
│   └── sim_state.dat                   # Serialized market state
│
├── db/
│   └── DBHelper.java                   # SQLite connection + user validation
│
├── models/
│   ├── Stock.java                      # Stock model: price, trend, volatility
│   ├── Portfolio.java                  # User holdings + valuation logic
│   └── SimulatorState.java             # Core persistent simulation state
│
├── services/
│   ├── MarketSimulator.java            # Price engine + buy/sell execution
│   └── PriceEngine.java                # Momentum-driven price updates
│
└── ui/
├── LoginFrame.java                 # Login UI
├── MainSimulatorLauncher.java      # Main dashboard launcher
└── simulator/
├── SimulatorDashboard.java     # Live simulation dashboard
└── components/                 # Custom Swing UI components





🏛️ System Architecture
Application Layers

Presentation Layer – Java Swing UI

Service Layer – MarketSimulator & PriceEngine

Domain Layer – Stock, Portfolio, SimulatorState

Persistence Layer – SQLite (users) + Serialization (market state)





🔀 System Flow (Text Diagram)

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






📈 Simulation Logic
The market engine uses a lightweight but realistic price model based on trend momentum, volatility, and noise.

✔ Key Behaviors💱 Buy/Sell Execution
When Buying

Balance decreases

Holdings increase

Transaction recorded

When Selling

Shares deducted

Profit/Loss realized

Balance updated

Portfolio Metrics

Total portfolio value

Unrealized P/L

Daily P/L

Available cash






🗄️ Data Persistence

SQLite Stores

User accounts

Passwords

Login validation

Serialization Stores

Market state

Prices

Positions

Daily reset data

This dual-system ensures the market behaves consistently across app launches.

Prices change every tick

Trend affects direction

Volatility controls magnitude

Gaussian noise introduces randomness

Momentum tag updates: Bullish / Bearish / Neutral

Price Update Formula
newPrice = oldPrice + (momentum * volatility) + randomNoise





🚀 How to Run on Any System

Install JDK 17+

Install IntelliJ IDEA / VS Code

Clone the repository:

git clone https://github.com/SHAUNSET/market_simulator.git


Open the project in your IDE

Build using Gradle or default compiler

Run:
src/app/Main.java





📘 Math Used in the Engine

Momentum = currentPrice – previousPrice

% Change = ((new – old) / old) × 100

Volatility = Gaussian random value

Portfolio Value = Σ (shares × currentPrice)

Daily P/L = todayValue – yesterdayValue

Simple, efficient math keeps the simulation realistic without heavy computation.





✨ Features
Momentum-based price simulation

Daily market reset logic

Full buy/sell trading system

Real-time portfolio tracking

SQLite login authentication

Trend indicators (Bullish/Bearish/Neutral)

Serialized simulation state

Clean Java Swing UI

Fully modular OOP architecture


