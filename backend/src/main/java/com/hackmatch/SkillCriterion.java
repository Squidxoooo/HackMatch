package com.hackmatch;

import java.util.ArrayList;
import java.util.List;

/** Awards up to 30 points according to matching required skills. */
public class SkillCriterion implements MatchCriterion {
    private static final int MAX_POINTS = 30;

    @Override
    public int calculateScore(StudentProfile student, Hackathon hackathon, List<String> reasons) {
        List<String> matchedSkills = new ArrayList<>();
        for (String requiredSkill : hackathon.getRequiredSkills()) {
            for (String studentSkill : student.getSkills()) {
                if (requiredSkill.trim().equalsIgnoreCase(studentSkill.trim())) {
                    matchedSkills.add(requiredSkill);
                    break;
                }
            }
        }

        if (matchedSkills.isEmpty() || hackathon.getRequiredSkills().isEmpty()) {
            return 0;
        }

        int score = (int) Math.round((double) MAX_POINTS * matchedSkills.size()
                / hackathon.getRequiredSkills().size());
        reasons.add("Matching skills: " + String.join(", ", matchedSkills) + ".");
        return score;
    }
}
