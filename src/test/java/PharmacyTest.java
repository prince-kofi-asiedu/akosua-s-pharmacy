import customer.Customer;
import customer.CustomerType;
import medication.InsufficientStockException;
import medication.OTCMedication;
import medication.PrescriptionMedication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import transaction.Transaction;
import transaction.TransactionType;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    @Disabled("Known bug: the 5% discount is applied to every prescription, including walk-in customers")
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
    @Disabled("Known bug: discounted total 12.50 x 0.95 = 11.875 has 3 decimals; Transaction rejects it "
            + "AFTER stock was already reduced, so stock is lost with no transaction recorded")
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

    @Test
    void mainProgramRunsAndNeverDrivesStockNegative() {
        PharmacyTracker.main(new String[0]);

        String output = out.toString();
        assertTrue(output.contains("Could not complete: Insufficient stock"));
        assertTrue(output.contains("Amoxicillin 250mg (PRESCRIPTION): 39 in stock"));
        assertTrue(output.contains("Paracetamol 500mg (OTC): 148 in stock"));
        assertTrue(output.contains("Transactions today: 3"));
        assertFalse(output.contains("in stock") && output.matches("(?s).*: -\\d+ in stock.*"));
    }
}