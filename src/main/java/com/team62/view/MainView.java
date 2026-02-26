package com.team62.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Main window shell for the POS demo.
 * Header hosts cashier/manager toggle; center swaps in role-specific views.
 */
public class MainView extends BorderPane {
    private Label statusLabel;
    private Pane contentPane;
    private Button cashierModeButton;
    private Button managerModeButton;
    
    public MainView() {
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        statusLabel = new Label("Ready");
        contentPane = new BorderPane();
        cashierModeButton = new Button("Cashier");
        managerModeButton = new Button("Manager");
        cashierModeButton.setFocusTraversable(false);
        managerModeButton.setFocusTraversable(false);
    }
    
    private void setupLayout() {
        setStyle("-fx-background-color: #f5f0eb;");
        
        // Header
        HBox header = new HBox(16);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0;");
        
        Label titleLabel = new Label("Boba Shop POS");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label viewLabel = new Label("View:");
        viewLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        
        cashierModeButton.setStyle("-fx-background-color: #222; -fx-text-fill: white;");
        managerModeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #333;");
        
        header.getChildren().addAll(titleLabel, spacer, viewLabel, cashierModeButton, managerModeButton);
        setTop(header);
        
        // Center placeholder until controller swaps real content
        VBox placeholder = new VBox(8);
        placeholder.setPadding(new Insets(24));
        placeholder.setAlignment(Pos.TOP_LEFT);
        
        Label welcome = new Label("Select Cashier or Manager view to begin.");
        welcome.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        placeholder.getChildren().addAll(welcome, new Separator(),
                new Label("Cashier: submit orders. Manager: manage menu, inventory, employees, and reports."));
        
        setCenter(placeholder);
        
        // Bottom status bar
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(8, 12, 8, 12));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #e8e4e0;");
        statusBar.getChildren().add(statusLabel);
        setBottom(statusBar);
    }
    
    public void setStatus(String status) {
        statusLabel.setText(status);
    }
    
    public void setContentPane(Pane pane) {
        contentPane = pane;
        setCenter(pane);
    }
    
    public Pane getContentPane() {
        return contentPane;
    }
    
    public Button getCashierModeButton() {
        return cashierModeButton;
    }
    
    public Button getManagerModeButton() {
        return managerModeButton;
    }
}

