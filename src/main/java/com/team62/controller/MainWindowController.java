package com.team62.controller;

import com.team62.view.MainView;

/**
 * Controller for Main Window
 * Coordinates between the View and the business logic Controller
 */
public class MainWindowController {
    private MainView view;
    private MainController controller;
    
    public MainWindowController(MainView view, MainController controller) {
        this.view = view;
        this.controller = controller;
        setupEventHandlers();
    }
    
    private void setupEventHandlers() {
        // Set up event handlers for view components
        // This connects UI events to business logic
        
        // Example: Update status when view is shown
        view.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                updateStatus("Application loaded");
            }
        });
    }
    
    public void updateStatus(String status) {
        view.setStatus(status);
    }
    
    public MainView getView() {
        return view;
    }
    
    public MainController getController() {
        return controller;
    }
}
