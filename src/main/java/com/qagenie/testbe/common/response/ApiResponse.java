package com.qagenie.testbe.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Standard envelope returned by every controller so FE has one shape to parse.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp,
        String errorCode
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "SUCCESS", data, Instant.now(), null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now(), null);
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, message, null, Instant.now(), errorCode);
    }
}
