package com.team62.view;

import com.team62.controller.MainController;
import com.team62.model.Employee;
import com.team62.model.InventoryItem;
import com.team62.model.MenuItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Manager view: sidebar navigation and content panes for Menu, Inventory,
 * Employees, and Reports.
 */
public class ManagerView extends BorderPane {

    private final MainController controller;
    private final StackPane contentStack = new StackPane();
    private final VBox menuPane = new VBox();
    private final VBox inventoryPane = new VBox();
    private final VBox employeesPane = new VBox();
    private final VBox reportsPane = new VBox();

    public ManagerView(MainController controller) {
        this.controller = controller;
        setStyle("-fx-background-color: #e8e4e0;");
        buildLayout();
    }

    private void buildLayout() {
        setPadding(new Insets(12));

        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(12));
        sidebar.setStyle("-fx-background-color: #d0ccc8; -fx-background-radius: 6;");
        sidebar.setPrefWidth(160);
        sidebar.setMinWidth(140);

        Label navLabel = new Label("Manager");
        navLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        sidebar.getChildren().add(navLabel);
        sidebar.getChildren().add(new Separator());

        Button menuBtn = navButton("Menu");
        menuBtn.setOnAction(e -> showPane(menuPane));
        Button invBtn = navButton("Inventory");
        invBtn.setOnAction(e -> showPane(inventoryPane));
        Button empBtn = navButton("Employees");
        empBtn.setOnAction(e -> showPane(employeesPane));
        Button reportsBtn = navButton("Reports");
        reportsBtn.setOnAction(e -> showPane(reportsPane));

        sidebar.getChildren().addAll(menuBtn, invBtn, empBtn, reportsBtn);

        buildMenuPane();
        buildInventoryPane();
        buildEmployeesPane();
        buildReportsPane();

        contentStack.getChildren().addAll(menuPane, inventoryPane, employeesPane, reportsPane);
        showPane(menuPane);

