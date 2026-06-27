package com.example.votingsystem;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * VotingApp.java
 * --------------
 * Main entry point. Builds a TabPane with the 4 screens required by the
 * spec:
 *   1. Import & Validate ballots.csv (with inline correction of bad rows)
 *   2. Votes Received Report for a given date
 *   3. Average age of the voter base per candidate seat
 *   4. District search (supports a trailing "*" wildcard) with text export
 *
 * Run with: java --module-path <path-to-javafx-lib> --add-modules javafx.controls votingapp.VotingApp
 * (or just run it from your IDE if the JavaFX SDK is already configured,
 * the same way you ran your Library Management System project.)
 */
public class VotingApp extends Application {

    private final DataStore dataStore = new DataStore();

    // Import tab fields, kept so loadFilesAndValidate() can update them
    private Label voterFileLabel;
    private Label candidateFileLabel;
    private Label ballotFileLabel;
    private Label importSummaryLabel;
    private TableView<Ballot> invalidTable;

    private File voterFile;
    private File candidateFile;
    private File ballotFile;

    @Override
    public void start(Stage primaryStage) {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(new Tab("1. Import & Validate", buildImportTab()));
        tabPane.getTabs().add(new Tab("2. Votes Received Report", buildVotesReceivedTab()));
        tabPane.getTabs().add(new Tab("3. Average Age per Seat", buildAverageAgeTab()));
        tabPane.getTabs().add(new Tab("4. District Search", buildDistrictSearchTab()));
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Scene scene = new Scene(tabPane, 900, 600);
        primaryStage.setTitle("Sri Lanka Voting System - Processing & Reporting");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // ======================================================================
    // TAB 1: IMPORT & VALIDATE
    // ======================================================================
    private VBox buildImportTab() {
        voterFileLabel = new Label("No file selected");
        candidateFileLabel = new Label("No file selected");
        ballotFileLabel = new Label("No file selected");
        importSummaryLabel = new Label("");

        Button chooseVoterBtn = new Button("Choose voter.csv");
        chooseVoterBtn.setOnAction(e -> {
            voterFile = chooseCsvFile("Select voter.csv");
            if (voterFile != null) voterFileLabel.setText(voterFile.getName());
        });

        Button chooseCandidateBtn = new Button("Choose candidate.csv");
        chooseCandidateBtn.setOnAction(e -> {
            candidateFile = chooseCsvFile("Select candidate.csv");
            if (candidateFile != null) candidateFileLabel.setText(candidateFile.getName());
        });

        Button chooseBallotBtn = new Button("Choose ballots.csv");
        chooseBallotBtn.setOnAction(e -> {
            ballotFile = chooseCsvFile("Select ballots.csv");
            if (ballotFile != null) ballotFileLabel.setText(ballotFile.getName());
        });

        Button loadBtn = new Button("Load Files & Validate");
        loadBtn.setOnAction(e -> loadFilesAndValidate());

        GridPane filePane = new GridPane();
        filePane.setHgap(10);
        filePane.setVgap(8);
        filePane.addRow(0, chooseVoterBtn, voterFileLabel);
        filePane.addRow(1, chooseCandidateBtn, candidateFileLabel);
        filePane.addRow(2, chooseBallotBtn, ballotFileLabel);

        invalidTable = buildInvalidBallotsTable();

        Label invalidLabel = new Label("Invalid rows (double-click a cell to fix, then it auto re-validates):");

        Button exportInvalidBtn = new Button("Export Remaining Invalid Rows to Text File");
        exportInvalidBtn.setOnAction(e -> exportList(dataStore.getInvalidBallots(), "invalid_ballots.txt"));

        VBox box = new VBox(10,
                new Label("Step 1: Choose the three CSV files exported by the Python registration app."),
                filePane,
                loadBtn,
                importSummaryLabel,
                invalidLabel,
                invalidTable,
                exportInvalidBtn);
        box.setPadding(new Insets(15));
        return box;
    }

    private TableView<Ballot> buildInvalidBallotsTable() {
        TableView<Ballot> table = new TableView<>();
        table.setEditable(true);
        table.setItems(dataStore.getInvalidBallots());

        TableColumn<Ballot, String> dateCol = editableColumn("Date", "date",
                (ballot, value) -> ballot.setDate(value));
        TableColumn<Ballot, String> voterCol = editableColumn("Voter Id", "voterId",
                (ballot, value) -> ballot.setVoterId(value));
        TableColumn<Ballot, String> candidateCol = editableColumn("Candidate Id", "candidateId",
                (ballot, value) -> ballot.setCandidateId(value));
        TableColumn<Ballot, String> seatCol = editableColumn("Seat", "candidateSeat",
                (ballot, value) -> ballot.setCandidateSeat(value));

        TableColumn<Ballot, String> reasonCol = new TableColumn<>("Error Reason");
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("errorReason"));
        reasonCol.setEditable(false);
        reasonCol.setPrefWidth(150);

        table.getColumns().add(dateCol);
        table.getColumns().add(voterCol);
        table.getColumns().add(candidateCol);
        table.getColumns().add(seatCol);
        table.getColumns().add(reasonCol);
        table.setPrefHeight(250);
        return table;
    }

