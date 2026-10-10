package com.phuc.studentmanagement.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, int id) {
        super(resourceName + " with id " + id + " not found");
    }
}