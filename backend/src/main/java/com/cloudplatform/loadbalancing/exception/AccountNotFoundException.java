package com.cloudplatform.loadbalancing.exception;

/**
 * Exception thrown when an AWS account is not found
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
