package net.bingosoft.oss.ssoclient.exception;

/**
 * @since 3.0.1
 */
public class HttpException extends RuntimeException {
    
    private final int code;
    private final String message;
    
    public HttpException(int code, String message) {
        this.code = code;
        this.message = message;
    }
    public int getCode(){
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
