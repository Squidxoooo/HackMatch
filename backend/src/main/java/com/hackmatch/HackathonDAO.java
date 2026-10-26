package com.hackmatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Performs JDBC queries for hackathon data. */
public class HackathonDAO {
    public List<Hackathon> getAllHackathons() throws SQLException {
        String sql = "SELECT * FROM hackathons ORDER BY deadline ASC, name ASC";
        List<Hackathon> hackathons = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                hackathons.add(mapHackathon(resultSet));
            }
        }
        return hackathons;
    }

    public Hackathon getHackathonById(int id) throws SQLException {
        String sql = "SELECT * FROM hackathons WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapHackathon(resultSet) : null;
            }
        }
    }

    public boolean canConnect() throws SQLException {
        try (Connection ignored = DBConnection.getConnection()) {
            return true;
        }
    }

    static Hackathon mapHackathon(ResultSet resultSet) throws SQLException {
        String skills = resultSet.getString("required_skills");
        List<String> skillList = skills == null || skills.isBlank() ? new ArrayList<>()
                : Arrays.stream(skills.split(","))
                    .map(String::trim)
                    .filter(skill -> !skill.isEmpty())
                    .collect(Collectors.toCollection(ArrayList::new));

        return new Hackathon(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("organizer"),
                resultSet.getString("domain"),
                resultSet.getDouble("prize"),
                resultSet.getDate("deadline").toLocalDate(),
                resultSet.getString("mode"),
                resultSet.getInt("min_team_size"),
                resultSet.getInt("max_team_size"),
                skillList,
                resultSet.getString("problem_statement"),
                resultSet.getString("eligibility"),
                resultSet.getString("registration_url")
        );
    }
}
