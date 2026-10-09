package com.institute.institute_api.exception;

public class DuplicateIdException extends ConflictException {
    public DuplicateIdException(String nationalId) {
      super("A student with national ID " + nationalId + "already exists.");
    }
}
