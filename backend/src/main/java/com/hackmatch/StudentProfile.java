package com.hackmatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Stores the preferences sent by a student for one recommendation request. */
public class StudentProfile {
    private int id;
    private String name;
    private List<String> skills;
    private List<String> interests;
    private String experienceLevel;
    private double preferredMinPrize;
    private int teamSize;

    public StudentProfile() {
        this.skills = new ArrayList<>();
        this.interests = new ArrayList<>();
    }

    public StudentProfile(String name, List<String> skills, List<String> interests,
                          String experienceLevel, double preferredMinPrize, int teamSize) {
        this();
        this.name = name;
        setSkills(skills);
        setInterests(interests);
        this.experienceLevel = experienceLevel;
        this.preferredMinPrize = preferredMinPrize;
        this.teamSize = teamSize;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getSkills() { return Collections.unmodifiableList(skills); }
    public void setSkills(List<String> skills) { this.skills = skills == null ? new ArrayList<>() : new ArrayList<>(skills); }
    public List<String> getInterests() { return Collections.unmodifiableList(interests); }
    public void setInterests(List<String> interests) { this.interests = interests == null ? new ArrayList<>() : new ArrayList<>(interests); }
    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }
    public double getPreferredMinPrize() { return preferredMinPrize; }
    public void setPreferredMinPrize(double preferredMinPrize) { this.preferredMinPrize = preferredMinPrize; }
    public int getTeamSize() { return teamSize; }
    public void setTeamSize(int teamSize) { this.teamSize = teamSize; }
}
