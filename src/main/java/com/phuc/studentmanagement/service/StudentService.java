package com.phuc.studentmanagement.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.phuc.studentmanagement.exception.ResourceInUseException;
import com.phuc.studentmanagement.exception.StudentCodeAlreadyExistsException;
import com.phuc.studentmanagement.model.Student;
import com.phuc.studentmanagement.repository.StudentRepository;

@Service
public class StudentService {
    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public List<Student> getAllStudents(Integer age) {
        if (age == null) {
            return studentRepository.findAll();
        }

        return studentRepository.findByAge(age);
    }

    public Optional<Student> getStudentById(int id) {
        return studentRepository.findById(id);
    }

    public Student addStudent(Student student) {
        try {
            return studentRepository.insert(student);
        } catch (DuplicateKeyException exception) {
            throw new StudentCodeAlreadyExistsException(student.getStudentCode(), exception);
        }
    }

    public Optional<Student> updateStudent(int id, Student student) {
        try {
            return studentRepository.update(id, student);
        } catch (DuplicateKeyException exception) {
            throw new StudentCodeAlreadyExistsException(student.getStudentCode(), exception);
        }
    }

    public boolean deleteStudent(int id) {
        try {
            return studentRepository.deleteById(id);
        } catch (DataIntegrityViolationException exception) {
            Throwable cause = exception.getMostSpecificCause();

            if (cause instanceof SQLException sqlException && "23503".equals(sqlException.getSQLState())) {
                throw new ResourceInUseException("Student", id, exception);
            }

            throw exception;
        }
    }
}