    /**
     * Builds an editable String column. On edit commit, applies the new
     * value via the given setter, then asks the DataStore to re-validate
     * the row - if it now passes, DataStore automatically moves it out of
     * the invalid list (which the table is bound to), so it simply
     * disappears from view once fixed.
     */
    private TableColumn<Ballot, String> editableColumn(String title, String property,
                                                       java.util.function.BiConsumer<Ballot, String> setter) {
        TableColumn<Ballot, String> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(event -> {
            Ballot ballot = event.getRowValue();
            setter.accept(ballot, event.getNewValue());
            dataStore.classifyBallot(ballot); // re-run the 4 validation rules
        });
        col.setPrefWidth(120);
        return col;
    }

    private void loadFilesAndValidate() {
        if (voterFile == null || candidateFile == null || ballotFile == null) {
            showAlert("Please choose all three CSV files first.");
            return;
        }
        try {
            dataStore.loadVoters(voterFile);
            dataStore.loadCandidates(candidateFile);
            dataStore.loadBallots(ballotFile);
            importSummaryLabel.setText(
                    "Loaded " + dataStore.getVoters().size() + " voters, "
                            + dataStore.getCandidates().size() + " candidates. "
                            + dataStore.getValidBallots().size() + " valid ballots, "
                            + dataStore.getInvalidBallots().size() + " invalid ballots need correction.");
        } catch (IOException e) {
            showAlert("Failed to load files: " + e.getMessage());
        }
    }

    // ======================================================================
    // TAB 2: VOTES RECEIVED REPORT (for a given date)
    // ======================================================================
    private VBox buildVotesReceivedTab() {
        DatePicker datePicker = new DatePicker();
        Button generateBtn = new Button("Generate Report");

        TableView<Map.Entry<String, Integer>> table = new TableView<>();
        TableColumn<Map.Entry<String, Integer>, String> nameCol = new TableColumn<>("Candidate");
        nameCol.setCellValueFactory(e -> new javafx.beans.property.SimpleStringProperty(e.getValue().getKey()));
        TableColumn<Map.Entry<String, Integer>, String> votesCol = new TableColumn<>("Votes Received");
        votesCol.setCellValueFactory(e -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(e.getValue().getValue())));
        nameCol.setPrefWidth(250);
        votesCol.setPrefWidth(150);
        table.getColumns().add(nameCol);
        table.getColumns().add(votesCol);

        generateBtn.setOnAction(e -> {
            if (datePicker.getValue() == null) {
                showAlert("Please pick a date.");
                return;
            }
            String date = datePicker.getValue().toString(); // yyyy-mm-dd
            Map<String, Integer> counts = dataStore.votesReceivedOnDate(date);
            ObservableList<Map.Entry<String, Integer>> rows = FXCollections.observableArrayList(counts.entrySet());
            table.setItems(rows);
        });

