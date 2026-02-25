package com.team62.view;

import com.team62.controller.MainController;
import com.team62.model.Employee;
import com.team62.model.InventoryItem;
import com.team62.model.MenuItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Manager view: sidebar navigation and content panes for Menu, Inventory, Employees, and Reports.
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

        Label title = new Label("Menu — View, Add & Update Items & Prices");
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
                int nextId = controller.getAllMenuItems().stream().mapToInt(MenuItem::getMenuItemId).max().orElse(0) + 1;
                MenuItem item = new MenuItem(nextId, nameF.getText().trim(), catF.getText().trim(), new BigDecimal(priceF.getText().trim()), true);
                controller.addMenuItem(item);
                table.setItems(javafx.collections.FXCollections.observableList(controller.getAllMenuItems()));
                nameF.clear();
                catF.clear();
                priceF.clear();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid input. Use a number for price.").showAndWait();
            }
        });
        Button updateBtn = new Button("Update selected");
        updateBtn.setOnAction(e -> {
            MenuItem sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            try {
                sel.setName(nameF.getText().trim());
                sel.setCategory(catF.getText().trim());
                sel.setBasePrice(new BigDecimal(priceF.getText().trim()));
                controller.updateMenuItem(sel);
                table.refresh();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid input.").showAndWait();
            }
        });
        form.getChildren().addAll(new Label("Name:"), nameF, new Label("Category:"), catF, new Label("Price:"), priceF, addBtn, updateBtn);
        menuPane.getChildren().add(form);
    }

    private void buildInventoryPane() {
        inventoryPane.setSpacing(12);
        inventoryPane.setPadding(new Insets(16));

        Label title = new Label("Inventory — View, Add & Update Items & Quantities");
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
                int nextId = controller.getAllInventoryItems().stream().mapToInt(InventoryItem::getInventoryItemId).max().orElse(0) + 1;
                InventoryItem item = new InventoryItem(nextId, nameF.getText().trim(), unitF.getText().trim(),
                        Integer.parseInt(qtyF.getText().trim()), Integer.parseInt(parF.getText().trim()), 1, true);
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
        Button updateQtyBtn = new Button("Update quantity (selected)");
        updateQtyBtn.setOnAction(e -> {
            InventoryItem sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            Optional<String> s = new javafx.scene.control.TextInputDialog(String.valueOf(sel.getCurrentQuantity())).showAndWait();
            s.ifPresent(str -> {
                try {
                    sel.setCurrentQuantity(Integer.parseInt(str.trim()));
                    controller.updateInventoryItem(sel);
                    table.refresh();
                } catch (NumberFormatException ignored) { }
            });
        });
        form.getChildren().addAll(new Label("Name:"), nameF, new Label("Unit:"), unitF, new Label("Qty:"), qtyF, new Label("Par:"), parF, addBtn, updateQtyBtn);
        inventoryPane.getChildren().add(form);
    }

    private void buildEmployeesPane() {
        employeesPane.setSpacing(12);
        employeesPane.setPadding(new Insets(16));

        Label title = new Label("Employees — View, Add, Update & Manage");
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
            int nextId = controller.getAllEmployees().stream().mapToInt(Employee::getEmployeeId).max().orElse(0) + 1;
            Employee emp = new Employee(nextId, nameF.getText().trim(), roleF.getText().trim(), true);
            controller.addEmployee(emp);
            table.setItems(javafx.collections.FXCollections.observableList(controller.getAllEmployees()));
            nameF.clear();
            roleF.clear();
        });
        Button updateBtn = new Button("Update selected");
        updateBtn.setOnAction(e -> {
            Employee sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            sel.setName(nameF.getText().trim());
            sel.setRole(roleF.getText().trim());
            controller.updateEmployee(sel);
            table.refresh();
        });
        Button toggleActiveBtn = new Button("Toggle active");
        toggleActiveBtn.setOnAction(e -> {
            Employee sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            sel.setActive(!sel.isActive());
            controller.updateEmployee(sel);
            table.refresh();
        });
        form.getChildren().addAll(new Label("Name:"), nameF, new Label("Role:"), roleF, addBtn, updateBtn, toggleActiveBtn);
        employeesPane.getChildren().add(form);
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

        Runnable refresh = () -> {
            LocalDate d = datePicker.getValue();
            if (d == null) return;
            long count = controller.getOrderCountForDate(d);
            java.math.BigDecimal total = controller.getTotalSalesForDate(d);
            orderCountLabel.setText("Orders: " + count);
            totalSalesLabel.setText("Total sales: $" + (total != null ? total.setScale(2, java.math.RoundingMode.HALF_UP) : "0.00"));
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
