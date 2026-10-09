package com.institute.institute_api.exception;

public abstract class ConflictException extends RuntimeException {
    protected ConflictException(String message) { super(message); }
}
