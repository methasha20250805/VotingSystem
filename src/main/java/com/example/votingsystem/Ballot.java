package com.example.votingsystem;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Ballot.java
 * -----------
 * Data holder for a ballot record. Uses JavaFX StringProperty fields
 * (rather than plain Strings) so that the Import screen's TableView can
 * bind directly to them and let the user edit invalid rows in place.
 *
 * voterAge and district are looked up from the matching Voter record
 * once the row is valid; until then they may be blank.
 */
public class Ballot {
    private final StringProperty date = new SimpleStringProperty();
    private final StringProperty voterId = new SimpleStringProperty();
    private final StringProperty candidateId = new SimpleStringProperty();
    private final StringProperty candidateSeat = new SimpleStringProperty();
    private final StringProperty voterAge = new SimpleStringProperty();
    private final StringProperty district = new SimpleStringProperty();

    // Not persisted to CSV - used only to show the user why a row failed import
    private final StringProperty errorReason = new SimpleStringProperty("");

    public Ballot(String date, String voterId, String candidateId, String candidateSeat,
                  String voterAge, String district) {
        this.date.set(date);
        this.voterId.set(voterId);
        this.candidateId.set(candidateId);
        this.candidateSeat.set(candidateSeat);
        this.voterAge.set(voterAge);
        this.district.set(district);
    }

    // ----- date -----
    public String getDate() { return date.get(); }
    public void setDate(String v) { date.set(v); }
    public StringProperty dateProperty() { return date; }

    // ----- voterId -----
    public String getVoterId() { return voterId.get(); }
    public void setVoterId(String v) { voterId.set(v); }
    public StringProperty voterIdProperty() { return voterId; }

    // ----- candidateId -----
    public String getCandidateId() { return candidateId.get(); }
    public void setCandidateId(String v) { candidateId.set(v); }
    public StringProperty candidateIdProperty() { return candidateId; }

    // ----- candidateSeat -----
    public String getCandidateSeat() { return candidateSeat.get(); }
    public void setCandidateSeat(String v) { candidateSeat.set(v); }
    public StringProperty candidateSeatProperty() { return candidateSeat; }

    // ----- voterAge -----
    public String getVoterAge() { return voterAge.get(); }
    public void setVoterAge(String v) { voterAge.set(v); }
    public StringProperty voterAgeProperty() { return voterAge; }

    // ----- district -----
    public String getDistrict() { return district.get(); }
    public void setDistrict(String v) { district.set(v); }
    public StringProperty districtProperty() { return district; }

    // ----- errorReason (import screen only) -----
    public String getErrorReason() { return errorReason.get(); }
    public void setErrorReason(String v) { errorReason.set(v); }
    public StringProperty errorReasonProperty() { return errorReason; }

    public String toCsvRow() {
        return date.get() + "," + voterId.get() + "," + candidateId.get() + ","
                + candidateSeat.get() + "," + voterAge.get() + "," + district.get();
    }

    @Override
    public String toString() {
        return "Date: " + date.get() + " | VoterID: " + voterId.get()
                + " | CandidateID: " + candidateId.get() + " | Seat: " + candidateSeat.get();
    }
}