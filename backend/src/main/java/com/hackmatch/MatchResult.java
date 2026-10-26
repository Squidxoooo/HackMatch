package com.hackmatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Holds one recommendation, its total score, and human-readable reasons. */
public class MatchResult {
    private final Hackathon hackathon;
    private int score;
    private final List<String> reasons;

    public MatchResult(Hackathon hackathon) {
        this.hackathon = hackathon;
        this.reasons = new ArrayList<>();
    }

    public Hackathon getHackathon() { return hackathon; }
    public int getScore() { return score; }
    public List<String> getReasons() { return Collections.unmodifiableList(reasons); }

    /** A result cannot grow beyond the documented score of 100. */
    public void addPoints(int points) {
        score = Math.min(100, score + Math.max(0, points));
    }

    public void addReason(String reason) {
        if (reason != null && !reason.isBlank()) {
            reasons.add(reason);
        }
    }
}
