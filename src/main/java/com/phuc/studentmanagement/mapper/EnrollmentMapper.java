package com.phuc.studentmanagement.mapper;

import com.phuc.studentmanagement.dto.EnrollmentRequestDTO;
import com.phuc.studentmanagement.dto.EnrollmentResponseDTO;
import com.phuc.studentmanagement.model.Enrollment;

public final class EnrollmentMapper {
    private EnrollmentMapper() {
    }

    public static Enrollment toEnrollment(EnrollmentRequestDTO request) {
        return new Enrollment(request.studentId(), request.courseId());
    }

    public static EnrollmentResponseDTO toResponseDTO(Enrollment enrollment) {
        return new EnrollmentResponseDTO(
                enrollment.getId(),
                enrollment.getStudentId(),
                enrollment.getCourseId());
    }
}