package com.phuc.studentmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.phuc.studentmanagement.model.Student;

@Repository
public class StudentRepository {
        private final JdbcTemplate jdbcTemplate;
        private static final RowMapper<Student> STUDENT_ROW_MAPPER = (rs, rowNum) -> new Student(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("age"));

        public StudentRepository(JdbcTemplate jdbcTemplate) {
                this.jdbcTemplate = jdbcTemplate;
        }

        public List<Student> findAll() {
                String sql = """
                                SELECT id, name, age
                                FROM students
                                ORDER BY id
                                """;

                return jdbcTemplate.query(sql, STUDENT_ROW_MAPPER);
        }

        public List<Student> findByAge(int age) {
                String sql = """
                                SELECT id, name, age
                                FROM students
                                WHERE age = ?
                                ORDER BY id
                                """;

                return jdbcTemplate.query(sql, STUDENT_ROW_MAPPER, age);
        }

        public Optional<Student> findById(int id) {
                String sql = """
                                SELECT id, name, age
                                FROM students
                                WHERE id = ?
                                """;

                return jdbcTemplate.query(sql, STUDENT_ROW_MAPPER, id)
                                .stream()
                                .findFirst();
        }

        public Student insert(Student student) {
                String sql = """
                                INSERT INTO students(name, age)
                                VALUES (?, ?)
                                RETURNING id, name, age;
                                        """;

                return jdbcTemplate.queryForObject(sql,
                                STUDENT_ROW_MAPPER,
                                student.getName(),
                                student.getAge());
        }

        public Optional<Student> update(int id, Student student) {
                String sql = """
                                UPDATE students
                                SET name = ?, age = ?
                                WHERE id = ?
                                RETURNING id, name, age
                                        """;

                return jdbcTemplate.query(sql,
                                STUDENT_ROW_MAPPER,
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