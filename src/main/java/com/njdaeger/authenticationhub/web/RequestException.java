package com.njdaeger.authenticationhub.web;

public class RequestException extends RuntimeException {

    private final int status;

    public RequestException(String message, int status) {
        super(message);
        this.status = status;
    }

    public RequestException(String message) {
        super(message);
        this.status = 400;
    }

    public RequestException(int status) {
        super("Bad Request");
        this.status = status;
    }

    public RequestException() {
        this(400);
    }

    public int getStatus() {
        return status;
    }
}
