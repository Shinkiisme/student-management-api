package com.phuc.studentmanagement.repository;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.phuc.studentmanagement.model.Role;
import com.phuc.studentmanagement.model.User;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> new User(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            Role.valueOf(rs.getString("role")),
            rs.getObject("student_id", Integer.class),
            rs.getBoolean("enabled"));

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByUsername(String username) {
        String sql = """
                SELECT id, username, password_hash, role, student_id, enabled
                FROM users
                WHERE username = ?
                """;

        return jdbcTemplate.query(
                sql,
                USER_ROW_MAPPER,
                username)
                .stream()
                .findFirst();
    }

    public Optional<User> findById(int id) {
        String sql = """
                SELECT id, username, password_hash, role, student_id, enabled
                FROM users
                WHERE id = ?
                """;

        return jdbcTemplate.query(
                sql,
                USER_ROW_MAPPER,
                id)
                .stream()
                .findFirst();
    }
}