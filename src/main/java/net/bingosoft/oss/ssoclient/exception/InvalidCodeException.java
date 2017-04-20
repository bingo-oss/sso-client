package net.bingosoft.oss.ssoclient.exception;

public class InvalidCodeException extends RuntimeException {
    public InvalidCodeException() {
        super();
    }

    public InvalidCodeException(String message) {
        super(message);
    }

    public InvalidCodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCodeException(Throwable cause) {
        super(cause);
    }
}
