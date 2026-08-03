package com.hbm_m.client.loader.dae;

/** Thrown when a COLLADA file cannot be parsed or is missing required data. */
public class DaeModelFormatException extends RuntimeException {

    public DaeModelFormatException(String message) {
        super(message);
    }

    public DaeModelFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
