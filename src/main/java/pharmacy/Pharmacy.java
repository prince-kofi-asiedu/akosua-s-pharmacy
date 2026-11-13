package pharmacy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import customer.Customer;
import customer.CustomerType;
import medication.ExpiredMedicationException;
import medication.InsufficientStockException;
import medication.Medication;
import transaction.Transaction;
import transaction.TransactionType;


public class Pharmacy {

    private String name;
    private List<Medication> medications = new ArrayList<>();
    private List<Customer> customers = new ArrayList<>();
    private List<Transaction> transactions = new ArrayList<>();
    private static final double REGISTERED_DISCOUNT = 0.95;
    private static final int EXPIRY_WARNING_DAYS = 30;
    private final Clock clock;

    public Pharmacy(String name) {
        this(name, Clock.systemDefaultZone());
    }

    public Pharmacy(String name, Clock clock) {
        this.name = name;
        this.clock = clock;
    }

    public static int getExpiryWarningWindow() {
        return EXPIRY_WARNING_DAYS;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    public void addMedication(Medication m) {
        medications.add(m);
    }

    public void addCustomer(Customer c) {
        customers.add(c);
    }

    private void ensureNotExpired(Medication m) {
        if (m.isExpiredOn(today())) {
            throw new ExpiredMedicationException(m.getName(), m.getId(), m.getExpiryDate());
        }
    }

    private BigDecimal computePrice(BigDecimal medicationPrice, int qty, Customer c) {
        BigDecimal total = BigDecimal.ZERO;
        total = medicationPrice.multiply(BigDecimal.valueOf(qty));
        if (c.getKind() == CustomerType.REGISTERED) {
            total = total.multiply(BigDecimal.valueOf(REGISTERED_DISCOUNT)); // registered patients get 5% off
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public void dispense(Customer c, Medication m, int qty) {
        dispense(c, m, qty, null);
    }

    public void dispense(Customer c, Medication m, int qty, String prescriber) {
        m.checkCanDispense(prescriber);
        ensureNotExpired(m);
        boolean cannotSatisfyDispense = qty > m.getStock();
        if (cannotSatisfyDispense) {
            throw new InsufficientStockException(m.getName(), m.getId(), qty, m.getStock());
        }

        BigDecimal total = computePrice(m.getPrice(), qty, c);
        m.reduceStockBy(qty);

        if (prescriber == null) {
            System.out.println("Dispensed " + qty + " x " + m.getName() + " to " + c.getName()
                    + " for GHS " + total);
        } else {
            System.out.println("Dispensed " + qty + " x " + m.getName() + " to " + c.getName()
                    + " (prescribed by " + prescriber + ") for GHS " + total);
        }
        transactions.add(new Transaction(c.getName(), c.getId(), m.getName(), qty, m.getId(), total, TransactionType.DISPENSE, LocalDateTime.now(clock)));
        c.recordHistory("Got " + qty + " x " + m.getName());

    }

    public void restock(Medication m, int qty) {
        m.increaseStockBy(qty);
        transactions.add(new Transaction("(supplier)", m.getName(), qty, m.getId(), BigDecimal.ZERO, TransactionType.RESTOCK, LocalDateTime.now(clock)));
        System.out.println("Restocked " + qty + " x " + m.getName());
    }

    public List<Transaction> getTransactions() {
        return List.copyOf(transactions);
    }

    public void printReport() {
        System.out.println();
        System.out.println("=== " + name + " - end of day ===");
        System.out.println("Stock levels:");
        for (Medication m : medications) {
            System.out.println("  " + m.getName() + " (" + m.category() + "): " + m.getStock() + " in stock");        }
        System.out.println("Transactions today: " + transactions.size());
        printExpiryWarnings();
    }

    private void printExpiryWarnings() {
        LocalDate today = today();
        List<Medication> expired = medications.stream().filter(m -> m.isExpiredOn(today)).toList();
        List<Medication> expiringSoon = medications.stream().filter(m -> m.expiresWithin(EXPIRY_WARNING_DAYS, today)).toList();
        if (expired.isEmpty() && expiringSoon.isEmpty()) {
            return;
        }

        System.out.println("Expiry check:");
        for (Medication m : expired) {
            System.out.println("  EXPIRED - remove from shelf: " + m.getName() + " (expired " + m.getExpiryDate() + ", " + m.getStock() + " in stock)");
        }
        for (Medication m : expiringSoon) {
            System.out.println("  Expiring within " + EXPIRY_WARNING_DAYS + " days: " + m.getName() + " (expires " + m.getExpiryDate() + ", " + m.getStock() + " in stock)");
        }
    }
}
