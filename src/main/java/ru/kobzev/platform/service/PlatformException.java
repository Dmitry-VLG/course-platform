package ru.kobzev.platform.service;

import org.springframework.http.HttpStatus;

public class PlatformException extends RuntimeException {
    private final HttpStatus status;

    private PlatformException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static PlatformException missing(String message) { return new PlatformException(HttpStatus.NOT_FOUND, message); }
    public static PlatformException invalid(String message) { return new PlatformException(HttpStatus.BAD_REQUEST, message); }
    public static PlatformException conflict(String message) { return new PlatformException(HttpStatus.CONFLICT, message); }
    public static PlatformException forbidden(String message) { return new PlatformException(HttpStatus.FORBIDDEN, message); }
}
