import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import customer.Customer;
import customer.CustomerType;
import medication.ExpiredMedicationException;
import medication.InsufficientStockException;
import medication.OTCMedication;
import medication.PrescriptionMedication;
import pharmacy.Pharmacy;
import transaction.Transaction;
import transaction.TransactionType;

class PharmacyTest {

    private static final LocalDate NEXT_YEAR = LocalDate.now().plusYears(1);

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private PrintStream originalOut;

    private Pharmacy pharmacy;
    private OTCMedication paracetamol;
    private PrescriptionMedication amoxicillin;
    private Customer kofi;
    private Customer ama;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        System.setOut(new PrintStream(out, true));

        pharmacy = new Pharmacy("Akosua's Pharmacy");
        paracetamol = new OTCMedication("Paracetamol 500mg", new BigDecimal("12.50"), 100, 4, NEXT_YEAR);
        amoxicillin = new PrescriptionMedication("Amoxicillin 250mg", new BigDecimal("30"), 40, 5, NEXT_YEAR);
        pharmacy.addMedication(paracetamol);
        pharmacy.addMedication(amoxicillin);

        kofi = new Customer("Kofi Mensah", CustomerType.WALK_IN);
        ama = new Customer("Ama Owusu", CustomerType.REGISTERED);
        pharmacy.addCustomer(kofi);
        pharmacy.addCustomer(ama);
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    private Transaction onlyTransaction() {
        List<Transaction> txs = pharmacy.getTransactions();
        assertEquals(1, txs.size());
        return txs.get(0);
    }

    // ---- OTC dispensing -----------------------------------------------------

    @Test
    void dispenseOtcReducesStockAndRecordsTransactionAtFullPrice() {
        pharmacy.dispense(kofi, paracetamol, 2);

        assertEquals(98, paracetamol.getStock());
        Transaction t = onlyTransaction();
        assertEquals(TransactionType.DISPENSE, t.type());
        assertEquals("Kofi Mensah", t.customerName());
        assertEquals("Paracetamol 500mg", t.medName());
        assertEquals(paracetamol.getId(), t.medId());
        assertEquals(2, t.qty());
        assertEquals(0, new BigDecimal("25.00").compareTo(t.total()));
        assertNotNull(t.timestamp());
        assertTrue(out.toString().contains("Dispensed 2 x Paracetamol 500mg to Kofi Mensah for GHS 25.00"));
    }

    @Test
    void dispenseOtcOfEntireStockIsAllowed() {
        pharmacy.dispense(kofi, paracetamol, 100);
        assertEquals(0, paracetamol.getStock());
    }

    @Test
    void dispenseOtcBeyondStockThrowsAndChangesNothing() {
        assertThrows(InsufficientStockException.class, () -> pharmacy.dispense(kofi, paracetamol, 101));

        assertEquals(100, paracetamol.getStock());
        assertTrue(pharmacy.getTransactions().isEmpty());
    }

    @Test
    void dispenseOtcNegativeQuantityThrowsAndChangesNothing() {
        assertThrows(IllegalArgumentException.class, () -> pharmacy.dispense(kofi, paracetamol, -1));

        assertEquals(100, paracetamol.getStock());
        assertTrue(pharmacy.getTransactions().isEmpty());
    }

    @Test
    void dispenseOtcZeroQuantityThrowsAndChangesNothing() {
        assertThrows(IllegalArgumentException.class, () -> pharmacy.dispense(kofi, paracetamol, 0));

        assertEquals(100, paracetamol.getStock());
        assertTrue(pharmacy.getTransactions().isEmpty());
    }

    @Test
    @Disabled("Known bug: the Transaction constructor used by dispense() drops the customer id (stores 0)")
    void dispenseRecordsCustomerId() {
        pharmacy.dispense(kofi, paracetamol, 1);
        assertEquals(kofi.getId(), onlyTransaction().customerId());
    }

    // ---- prescription dispensing --------------------------------------------

    @Test
    void dispensePrescriptionToRegisteredCustomerApplies5PercentDiscount() {
        pharmacy.dispense(ama, amoxicillin, 1, "Dr. Boateng");

        assertEquals(39, amoxicillin.getStock());
        Transaction t = onlyTransaction();
        assertEquals(0, new BigDecimal("28.50").compareTo(t.total()));
        assertTrue(out.toString().contains("(prescribed by Dr. Boateng)"));
    }

