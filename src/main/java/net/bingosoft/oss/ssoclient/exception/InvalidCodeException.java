package net.bingosoft.oss.ssoclient.exception;

/**
 * @since 3.0.1
 */
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
