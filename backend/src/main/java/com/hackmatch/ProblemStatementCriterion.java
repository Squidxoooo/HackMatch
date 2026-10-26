package com.hackmatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Awards up to 20 points for meaningful preference keywords found in the problem statement. */
public class ProblemStatementCriterion implements MatchCriterion {
    private static final int MAX_POINTS = 20;
    private static final Set<String> COMMON_WORDS = Set.of("and", "the", "for", "with", "from", "development", "open");

    @Override
    public int calculateScore(StudentProfile student, Hackathon hackathon, List<String> reasons) {
        Set<String> profileKeywords = new LinkedHashSet<>();
        addKeywords(profileKeywords, student.getSkills());
        addKeywords(profileKeywords, student.getInterests());

        if (profileKeywords.isEmpty() || hackathon.getProblemStatement() == null) {
            return 0;
        }

        Set<String> statementWords = new HashSet<>();
        for (String word : hackathon.getProblemStatement().toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (!word.isBlank()) {
                statementWords.add(word);
            }
        }

        List<String> matchedKeywords = new ArrayList<>();
        for (String keyword : profileKeywords) {
            if (statementWords.contains(keyword)) {
                matchedKeywords.add(keyword);
            }
        }

        if (matchedKeywords.isEmpty()) {
            return 0;
        }

        int score = (int) Math.round((double) MAX_POINTS * matchedKeywords.size() / profileKeywords.size());
        reasons.add("Problem-statement keywords found: " + String.join(", ", matchedKeywords) + ".");
        return score;
    }

    private void addKeywords(Set<String> target, List<String> values) {
        for (String value : values) {
            for (String word : value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
                if (word.length() >= 3 && !COMMON_WORDS.contains(word)) {
                    target.add(word);
                }
            }
        }
    }
}
