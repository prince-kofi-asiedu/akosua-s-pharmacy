package transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transaction(int id,
        String customerName,
        int customerId,
        String medName,
        int medId,
        int qty,
        BigDecimal total,
        TransactionType type,
        LocalDateTime timestamp) {

    private static int counter = 1;

    public Transaction {
        if (type == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }
        if (medName == null || medName.isBlank()) {
            throw new IllegalArgumentException("Medication name must not be blank");
        }
        if (medId <= 0) {
            throw new IllegalArgumentException("Medication id is invalid");
        }

        if (type == TransactionType.DISPENSE && (customerName == null || customerName.isBlank())) {
            throw new IllegalArgumentException("Customer name is required for a dispense transaction");
        }
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, got " + qty);
        }

        if (total == null) {
            throw new IllegalArgumentException("Total is required");
        }
        if (total.signum() < 0) {
            throw new IllegalArgumentException("Total must be positive, got " + total);
        }
        if (total.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Total cannot have more than 2 decimal places, got " + total);
        }

    }

    public Transaction(String customerName, int customerId, String medName, int qty, int medId, BigDecimal total, TransactionType type, LocalDateTime timestamp) {
        this(counter++, customerName, 0, medName, medId, qty, total, type, timestamp);
    }

    public Transaction(String customerName, String medName, int qty, int medId, BigDecimal total, TransactionType type, LocalDateTime timestamp) {
        this(counter++, customerName, 0, medName, medId, qty, total, type, timestamp);
    }
}
