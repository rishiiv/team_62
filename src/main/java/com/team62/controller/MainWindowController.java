package com.team62.controller;

import com.team62.view.CashierView;
import com.team62.view.MainView;
import com.team62.view.ManagerView;
import javafx.scene.layout.Pane;

/**
 * Controller for Main Window.
 * Coordinates between the View and the business logic Controller; switches Cashier/Manager content.
 */
public class MainWindowController {
    private static final String BTN_ACTIVE = "-fx-background-color: #222; -fx-text-fill: white;";
    private static final String BTN_INACTIVE = "-fx-background-color: transparent; -fx-text-fill: #333;";

    private final MainView view;
    private final MainController controller;
    private final Pane cashierPane;
    private final Pane managerPane;
    
    public MainWindowController(MainView view, MainController controller) {
        this.view = view;
        this.controller = controller;
        this.cashierPane = new CashierView(controller);
        this.managerPane = new ManagerView(controller);
        setupEventHandlers();
        view.setContentPane(cashierPane);
        setActiveMode(true);
    }
    
    private void setupEventHandlers() {
        view.getCashierModeButton().setOnAction(e -> {
            view.setContentPane(cashierPane);
            ((CashierView) cashierPane).refreshMenu();
            setActiveMode(true);
            updateStatus("Cashier — submit orders");
        });
        
        view.getManagerModeButton().setOnAction(e -> {
            view.setContentPane(managerPane);
            setActiveMode(false);
            updateStatus("Manager — menu, inventory, employees, reports");
        });
        
        view.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                updateStatus("Boba Shop POS ready");
            }
        });
    }
    
    private void setActiveMode(boolean cashierActive) {
        view.getCashierModeButton().setStyle(cashierActive ? BTN_ACTIVE : BTN_INACTIVE);
        view.getManagerModeButton().setStyle(cashierActive ? BTN_INACTIVE : BTN_ACTIVE);
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