        HBox controls = new HBox(10, new Label("Date:"), datePicker, generateBtn);
        VBox box = new VBox(15, controls, table);
        box.setPadding(new Insets(15));
        return box;
    }

    // ======================================================================
    // TAB 3: AVERAGE AGE OF VOTER BASE PER CANDIDATE SEAT
    // ======================================================================
    private VBox buildAverageAgeTab() {
        Button calcBtn = new Button("Calculate Average Age per Seat");

        TableView<Map.Entry<String, Double>> table = new TableView<>();
        TableColumn<Map.Entry<String, Double>, String> seatCol = new TableColumn<>("Seat Number");
        seatCol.setCellValueFactory(e -> new javafx.beans.property.SimpleStringProperty(e.getValue().getKey()));
        TableColumn<Map.Entry<String, Double>, String> avgCol = new TableColumn<>("Average Voter Age");
        avgCol.setCellValueFactory(e -> new javafx.beans.property.SimpleStringProperty(
                String.format("%.2f", e.getValue().getValue())));
        seatCol.setPrefWidth(150);
        avgCol.setPrefWidth(200);
        table.getColumns().add(seatCol);
        table.getColumns().add(avgCol);

        calcBtn.setOnAction(e -> {
            Map<String, Double> averages = dataStore.averageAgePerSeat();
            table.setItems(FXCollections.observableArrayList(averages.entrySet()));
        });

        VBox box = new VBox(15, calcBtn, table);
        box.setPadding(new Insets(15));
        return box;
    }

    // ======================================================================
    // TAB 4: SEARCH BALLOTS BY DISTRICT (supports trailing "*" wildcard)
    // ======================================================================
    private VBox buildDistrictSearchTab() {
        TextField districtField = new TextField();
        districtField.setPromptText("e.g. Colombo  or  Col*");
        Button searchBtn = new Button("Search");
        Button exportBtn = new Button("Export Results to Text File");

        TableView<Ballot> resultsTable = new TableView<>();
        TableColumn<Ballot, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<Ballot, String> voterCol = new TableColumn<>("Voter Id");
        voterCol.setCellValueFactory(new PropertyValueFactory<>("voterId"));
        TableColumn<Ballot, String> candidateCol = new TableColumn<>("Candidate Id");
        candidateCol.setCellValueFactory(new PropertyValueFactory<>("candidateId"));
        TableColumn<Ballot, String> seatCol = new TableColumn<>("Seat");
        seatCol.setCellValueFactory(new PropertyValueFactory<>("candidateSeat"));
        TableColumn<Ballot, String> ageCol = new TableColumn<>("Voter Age");
        ageCol.setCellValueFactory(new PropertyValueFactory<>("voterAge"));
        TableColumn<Ballot, String> districtCol = new TableColumn<>("District");
        districtCol.setCellValueFactory(new PropertyValueFactory<>("district"));

        resultsTable.getColumns().add(dateCol);
        resultsTable.getColumns().add(voterCol);
        resultsTable.getColumns().add(candidateCol);
        resultsTable.getColumns().add(seatCol);
        resultsTable.getColumns().add(ageCol);
        resultsTable.getColumns().add(districtCol);

        searchBtn.setOnAction(e -> {
            String pattern = districtField.getText().trim();
            List<Ballot> results = dataStore.searchByDistrict(pattern);
            resultsTable.setItems(FXCollections.observableArrayList(results));
        });

        exportBtn.setOnAction(e -> exportList(resultsTable.getItems(), "district_search_results.txt"));

        HBox controls = new HBox(10, new Label("District:"), districtField, searchBtn, exportBtn);
        Label hint = new Label("Tip: end your search with * to match a prefix, e.g. \"Col*\" matches Colombo.");
        VBox box = new VBox(10, controls, hint, resultsTable);
        box.setPadding(new Insets(15));
        return box;
    }

    // ======================================================================
    // Shared helpers
    // ======================================================================
    private File chooseCsvFile(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        return chooser.showOpenDialog(null);
    }

    private void exportList(List<Ballot> ballots, String defaultFileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export to Text File");
        chooser.setInitialFileName(defaultFileName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = chooser.showSaveDialog(null);
        if (file == null) return;
        try {
            dataStore.exportBallotsToTextFile(ballots, file);
            showAlert("Exported " + ballots.size() + " record(s) to " + file.getName());
        } catch (IOException e) {
            showAlert("Export failed: " + e.getMessage());
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
