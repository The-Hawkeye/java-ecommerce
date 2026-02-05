package com.impetus.order_service.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ApiError {
    private boolean success = false;
    private ErrorDetails error;
    private Instant timestamp = Instant.now();

    public ApiError(String code, String message, Map<String, Object> details){
        this.error = new ErrorDetails(code, message, details);
    }
    public record ErrorDetails(String code, String message, Map<String, Object> details){}
}
