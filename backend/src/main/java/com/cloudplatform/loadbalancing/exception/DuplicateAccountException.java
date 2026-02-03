package com.cloudplatform.loadbalancing.exception;

/**
 * Exception thrown when attempting to create a duplicate AWS account
 */
public class DuplicateAccountException extends RuntimeException {
    public DuplicateAccountException(String message) {
        super(message);
    }
}
