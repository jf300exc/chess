package UI;

public class ClientHttpRequestException extends RuntimeException {
    private final int statusCode;

    public ClientHttpRequestException(int status) {
        super("Status Code: " + status);
        statusCode = status;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
