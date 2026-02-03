package com.cloudplatform.loadbalancing.exception;

/**
 * Exception thrown when account validation fails
 */
public class AccountValidationException extends RuntimeException {
    public AccountValidationException(String message) {
        super(message);
    }

    public AccountValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
