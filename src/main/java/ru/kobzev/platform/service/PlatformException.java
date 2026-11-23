package ru.kobzev.platform.service;

public class PlatformException extends RuntimeException {
    public enum Reason {
        MISSING, INVALID, CONFLICT, FORBIDDEN
    }

    private final Reason reason;

    private PlatformException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public static PlatformException missing(String message) {
        return new PlatformException(Reason.MISSING, message);
    }

    public static PlatformException invalid(String message) {
        return new PlatformException(Reason.INVALID, message);
    }

    public static PlatformException conflict(String message) {
        return new PlatformException(Reason.CONFLICT, message);
    }

    public static PlatformException forbidden(String message) {
        return new PlatformException(Reason.FORBIDDEN, message);
    }
}
