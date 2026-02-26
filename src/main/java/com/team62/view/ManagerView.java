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
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

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

        // Left sidebar
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
        menuPane.getChildren().add(title);
        menuPane.getChildren().add(new Separator());

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
        table.setPrefHeight(200);
        menuPane.getChildren().add(table);

        HBox tableActions = new HBox(8);
        tableActions.setAlignment(Pos.CENTER_LEFT);
        Button editMenuBtn = new Button("Edit selected");
        editMenuBtn.setOnAction(e -> {
            MenuItem sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                new Alert(Alert.AlertType.WARNING, "Select a row to edit.").showAndWait();
                return;
            }
            if (showEditMenuDialog(sel)) {
                table.setItems(javafx.collections.FXCollections.observableList(controller.getAllMenuItems()));
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
                    table.setItems(javafx.collections.FXCollections.observableList(controller.getAllMenuItems()));
                } else {
                    new Alert(Alert.AlertType.ERROR, err).showAndWait();
                }
            }
        });
        tableActions.getChildren().addAll(editMenuBtn, deleteMenuBtn);
        menuPane.getChildren().add(tableActions);

        Label addLabel = new Label("Add new item");
        addLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        menuPane.getChildren().add(addLabel);
        HBox form = new HBox(8);
        form.setAlignment(Pos.CENTER_LEFT);
        TextField nameF = new TextField();
        nameF.setPromptText("Name");
        nameF.setPrefWidth(140);
        TextField catF = new TextField();
        catF.setPromptText("Category");
        catF.setPrefWidth(100);
        TextField priceF = new TextField();
        priceF.setPromptText("Price");
        priceF.setPrefWidth(80);
        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            try {
                int nextId = controller.getAllMenuItems().stream()
                        .mapToInt(MenuItem::getMenuItemId)
                        .max()
                        .orElse(0) + 1;
                MenuItem item = new MenuItem(nextId,
                        nameF.getText().trim(),
                        catF.getText().trim(),
                        new BigDecimal(priceF.getText().trim()),
                        true);
                controller.addMenuItem(item);
                table.setItems(javafx.collections.FXCollections.observableList(controller.getAllMenuItems()));
                nameF.clear();
                catF.clear();
                priceF.clear();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid input. Use a number for price.").showAndWait();
            }
        });
        form.getChildren().addAll(
                new Label("Name:"), nameF,
                new Label("Category:"), catF,
                new Label("Price:"), priceF,
                addBtn);
        menuPane.getChildren().add(form);
    }

    private boolean showEditMenuDialog(MenuItem item) {
        TextField nameF = new TextField(item.getName());
        nameF.setPrefWidth(200);
        TextField catF = new TextField(item.getCategory());
        catF.setPrefWidth(120);
        TextField priceF = new TextField(item.getBasePrice().toString());
        priceF.setPrefWidth(80);
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));
        root.getChildren().addAll(
                new Label("Name:"), nameF,
                new Label("Category:"), catF,
                new Label("Price:"), priceF);
        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");
        HBox buttons = new HBox(8);
        buttons.getChildren().addAll(saveBtn, cancelBtn);
        root.getChildren().add(buttons);

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
        inventoryPane.getChildren().add(title);
        inventoryPane.getChildren().add(new Separator());

        TableView<InventoryItem> table = new TableView<>();
        TableColumn<InventoryItem, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("inventoryItemId"));
        TableColumn<InventoryItem, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<InventoryItem, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        TableColumn<InventoryItem, Integer> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("currentQuantity"));
        TableColumn<InventoryItem, Integer> parCol = new TableColumn<>("Par");
        parCol.setCellValueFactory(new PropertyValueFactory<>("parLevel"));
        table.getColumns().addAll(idCol, nameCol, unitCol, qtyCol, parCol);
        table.setItems(javafx.collections.FXCollections.observableList(controller.getAllInventoryItems()));
        table.setPrefHeight(200);
        inventoryPane.getChildren().add(table);

        HBox tableActions = new HBox(8);
        tableActions.setAlignment(Pos.CENTER_LEFT);
        Button editInvBtn = new Button("Edit selected");
        editInvBtn.setOnAction(e -> {
            InventoryItem sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                new Alert(Alert.AlertType.WARNING, "Select a row to edit.").showAndWait();
                return;
            }
            if (showEditInventoryDialog(sel)) {
                table.setItems(javafx.collections.FXCollections.observableList(controller.getAllInventoryItems()));
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
                    table.setItems(javafx.collections.FXCollections.observableList(controller.getAllInventoryItems()));
                } else {
                    new Alert(Alert.AlertType.ERROR, err).showAndWait();
                }
            }
        });
        tableActions.getChildren().addAll(editInvBtn, deleteInvBtn);
        inventoryPane.getChildren().add(tableActions);

        Label addLabel = new Label("Add new inventory (name must match an existing menu item)");
        addLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        inventoryPane.getChildren().add(addLabel);
        HBox form = new HBox(8);
        form.setAlignment(Pos.CENTER_LEFT);
        TextField nameF = new TextField();
        nameF.setPromptText("Name");
        nameF.setPrefWidth(120);
        TextField unitF = new TextField();
        unitF.setPromptText("Unit");
        unitF.setPrefWidth(60);
        TextField qtyF = new TextField();
        qtyF.setPromptText("Qty");
        qtyF.setPrefWidth(50);
        TextField parF = new TextField();
        parF.setPromptText("Par");
        parF.setPrefWidth(50);
        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            try {
                int nextId = controller.getAllInventoryItems().stream()
                        .mapToInt(InventoryItem::getInventoryItemId)
                        .max()
                        .orElse(0) + 1;
                InventoryItem item = new InventoryItem(
                        nextId,
                        nameF.getText().trim(),
                        unitF.getText().trim(),
                        Integer.parseInt(qtyF.getText().trim()),
                        Integer.parseInt(parF.getText().trim()),
                        1,
                        true);
                controller.addInventoryItem(item);
                table.setItems(javafx.collections.FXCollections.observableList(controller.getAllInventoryItems()));
                nameF.clear();
                unitF.clear();
                qtyF.clear();
                parF.clear();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid input. Use numbers for qty and par.").showAndWait();
            }
        });
        form.getChildren().addAll(
                new Label("Name:"), nameF,
                new Label("Unit:"), unitF,
                new Label("Qty:"), qtyF,
                new Label("Par:"), parF,
                addBtn);
        inventoryPane.getChildren().add(form);
    }

    private boolean showEditInventoryDialog(InventoryItem item) {
        Label nameLabel = new Label(item.getName());
        TextField qtyF = new TextField(String.valueOf(item.getCurrentQuantity()));
        qtyF.setPrefWidth(80);
        TextField parF = new TextField(String.valueOf(item.getParLevel()));
        parF.setPrefWidth(80);
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));
        root.getChildren().addAll(
                new Label("Item (read-only):"), nameLabel,
                new Label("Quantity:"), qtyF,
                new Label("Par level:"), parF);
        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");
        HBox buttons = new HBox(8);
        buttons.getChildren().addAll(saveBtn, cancelBtn);
        root.getChildren().add(buttons);

        Stage stage = new Stage();
        stage.setTitle("Edit inventory");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new javafx.scene.Scene(root));
        final boolean[] saved = { false };
        saveBtn.setOnAction(ev -> {
            try {
                item.setCurrentQuantity(Integer.parseInt(qtyF.getText().trim()));
                item.setParLevel(Integer.parseInt(parF.getText().trim()));
                controller.updateInventoryItem(item);
                saved[0] = true;
                stage.close();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid input. Use numbers for quantity and par.").showAndWait();
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
        employeesPane.getChildren().add(title);
        employeesPane.getChildren().add(new Separator());

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
        tableActions.setAlignment(Pos.CENTER_LEFT);
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
        employeesPane.getChildren().add(addLabel);
        HBox form = new HBox(8);
        form.setAlignment(Pos.CENTER_LEFT);
        TextField nameF = new TextField();
        nameF.setPromptText("Name");
        nameF.setPrefWidth(140);
        TextField roleF = new TextField();
        roleF.setPromptText("Role");
        roleF.setPrefWidth(100);
        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            int nextId = controller.getAllEmployees().stream()
                    .mapToInt(Employee::getEmployeeId)
                    .max()
                    .orElse(0) + 1;
            Employee emp = new Employee(nextId,
                    nameF.getText().trim(),
                    roleF.getText().trim(),
                    true);
            controller.addEmployee(emp);
            table.setItems(javafx.collections.FXCollections.observableList(controller.getAllEmployees()));
            nameF.clear();
            roleF.clear();
        });
        form.getChildren().addAll(
                new Label("Name:"), nameF,
                new Label("Role:"), roleF,
                addBtn);
        employeesPane.getChildren().add(form);
    }

    private boolean showEditEmployeeDialog(Employee emp) {
        TextField nameF = new TextField(emp.getName());
        nameF.setPrefWidth(200);
        TextField roleF = new TextField(emp.getRole() != null ? emp.getRole() : "");
        roleF.setPrefWidth(120);
        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(emp.isActive());
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));
        root.getChildren().addAll(
                new Label("Name:"), nameF,
                new Label("Role:"), roleF,
                activeCheck);
        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");
        HBox buttons = new HBox(8);
        buttons.getChildren().addAll(saveBtn, cancelBtn);
        root.getChildren().add(buttons);

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

        Label title = new Label("Reports — View & Create Day-to-Day Reports");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        reportsPane.getChildren().add(title);
        reportsPane.getChildren().add(new Separator());

        DatePicker datePicker = new DatePicker(LocalDate.now());
        Button refreshBtn = new Button("Refresh report");
        Label orderCountLabel = new Label("Orders: —");
        Label totalSalesLabel = new Label("Total sales: $—");

        VBox reportBox = new VBox(8);
        reportBox.getChildren().addAll(orderCountLabel, totalSalesLabel);
        VBox.setVgrow(reportBox, Priority.NEVER);

        Runnable refresh = () -> {
            LocalDate d = datePicker.getValue();
            if (d == null) {
                return;
            }
            long count = controller.getOrderCountForDate(d);
            BigDecimal total = controller.getTotalSalesForDate(d);
            orderCountLabel.setText("Orders: " + count);
            totalSalesLabel.setText(
                    "Total sales: $" + (total != null ? total.setScale(2, java.math.RoundingMode.HALF_UP) : "0.00"));
        };

        refreshBtn.setOnAction(e -> refresh.run());
        datePicker.valueProperty().addListener((o, a, b) -> refresh.run());

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getChildren().addAll(new Label("Date:"), datePicker, refreshBtn);
        reportsPane.getChildren().add(top);
        reportsPane.getChildren().add(reportBox);
        refresh.run();
    }
}