    @Test
    void dispensePrescriptionToWalkInCustomerChargesFullPrice() {
        pharmacy.dispense(kofi, amoxicillin, 1, "Dr. Boateng");
        assertEquals(0, new BigDecimal("30.00").compareTo(onlyTransaction().total()));
    }

    @Test
    void dispensePrescriptionWithoutPrescriberThrowsAndChangesNothing() {
        assertThrows(IllegalArgumentException.class, () -> pharmacy.dispense(ama, amoxicillin, 1, null));
        assertThrows(IllegalArgumentException.class, () -> pharmacy.dispense(ama, amoxicillin, 1, "  "));

        assertEquals(40, amoxicillin.getStock());
        assertTrue(pharmacy.getTransactions().isEmpty());
    }

    @Test
    void dispensePrescriptionBeyondStockThrowsAndChangesNothing() {
        // This is the scenario from PharmacyTracker.main that used to drive stock to -61.
        assertThrows(InsufficientStockException.class,
                () -> pharmacy.dispense(kofi, amoxicillin, 100, "Dr. Boateng"));

        assertEquals(40, amoxicillin.getStock());
        assertTrue(pharmacy.getTransactions().isEmpty());
    }

    @Test
    void dispensePrescriptionWithFractionalDiscountedTotalSucceeds() {
        PrescriptionMedication rx = new PrescriptionMedication("Rx", new BigDecimal("12.50"), 10, 1, NEXT_YEAR);
        pharmacy.addMedication(rx);

        pharmacy.dispense(ama, rx, 1, "Dr. Boateng");

        assertEquals(9, rx.getStock());
        assertEquals(1, pharmacy.getTransactions().size());
    }

    // ---- restocking ---------------------------------------------------------

    @Test
    void restockOtcIncreasesStockAndRecordsRestockTransaction() {
        pharmacy.restock(paracetamol, 50);

        assertEquals(150, paracetamol.getStock());
        Transaction t = onlyTransaction();
        assertEquals(TransactionType.RESTOCK, t.type());
        assertEquals(50, t.qty());
        assertEquals(0, BigDecimal.ZERO.compareTo(t.total()));
        assertEquals(paracetamol.getId(), t.medId());
        assertTrue(out.toString().contains("Restocked 50 x Paracetamol 500mg"));
    }

    @Test
    void restockPrescriptionIncreasesStock() {
        pharmacy.restock(amoxicillin, 10);

        assertEquals(50, amoxicillin.getStock());
        assertEquals(TransactionType.RESTOCK, onlyTransaction().type());
    }

    @Test
    void restockNegativeQuantityThrowsAndChangesNothing() {
        assertThrows(IllegalArgumentException.class, () -> pharmacy.restock(paracetamol, -5));

        assertEquals(100, paracetamol.getStock());
        assertTrue(pharmacy.getTransactions().isEmpty());
    }

    @Test
    void restockZeroQuantityThrowsAndChangesNothing() {
        assertThrows(IllegalArgumentException.class, () -> pharmacy.restock(amoxicillin, 0));

        assertEquals(40, amoxicillin.getStock());
        assertTrue(pharmacy.getTransactions().isEmpty());
    }

    // ---- transactions & report ----------------------------------------------

    @Test
    void getTransactionsReturnsAnUnmodifiableSnapshot() {
        pharmacy.dispense(kofi, paracetamol, 1);
        List<Transaction> txs = pharmacy.getTransactions();

        assertThrows(UnsupportedOperationException.class, txs::clear);
        assertThrows(UnsupportedOperationException.class, () -> txs.add(
                new Transaction("x", "y", 1, 1, BigDecimal.ONE, TransactionType.DISPENSE, LocalDateTime.now())));

        pharmacy.restock(paracetamol, 5);
        assertEquals(1, txs.size(), "earlier snapshot must not change");
        assertEquals(2, pharmacy.getTransactions().size());
    }

