package com.institute.institute_api.exception;

public class DuplicateCourseNameException extends ConflictException {
    public DuplicateCourseNameException(String name) {
        super("A course named \"" + name + "\" already exists.");
    }
}
