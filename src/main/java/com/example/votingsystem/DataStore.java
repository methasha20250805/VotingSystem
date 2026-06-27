package com.example.votingsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * DataStore.java
 * --------------
 * Loads voter.csv, candidate.csv and ballots.csv (produced by the Python
 * registration app) into memory, validates ballots against the 4 rules
 * required by the spec, and provides the queries needed by the report
 * screens (votes received, average age per seat, district search).
 *
 * Design note: voters/candidates are kept in HashMaps keyed by id for
 * O(1) lookup during validation of (potentially) thousands of ballot rows.
 */
public class DataStore {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Map<String, Voter> voters = new HashMap<>();
    private final Map<String, Candidate> candidates = new HashMap<>();

    // Ballots that passed validation
    private final ObservableList<Ballot> validBallots = FXCollections.observableArrayList();
    // Ballots that failed validation on import - shown to the user for correction
    private final ObservableList<Ballot> invalidBallots = FXCollections.observableArrayList();

    // ------------------------------------------------------------------
    // Loading voter.csv and candidate.csv (assumed clean, per spec these
    // are "preloaded with valid data" - only ballots.csv needs validation)
    // ------------------------------------------------------------------
    public void loadVoters(File file) throws IOException {
        voters.clear();
        List<String> lines = Files.readAllLines(file.toPath());
        for (int i = 1; i < lines.size(); i++) { // skip header
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",");
            if (parts.length < 3) continue;
            String voterId = parts[0].trim();
            String district = parts[1].trim();
            int age;
            try {
                age = Integer.parseInt(parts[2].trim());
            } catch (NumberFormatException e) {
                age = 18; // fall back to default if malformed
            }
            voters.put(voterId, new Voter(voterId, district, age));
        }
    }

    public void loadCandidates(File file) throws IOException {
        candidates.clear();
        List<String> lines = Files.readAllLines(file.toPath());
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",");
            if (parts.length < 3) continue;
            String id = parts[0].trim();
            String name = parts[1].trim();
            String seat = parts[2].trim();
            candidates.put(id, new Candidate(id, name, seat));
        }
    }

    // ------------------------------------------------------------------
    // Loading ballots.csv WITH validation (the focus of requirement 1)
    // ------------------------------------------------------------------
    public void loadBallots(File file) throws IOException {
        validBallots.clear();
        invalidBallots.clear();

        List<String> lines = Files.readAllLines(file.toPath());
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",");
            // Pad missing columns so a short row still becomes an editable
            // invalid record rather than crashing the import.
            String date = parts.length > 0 ? parts[0].trim() : "";
            String voterId = parts.length > 1 ? parts[1].trim() : "";
            String candidateId = parts.length > 2 ? parts[2].trim() : "";
            String candidateSeat = parts.length > 3 ? parts[3].trim() : "";
            String voterAge = parts.length > 4 ? parts[4].trim() : "";
            String district = parts.length > 5 ? parts[5].trim() : "";

            Ballot ballot = new Ballot(date, voterId, candidateId, candidateSeat, voterAge, district);
            classifyBallot(ballot);
        }
    }

    /**
     * Runs all 4 validation rules against a ballot. If it passes, fills in
     * voterAge/district from the matching Voter and files it under valid;
     * otherwise files it under invalid with a human-readable reason.
     * Used both during initial import and after the user edits a row.
     */
    public void classifyBallot(Ballot ballot) {
        String error = validate(ballot);
        if (error == null) {
            // Auto-fill age/district from the voter record, since these
            // are derived fields, not something the user should retype.
            Voter v = voters.get(ballot.getVoterId());
            if (v != null) {
                ballot.setVoterAge(String.valueOf(v.getAge()));
                ballot.setDistrict(v.getDistrict());
            }
            ballot.setErrorReason("");
            if (!validBallots.contains(ballot)) validBallots.add(ballot);
            invalidBallots.remove(ballot);
        } else {
            ballot.setErrorReason(error);
            if (!invalidBallots.contains(ballot)) invalidBallots.add(ballot);
            validBallots.remove(ballot);
        }
    }

    /** Returns null if valid, or an error message describing the first failed rule. */
    public String validate(Ballot ballot) {
        // a) Invalid voter id
        String voterId = ballot.getVoterId();
        if (voterId == null || !voterId.matches("\\d{10}") || !voters.containsKey(voterId)) {
            return "Invalid voter id";
        }

        // b) Invalid candidate id
        String candidateId = ballot.getCandidateId();
        Candidate candidate = candidates.get(candidateId);
        if (candidateId == null || !candidateId.matches("\\d{10}") || candidate == null) {
            return "Invalid candidate id";
        }

        // c) Invalid seat number - must match the seat registered for that candidate
        String seat = ballot.getCandidateSeat();
        if (seat == null || !seat.equals(candidate.getSeatNumber())) {
            return "Invalid seat number";
        }

        // d) Invalid date - must be yyyy-mm-dd
        try {
            LocalDate.parse(ballot.getDate(), DATE_FORMAT);
        } catch (DateTimeParseException | NullPointerException e) {
            return "Invalid date";
        }

        return null;
    }

    // ------------------------------------------------------------------
    // Report 2: Votes Received Report for a given date
    // ------------------------------------------------------------------
    public Map<String, Integer> votesReceivedOnDate(String date) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Ballot b : validBallots) {
            if (b.getDate().equals(date)) {
                String name = candidates.containsKey(b.getCandidateId())
                        ? candidates.get(b.getCandidateId()).getFirstName()
                        : b.getCandidateId();
                counts.merge(name, 1, Integer::sum);
            }
        }
        return counts;
    }

    // ------------------------------------------------------------------
    // Report 3: Average age of the voter base per candidate seat
    // ------------------------------------------------------------------
    public Map<String, Double> averageAgePerSeat() {
        Map<String, Integer> totalAge = new TreeMap<>();
        Map<String, Integer> count = new TreeMap<>();

        for (Ballot b : validBallots) {
            String seat = b.getCandidateSeat();
            int age;
            try {
                age = Integer.parseInt(b.getVoterAge());
            } catch (NumberFormatException e) {
                continue;
            }
            totalAge.merge(seat, age, Integer::sum);
            count.merge(seat, 1, Integer::sum);
        }

        Map<String, Double> averages = new TreeMap<>();
        for (String seat : totalAge.keySet()) {
            averages.put(seat, totalAge.get(seat) / (double) count.get(seat));
        }
        return averages;
    }

    // ------------------------------------------------------------------
    // Report 4: Search ballots by district, with trailing wildcard "*"
    // ------------------------------------------------------------------
    public List<Ballot> searchByDistrict(String pattern) {
        List<Ballot> results = new ArrayList<>();
        if (pattern == null || pattern.isEmpty()) return results;

        boolean wildcard = pattern.endsWith("*");
        String prefix = wildcard ? pattern.substring(0, pattern.length() - 1) : pattern;

        for (Ballot b : validBallots) {
            String district = b.getDistrict();
            if (district == null) continue;
            boolean matches = wildcard
                    ? district.toLowerCase().startsWith(prefix.toLowerCase())
                    : district.equalsIgnoreCase(prefix);
            if (matches) results.add(b);
        }
        return results;
    }

    public void exportBallotsToTextFile(List<Ballot> ballots, File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Date,VoterID,CandidateID,CandidateSeat,VoterAge,District");
            for (Ballot b : ballots) {
                writer.println(b.toCsvRow());
            }
        }
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------
    public ObservableList<Ballot> getValidBallots() { return validBallots; }
    public ObservableList<Ballot> getInvalidBallots() { return invalidBallots; }
    public Map<String, Voter> getVoters() { return voters; }
    public Map<String, Candidate> getCandidates() { return candidates; }
}