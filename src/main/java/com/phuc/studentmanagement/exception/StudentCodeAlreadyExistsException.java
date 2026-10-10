package com.phuc.studentmanagement.exception;

public class StudentCodeAlreadyExistsException extends RuntimeException {
    public StudentCodeAlreadyExistsException(String studentCode, Throwable cause) {
        super("Student code " + studentCode + " already exists", cause);
    }
}