package com.phuc.studentmanagement.model;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private Role role;
    private Integer studentId;
    private boolean enabled;

    public User(
            int id,
            String username,
            String passwordHash,
            Role role,
            Integer studentId,
            boolean enabled) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.studentId = studentId;
        this.enabled = enabled;
    }

    public int getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public Role getRole() {
        return this.role;
    }

    public Integer getStudentId() {
        return this.studentId;
    }

    public boolean isEnabled() {
        return this.enabled;
    }
}