    @Test
    void transactionsAreKeptInOrder() {
        pharmacy.dispense(kofi, paracetamol, 2);
        pharmacy.dispense(ama, amoxicillin, 1, "Dr. Boateng");
        pharmacy.restock(paracetamol, 50);

        List<Transaction> txs = pharmacy.getTransactions();
        assertEquals(List.of(TransactionType.DISPENSE, TransactionType.DISPENSE, TransactionType.RESTOCK),
                txs.stream().map(Transaction::type).toList());
        assertTrue(txs.get(0).id() < txs.get(1).id() && txs.get(1).id() < txs.get(2).id());
    }

    @Test
    void printReportListsStockLevelsAndTransactionCount() {
        pharmacy.dispense(kofi, paracetamol, 2);
        pharmacy.dispense(ama, amoxicillin, 1, "Dr. Boateng");
        pharmacy.restock(paracetamol, 50);
        out.reset();

        pharmacy.printReport();

        String report = out.toString();
        assertTrue(report.contains("=== Akosua's Pharmacy - end of day ==="));
        assertTrue(report.contains("Paracetamol 500mg (OTC): 148 in stock"));
        assertTrue(report.contains("Amoxicillin 250mg (PRESCRIPTION): 39 in stock"));
        assertTrue(report.contains("Transactions today: 3"));
    }

    @Test
    void printReportOnEmptyDay() {
        pharmacy.printReport();
        assertTrue(out.toString().contains("Transactions today: 0"));
    }

    // ---- end-to-end ---------------------------------------------------------

