package com.team62.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * View class for JavaFX - handles only UI presentation
 * No business logic should be here
 */
public class MainView extends BorderPane {
    private Label statusLabel;
    private Pane contentPane;
    
    public MainView() {
        initializeComponents();
        setupLayout();
    }
    
    private void initializeComponents() {
        statusLabel = new Label("Status: Ready");
        contentPane = new BorderPane();
    }
    
    private void setupLayout() {
        // Top section
        Label welcomeLabel = new Label("Welcome to Team 62 Application");
        welcomeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        setAlignment(welcomeLabel, Pos.CENTER);
        setTop(welcomeLabel);
        setMargin(welcomeLabel, new Insets(20));
        
        // Center content area
        setCenter(contentPane);
        
        // Bottom status bar
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.getChildren().add(statusLabel);
        setBottom(statusBar);
    }
    
    // View methods - only for updating UI, no business logic
    public void setStatus(String status) {
        statusLabel.setText("Status: " + status);
    }
    
    public void setContentPane(Pane pane) {
        setCenter(pane);
    }
    
    public Pane getContentPane() {
        return contentPane;
    }
}
