import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.logging.Logger;

import pharmacy.Pharmacy;
import customer.Customer;
import customer.CustomerType;
import medication.ExpiredMedicationException;
import medication.InsufficientStockException;
import medication.OTCMedication;
import medication.PrescriptionMedication;

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
            LOG.warning(e.getClass().getSimpleName() + " at " + e.getStackTrace()[0] + ": " + e.getMessage());
        }
        p.printReport();
    }

    private static void tryRun(Runnable action) {
        try {
            action.run();
        } catch (InsufficientStockException | ExpiredMedicationException | IllegalArgumentException | IllegalStateException e) {
            LOG.warning(e.getClass().getSimpleName() + " at " + e.getStackTrace()[0] + ": " + e.getMessage());
        }
    }
}
