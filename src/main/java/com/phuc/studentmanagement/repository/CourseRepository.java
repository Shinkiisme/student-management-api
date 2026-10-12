package com.phuc.studentmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.phuc.studentmanagement.exception.InvalidPaginationException;
import com.phuc.studentmanagement.model.Course;
import com.phuc.studentmanagement.pagination.PageRequest;
import com.phuc.studentmanagement.pagination.PageResult;

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

        public PageResult<Course> findPage(PageRequest request, Integer credits) {
                String sortColumn = switch (request.sortBy()) {
                        case "id" -> "id";
                        case "code" -> "code";
                        case "name" -> "name";
                        case "credits" -> "credits";
                        default -> throw new InvalidPaginationException("Invalid sort field: " + request.sortBy());
                };

                String direction = request.direction().name();

                String whereClause = credits == null
                                ? ""
                                : "WHERE credits = ?";

                String orderClause = sortColumn.equals("id")
                                ? "id " + direction
                                : sortColumn + " " + direction + ", id ASC";

                String dataSql = """
                                SELECT id, code, name, credits
                                FROM courses
                                %s
                                ORDER BY %s
                                LIMIT ? OFFSET ?
                                """.formatted(whereClause, orderClause);

                String countSql = """
                                SELECT COUNT(*)
                                FROM courses
                                %s
                                """.formatted(whereClause);

                long offset = (long) request.page() * request.size();

                long totalElements;
                List<Course> courses;

                if (credits == null) {
                        totalElements = jdbcTemplate.queryForObject(
                                        countSql,
                                        Long.class);

                        courses = jdbcTemplate.query(
                                        dataSql,
                                        COURSE_ROW_MAPPER,
                                        request.size(),
                                        offset);
                } else {
                        totalElements = jdbcTemplate.queryForObject(
                                        countSql,
                                        Long.class,
                                        credits);

                        courses = jdbcTemplate.query(
                                        dataSql,
                                        COURSE_ROW_MAPPER,
                                        credits,
                                        request.size(),
                                        offset);
                }

                return new PageResult<>(
                                courses,
                                request.page(),
                                request.size(),
                                totalElements);
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