package com.institute.institute_api.exception;

public class DuplicateNationalIdException extends ConflictException {
    public DuplicateNationalIdException(String nationalId) {
      super("A student with national ID " + nationalId + "already exists.");
    }
}
