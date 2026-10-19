package com.institute.institute_api.exception;

import java.time.LocalDate;

public class BackToTheFutureException extends BadRequestException {
    public BackToTheFutureException(LocalDate from, LocalDate to) {
        super("'from' date (" + from + ") must not be after 'to' date (" + to + ").");
    }
}