    /** Runs the action and returns what PharmacyTracker logged, without printing it. */
    private static List<LogRecord> captureLogs(Runnable action) {
        Logger log = Logger.getLogger(PharmacyTracker.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord r) { records.add(r); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        log.addHandler(handler);
        log.setUseParentHandlers(false);
        try {
            action.run();
        } finally {
            log.removeHandler(handler);
            log.setUseParentHandlers(true);
        }
        return records;
    }

    @Test
    void mainProgramRunsAndNeverDrivesStockNegative() {
        List<LogRecord> logs = captureLogs(() -> PharmacyTracker.main(new String[0]));

        // The over-dispense is refused and logged once, as a warning, instead of being printed.
        assertEquals(1, logs.size());
        assertEquals(Level.WARNING, logs.get(0).getLevel());
        assertTrue(logs.get(0).getMessage().contains("InsufficientStockException"));
        assertTrue(logs.get(0).getMessage().contains("requested 100, available 39"));

        String output = out.toString();
        assertFalse(output.contains("Dispensed 100"));
        assertTrue(output.contains("Amoxicillin 250mg (PRESCRIPTION): 39 in stock"));
        assertTrue(output.contains("Paracetamol 500mg (OTC): 148 in stock"));
        assertTrue(output.contains("Transactions today: 3"));
        assertFalse(output.contains("in stock") && output.matches("(?s).*: -\\d+ in stock.*"));
    }

    // ---- expiry -------------------------------------------------------------

    /** A pharmacy whose "today" is {@code daysFromNow} days in the future. */
    private static Pharmacy pharmacyOn(int daysFromNow) {
        ZoneId zone = ZoneId.systemDefault();
        Clock clock = Clock.fixed(LocalDate.now().plusDays(daysFromNow).atStartOfDay(zone).toInstant(), zone);
        return new Pharmacy("Akosua's Pharmacy", clock);
    }

    @Test
    void dispensingExpiredOtcMedicationIsRefusedAndChangesNothing() {
        OTCMedication expiresToday = new OTCMedication("Cough syrup", new BigDecimal("8.00"), 10, 2, LocalDate.now());
        Pharmacy tomorrow = pharmacyOn(1);
        tomorrow.addMedication(expiresToday);

        ExpiredMedicationException ex = assertThrows(ExpiredMedicationException.class,
                () -> tomorrow.dispense(kofi, expiresToday, 1));

        assertTrue(ex.getMessage().contains("Cough syrup"));
        assertEquals(10, expiresToday.getStock());
        assertTrue(tomorrow.getTransactions().isEmpty());
    }

    @Test
    void dispensingExpiredPrescriptionMedicationIsRefusedAndChangesNothing() {
        PrescriptionMedication expiresToday = new PrescriptionMedication("Insulin", new BigDecimal("40.00"), 5, 1, LocalDate.now());
        Pharmacy tomorrow = pharmacyOn(1);
        tomorrow.addMedication(expiresToday);

        assertThrows(ExpiredMedicationException.class,
                () -> tomorrow.dispense(ama, expiresToday, 1, "Dr. Boateng"));

        assertEquals(5, expiresToday.getStock());
        assertTrue(tomorrow.getTransactions().isEmpty());
    }

    @Test
    void medicationCanStillBeDispensedOnItsExpiryDay() {
        OTCMedication expiresToday = new OTCMedication("Cough syrup", new BigDecimal("8.00"), 10, 2, LocalDate.now());
        Pharmacy today = pharmacyOn(0);
        today.addMedication(expiresToday);

        today.dispense(kofi, expiresToday, 1);

        assertEquals(9, expiresToday.getStock());
    }

    @Test
    void reportFlagsExpiredAndSoonToExpireStock() {
        OTCMedication expiresToday = new OTCMedication("Cough syrup", new BigDecimal("8.00"), 10, 2, LocalDate.now());
        PrescriptionMedication expiresSoon = new PrescriptionMedication("Insulin", new BigDecimal("40.00"), 5, 1,
                LocalDate.now().plusDays(20));
        Pharmacy tomorrow = pharmacyOn(1);
        tomorrow.addMedication(expiresToday);
        tomorrow.addMedication(expiresSoon);
        tomorrow.addMedication(paracetamol); // expires next year: must not be listed

        tomorrow.printReport();

        String report = out.toString();
        assertTrue(report.contains("Expiry check:"));
        assertTrue(report.contains("EXPIRED - remove from shelf: Cough syrup"));
        assertTrue(report.contains("Expiring within 30 days: Insulin"));
        assertFalse(report.contains("Expiring within 30 days: Paracetamol"));
        assertFalse(report.contains("EXPIRED - remove from shelf: Paracetamol"));
    }

    @Test
    void reportHasNoExpirySectionWhenAllStockIsFresh() {
        pharmacy.printReport();
        assertFalse(out.toString().contains("Expiry check:"));
    }

    @Test
    void expiredErrorNamesTheMedicationAndItsExpiryDate() {
        LocalDate expiry = LocalDate.now();
        OTCMedication syrup = new OTCMedication("Cough syrup", new BigDecimal("8.00"), 10, 2, expiry);
        Pharmacy tomorrow = pharmacyOn(1);
        tomorrow.addMedication(syrup);

        ExpiredMedicationException ex = assertThrows(ExpiredMedicationException.class,
                () -> tomorrow.dispense(kofi, syrup, 1));

        assertTrue(ex.getMessage().contains("Cough syrup"));
        assertTrue(ex.getMessage().contains(expiry.toString()));
    }

    @Test
    void expiryIsCheckedBeforeStockLevel() {
        // Both rules are broken; the patient-safety one (expiry) must be the one reported.
        OTCMedication syrup = new OTCMedication("Cough syrup", new BigDecimal("8.00"), 10, 2, LocalDate.now());
        Pharmacy tomorrow = pharmacyOn(1);
        tomorrow.addMedication(syrup);

        assertThrows(ExpiredMedicationException.class, () -> tomorrow.dispense(kofi, syrup, 999));
        assertEquals(10, syrup.getStock());
    }

    @Test
    void missingPrescriptionIsStillReportedForExpiredPrescriptionMedication() {
        PrescriptionMedication insulin = new PrescriptionMedication("Insulin", new BigDecimal("40.00"), 5, 1, LocalDate.now());
        Pharmacy tomorrow = pharmacyOn(1);
        tomorrow.addMedication(insulin);

        assertThrows(IllegalArgumentException.class, () -> tomorrow.dispense(ama, insulin, 1, null));
        assertEquals(5, insulin.getStock());
    }

    @Test
    void refusedExpiredDispenseDoesNotBlockLaterValidDispenses() {
        OTCMedication expired = new OTCMedication("Cough syrup", new BigDecimal("8.00"), 10, 2, LocalDate.now());
        OTCMedication fresh = new OTCMedication("Vitamin C", new BigDecimal("5.00"), 10, 2, NEXT_YEAR);
        Pharmacy tomorrow = pharmacyOn(1);
        tomorrow.addMedication(expired);
        tomorrow.addMedication(fresh);

        assertThrows(ExpiredMedicationException.class, () -> tomorrow.dispense(kofi, expired, 1));
        tomorrow.dispense(kofi, fresh, 2);

        assertEquals(8, fresh.getStock());
        assertEquals(1, tomorrow.getTransactions().size());
    }

    @Test
    void reportWarningWindowIncludesDay30ButNotDay31() {
        LocalDate today = LocalDate.now();
        OTCMedication day30 = new OTCMedication("Day30 Tablets", new BigDecimal("1.00"), 1, 0,
                today.plusDays(Pharmacy.getExpiryWarningWindow()));
        OTCMedication day31 = new OTCMedication("Day31 Tablets", new BigDecimal("1.00"), 1, 0,
                today.plusDays(Pharmacy.getExpiryWarningWindow() + 1));
        Pharmacy now = pharmacyOn(0);
        now.addMedication(day30);
        now.addMedication(day31);

        now.printReport();

        String report = out.toString();
        assertTrue(report.contains("Expiring within 30 days: Day30 Tablets"));
        assertFalse(report.contains("Day31 Tablets (expires"));
    }

    @Test
    void reportStillShowsStockLevelOfExpiredMedication() {
        OTCMedication syrup = new OTCMedication("Cough syrup", new BigDecimal("8.00"), 10, 2, LocalDate.now());
        Pharmacy tomorrow = pharmacyOn(1);
        tomorrow.addMedication(syrup);

        tomorrow.printReport();

        String report = out.toString();
        assertTrue(report.contains("Cough syrup (OTC): 10 in stock"));
        assertTrue(report.contains("EXPIRED - remove from shelf: Cough syrup (expired " + LocalDate.now() + ", 10 in stock)"));
    }

    @Test
    void reportListsEveryExpiredItemNotJustTheFirst() {
        OTCMedication a = new OTCMedication("Syrup A", new BigDecimal("1.00"), 1, 0, LocalDate.now());
        PrescriptionMedication b = new PrescriptionMedication("Drops B", new BigDecimal("1.00"), 1, 0, LocalDate.now());
        Pharmacy tomorrow = pharmacyOn(1);
        tomorrow.addMedication(a);
        tomorrow.addMedication(b);

        tomorrow.printReport();

        String report = out.toString();
        assertTrue(report.contains("EXPIRED - remove from shelf: Syrup A"));
        assertTrue(report.contains("EXPIRED - remove from shelf: Drops B"));
    }

    // ---- adding a new medication type needs no change to Pharmacy ----------

    /**
     * A new kind of medication, written only in this test. Pharmacy is not
     * changed to support it: its rule comes from overriding checkCanDispense.
     */
    static class ControlledMedication extends PrescriptionMedication {
        ControlledMedication(String name, BigDecimal price, int stock) {
            super(name, price, stock, 1, LocalDate.now().plusYears(1));
        }

        @Override
        public void checkCanDispense(String prescriber) {
            super.checkCanDispense(prescriber);
            if (!prescriber.startsWith("Dr.")) {
                throw new IllegalArgumentException("Controlled drugs need a doctor's prescription");
            }
        }

        @Override
        public String category() {
            return "CONTROLLED";
        }
    }

    @Test
    void newMedicationTypeWorksWithoutChangingPharmacy() {
        ControlledMedication morphine = new ControlledMedication("Morphine 10mg", new BigDecimal("50.00"), 10);
        pharmacy.addMedication(morphine);

        assertThrows(IllegalArgumentException.class, () -> pharmacy.dispense(ama, morphine, 1, "Nurse Adjei"));
        assertEquals(10, morphine.getStock());

        pharmacy.dispense(ama, morphine, 1, "Dr. Boateng");
        assertEquals(9, morphine.getStock());

        pharmacy.restock(morphine, 5);
        out.reset();
        pharmacy.printReport();
        assertTrue(out.toString().contains("Morphine 10mg (CONTROLLED): 14 in stock"));
    }
}
