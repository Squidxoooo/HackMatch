package com.hackmatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Performs the small set of JDBC operations needed for bookmarks. */
public class BookmarkDAO {
    public boolean studentExists(int studentId) throws SQLException {
        String sql = "SELECT 1 FROM student_profiles WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public int createBookmark(int studentId, int hackathonId) throws SQLException {
        String sql = "INSERT INTO bookmarks (student_id, hackathon_id) VALUES (?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, studentId);
            statement.setInt(2, hackathonId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public List<Bookmark> getBookmarksByStudentId(int studentId) throws SQLException {
        String sql = "SELECT b.id AS bookmark_id, b.student_id, b.created_at, h.* "
                + "FROM bookmarks b JOIN hackathons h ON b.hackathon_id = h.id "
                + "WHERE b.student_id = ? ORDER BY b.created_at DESC";
        List<Bookmark> bookmarks = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Timestamp createdAt = resultSet.getTimestamp("created_at");
                    bookmarks.add(new Bookmark(
                            resultSet.getInt("bookmark_id"),
                            resultSet.getInt("student_id"),
                            HackathonDAO.mapHackathon(resultSet),
                            createdAt == null ? null : createdAt.toLocalDateTime()
                    ));
                }
            }
        }
        return bookmarks;
    }

    public boolean deleteBookmark(int bookmarkId) throws SQLException {
        String sql = "DELETE FROM bookmarks WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookmarkId);
            return statement.executeUpdate() == 1;
        }
    }
}
