package com.ecommerce.demo.exception;

public class DeviceNotFoundException extends RuntimeException{

    public DeviceNotFoundException(String message) {
        super(message);
    }
}
