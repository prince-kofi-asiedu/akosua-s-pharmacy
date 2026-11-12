// Akosua's Pharmacy - stock and dispensing tracker
// Original program. Written in a hurry by the intern who has since left.
// It works, but the team is nervous about changing it. That is your job.
//
// Run it as-is first to see what it currently does before you change anything.

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

import customer.Customer;
import medication.InsufficientStockException;
import medication.OTCMedication;
import medication.PrescriptionMedication;
import transaction.TransactionType;
import transaction.Transaction;

public class PharmacyTracker {

    public static void main(String[] args) {
        Pharmacy p = new Pharmacy("Akosua's Pharmacy");

        OTCMedication paracetamol = new OTCMedication("Paracetamol 500mg", 12.50, 100, 4, LocalDate.parse("2027-11-12"));
        PrescriptionMedication amoxicillin = new PrescriptionMedication("Amoxicillin 250mg", 30.00, 40, 5, LocalDate.parse("2028-12-12"));
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
        } catch (InsufficientStockException | IllegalArgumentException e) {
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

    public Pharmacy(String name) {
        this.name = name;
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

    private double computePrice(double medicationPrice, int qty, double discount) {
        double total = 0;
        total = medicationPrice * qty;
        boolean noDiscount = !(discount <= 0) || (discount >= 1);
        if (noDiscount) {
            total = total * discount; // registered patients get 5% off
        }
        return total;
    }

    public void dispense(Customer c, OTCMedication m, int qty) {
        boolean cannotSatisfyDispense = qty > m.getStock();
        if (cannotSatisfyDispense) throw new InsufficientStockException(m.getName(), m.getId(), qty, m.getStock());

        double total = computePrice(m.getPrice(), qty, 0);
        m.reduceStockBy(qty);
        System.out.println("Dispensed " + qty + " x " + m.getName() + " to " + c.getName()
                + " for GHS " + total);
        c.recordHistory("Got " + qty + " x " + m.getName()); //TODO: catch an error here and reverse the transaction
        transactions.add(new Transaction(c.getName(), c.getId(), m.getName(), qty, m.getId(), total, TransactionType.DISPENSE, LocalDateTime.now()));
    }

    public void dispense(Customer c, PrescriptionMedication m, int qty, String prescriber) {
        if (prescriber == null || prescriber.isBlank()) throw new IllegalArgumentException("Prescription required for " + m.getName());        
        boolean cannotSatisfyDispense = qty > m.getStock();
        if (cannotSatisfyDispense) throw new InsufficientStockException(m.getName(), m.getId(), qty, m.getStock());

        double total = computePrice(m.getPrice(), qty, discount);
        m.reduceStockBy(qty);
        System.out.println("Dispensed " + qty + " x " + m.getName() + " to " + c.getName()
                + " (prescribed by " + prescriber + ") for GHS " + total);
        c.recordHistory("Got " + qty + " x " + m.getName()); //TODO: catch an error here and reverse the transaction
        transactions.add(new Transaction(c.getName(), c.getId(), m.getName(), qty, m.getId(), total, TransactionType.DISPENSE, LocalDateTime.now()));

    }

     public void restock(OTCMedication m, int qty) {
             m.increaseStockBy(qty);
            transactions.add(new Transaction("(supplier)", m.getName(), qty, m.getId(), 0, TransactionType.RESTOCK, LocalDateTime.now()));
            System.out.println("Restocked " + qty + " x " + m.getName());
    }

    public void restock(PrescriptionMedication m, int qty) {
        m.increaseStockBy(qty);
        transactions.add(new Transaction("(supplier)", m.getName(), qty, m.getId(), 0, TransactionType.RESTOCK, LocalDateTime.now()));
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
    }
}
