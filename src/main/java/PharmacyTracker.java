
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.stream.Stream;
import java.util.logging.Level;
import java.util.logging.Logger;

import customer.Customer;
import customer.CustomerType;
import medication.ExpiredMedicationException;
import medication.InsufficientStockException;
import medication.OTCMedication;
import medication.PrescriptionMedication;
import transaction.TransactionType;
import transaction.Transaction;
import medication.Medication;

public class PharmacyTracker {
    private static final Logger LOG = Logger.getLogger(PharmacyTracker.class.getName());

    static {
    if (System.getProperty("java.util.logging.SimpleFormatter.format") == null) {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT %4$s %3$s - %5$s%6$s%n");
    }
}

    public static void main(String[] args) {

        Pharmacy p = new Pharmacy("Akosua's Pharmacy");

        OTCMedication paracetamol = new OTCMedication("Paracetamol 500mg", BigDecimal.valueOf(12.50), 100, 4, LocalDate.now().plusDays(30));
        PrescriptionMedication amoxicillin = new PrescriptionMedication("Amoxicillin 250mg", BigDecimal.valueOf(30), 40, 5, LocalDate.now().plusYears(2));
        p.addMedication(paracetamol);
        p.addMedication(amoxicillin);

        Customer kofi = new Customer("Kofi Mensah", CustomerType.WALK_IN);
        Customer ama = new Customer("Ama Owusu", CustomerType.REGISTERED);
        ama.registerPhone("0244 000 000");
        p.addCustomer(kofi);
        p.addCustomer(ama);

        try {
            // a normal morning
            tryRun(() -> p.dispense(kofi, paracetamol, 2));
            tryRun(() -> p.dispense(ama, amoxicillin, 1, "Dr. Boateng")); // prescription-only
            tryRun(() -> p.restock(paracetamol, 50));

            // ... and the sort of thing that keeps Akosua up at night:
            tryRun(() -> p.dispense(kofi, amoxicillin, 100, "Dr. Boateng")); // far more than we have in stock

        } catch (Exception e) {
            System.out.println("Unexpected Error" + e.getMessage());
        }
        p.printReport();
    }

    private static void tryRun(Runnable action) {
        try {
            action.run();
        } catch (InsufficientStockException | ExpiredMedicationException | IllegalArgumentException e) {
            LOG.warning(e.getClass().getSimpleName() + " at " + e.getStackTrace()[0] + ": " + e.getMessage());
        }
    }
}

class Pharmacy {

    private String name;
    private List<OTCMedication> otcMeds = new ArrayList<>();
    private List<PrescriptionMedication> prescriptionMeds = new ArrayList<>();
    private List<Customer> customers = new ArrayList<>();
    private List<Transaction> transactions = new ArrayList<>();
    private static final double REGISTERED_DISCOUNT = 0.95;
    static final int EXPIRY_WARNING_DAYS = 30;
    private final Clock clock;

    public Pharmacy(String name) {
        this(name, Clock.systemDefaultZone());
    }

    Pharmacy(String name, Clock clock) {
        this.name = name;
        this.clock = clock;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    public void addMedication(OTCMedication m) {
        otcMeds.add(m);
    }

    public void addMedication(PrescriptionMedication m) {
        prescriptionMeds.add(m);
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
        return total.setScale(1, RoundingMode.HALF_UP);
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
        for (OTCMedication m : otcMeds) {
            System.out.println("  " + m.getName() + " (OTC): " + m.getStock() + " in stock");
        }
        for (PrescriptionMedication m : prescriptionMeds) {
            System.out.println("  " + m.getName() + " (PRESCRIPTION): " + m.getStock() + " in stock");
        }
        System.out.println("Transactions today: " + transactions.size());
        printExpiryWarnings();
    }

    private void printExpiryWarnings() {
        LocalDate today = today();
        List<Medication> meds = Stream.concat(otcMeds.stream(), prescriptionMeds.stream())
                .map(m -> (Medication) m).toList();
        List<Medication> expired = meds.stream().filter(m -> m.isExpiredOn(today)).toList();
        List<Medication> expiringSoon = meds.stream().filter(m -> m.expiresWithin(EXPIRY_WARNING_DAYS, today)).toList();
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
