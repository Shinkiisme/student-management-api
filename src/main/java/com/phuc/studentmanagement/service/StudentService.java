package com.phuc.studentmanagement.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

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
        return studentRepository.insert(student);
    }

    public Optional<Student> updateStudent(int id, Student student) {
        return studentRepository.update(id, student);
    }

    public boolean deleteStudent(int id) {
        return studentRepository.deleteById(id);
    }
}