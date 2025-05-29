package enums;

public enum PaymentType {
    REGULAR("Обычный платеж"),
    FULL_PREPAYMENT("Досрочное погашение (полное)"),
    PARTIAL_PREPAYMENT("Досрочное погашение (частичное)");

    private final String description;

    PaymentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static PaymentType fromDescription(String description) {
        for (PaymentType type : values()) {
            if (type.description.equals(description)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown payment type: " + description);
    }

    public static PaymentType fromString(String name) {
        try {
            return PaymentType.valueOf(name);
        } catch (IllegalArgumentException e) {
            for (PaymentType type : values()) {
                if (type.description.equals(name)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown payment type: " + name);
        }
    }
}
