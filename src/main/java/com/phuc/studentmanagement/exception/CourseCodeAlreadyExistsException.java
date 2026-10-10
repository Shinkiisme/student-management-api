package com.phuc.studentmanagement.exception;

public class CourseCodeAlreadyExistsException extends RuntimeException {
    public CourseCodeAlreadyExistsException(String code, Throwable cause) {
        super("Course code " + code + " already exists", cause);
    }
}