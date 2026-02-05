package com.impetus.order_service.exception;

public class InternalServerError extends RuntimeException{
    public InternalServerError(String message){
        super(message);
    }
}
