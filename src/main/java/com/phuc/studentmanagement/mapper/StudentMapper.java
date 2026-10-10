package com.phuc.studentmanagement.mapper;

import com.phuc.studentmanagement.dto.StudentRequestDTO;
import com.phuc.studentmanagement.dto.StudentResponseDTO;
import com.phuc.studentmanagement.model.Student;

public final class StudentMapper {
    private StudentMapper() {
    }

    public static Student toStudent(StudentRequestDTO request) {
        return new Student(request.name(), request.age());
    }

    public static StudentResponseDTO toResponseDTO(Student student) {
        return new StudentResponseDTO(student.getId(), student.getName(), student.getAge());
    }
}