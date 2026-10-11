package com.phuc.studentmanagement.exception;

public class EnrollmentAlreadyExistsException extends RuntimeException {
    public EnrollmentAlreadyExistsException(int studentId,
            int courseId, Throwable cause) {
        super("Student with id " + studentId + " is already enrolled in course with id " + courseId, cause);
    }
}