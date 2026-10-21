package com.institute.institute_api.exception;

public class StudentHasPaymentsException extends ConflictException {
    public StudentHasPaymentsException(String studentName) {
        super("Cannot delete student \"" + studentName + "\" because they have payments registered.");
    }
}