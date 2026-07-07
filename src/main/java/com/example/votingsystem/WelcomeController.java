package com.example.votingsystem;

import javafx.fxml.FXML;
import javafx.stage.Stage;


public class WelcomeController {

    private Stage stage;

    /** Called by Launcher right after loading the FXML, so we have a Stage to hand off to. */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void onEnterSystem() {
        VotingApp votingApp = new VotingApp();
        votingApp.start(stage); // reuses VotingApp's existing TabPane UI as-is
    }
}