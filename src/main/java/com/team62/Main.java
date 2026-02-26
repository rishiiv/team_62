package com.team62;

import com.team62.controller.MainController;
import com.team62.controller.MainWindowController;
import com.team62.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for JavaFX GUI application
 * Uses MVC pattern: Model-View-Controller
 */
public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        // MVC Setup
        MainView view = new MainView();
        MainController controller = new MainController();
        MainWindowController windowController = new MainWindowController(view, controller);
        
        Scene scene = new Scene(view, 800, 600);
        
        primaryStage.setTitle("Team 62 Application");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(300);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
