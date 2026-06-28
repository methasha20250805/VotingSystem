package com.example.votingsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Launcher.java
 * -------------
 * New main entry point. Loads WelcomeView.fxml as the first screen the
 * user sees, then hands off to the existing VotingApp once they click
 * "Enter System". VotingApp.java itself is completely unmodified - run
 * this class instead of VotingApp directly if you want the new welcome
 * screen; running VotingApp.main() still works exactly as before.
 */
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