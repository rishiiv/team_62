# team_62

Boba Shop POS — a JavaFX point-of-sale and manager dashboard backed by a shared PostgreSQL database.

## Architecture: MVC Pattern

This project follows the **Model-View-Controller (MVC)** architecture pattern for clean separation of concerns:

- **Model** (`com.team62.model`): Data models representing business entities (many carry a `dbId` for the real DB primary key)
- **View** (`com.team62.view`): JavaFX UI components (Cashier and Manager screens)
- **Controller** (`com.team62.controller`): Business logic and coordination; **MainController** talks to the database for all reads/writes
- **Database** (`com.team62.db`): JDBC connection helper used by MainController to reach PostgreSQL

## Project Structure

```
team_62/
├── src/main/java/com/team62/              # Source (Maven-style layout)
│   ├── model/                             # Data entities
│   ├── view/                              # JavaFX UI (MainView, CashierView, ManagerView)
│   ├── controller/                        # Business logic + DB access
│   ├── db/                                # JDBC connection helper
│   └── Main.java
├── sql/                                   # Schema and SQL scripts
│   ├── schema.sql                         # DB schema (Customer, Employee, Item, Order, etc.)
│   ├── seed.py                            # Optional seed script
│   └── queries/                          # Analytics / report queries
├── lib/                                   # Third-party JARs (see lib/README.md)
│   └── postgresql-42.7.10.jar            # PostgreSQL JDBC driver (add manually)
├── build/classes/                         # Compiled output
├── compile.sh                             # Build script
├── run.sh                                 # Run script (uses lib/postgresql-*.jar if present)
└── README.md
```

## MVC Architecture Details

### Model Layer (`com.team62.model`)
Contains data models that represent business entities (many include a `dbId` for the Postgres UUID):
- `MenuItem` - Menu items with pricing (`"Item"`)
- `InventoryItem` - Inventory quantities and link to items (`"Inventory_Quantity"` / `"Item_Inventory"`)
- `SalesOrder` / `SalesOrderItem` - Orders and line items (`"Order"`, `"Order_Item"`); `SalesOrderItem` has `itemDbId` for the Item UUID
- `Employee` - Staff (name, role, active) — maps to `"Employee"` with role/active in `work_history` JSONB
- `InventoryPurchase` / `InventoryUsage` - Supporting models

### View Layer (`com.team62.view`)
Contains JavaFX UI components with **no business logic**:
- `MainView` - App shell with Cashier/Manager toggle and status bar
- `CashierView` - Menu item grid (left) and order summary (right); Submit Order calls `MainController.processOrder(...)`
- `ManagerView` - Sidebar + panes for Menu, Inventory, Employees, Reports; all data comes from `MainController` (which reads/writes the database)

Views only handle UI rendering, user input display, and status updates via controller methods.

### Controller Layer (`com.team62.controller`)
Contains business logic and coordinates Model-View interactions:
- `MainController` - Core business logic; **all list/get/add/update/report methods use the real PostgreSQL database** via `Database.getConnection()`. No in-memory-only data for menu, inventory, employees, or orders (see Database connection below).
- `MainWindowController` - JavaFX event handling, view coordination, and switching between Cashier and Manager content.

### Database Layer (`com.team62.db`)
- `Database.java` - Static helper that provides JDBC connections to the shared Postgres instance. Connection details (host, database, user, password) are in this class. The PostgreSQL JDBC driver (`org.postgresql.Driver`) must be on the **runtime classpath** (see Run script below).

---

## How Real Data Is Connected (for teammates)

The app is **fully backed by the shared PostgreSQL database**. There is no separate in-memory store for menu items, inventory, employees, or orders once the app is running.

1. **Single entry point**  
   Every DB access goes through `com.team62.db.Database.getConnection()`. The connection URL, user, and password are defined in `Database.java` (host: `csce-315-db.engr.tamu.edu`, database: `team_62_db`).

2. **Who uses it**  
   `MainController` is the only class that calls `Database.getConnection()`. The UI (CashierView, ManagerView) never touches the DB directly; they only call methods on `MainController`.

3. **Flow**  
   - User action in the UI (e.g. "Submit Order", "Add menu item", "Refresh report") triggers a method on `MainController`.
   - That method obtains a connection with `Database.getConnection()`, runs the appropriate `SELECT` / `INSERT` / `UPDATE`, then closes the connection (via try-with-resources).
   - The same method may return data (e.g. `getAllMenuItems()`) or a status string (e.g. `processOrder()`). The view just displays what the controller returns.

