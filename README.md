
# 🚀Market Simulator  

A desktop‑based virtual stock‑market simulation platform with realistic price movements, buy/sell execution, portfolio tracking and user login — built in Java + Swing + SQLite.

## Overview  
Market Simulator is designed to replicate a simplified yet realistic stock‑market environment on your local machine.  
It lets users: log in, simulate real‑time price changes based on momentum & volatility, place buy/sell orders, track portfolio value and P/L, and persist market state for continuity between sessions.  
Great for testing trading ideas, learning how markets respond, or as a sandbox for algorithmic strategies — without risking real money.










## Features  

- Momentum‑based dynamic price updates (trend + volatility + randomness)  
- Daily market cycles and resets  
- Real-time portfolio valuation and profit/loss tracking  
- Buy/Sell trade execution engine  
- User authentication & login (SQLite‑backed)  
- Persistent simulation state (serialized + DB)  
- Clean desktop UI built with Java Swing  
- Modular OOP architecture for easy extension  


##  Project Structure

```bash
src/  
 ├── app/            # Entry point (Main.java)  
 ├── data/           # Serialized market state / data files  
 ├── db/             # SQLite connection & user auth  
 ├── models/         # Domain models: Stock, Portfolio, SimulatorState, etc.  
 ├── services/       # Core logic: MarketSimulator, PriceEngine, trade exec, etc.  
 └── ui/             # GUI components: login screen, dashboard, trading UI, etc.  


Plus:  
- `.gitignore`  
- `README.md`  
- State/data files: sim_state.dat, market_sim_state.dat, etc.  

```


## How It Works (System Architecture) 

1. User logs in via SQLite‑based authentication.  
2. On successful login, the app loads the last saved simulation state (prices, holdings, history).  
3. The price engine runs in ticks (or cycles), updating stock prices using a simple formula:  
   `newPrice = oldPrice + (momentum * volatility) + randomNoise` — combining trend, volatility & randomness.  
4. Users can place buy/sell orders via the UI; the engine updates holdings, cash balance, transaction history.  
5. Portfolio value, daily/unrealized P&L, cash, holdings are updated in real‑time and reflected in the UI.  
6. On exit or periodic save, the new market and portfolio state is serialized (and DB updated), so next login continues where you left off.  
This keeps simulation persistent and consistent across sessions.


## Installation & Setup  

1. Ensure you have **JDK 17+** installed.  

2. Clone the repository:  
```bash
git clone https://github.com/SHAUNSET/market_simulator.git
```

3. Open the project in your preferred IDE (e.g. IntelliJ IDEA or VS Code with Java support).

4. Build the project (via Gradle or default compiler).

5. Run the application:
```bash
java -cp path/to/classes app.Main  
```


## Usage  
- On launch, log in (or create a user) via the login screen.  
- Once logged in, the market simulation dashboard shows live‑like price updates.  
- Place buy or sell orders to test trading — portfolio value, cash balance and P/L will update dynamically.  
- Track holdings, view history, monitor market trends via UI.  
- Exit and re‑open later — simulation state is saved, so you continue from the last state.  



## Screenshots / Demo  

<img src="screenshots/Screenshot (142).png" width="600"/>
<img src="screenshots/Screenshot (141).png" width="600"/>

## TECH STACK

- Java → core programming, OOP principles, logic implementation

**Frontend / UI:**
- Java Swing → GUI for login screen, dashboard, portfolio view, trade execution

**Backend / Logic:**
- Core Java classes → Price engine, trade execution, portfolio management
- Object-Oriented Programming → encapsulation of stocks, portfolios, simulator state

**Database / Storage:**
- SQLite → user authentication, persistent storage of portfolio & market state
- Serialization → saving simulation state locally between sessions

**Tools:**
- IDE (IntelliJ IDEA / Eclipse / VS Code with Java support)
- Git → version control

## LESSONS
**Programming & OOP:**
- Proper use of classes, objects, and encapsulation
- Designing a modular system (UI, services, data models separated)
- Handling edge cases in logic (like buying more stocks than cash allows)

**UI / UX:**
- Building desktop apps with Swing (layouts, buttons, tables)
- Updating UI dynamically based on backend state
- Importance of responsive and intuitive design

**Data & Persistence:**
- Connecting Java applications to SQLite
- Serialization for saving & loading complex objects
- Managing data consistency across sessions

**Algorithmic / Logic Skills:**
- Designing price engine (momentum + volatility + randomness)
- Trade execution logic: updating cash, holdings, P/L
- Handling simultaneous updates to portfolio and UI safely

**Project / Engineering Skills:**
- Integrating multiple components (UI + backend + database)
- Debugging complex interactions between modules
- Version control workflow with Git
- Structuring a project for scalability and future features


## Future Roadmap  
- Add more realistic market dynamics (news, random events, market-wide volatility)  
- Include charts/graphs (price history, portfolio value over time)  
- Support for multiple asset classes (e.g. derivatives / options simulation)  
- Add transaction history export (CSV/Excel)  
- Better UI/UX — more interactive, customizable, theme support  
- Multi‑user support (multiple user profiles, separate portfolios)  
## Contributing  

Contributions are welcome! If you want to:  
- Report bugs / issues  
- Propose improvements / features  
- Submit pull requests  
Please fork the repo, create a new branch with a descriptive name, and submit a PR.  
Ensure code style consistency, and include brief description of changes & testing steps in your PR message.  

## License

This project is licensed under the MIT License.

