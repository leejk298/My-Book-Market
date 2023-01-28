package mybook.mymarket.exception;

public class NotCorrectAccess extends RuntimeException {
    public NotCorrectAccess() {
        super();
    }
    public NotCorrectAccess(String message) {
        super(message);
    }
    public NotCorrectAccess(String message, Throwable cause) {
        super(message, cause);
    }
    public NotCorrectAccess(Throwable cause) {
        super(cause);
    }
}
