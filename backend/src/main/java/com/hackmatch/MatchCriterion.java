package com.hackmatch;

import java.util.List;

/** Abstraction for one independent part of the recommendation score. */
public interface MatchCriterion {
    int calculateScore(StudentProfile student, Hackathon hackathon, List<String> reasons);
}
