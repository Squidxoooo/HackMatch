package com.hackmatch;

import java.time.LocalDateTime;

/** Represents one saved hackathon for a student. */
public class Bookmark {
    private final int id;
    private final int studentId;
    private final Hackathon hackathon;
    private final LocalDateTime createdAt;

    public Bookmark(int id, int studentId, Hackathon hackathon, LocalDateTime createdAt) {
        this.id = id;
        this.studentId = studentId;
        this.hackathon = hackathon;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public Hackathon getHackathon() { return hackathon; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
