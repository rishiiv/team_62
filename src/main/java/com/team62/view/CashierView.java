package com.team62.view;

import com.team62.controller.MainController;
import com.team62.model.MenuItem;
import com.team62.model.SalesOrder;
import com.team62.model.SalesOrderItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Cashier view: grid of menu items (left) and order summary (right) for submitting orders.
 */
public class CashierView extends BorderPane {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final BigDecimal TIP_RATE = new BigDecimal("0.00");

    private final MainController controller;
    private final List<OrderLine> currentOrder = new ArrayList<>();
    private final VBox orderItemsBox = new VBox(4);
    private final Label subtotalLabel = new Label("Subtotal: $0.00");
    private final Label taxTipLabel = new Label("Tax/Tip: $0.00");
    private final Label totalLabel = new Label("TOTAL: $0.00");
    private long nextOrderItemId = 1;

    public CashierView(MainController controller) {
        this.controller = controller;
        setStyle("-fx-background-color: #f5f0eb;");
        buildLayout();
    }

    private void buildLayout() {
        setPadding(new Insets(16));

        // Left: menu item grid
        FlowPane menuGrid = new FlowPane(12, 12);
        menuGrid.setPrefWrapLength(400);
        menuGrid.setPadding(new Insets(8));

        for (MenuItem item : controller.getAllMenuItems()) {
            if (!item.isActive()) {
                continue;
            }
            Button btn = new Button(item.getName() + "\n$" + item.getBasePrice());
            btn.setPrefSize(120, 70);
            btn.setWrapText(true);
            btn.setStyle(
                    "-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 4; -fx-background-radius: 4;");
            btn.setOnAction(e -> addToOrder(item));
            menuGrid.getChildren().add(btn);
        }

        ScrollPane menuScroll = new ScrollPane(menuGrid);
        menuScroll.setFitToWidth(true);
        menuScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Right: order summary
        VBox orderPanel = new VBox(12);
        orderPanel.setPadding(new Insets(12));
        orderPanel.setStyle(
                "-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 6; -fx-background-radius: 6;");
        orderPanel.setPrefWidth(280);
        orderPanel.setMinWidth(260);

        Label orderTitle = new Label("Order Summary");
        orderTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        orderPanel.getChildren().add(orderTitle);
        orderPanel.getChildren().add(new Separator());

        orderItemsBox.setPadding(new Insets(0, 0, 8, 0));
        VBox.setVgrow(orderItemsBox, Priority.ALWAYS);
        orderPanel.getChildren().add(orderItemsBox);

        orderPanel.getChildren().add(new Separator());
        orderPanel.getChildren().addAll(subtotalLabel, taxTipLabel, totalLabel);
        totalLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        Button submitBtn = new Button("Submit Order");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setStyle("-fx-background-color: #2d6a2d; -fx-text-fill: white;");
        submitBtn.setOnAction(e -> submitOrder());
        orderPanel.getChildren().add(submitBtn);

        HBox center = new HBox(20);
        center.setAlignment(Pos.TOP_LEFT);
        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        center.getChildren().addAll(menuScroll, leftSpacer, orderPanel);

        setCenter(center);
    }

    private static class OrderLine {

        MenuItem menuItem;
        int quantity;

        OrderLine(MenuItem menuItem, int quantity) {
            this.menuItem = menuItem;
            this.quantity = quantity;
        }
    }

    private void addToOrder(MenuItem item) {
        for (OrderLine line : currentOrder) {
            if (line.menuItem.getMenuItemId() == item.getMenuItemId()) {
                line.quantity++;
                refreshOrderDisplay();
                return;
            }
        }
        currentOrder.add(new OrderLine(item, 1));
        refreshOrderDisplay();
    }

    private void refreshOrderDisplay() {
        orderItemsBox.getChildren().clear();
        for (OrderLine line : currentOrder) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Label lbl = new Label(
                    line.quantity + "x " + line.menuItem.getName() + "  $"
                            + line.menuItem.getBasePrice()
                                    .multiply(BigDecimal.valueOf(line.quantity))
                                    .setScale(2, RoundingMode.HALF_UP));
            lbl.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(lbl, Priority.ALWAYS);

            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-font-size: 11;");
            editBtn.setOnAction(e -> editQuantity(line));

            Button removeBtn = new Button("Remove");
            removeBtn.setStyle("-fx-font-size: 11; -fx-text-fill: #c00;");
            removeBtn.setOnAction(e -> removeLine(line));

            row.getChildren().addAll(lbl, editBtn, removeBtn);
            orderItemsBox.getChildren().add(row);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderLine line : currentOrder) {
            subtotal = subtotal.add(
                    line.menuItem.getBasePrice().multiply(BigDecimal.valueOf(line.quantity)));
        }
        BigDecimal taxTip = subtotal.multiply(TAX_RATE)
                .add(subtotal.multiply(TIP_RATE))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(taxTip).setScale(2, RoundingMode.HALF_UP);

        subtotalLabel.setText("Subtotal: $"
                + subtotal.setScale(2, RoundingMode.HALF_UP));
        taxTipLabel.setText("Tax/Tip: $" + taxTip);
        totalLabel.setText("TOTAL: $" + total);
    }

    private void editQuantity(OrderLine line) {
        TextInputDialog d = new TextInputDialog(String.valueOf(line.quantity));
        d.setTitle("Edit quantity");
        d.setHeaderText(line.menuItem.getName());
        d.setContentText("Quantity:");
        d.showAndWait().ifPresent(s -> {
            try {
                int q = Integer.parseInt(s.trim());
                if (q <= 0) {
                    currentOrder.remove(line);
                } else {
                    line.quantity = q;
                }
                refreshOrderDisplay();
            } catch (NumberFormatException ignored) {
            }
        });
    }

    private void removeLine(OrderLine line) {
        currentOrder.remove(line);
        refreshOrderDisplay();
    }

    private void submitOrder() {
        if (currentOrder.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "Add at least one item to the order.").showAndWait();
            return;
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderLine line : currentOrder) {
            subtotal = subtotal.add(
                    line.menuItem.getBasePrice().multiply(BigDecimal.valueOf(line.quantity)));
        }
        BigDecimal taxTip = subtotal.multiply(TAX_RATE)
                .add(subtotal.multiply(TIP_RATE))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(taxTip).setScale(2, RoundingMode.HALF_UP);

        long orderId = System.currentTimeMillis();
        SalesOrder order = new SalesOrder(orderId,
                new Timestamp(System.currentTimeMillis()), total, "Cash");
        for (OrderLine line : currentOrder) {
            SalesOrderItem oi = new SalesOrderItem(
                    nextOrderItemId++, orderId, line.menuItem.getMenuItemId(),
                    line.quantity, line.menuItem.getBasePrice());
            order.addOrderItem(oi);
        }

        String result = controller.processOrder(order);
        if (result.contains("success")) {
            currentOrder.clear();
            refreshOrderDisplay();
            new Alert(Alert.AlertType.INFORMATION,
                    "Order submitted. Total: $" + total).showAndWait();
        } else {
            new Alert(Alert.AlertType.ERROR, result).showAndWait();
        }
    }
}

