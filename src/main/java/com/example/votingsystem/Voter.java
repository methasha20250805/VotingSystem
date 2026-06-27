package com.example.votingsystem;

public class Voter {
    private String voterId;
    private String district;
    private int age;

    public Voter(String voterId, String district, int age) {
        this.voterId = voterId;
        this.district = district;
        this.age = age;
    }

    public String getVoterId() { return voterId; }
    public String getDistrict() { return district; }
    public int getAge() { return age; }

    public void setDistrict(String district) { this.district = district; }
    public void setAge(int age) { this.age = age; }

    public String toCsvRow() {
        return voterId + "," + district + "," + age;
    }

    @Override
    public String toString() {
        return "VoterID: " + voterId + " | District: " + district + " | Age: " + age;
    }
}

