package com.hackmatch;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Rule-based recommendation service. It does not use AI or machine learning. */
public class MatchingService {
    private final List<MatchCriterion> criteria;

    public MatchingService() {
        criteria = new ArrayList<>();
        criteria.add(new SkillCriterion());
        criteria.add(new InterestCriterion());
        criteria.add(new ProblemStatementCriterion());
    }

    public List<MatchResult> getRecommendations(StudentProfile student, List<Hackathon> hackathons) {
        List<MatchResult> results = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Hackathon hackathon : hackathons) {
            // Registration is no longer possible after the deadline.
            if (hackathon.getDeadline() == null || hackathon.getDeadline().isBefore(today)) {
                continue;
            }

            MatchResult result = new MatchResult(hackathon);
            for (MatchCriterion criterion : criteria) {
                List<String> criterionReasons = new ArrayList<>();
                result.addPoints(criterion.calculateScore(student, hackathon, criterionReasons));
                for (String reason : criterionReasons) {
                    result.addReason(reason);
                }
            }

            result.addPoints(calculatePrizeScore(student, hackathon, result));
            result.addPoints(calculateDeadlineScore(hackathon, result));
            result.addPoints(calculateTeamSizeScore(student, hackathon, result));
            results.add(result);
        }

        results.sort(Comparator.comparingInt(MatchResult::getScore).reversed()
                .thenComparing(result -> result.getHackathon().getName()));
        return results;
    }

    private int calculatePrizeScore(StudentProfile student, Hackathon hackathon, MatchResult result) {
        double preferredPrize = student.getPreferredMinPrize();
        if (preferredPrize <= 0 || hackathon.getPrize() >= preferredPrize) {
            result.addReason("Prize of INR " + Math.round(hackathon.getPrize()) + " meets your minimum preference.");
            return 10;
        }
        return (int) Math.round(10 * hackathon.getPrize() / preferredPrize);
    }

    private int calculateDeadlineScore(Hackathon hackathon, MatchResult result) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), hackathon.getDeadline());
        if (daysLeft >= 14) {
            result.addReason("There are " + daysLeft + " days left before registration closes.");
            return 10;
        }
        if (daysLeft >= 7) {
            result.addReason("Registration is still open for " + daysLeft + " more days.");
            return 7;
        }
        if (daysLeft >= 0) {
            result.addReason("Registration closes soon, in " + daysLeft + " days.");
            return 4;
        }
        return 0;
    }

    private int calculateTeamSizeScore(StudentProfile student, Hackathon hackathon, MatchResult result) {
        if (student.getTeamSize() >= hackathon.getMinTeamSize()
                && student.getTeamSize() <= hackathon.getMaxTeamSize()) {
            result.addReason("Your preferred team size fits the " + hackathon.getMinTeamSize()
                    + "-" + hackathon.getMaxTeamSize() + " member requirement.");
            return 5;
        }
        return 0;
    }
}
