package com.hackmatch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A plain Java object that represents one hackathon row from the database. */
public class Hackathon {
    private int id;
    private String name;
    private String organizer;
    private String domain;
    private double prize;
    private LocalDate deadline;
    private String mode;
    private int minTeamSize;
    private int maxTeamSize;
    private List<String> requiredSkills;
    private String problemStatement;
    private String eligibility;
    private String registrationUrl;

    public Hackathon() {
        this.requiredSkills = new ArrayList<>();
    }

    public Hackathon(int id, String name, String organizer, String domain, double prize,
                     LocalDate deadline, String mode, int minTeamSize, int maxTeamSize,
                     List<String> requiredSkills, String problemStatement, String eligibility,
                     String registrationUrl) {
        this.id = id;
        this.name = name;
        this.organizer = organizer;
        this.domain = domain;
        this.prize = prize;
        this.deadline = deadline;
        this.mode = mode;
        this.minTeamSize = minTeamSize;
        this.maxTeamSize = maxTeamSize;
        setRequiredSkills(requiredSkills);
        this.problemStatement = problemStatement;
        this.eligibility = eligibility;
        this.registrationUrl = registrationUrl;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOrganizer() { return organizer; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public double getPrize() { return prize; }
    public void setPrize(double prize) { this.prize = prize; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public int getMinTeamSize() { return minTeamSize; }
    public void setMinTeamSize(int minTeamSize) { this.minTeamSize = minTeamSize; }
    public int getMaxTeamSize() { return maxTeamSize; }
    public void setMaxTeamSize(int maxTeamSize) { this.maxTeamSize = maxTeamSize; }
    public List<String> getRequiredSkills() { return Collections.unmodifiableList(requiredSkills); }
    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills == null ? new ArrayList<>() : new ArrayList<>(requiredSkills);
    }
    public String getProblemStatement() { return problemStatement; }
    public void setProblemStatement(String problemStatement) { this.problemStatement = problemStatement; }
    public String getEligibility() { return eligibility; }
    public void setEligibility(String eligibility) { this.eligibility = eligibility; }
    public String getRegistrationUrl() { return registrationUrl; }
    public void setRegistrationUrl(String registrationUrl) { this.registrationUrl = registrationUrl; }

    /** Converts database names to the camelCase fields already used by frontend/script.js. */
    public Map<String, Object> toApiMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("name", name);
        result.put("organizer", organizer);
        result.put("domain", domain);
        result.put("prize", prize);
        result.put("deadline", deadline == null ? null : deadline.toString());
        result.put("mode", mode);
        result.put("teamMin", minTeamSize);
        result.put("teamMax", maxTeamSize);
        result.put("skills", requiredSkills);
        result.put("description", getShortDescription());
        result.put("problemStatement", problemStatement);
        result.put("eligibility", eligibility);
        result.put("registrationLink", registrationUrl);
        result.put("registrationUrl", registrationUrl);
        result.put("status", getStatus());
        return result;
    }

    private String getShortDescription() {
        if (problemStatement == null || problemStatement.isBlank()) {
            return "No description available.";
        }
        int sentenceEnd = problemStatement.indexOf(". ");
        return sentenceEnd >= 0 ? problemStatement.substring(0, sentenceEnd + 1) : problemStatement;
    }

    private String getStatus() {
        if (deadline == null || deadline.isBefore(LocalDate.now())) {
            return "Closed";
        }
        return deadline.isAfter(LocalDate.now().plusDays(30)) ? "Upcoming" : "Active";
    }
}
