package com.phuc.studentmanagement.pagination;

public record PageRequest(
        int page,
        int size,
        String sortBy,
        SortDirection direction) {
    public PageRequest {
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }

        if (sortBy == null || sortBy.isBlank()) {
            throw new IllegalArgumentException("Sort by is required");
        }

        if (direction == null) {
            throw new IllegalArgumentException("Direction is required");
        }
    }
}