        setLeft(sidebar);
        setCenter(contentStack);
    }

    private Button navButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    private void showPane(VBox pane) {
        for (javafx.scene.Node n : contentStack.getChildren()) {
            n.setVisible(n == pane);
            n.setManaged(n == pane);
        }
    }

    private void buildMenuPane() {
        menuPane.setSpacing(12);
        menuPane.setPadding(new Insets(16));

        Label title = new Label("Menu — View, Add & Edit Items & Prices");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        menuPane.getChildren().addAll(title, new Separator());

        TableView<MenuItem> table = new TableView<>();
        TableColumn<MenuItem, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("menuItemId"));
        TableColumn<MenuItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<MenuItem, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        TableColumn<MenuItem, BigDecimal> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("basePrice"));
        table.getColumns().addAll(idCol, nameCol, catCol, priceCol);
        table.setItems(javafx.collections.FXCollections.observableList(controller.getAllMenuItems()));
        table.setPrefHeight(220);
        menuPane.getChildren().add(table);

        Runnable refreshMenu = () -> table.setItems(javafx.collections.FXCollections.observableList(controller.getAllMenuItems()));

        HBox tableActions = new HBox(8);
        Button editMenuBtn = new Button("Edit selected");
        editMenuBtn.setOnAction(e -> {
            MenuItem sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                new Alert(Alert.AlertType.WARNING, "Select a row to edit.").showAndWait();
                return;
            }
            if (showEditMenuDialog(sel)) {
                refreshMenu.run();
            }
        });
        Button deleteMenuBtn = new Button("Delete selected");
        deleteMenuBtn.setStyle("-fx-text-fill: #c00;");
        deleteMenuBtn.setOnAction(e -> {
            MenuItem sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                new Alert(Alert.AlertType.WARNING, "Select a row to delete.").showAndWait();
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete menu item \"" + sel.getName() + "\"? This cannot be undone.",
                    ButtonType.OK, ButtonType.CANCEL);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String err = controller.deleteMenuItem(sel);
                if (err == null) {
                    refreshMenu.run();
                } else {
                    new Alert(Alert.AlertType.ERROR, err).showAndWait();
                }
            }
        });
        tableActions.getChildren().addAll(editMenuBtn, deleteMenuBtn);
        menuPane.getChildren().add(tableActions);

        Label addLabel = new Label("Add new standard item");
        addLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        HBox form = new HBox(8);
        TextField nameF = new TextField();
        nameF.setPromptText("Name");
        TextField catF = new TextField();
        catF.setPromptText("Category");
        TextField priceF = new TextField();
        priceF.setPromptText("Price");
        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            try {
                int nextId = controller.getAllMenuItems().stream().mapToInt(MenuItem::getMenuItemId).max().orElse(0) + 1;
                controller.addMenuItem(new MenuItem(nextId, nameF.getText().trim(), catF.getText().trim(),
                        new BigDecimal(priceF.getText().trim()), true));
                refreshMenu.run();
                nameF.clear();
                catF.clear();
                priceF.clear();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid input. Use a number for price.").showAndWait();
            }
        });
        form.getChildren().addAll(new Label("Name:"), nameF, new Label("Category:"), catF,
                new Label("Price:"), priceF, addBtn);
        menuPane.getChildren().addAll(addLabel, form);

        Label seasonalLabel = new Label("Add new seasonal menu item + associated inventory items");
        seasonalLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        VBox seasonalBox = new VBox(8);
        seasonalBox.setStyle("-fx-background-color: #f7f4ef; -fx-border-color: #d8d2cb; -fx-padding: 10; -fx-background-radius: 6;");
        TextField sName = new TextField();
        sName.setPromptText("Seasonal item name");
        TextField sCat = new TextField("Seasonal");
        TextField sPrice = new TextField();
        sPrice.setPromptText("Price");
        TextField sIngredients = new TextField();
        sIngredients.setPromptText("Associated inventory names, comma separated");
        TextField sUse = new TextField("1");
        TextField sStart = new TextField("25");
        TextField sMin = new TextField("8");
        Button seasonalBtn = new Button("Add Seasonal Item");
        seasonalBtn.setOnAction(e -> {
            try {
                String result = controller.addSeasonalMenuItem(
                        sName.getText().trim(),
                        sCat.getText().trim(),
                        new BigDecimal(sPrice.getText().trim()),
                        sIngredients.getText().trim(),
                        Integer.parseInt(sUse.getText().trim()),
                        Integer.parseInt(sStart.getText().trim()),
                        Integer.parseInt(sMin.getText().trim()));
                if (result.toLowerCase().contains("success")) {
                    refreshMenu.run();
                    sName.clear();
                    sPrice.clear();
                    sIngredients.clear();
                    new Alert(Alert.AlertType.INFORMATION,
                            result + " It is now available in the POS and its ingredient inventory records were created/linked.")
                            .showAndWait();
                } else {
                    new Alert(Alert.AlertType.ERROR, result).showAndWait();
                }
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Invalid seasonal item input. Check price, usage per sale, starting inventory, and minimum inventory.")
                        .showAndWait();
            }
        });
        HBox seasonalTop = new HBox(8, new Label("Name:"), sName, new Label("Category:"), sCat,
                new Label("Price:"), sPrice);
        HBox seasonalBottom = new HBox(8, new Label("Ingredients:"), sIngredients,
                new Label("Use/sale:"), sUse,
                new Label("Start qty:"), sStart,
                new Label("Min qty:"), sMin,
                seasonalBtn);
        seasonalBox.getChildren().addAll(
                new Label("Example: Maple Cold Brew with ingredients 'Cold Brew, Maple Syrup, Cream Top'"),
                seasonalTop, seasonalBottom);
        menuPane.getChildren().addAll(seasonalLabel, seasonalBox);
    }

    private boolean showEditMenuDialog(MenuItem item) {
        TextField nameF = new TextField(item.getName());
        TextField catF = new TextField(item.getCategory());
        TextField priceF = new TextField(item.getBasePrice().toString());
        VBox root = new VBox(10,
                new Label("Name:"), nameF,
                new Label("Category:"), catF,
                new Label("Price:"), priceF);
        root.setPadding(new Insets(16));
        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");
        root.getChildren().add(new HBox(8, saveBtn, cancelBtn));

        Stage stage = new Stage();
        stage.setTitle("Edit menu item");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new javafx.scene.Scene(root));
        final boolean[] saved = { false };
        saveBtn.setOnAction(ev -> {
            try {
                item.setName(nameF.getText().trim());
                item.setCategory(catF.getText().trim());
                item.setBasePrice(new BigDecimal(priceF.getText().trim()));
                controller.updateMenuItem(item);
                saved[0] = true;
                stage.close();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid input. Use a number for price.").showAndWait();
            }
        });
        cancelBtn.setOnAction(ev -> stage.close());
        stage.showAndWait();
        return saved[0];
    }

    private void buildInventoryPane() {
        inventoryPane.setSpacing(12);
        inventoryPane.setPadding(new Insets(16));

        Label title = new Label("Inventory — View, Add & Edit Items & Quantities");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        inventoryPane.getChildren().addAll(title, new Separator());

        TableView<InventoryItem> table = new TableView<>();
        TableColumn<InventoryItem, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("inventoryItemId"));
        TableColumn<InventoryItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        // v unit -> category to reflect changes
        TableColumn<InventoryItem, String> unitCol = new TableColumn<>("Category");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        TableColumn<InventoryItem, Integer> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("currentQuantity"));
        TableColumn<InventoryItem, Integer> parCol = new TableColumn<>("Minimum");
        parCol.setCellValueFactory(new PropertyValueFactory<>("parLevel"));
        table.getColumns().addAll(idCol, nameCol, unitCol, qtyCol, parCol);
        table.setItems(javafx.collections.FXCollections.observableList(controller.getAllInventoryItems()));
        table.setPrefHeight(240);
        inventoryPane.getChildren().add(table);

        Runnable refreshInventory = () -> table.setItems(javafx.collections.FXCollections.observableList(controller.getAllInventoryItems()));

        HBox tableActions = new HBox(8);
        Button editInvBtn = new Button("Edit selected");
        editInvBtn.setOnAction(e -> {
            InventoryItem sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                new Alert(Alert.AlertType.WARNING, "Select a row to edit.").showAndWait();
                return;
            }
            if (showEditInventoryDialog(sel)) {
                refreshInventory.run();
            }
        });
        Button deleteInvBtn = new Button("Delete selected");
        deleteInvBtn.setStyle("-fx-text-fill: #c00;");
        deleteInvBtn.setOnAction(e -> {
            InventoryItem sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                new Alert(Alert.AlertType.WARNING, "Select a row to delete.").showAndWait();
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete inventory entry \"" + sel.getName() + "\"? This cannot be undone.",
                    ButtonType.OK, ButtonType.CANCEL);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String err = controller.deleteInventoryItem(sel);
                if (err == null) {
                    refreshInventory.run();
                } else {
                    new Alert(Alert.AlertType.ERROR, err).showAndWait();
                }
            }
        });
        tableActions.getChildren().addAll(editInvBtn, deleteInvBtn);
        inventoryPane.getChildren().add(tableActions);

        Label addLabel = new Label("Add new inventory item");
        addLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        HBox form = new HBox(8);
        TextField nameF = new TextField();
        nameF.setPromptText("Name");
        TextField unitF = new TextField();
        unitF.setPromptText("Category"); // Unit -> Category to reflect changes
        TextField qtyF = new TextField();
        qtyF.setPromptText("Qty");
        TextField parF = new TextField();
        parF.setPromptText("Minimum");
        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            try {
                int nextId = controller.getAllInventoryItems().stream().mapToInt(InventoryItem::getInventoryItemId).max().orElse(0) + 1;
                controller.addInventoryItem(new InventoryItem(nextId, nameF.getText().trim(), unitF.getText().trim(),
                        Integer.parseInt(qtyF.getText().trim()), Integer.parseInt(parF.getText().trim()), 1, true));
                refreshInventory.run();
                nameF.clear();
                unitF.clear();
                qtyF.clear();
                parF.clear();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid input. Use numbers for qty and minimum.").showAndWait();
            }
        });
        // v unit -> category to reflect changes
        form.getChildren().addAll(new Label("Name:"), nameF, new Label("Category:"), unitF,
                new Label("Qty:"), qtyF, new Label("Minimum:"), parF, addBtn);
        inventoryPane.getChildren().addAll(addLabel, form);
    }

    private boolean showEditInventoryDialog(InventoryItem item) {
        Label nameLabel = new Label(item.getName());
        TextField unitF = new TextField(item.getUnit());
        TextField qtyF = new TextField(String.valueOf(item.getCurrentQuantity()));
        TextField parF = new TextField(String.valueOf(item.getParLevel()));
        VBox root = new VBox(10,
                new Label("Item (read-only):"), nameLabel,
                new Label("Unit:"), unitF,
                new Label("Quantity:"), qtyF,
                new Label("Minimum stock:"), parF);
        root.setPadding(new Insets(16));
        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");
        root.getChildren().add(new HBox(8, saveBtn, cancelBtn));

        Stage stage = new Stage();
        stage.setTitle("Edit inventory");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new javafx.scene.Scene(root));
        final boolean[] saved = { false };
        saveBtn.setOnAction(ev -> {
            try {
                item.setUnit(unitF.getText().trim());
                item.setCurrentQuantity(Integer.parseInt(qtyF.getText().trim()));
                item.setParLevel(Integer.parseInt(parF.getText().trim()));
                controller.updateInventoryItem(item);
                saved[0] = true;
                stage.close();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid input. Use numbers for quantity and minimum.").showAndWait();
            }
        });
        cancelBtn.setOnAction(ev -> stage.close());
        stage.showAndWait();
        return saved[0];
    }

    private void buildEmployeesPane() {
        employeesPane.setSpacing(12);
        employeesPane.setPadding(new Insets(16));

        Label title = new Label("Employees — View, Add & Edit");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        employeesPane.getChildren().addAll(title, new Separator());

        TableView<Employee> table = new TableView<>();
        TableColumn<Employee, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        TableColumn<Employee, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Employee, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        TableColumn<Employee, Boolean> activeCol = new TableColumn<>("Active");
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        table.getColumns().addAll(idCol, nameCol, roleCol, activeCol);
        table.setItems(javafx.collections.FXCollections.observableList(controller.getAllEmployees()));
        table.setPrefHeight(200);
        employeesPane.getChildren().add(table);

        HBox tableActions = new HBox(8);
        Button editEmpBtn = new Button("Edit selected");
        editEmpBtn.setOnAction(e -> {
            Employee sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                new Alert(Alert.AlertType.WARNING, "Select a row to edit.").showAndWait();
                return;
            }
            if (showEditEmployeeDialog(sel)) {
                table.setItems(javafx.collections.FXCollections.observableList(controller.getAllEmployees()));
            }
        });
        Button deleteEmpBtn = new Button("Delete selected");
        deleteEmpBtn.setStyle("-fx-text-fill: #c00;");
        deleteEmpBtn.setOnAction(e -> {
            Employee sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                new Alert(Alert.AlertType.WARNING, "Select a row to delete.").showAndWait();
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete employee \"" + sel.getName() + "\"? This cannot be undone.",
                    ButtonType.OK, ButtonType.CANCEL);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String err = controller.deleteEmployee(sel);
                if (err == null) {
                    table.setItems(javafx.collections.FXCollections.observableList(controller.getAllEmployees()));
                } else {
                    new Alert(Alert.AlertType.ERROR, err).showAndWait();
                }
            }
        });
        tableActions.getChildren().addAll(editEmpBtn, deleteEmpBtn);
        employeesPane.getChildren().add(tableActions);

        Label addLabel = new Label("Add new employee");
        addLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        HBox form = new HBox(8);
        TextField nameF = new TextField();
        nameF.setPromptText("Name");
        TextField roleF = new TextField();
        roleF.setPromptText("Role");
        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            int nextId = controller.getAllEmployees().stream().mapToInt(Employee::getEmployeeId).max().orElse(0) + 1;
            controller.addEmployee(new Employee(nextId, nameF.getText().trim(), roleF.getText().trim(), true));
            table.setItems(javafx.collections.FXCollections.observableList(controller.getAllEmployees()));
            nameF.clear();
            roleF.clear();
        });
        form.getChildren().addAll(new Label("Name:"), nameF, new Label("Role:"), roleF, addBtn);
        employeesPane.getChildren().addAll(addLabel, form);
    }

    private boolean showEditEmployeeDialog(Employee emp) {
        TextField nameF = new TextField(emp.getName());
        TextField roleF = new TextField(emp.getRole() != null ? emp.getRole() : "");
        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(emp.isActive());
        VBox root = new VBox(10,
                new Label("Name:"), nameF,
                new Label("Role:"), roleF,
                activeCheck);
        root.setPadding(new Insets(16));
        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");
        root.getChildren().add(new HBox(8, saveBtn, cancelBtn));

        Stage stage = new Stage();
        stage.setTitle("Edit employee");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new javafx.scene.Scene(root));
        final boolean[] saved = { false };
        saveBtn.setOnAction(ev -> {
            emp.setName(nameF.getText().trim());
            emp.setRole(roleF.getText().trim());
            emp.setActive(activeCheck.isSelected());
            controller.updateEmployee(emp);
            saved[0] = true;
            stage.close();
        });
        cancelBtn.setOnAction(ev -> stage.close());
        stage.showAndWait();
        return saved[0];
    }

    private void buildReportsPane() {
        reportsPane.setSpacing(12);
        reportsPane.setPadding(new Insets(16));

        Label title = new Label("Reports — Product Usage, X/Z, Sales, and Restock");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        DatePicker startPicker = new DatePicker(LocalDate.now().minusDays(7));
        DatePicker endPicker = new DatePicker(LocalDate.now());
        TextArea output = new TextArea();
        output.setEditable(false);
        output.setPrefRowCount(24);
        VBox.setVgrow(output, Priority.ALWAYS);

        Button productUsageBtn = new Button("Product Usage Chart");
        productUsageBtn.setOnAction(e -> output.setText(controller.getInventoryUsageChart(startPicker.getValue(), endPicker.getValue())));

        Button xReportBtn = new Button("X-Report (today)");
        xReportBtn.setOnAction(e -> output.setText(controller.getXReport(LocalDate.now())));

        Button zReportBtn = new Button("Run Z-Report (today)");
        zReportBtn.setOnAction(e -> output.setText(controller.runZReport(LocalDate.now())));

        Button salesReportBtn = new Button("Sales Report");
        salesReportBtn.setOnAction(e -> output.setText(controller.getSalesReport(startPicker.getValue(), endPicker.getValue())));

        Button restockBtn = new Button("Restock Report");
        restockBtn.setOnAction(e -> output.setText(controller.getRestockReport()));

        HBox top = new HBox(10,
                new Label("Start:"), startPicker,
                new Label("End:"), endPicker,
                productUsageBtn,
                salesReportBtn,
                xReportBtn,
                zReportBtn,
                restockBtn);
        top.setAlignment(Pos.CENTER_LEFT);

        reportsPane.getChildren().addAll(title, new Separator(), top, output);
        output.setText(controller.getXReport(LocalDate.now()));
    }
}
