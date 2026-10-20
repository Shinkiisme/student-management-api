package com.phuc.studentmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.phuc.studentmanagement.exception.InvalidPaginationException;
import com.phuc.studentmanagement.model.Enrollment;
import com.phuc.studentmanagement.pagination.PageRequest;
import com.phuc.studentmanagement.pagination.PageResult;

@Repository
public class EnrollmentRepository {
        private final JdbcTemplate jdbcTemplate;
        private static final RowMapper<Enrollment> ENROLLMENT_ROW_MAPPER = (rs, rowNum) -> new Enrollment(
                        rs.getInt("id"),
                        rs.getInt("student_id"),
                        rs.getInt("course_id"));

        public EnrollmentRepository(JdbcTemplate jdbcTemplate) {
                this.jdbcTemplate = jdbcTemplate;
        }

        public List<Enrollment> findAll() {
                String sql = """
                                SELECT id, student_id, course_id
                                FROM enrollments
                                ORDER BY id
                                """;

                return jdbcTemplate.query(sql, ENROLLMENT_ROW_MAPPER);
        }

        public PageResult<Enrollment> findPage(
                        PageRequest request,
                        Integer studentId) {
                String sortColumn = switch (request.sortBy()) {
                        case "id" -> "id";
                        case "studentId" -> "student_id";
                        case "courseId" -> "course_id";
                        default -> throw new InvalidPaginationException("Invalid sort field: " + request.sortBy());
                };

                String direction = request.direction().name();

                String whereClause = studentId == null
                                ? ""
                                : "WHERE student_id = ?";

                String orderClause = sortColumn.equals("id")
                                ? "id " + direction
                                : sortColumn + " " + direction + ", id ASC";

                String dataSql = """
                                        SELECT id, student_id, course_id
                                        FROM enrollments
                                        %s
                                        ORDER BY %s
                                        LIMIT ? OFFSET ?
                                """.formatted(whereClause, orderClause);

                String countSql = """
                                SELECT COUNT(*)
                                FROM enrollments
                                %s
                                """.formatted(whereClause);

                long offset = (long) request.page() * request.size();

                long totalElements;
                List<Enrollment> enrollments;

                if (studentId == null) {
                        totalElements = jdbcTemplate.queryForObject(
                                        countSql,
                                        Long.class);

                        enrollments = jdbcTemplate.query(
                                        dataSql,
                                        ENROLLMENT_ROW_MAPPER,
                                        request.size(),
                                        offset);
                } else {
                        totalElements = jdbcTemplate.queryForObject(
                                        countSql,
                                        Long.class,
                                        studentId);

                        enrollments = jdbcTemplate.query(
                                        dataSql,
                                        ENROLLMENT_ROW_MAPPER,
                                        studentId,
                                        request.size(),
                                        offset);
                }

                return new PageResult<>(
                                enrollments,
                                request.page(),
                                request.size(),
                                totalElements);
        }

        public List<Enrollment> findByStudentId(int studentId) {
                String sql = """
                                SELECT id, student_id, course_id
                                FROM enrollments
                                WHERE student_id = ?
                                ORDER BY id
                                """;

                return jdbcTemplate.query(sql, ENROLLMENT_ROW_MAPPER, studentId);
        }

        public Optional<Enrollment> findById(int id) {
                String sql = """
                                SELECT id, student_id, course_id
                                FROM enrollments
                                WHERE id = ?
                                """;

                return jdbcTemplate.query(sql, ENROLLMENT_ROW_MAPPER, id)
                                .stream()
                                .findFirst();
        }

        public Enrollment insert(Enrollment enrollment) {
                String sql = """
                                INSERT INTO enrollments(student_id, course_id)
                                VALUES (?, ?)
                                RETURNING id, student_id, course_id
                                """;

                return jdbcTemplate.queryForObject(sql,
                                ENROLLMENT_ROW_MAPPER,
                                enrollment.getStudentId(),
                                enrollment.getCourseId());
        }

        public boolean deleteById(int id) {
                String sql = """
                                DELETE FROM enrollments
                                WHERE id = ?
                                """;

                int affectedRows = jdbcTemplate.update(sql, id);

                return affectedRows > 0;
        }
}