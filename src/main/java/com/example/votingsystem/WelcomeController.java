package com.example.votingsystem;

import javafx.fxml.FXML;
import javafx.stage.Stage;

/**
 * WelcomeController.java
 * -----------------------
 * Controller for WelcomeView.fxml. Its only job is to swap the welcome
 * screen out for the existing VotingApp UI when the user clicks
 * "Enter System". It does NOT reimplement any screen logic - it simply
 * calls the unmodified VotingApp.start(Stage) method, reusing every
 * import/report/search screen exactly as already built.
 */
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