package com.njdaeger.authenticationhub.web;

import static com.njdaeger.authenticationhub.web.WebUtils.BAD_REQUEST;

/**
 * Represents a Request Exception with the API.
 */
public class RequestException extends RuntimeException {

    private final int status;

    /**
     * Creates a request exception
     * @param message Message to send
     * @param status Status
     */
    public RequestException(String message, int status) {
        super(message);
        this.status = status;
    }

    /**
     * Creates a request exception with default status of 400
     * @param message Message to send.
     */
    public RequestException(String message) {
        super(message);
        this.status = BAD_REQUEST;
    }

    /**
     * Creates a request exception with the default message of "Bad Request"
     * @param status Status
     */
    public RequestException(int status) {
        super("Bad Request");
        this.status = status;
    }

    /**
     * Creates a request exception with the default message of "Bad Request" and status of 400
     */
    public RequestException() {
        this(BAD_REQUEST);
    }

    /**
     * Get the status code of this RequestException
     * @return The status code
     */
    public int getStatus() {
        return status;
    }
}
