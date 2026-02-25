# team_62

## Architecture: MVC Pattern

This project follows the **Model-View-Controller (MVC)** architecture pattern for clean separation of concerns:

- **Model** (`com.team62.model`): Data models representing business entities
- **View** (`com.team62.view`): JavaFX UI components
- **Controller** (`com.team62.controller`): Business logic and coordination between Model and View

## Project Structure

```
team_62/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── team62/
│                   ├── model/              # Model layer - data entities
│                   │   ├── MenuItem.java
│                   │   ├── InventoryItem.java
│                   │   ├── SalesOrder.java
│                   │   ├── SalesOrderItem.java
│                   │   ├── InventoryPurchase.java
│                   │   └── InventoryUsage.java
│                   ├── view/               # View layer - UI presentation
│                   │   └── MainView.java
│                   ├── controller/         # Controller layer - business logic
│                   │   ├── MainController.java
│                   │   └── MainWindowController.java
│                   └── Main.java          # Application entry point
├── data/                                  # schema and seed files
│   ├── schema.sql
│   └── seed.py
├── build/classes/                         # Compiled classes
├── compile.sh                             # Compilation script
├── run.sh                                 # Run script
└── README.md
```

## MVC Architecture Details

### Model Layer (`com.team62.model`)
Contains data models that represent business entities:
- `MenuItem` - Menu items with pricing
- `InventoryItem` - Inventory items with par levels
- `SalesOrder` - Sales orders with items
- `SalesOrderItem` - Individual order line items
- `InventoryPurchase` - Inventory purchase records
- `InventoryUsage` - Inventory usage records

### View Layer (`com.team62.view`)
Contains JavaFX UI components with **no business logic**:
- `MainView` - JavaFX GUI components

Views only handle:
- UI rendering
- User input display
- Status updates (via controller methods)

### Controller Layer (`com.team62.controller`)
Contains business logic and coordinates Model-View interactions:
- `MainController` - Core business logic
- `MainWindowController` - JavaFX event handling and view coordination

Controllers handle:
- Business logic operations
- Model updates
- View updates based on model changes
- Event handling and user actions

## Building and Running

### Prerequisites

**JavaFX SDK:** This project requires JavaFX SDK. Download from [OpenJFX](https://openjfx.io/) or use a JDK that includes JavaFX.

### Quick Start

**Option 1: Using scripts (recommended)**

The scripts are pre-configured to use JavaFX SDK at `/Users/adavi/development/javafx-sdk-25.0.1/lib`.

If your JavaFX is installed elsewhere, you can either:
- Set environment variable: `export JAVA_FX_PATH=/path/to/javafx-sdk/lib`
- Or pass it as argument: `./compile.sh /path/to/javafx-sdk/lib`

1. Compile:
   ```bash
   chmod +x compile.sh run.sh
   ./compile.sh
   ```

2. Run:
   ```bash
   ./run.sh
   ```

**Option 2: Manual compilation**

1. Compile:
   ```bash
   mkdir -p build/classes
   javac --module-path /path/to/javafx-sdk/lib \
         --add-modules javafx.controls,javafx.fxml \
         -d build/classes \
         src/main/java/com/team62/model/*.java \
         src/main/java/com/team62/view/*.java \
         src/main/java/com/team62/controller/*.java \
         src/main/java/com/team62/*.java
   ```

2. Run:
   ```bash
   java --module-path /path/to/javafx-sdk/lib \
        --add-modules javafx.controls,javafx.fxml \
        -cp build/classes \
        com.team62.Main
   ```

## Requirements

- Java 8 or higher (Java 11+ recommended for JavaFX)
- For JavaFX: JavaFX SDK (if not included with your JDK)
