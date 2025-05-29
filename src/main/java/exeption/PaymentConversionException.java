package exeption;

public class PaymentConversionException extends RuntimeException {
    public PaymentConversionException(String message) {
        super(message);
    }
}
