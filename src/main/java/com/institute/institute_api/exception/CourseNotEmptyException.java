package com.institute.institute_api.exception;

public class CourseNotEmptyException extends ConflictException {
    public CourseNotEmptyException(String name) {
        super("Cannot delete course \"" + name + "\" because it has students enrolled.");
    }
}
