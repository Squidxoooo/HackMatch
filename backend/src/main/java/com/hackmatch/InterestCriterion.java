package com.hackmatch;

import java.util.List;

/** Awards 25 points when one selected interest equals the hackathon domain. */
public class InterestCriterion implements MatchCriterion {
    private static final int MAX_POINTS = 25;

    @Override
    public int calculateScore(StudentProfile student, Hackathon hackathon, List<String> reasons) {
        for (String interest : student.getInterests()) {
            if (interest.trim().equalsIgnoreCase(hackathon.getDomain().trim())) {
                reasons.add("Your interest in " + hackathon.getDomain() + " matches the hackathon domain.");
                return MAX_POINTS;
            }
        }
        return 0;
    }
}
