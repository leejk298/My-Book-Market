package mybook.mymarket.exception;

public class NotCorrespondingEmailException extends RuntimeException {  // RuntimeException 상속
    public NotCorrespondingEmailException() {
        super();
    }

    public NotCorrespondingEmailException(String message) {
        super(message);
    }

    public NotCorrespondingEmailException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotCorrespondingEmailException(Throwable cause) {
        super(cause);
    }
}
