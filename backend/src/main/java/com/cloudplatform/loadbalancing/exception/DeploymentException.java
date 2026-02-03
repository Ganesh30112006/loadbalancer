package com.cloudplatform.loadbalancing.exception;

public class DeploymentException extends RuntimeException {
    public DeploymentException(String message) {
        super(message);
    }

    public DeploymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
