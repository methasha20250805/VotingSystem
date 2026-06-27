package com.example.votingsystem;

public class Candidate {
    private String candidateId;
    private String firstName;
    private String seatNumber;

    public Candidate(String candidateId, String firstName, String seatNumber) {
        this.candidateId = candidateId;
        this.firstName = firstName;
        this.seatNumber = seatNumber;
    }

    public String getCandidateId() { return candidateId; }
    public String getFirstName() { return firstName; }
    public String getSeatNumber() { return seatNumber; }

    public String toCsvRow() {
        return candidateId + "," + firstName + "," + seatNumber;
    }

    @Override
    public String toString() {
        return "CandidateID: " + candidateId + " | Name: " + firstName + " | Seat No: " + seatNumber;
    }
}