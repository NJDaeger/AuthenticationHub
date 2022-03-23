package com.njdaeger.authenticationhub.web;

public class RequestException extends RuntimeException {

    private final int status;
    private final String message;

    public RequestException(String message, int status) {
        this.status = status;
        this.message = message;
    }

    public RequestException(String message) {
        this.status = 400;
        this.message = message;
    }

    public RequestException(int status) {
        this.message = "Bad Request";
        this.status = status;
    }



}
