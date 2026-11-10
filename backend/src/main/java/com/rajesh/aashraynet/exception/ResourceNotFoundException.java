package com.rajesh.aashraynet.exception;

import org.springframework.http.HttpStatus;

/** 404. Also used for records outside the caller's district, so their existence is not revealed. */
public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String what, Object id) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " " + id + " not found");
    }
}
