// Akosua's Pharmacy - stock and dispensing tracker
// Original program. Written in a hurry by the intern who has since left.
// It works, but the team is nervous about changing it. That is your job.
//
// Run it as-is first to see what it currently does before you change anything.

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.stream.Stream;

import customer.Customer;
import medication.ExpiredMedicationException;
import medication.InsufficientStockException;
import medication.OTCMedication;
import medication.PrescriptionMedication;
import transaction.TransactionType;
import transaction.Transaction;
import medication.Medication;

public class PharmacyTracker {

    public static void main(String[] args) {
        Pharmacy p = new Pharmacy("Akosua's Pharmacy");

        OTCMedication paracetamol = new OTCMedication("Paracetamol 500mg", BigDecimal.valueOf(12.50), 100, 4, LocalDate.parse("2027-11-12"));
        PrescriptionMedication amoxicillin = new PrescriptionMedication("Amoxicillin 250mg", BigDecimal.valueOf(30), 40, 5, LocalDate.parse("2028-12-12"));
        OTCMedication coughSyrup = new OTCMedication("Cough Syrup 100ml",
                new BigDecimal("8.00"), 20, 5, LocalDate.now().plusDays(30));
        p.addMedication(paracetamol);
        p.addMedication(amoxicillin);

        Customer kofi = new Customer("Kofi Mensah", customer.CustomerType.WALK_IN);
        Customer ama = new Customer("Ama Owusu", customer.CustomerType.REGISTERED);
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

        } catch(Exception e) {
            System.out.println("Unexpected Error" + e.getMessage());
        }
        p.printReport();
    }

    private static void tryRun(Runnable action) {
        try {
            action.run();
        } catch (InsufficientStockException | ExpiredMedicationException | IllegalArgumentException e){
            System.out.println("Could not complete: " + e.getMessage());
        }
    }
}


class Pharmacy {
    public String name;
    public List<OTCMedication> otcMeds = new ArrayList<>();
    public List<PrescriptionMedication> prescriptionMeds = new ArrayList<>();
    public List<Customer> customers = new ArrayList<>();
    private List<Transaction> transactions = new ArrayList<>();
    private static final double discount = 0.95;
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
        if (m.isExpiredOn(today())) throw new ExpiredMedicationException(m.getName(), m.getId(), m.getExpiryDate());
    }

    private BigDecimal computePrice(BigDecimal medicationPrice, int qty, double discount) {
        BigDecimal total = BigDecimal.ZERO;
        total = medicationPrice.multiply(BigDecimal.valueOf(qty));
        boolean isDiscounted = !(discount <= 0) && !(discount >= 1);
        if (isDiscounted) {
            total =  total.multiply(BigDecimal.valueOf(discount)); // registered patients get 5% off
        }
        return total;
    }

    public void dispense(Customer c, OTCMedication m, int qty) {
        ensureNotExpired(m);
        boolean cannotSatisfyDispense = qty > m.getStock();
        if (cannotSatisfyDispense) throw new InsufficientStockException(m.getName(), m.getId(), qty, m.getStock());

        BigDecimal total = computePrice(m.getPrice(), qty, 0);
        m.reduceStockBy(qty);
        System.out.println("Dispensed " + qty + " x " + m.getName() + " to " + c.getName()
                + " for GHS " + total);
        c.recordHistory("Got " + qty + " x " + m.getName());
        transactions.add(new Transaction(c.getName(), c.getId(), m.getName(), qty, m.getId(), total, TransactionType.DISPENSE, LocalDateTime.now()));
    }

    public void dispense(Customer c, PrescriptionMedication m, int qty, String prescriber) {
        if (prescriber == null || prescriber.isBlank()) throw new IllegalArgumentException("Prescription required for " + m.getName());    
        ensureNotExpired(m);    
        boolean cannotSatisfyDispense = qty > m.getStock();
        if (cannotSatisfyDispense) throw new InsufficientStockException(m.getName(), m.getId(), qty, m.getStock());

        BigDecimal total = computePrice(m.getPrice(), qty, discount);
        m.reduceStockBy(qty);
        System.out.println("Dispensed " + qty + " x " + m.getName() + " to " + c.getName()
                + " (prescribed by " + prescriber + ") for GHS " + total);
        c.recordHistory("Got " + qty + " x " + m.getName());
        transactions.add(new Transaction(c.getName(), c.getId(), m.getName(), qty, m.getId(), total, TransactionType.DISPENSE, LocalDateTime.now()));

    }

     public void restock(OTCMedication m, int qty) {
             m.increaseStockBy(qty);
            transactions.add(new Transaction("(supplier)", m.getName(), qty, m.getId(), BigDecimal.ZERO, TransactionType.RESTOCK, LocalDateTime.now()));
            System.out.println("Restocked " + qty + " x " + m.getName());
    }

    public void restock(PrescriptionMedication m, int qty) {
        m.increaseStockBy(qty);
        transactions.add(new Transaction("(supplier)", m.getName(), qty, m.getId(), BigDecimal.ZERO, TransactionType.RESTOCK, LocalDateTime.now()));
        System.out.println("Restocked " + qty + " x " + m.getName());
    }

    public List<Transaction> getTransactions() {
        return List.copyOf(transactions); // hand the list back so callers can read it
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
        if (expired.isEmpty() && expiringSoon.isEmpty()) return;

        System.out.println("Expiry check:");
        for (Medication m : expired) {
            System.out.println("  EXPIRED - remove from shelf: " + m.getName() + " (expired " + m.getExpiryDate() + ", " + m.getStock() + " in stock)");
        }
        for (Medication m : expiringSoon) {
            System.out.println("  Expiring within " + EXPIRY_WARNING_DAYS + " days: " + m.getName() + " (expires " + m.getExpiryDate() + ", " + m.getStock() + " in stock)");
        }
    }
}