4. **Table mapping (what lives in the DB)**  
   - **Menu items & prices** → `"Item"` (view/add/update from Manager → Menu).
   - **Inventory quantities** → `"Inventory_Quantity"` and `"Item_Inventory"` joined with `"Item"` (view/update from Manager → Inventory; add creates new inventory row linked to an existing Item by name).
   - **Employees** → `"Employee"` (role/active stored in `work_history` JSONB); view/add/update from Manager → Employees.
   - **Orders** → `"Order"` (with `item_quantity` JSONB) and `"Order_Item"` (one row per item). Cashier Submit Order inserts into both; Reports read from `"Order"` (and join to `"Item"` for totals).
   - **Customer / Employee for orders** → If the DB has no rows, the app inserts a single "Walk-up Customer" and "Demo Employee" so `"Order"` foreign keys are satisfied.

5. **IDs in the app vs DB**  
   Models use both a simple integer id for the UI (e.g. `menuItemId`, `employeeId`) and a `dbId` (String) holding the real UUID primary key from Postgres. When saving (e.g. update menu item, submit order), the code uses `dbId` so the correct row is updated or referenced.

### Demo features (all DB-backed)

| Feature | User | Where in app | DB tables |
|--------|------|----------------|-----------|
| Submit orders | Cashier | Cashier view → Order Summary → Submit Order | `"Order"`, `"Order_Item"` |
| View / add / update menu items & prices | Manager | Manager → Menu | `"Item"` |
| View / add / update inventory items & quantities | Manager | Manager → Inventory | `"Inventory_Quantity"`, `"Item_Inventory"`, `"Item"` |
| View / add / update / manage employees | Manager | Manager → Employees | `"Employee"` (role/active in `work_history`) |
| View and create day-to-day reports | Manager | Manager → Reports (date picker + refresh) | `"Order"`, `"Item"` (aggregates) |

---

## Building and Running

### Prerequisites

1. **Java** — Java 11+ recommended.
2. **JavaFX SDK** — Download from [OpenJFX](https://openjfx.io/). The scripts default to `/Users/adavi/development/javafx-sdk-25.0.1/lib`. To use another path:
   - `export JAVA_FX_PATH=/path/to/javafx-sdk/lib`, or
   - `./compile.sh /path/to/javafx-sdk/lib` and `./run.sh /path/to/javafx-sdk/lib`.
3. **PostgreSQL JDBC driver** — Required at **runtime**. Download the JAR from [jdbc.postgresql.org](https://jdbc.postgresql.org/download/) and place it in the project’s **`lib/`** folder (e.g. `lib/postgresql-42.7.10.jar`). The run script automatically uses any `postgresql-*.jar` in `lib/`. To use a JAR elsewhere: `export PG_JDBC_JAR=/path/to/postgresql-XX.x.x.jar`.

### Quick Start (recommended) — Chmod is only linux. You should compile through wsl or git bash if you are on windows.

```bash
chmod +x compile.sh run.sh
./compile.sh
./run.sh
```

- **Compile:** Builds `model`, `db`, `view`, `controller`, and `Main`. The `db` package is included so `Database` is available to `MainController`.
- **Run:** Starts the JavaFX app with `build/classes` and the Postgres JDBC JAR on the classpath. If the driver is missing, you’ll see `ClassNotFoundException: org.postgresql.Driver` or "PostgreSQL JDBC driver not found on classpath".

### Manual compile and run

Compile (include the `db` package and your JavaFX path):

```bash
mkdir -p build/classes
javac --module-path /path/to/javafx-sdk/lib \
      --add-modules javafx.controls,javafx.fxml \
      -d build/classes \
      src/main/java/com/team62/model/*.java \
      src/main/java/com/team62/db/*.java \
      src/main/java/com/team62/view/*.java \
      src/main/java/com/team62/controller/*.java \
      src/main/java/com/team62/*.java
```

Run (classpath must include both `build/classes` and the Postgres driver JAR):

```bash
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp "build/classes:/path/to/postgresql-42.7.10.jar" \
     com.team62.Main
```

## Requirements

- Java 11 or higher
- JavaFX SDK (if not included with your JDK)
- PostgreSQL JDBC driver JAR on the runtime classpath for database connectivity
