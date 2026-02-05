package com.impetus.order_service.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

    private boolean success = true;
    private String message;
    private T data;
    private Instant timestamp = Instant.now();

    public ApiResponse(String message, T data){
        this.message = message;
        this.data = data;
    }
}