package transaction;

public record Transaction(int id,
    String customerName,
    String medName,
    int medId,
    int qty,
    double total,
    TransactionType type) {
    // "DISPENSE" or "RESTOCK"
    private static int counter = 1;

    public Transaction {
         if (type == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }
        if (medName == null || medName.isBlank()) {
            throw new IllegalArgumentException("Medication name must not be blank");
        }
        if (type == TransactionType.DISPENSE && (customerName == null || customerName.isBlank())) {
            throw new IllegalArgumentException("Customer name is required for a dispense transaction");
        }
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, got " + qty);
        }
        if (!Double.isFinite(total) || total < 0) {
            throw new IllegalArgumentException("Total must be a non-negative number, got " + total);
        }
    }

    public Transaction(String customerName, String medName, int qty, int medId, double total, TransactionType type) {
        this(counter++, customerName, medName, medId, qty, total, type);
    }
}
