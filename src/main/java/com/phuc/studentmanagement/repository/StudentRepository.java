package com.phuc.studentmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.phuc.studentmanagement.exception.InvalidPaginationException;
import com.phuc.studentmanagement.model.Student;
import com.phuc.studentmanagement.pagination.PageRequest;
import com.phuc.studentmanagement.pagination.PageResult;

@Repository
public class StudentRepository {
        private final JdbcTemplate jdbcTemplate;
        private static final RowMapper<Student> STUDENT_ROW_MAPPER = (rs, rowNum) -> new Student(
                        rs.getInt("id"),
                        rs.getString("student_code"),
                        rs.getString("name"),
                        rs.getInt("age"));

        public StudentRepository(JdbcTemplate jdbcTemplate) {
                this.jdbcTemplate = jdbcTemplate;
        }

        public List<Student> findAll() {
                String sql = """
                                SELECT id, student_code, name, age
                                FROM students
                                ORDER BY id
                                """;

                return jdbcTemplate.query(sql, STUDENT_ROW_MAPPER);
        }

        public List<Student> findByAge(int age) {
                String sql = """
                                SELECT id, student_code, name, age
                                FROM students
                                WHERE age = ?
                                ORDER BY id
                                """;

                return jdbcTemplate.query(sql, STUDENT_ROW_MAPPER, age);
        }

        public Optional<Student> findById(int id) {
                String sql = """
                                SELECT id, student_code, name, age
                                FROM students
                                WHERE id = ?
                                """;

                return jdbcTemplate.query(sql, STUDENT_ROW_MAPPER, id)
                                .stream()
                                .findFirst();
        }

        public PageResult<Student> findPage(PageRequest request, Integer age) {
                String sortColumn = switch (request.sortBy()) {
                        case "id" -> "id";
                        case "studentCode" -> "student_code";
                        case "name" -> "name";
                        case "age" -> "age";
                        default -> throw new InvalidPaginationException("Invalid sort field: " + request.sortBy());
                };

                String direction = request.direction().name();

                String whereClause = age == null ? "" : "WHERE age = ?";

                String orderClause = sortColumn.equals("id")
                                ? "id " + direction
                                : sortColumn + " " + direction + ", id ASC";

                String dataSql = """
                                SELECT id, student_code, name, age
                                FROM students
                                %s
                                ORDER BY %s
                                LIMIT ? OFFSET ?
                                """.formatted(whereClause, orderClause);

                String countSql = """
                                SELECT COUNT(*)
                                FROM students
                                %s
                                """.formatted(whereClause);

                long offset = (long) request.page() * request.size();

                long totalElements;
                List<Student> students;

                if (age == null) {
                        totalElements = jdbcTemplate.queryForObject(countSql, Long.class);

                        students = jdbcTemplate.query(
                                        dataSql,
                                        STUDENT_ROW_MAPPER,
                                        request.size(),
                                        offset);
                } else {
                        totalElements = jdbcTemplate.queryForObject(
                                        countSql,
                                        Long.class,
                                        age);

                        students = jdbcTemplate.query(
                                        dataSql,
                                        STUDENT_ROW_MAPPER,
                                        age,
                                        request.size(),
                                        offset);
                }

                return new PageResult<>(
                                students,
                                request.page(),
                                request.size(),
                                totalElements);
        }

        public Student insert(Student student) {
                String sql = """
                                INSERT INTO students(student_code, name, age)
                                VALUES (?, ?, ?)
                                RETURNING id, student_code, name, age
                                        """;

                return jdbcTemplate.queryForObject(sql,
                                STUDENT_ROW_MAPPER,
                                student.getStudentCode(),
                                student.getName(),
                                student.getAge());
        }

        public Optional<Student> update(int id, Student student) {
                String sql = """
                                UPDATE students
                                SET student_code = ?, name = ?, age = ?
                                WHERE id = ?
                                RETURNING id, student_code, name, age
                                        """;

                return jdbcTemplate.query(sql,
                                STUDENT_ROW_MAPPER,
                                student.getStudentCode(),
                                student.getName(),
                                student.getAge(),
                                id)
                                .stream()
                                .findFirst();
        }

        public boolean deleteById(int id) {
                String sql = """
                                DELETE FROM students
                                WHERE id = ?
                                """;

                int affectedRows = jdbcTemplate.update(sql, id);

                return affectedRows > 0;
        }
}