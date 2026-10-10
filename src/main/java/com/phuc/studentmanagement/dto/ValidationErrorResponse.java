package com.phuc.studentmanagement.dto;

import java.util.Map;

public record ValidationErrorResponse(Map<String, String> errors) {
}