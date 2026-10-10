package com.phuc.studentmanagement.mapper;

import com.phuc.studentmanagement.dto.CourseRequestDTO;
import com.phuc.studentmanagement.dto.CourseResponseDTO;
import com.phuc.studentmanagement.model.Course;

public final class CourseMapper {
    private CourseMapper() {
    }

    public static Course toCourse(CourseRequestDTO request) {
        return new Course(request.code(), request.name(), request.credits());
    }

    public static CourseResponseDTO toResponseDTO(Course course) {
        return new CourseResponseDTO(course.getId(), course.getCode(), course.getName(), course.getCredits());
    }
}