package com.phuc.studentmanagement.pagination;

import java.util.List;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements) {
    public long totalPages() {
        return Math.ceilDiv(totalElements, size);
    }
}