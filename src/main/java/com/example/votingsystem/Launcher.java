package com.example.votingsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Launcher extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("WelcomeView.fxml"));
        Parent root = loader.load();

        WelcomeController controller = loader.getController();
        controller.setStage(primaryStage);

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("welcome.css").toExternalForm());

        primaryStage.setTitle("Sri Lanka Voting System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}