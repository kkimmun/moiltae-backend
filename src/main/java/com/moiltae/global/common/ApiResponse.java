package com.moiltae.global.common;

import java.time.LocalDateTime;
import java.util.List;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        List<FieldValidationError> errors,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return new ApiResponse<>(true, code, message, data, List.of(), LocalDateTime.now());
    }

    public static ApiResponse<Void> failure(String code, String message) {
        return new ApiResponse<>(false, code, message, null, List.of(), LocalDateTime.now());
    }

    public static ApiResponse<Void> validationFailure(List<FieldValidationError> errors) {
        return new ApiResponse<>(false, "VALIDATION_ERROR", "입력값을 확인해 주세요.", null, errors, LocalDateTime.now());
    }
}

