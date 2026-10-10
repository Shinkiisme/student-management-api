package com.phuc.studentmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.phuc.studentmanagement.model.Course;

@Repository
public class CourseRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final RowMapper<Course> COURSE_ROW_MAPPER = (rs, rowNum) -> new Course(
            rs.getInt("id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getInt("credits"));

    public CourseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Course> findAll() {
        String sql = """
                SELECT id, code, name, credits
                FROM courses
                ORDER BY id
                """;

        return jdbcTemplate.query(sql, COURSE_ROW_MAPPER);
    }

    public Optional<Course> findById(int id) {
        String sql = """
                SELECT id, code, name, credits
                FROM courses
                WHERE id = ?
                """;

        return jdbcTemplate.query(sql, COURSE_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    public Course insert(Course course) {
        String sql = """
                INSERT INTO courses(code, name, credits)
                VALUES (?, ?, ?)
                RETURNING id, code, name, credits
                """;

        return jdbcTemplate.queryForObject(sql,
                COURSE_ROW_MAPPER,
                course.getCode(),
                course.getName(),
                course.getCredits());
    }

    public Optional<Course> update(int id, Course course) {
        String sql = """
                UPDATE courses
                SET code = ?, name = ?, credits = ?
                WHERE id = ?
                RETURNING id, code, name, credits
                """;

        return jdbcTemplate.query(sql,
                COURSE_ROW_MAPPER,
                course.getCode(),
                course.getName(),
                course.getCredits(),
                id)
                .stream()
                .findFirst();
    }

    public boolean deleteById(int id) {
        String sql = """
                DELETE FROM courses
                WHERE id = ?
                """;

        int affectedRows = jdbcTemplate.update(sql, id);

        return affectedRows > 0;
    }
}