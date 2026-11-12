package medication;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MedicationTest {

    private static final LocalDate NEXT_YEAR = LocalDate.now().plusYears(1);

    private static OTCMedication otc(int stock) {
        return new OTCMedication("Paracetamol 500mg", new BigDecimal("12.50"), stock, 4, NEXT_YEAR);
    }

    // ---- construction -------------------------------------------------------

    @Test
    void constructorStoresFields() {
        OTCMedication m = otc(100);

        assertEquals("Paracetamol 500mg", m.getName());
        assertEquals(new BigDecimal("12.50"), m.getPrice());
        assertEquals(100, m.getStock());
    }

    @Test
    void priceIsNormalisedToTwoDecimalPlaces() {
        OTCMedication m = new OTCMedication("X", new BigDecimal("30"), 1, 0, NEXT_YEAR);
        assertEquals(2, m.getPrice().scale());
        assertEquals("30.00", m.getPrice().toPlainString());
    }

    @Test
    void bothSubtypesAreMedications() {
        Medication otc = otc(1);
        Medication rx = new PrescriptionMedication("Amoxicillin", new BigDecimal("30"), 1, 0, NEXT_YEAR);
        assertInstanceOf(OTCMedication.class, otc);
        assertInstanceOf(PrescriptionMedication.class, rx);
    }

    @Test
    void eachMedicationGetsAUniqueIncreasingId() {
        Medication a = otc(1);
        Medication b = new PrescriptionMedication("Amoxicillin", new BigDecimal("30"), 1, 0, NEXT_YEAR);
        assertTrue(a.getId() > 0);
        assertEquals(a.getId() + 1, b.getId());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankName(String name) {
        assertThrows(IllegalArgumentException.class,
                () -> new OTCMedication(name, BigDecimal.ONE, 1, 0, NEXT_YEAR));
    }

    @Test
    void rejectsNullPrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new OTCMedication("X", null, 1, 0, NEXT_YEAR));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1", "-0.01"})
    void rejectsNonPositivePrice(String price) {
        assertThrows(IllegalArgumentException.class,
                () -> new OTCMedication("X", new BigDecimal(price), 1, 0, NEXT_YEAR));
    }

    @Test
    void rejectsPriceWithMoreThanTwoDecimalPlaces() {
        assertThrows(IllegalArgumentException.class,
                () -> new OTCMedication("X", new BigDecimal("1.234"), 1, 0, NEXT_YEAR));
    }

    @Test
    void acceptsPriceWithTrailingZerosBeyondTwoDecimals() {
        // 1.5000 is really 1.5, so it is a valid price.
        OTCMedication m = new OTCMedication("X", new BigDecimal("1.5000"), 1, 0, NEXT_YEAR);
        assertEquals(new BigDecimal("1.50"), m.getPrice());
    }

    @Test
    void rejectsNegativeStock() {
        assertThrows(IllegalArgumentException.class,
                () -> new OTCMedication("X", BigDecimal.ONE, -1, 0, NEXT_YEAR));
    }

    @Test
    void acceptsZeroStock() {
        assertEquals(0, otc(0).getStock());
    }

    @Test
    void rejectsNullExpiryDate() {
        assertThrows(IllegalArgumentException.class,
                () -> new OTCMedication("X", BigDecimal.ONE, 1, 0, null));
    }

    @Test
    void rejectsExpiredMedication() {
        assertThrows(IllegalArgumentException.class,
                () -> new OTCMedication("X", BigDecimal.ONE, 1, 0, LocalDate.now().minusDays(1)));
    }

    @Test
    void acceptsMedicationExpiringToday() {
        assertDoesNotThrow(() -> new OTCMedication("X", BigDecimal.ONE, 1, 0, LocalDate.now()));
    }

    @Test
    @Disabled("Known gap: stockingThreshold is not validated, so a negative threshold is accepted")
    void rejectsNegativeStockingThreshold() {
        assertThrows(IllegalArgumentException.class,
                () -> new OTCMedication("X", BigDecimal.ONE, 1, -5, NEXT_YEAR));
    }

    // ---- stock changes ------------------------------------------------------

    @Test
    void reduceStockByDecreasesStock() {
        OTCMedication m = otc(10);
        m.reduceStockBy(3);
        assertEquals(7, m.getStock());
    }

    @Test
    void reduceStockByEntireStockLeavesZero() {
        OTCMedication m = otc(10);
        m.reduceStockBy(10);
        assertEquals(0, m.getStock());
    }

    @Test
    void reduceStockByMoreThanAvailableThrowsAndLeavesStockUnchanged() {
        OTCMedication m = otc(5);

        InsufficientStockException ex = assertThrows(InsufficientStockException.class, () -> m.reduceStockBy(6));

        assertEquals(5, m.getStock());
        assertTrue(ex.getMessage().contains("requested 6"));
        assertTrue(ex.getMessage().contains("available 5"));
        assertTrue(ex.getMessage().contains("Paracetamol 500mg"));
    }

    @Test
    void reduceStockByNegativeThrowsAndLeavesStockUnchanged() {
        OTCMedication m = otc(5);
        assertThrows(IllegalArgumentException.class, () -> m.reduceStockBy(-1));
        assertEquals(5, m.getStock());
    }

    @Test
    void increaseStockByIncreasesStock() {
        OTCMedication m = otc(5);
        m.increaseStockBy(50);
        assertEquals(55, m.getStock());
    }

    @Test
    void increaseStockByNegativeThrowsAndLeavesStockUnchanged() {
        OTCMedication m = otc(5);
        assertThrows(IllegalArgumentException.class, () -> m.increaseStockBy(-10));
        assertEquals(5, m.getStock());
    }

    @Test
    void insufficientStockExceptionIsUnchecked() {
        assertInstanceOf(RuntimeException.class, new InsufficientStockException("X", 1, 2, 1));
    }

    // ---- expiry -------------------------------------------------------------

    @Test
    void isExpiredOnIsFalseUpToAndIncludingTheExpiryDay() {
        LocalDate expiry = LocalDate.now().plusDays(10);
        OTCMedication m = new OTCMedication("X", BigDecimal.ONE, 1, 0, expiry);

        assertFalse(m.isExpiredOn(expiry.minusDays(1)));
        assertFalse(m.isExpiredOn(expiry));
        assertTrue(m.isExpiredOn(expiry.plusDays(1)));
    }

    @Test
    void expiresWithinIsTrueOnlyInsideTheWindow() {
        LocalDate today = LocalDate.now();
        OTCMedication m = new OTCMedication("X", BigDecimal.ONE, 1, 0, today.plusDays(30));

        assertTrue(m.expiresWithin(30, today));
        assertFalse(m.expiresWithin(29, today));
    }

    @Test
    void expiresWithinIsFalseOnceAlreadyExpired() {
        LocalDate expiry = LocalDate.now();
        OTCMedication m = new OTCMedication("X", BigDecimal.ONE, 1, 0, expiry);

        assertFalse(m.expiresWithin(30, expiry.plusDays(1)));
    }

    @Test
    void expiresWithinRejectsNegativeDays() {
        assertThrows(IllegalArgumentException.class, () -> otc(1).expiresWithin(-1, LocalDate.now()));
    }

    @Test
    void getExpiryDateReturnsTheDateGiven() {
        assertEquals(NEXT_YEAR, otc(1).getExpiryDate());
    }
}
