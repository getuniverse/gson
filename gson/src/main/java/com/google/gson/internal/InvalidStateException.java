package com.google.gson.internal;

/**
 * Internal API.
 */
public final class InvalidStateException extends RuntimeException {

    public InvalidStateException() {
        // empty
    }

    public InvalidStateException(final String message) {
        super(message);
    }

    public InvalidStateException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public InvalidStateException(final Throwable cause) {
        super(cause);
    }
}
