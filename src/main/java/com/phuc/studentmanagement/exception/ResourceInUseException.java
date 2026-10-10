package com.phuc.studentmanagement.exception;

public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String resourceName, int id, Throwable cause) {
        super(resourceName + " with id " + id + " cannot be deleted because it is in use", cause);
    }
}