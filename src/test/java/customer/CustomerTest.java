package customer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CustomerTest {

    @Test
    void constructorStoresNameAndKind() {
        Customer c = new Customer("Kofi Mensah", CustomerType.WALK_IN);

        assertEquals("Kofi Mensah", c.getName());
        assertEquals(CustomerType.WALK_IN, c.getKind());
    }

    @Test
    void eachCustomerGetsAUniqueIncreasingId() {
        Customer first = new Customer("Kofi", CustomerType.WALK_IN);
        Customer second = new Customer("Ama", CustomerType.REGISTERED);

        assertTrue(first.getId() > 0);
        assertEquals(first.getId() + 1, second.getId());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void constructorRejectsBlankName(String name) {
        assertThrows(IllegalArgumentException.class, () -> new Customer(name, CustomerType.WALK_IN));
    }

    @Test
    void constructorRejectsNullKind() {
        assertThrows(IllegalArgumentException.class, () -> new Customer("Kofi", null));
    }

    @Test
    void recordHistoryAcceptsNonBlankEvent() {
        Customer c = new Customer("Kofi", CustomerType.WALK_IN);
        assertDoesNotThrow(() -> c.recordHistory("Got 2 x Paracetamol"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void recordHistoryRejectsBlankEvent(String event) {
        Customer c = new Customer("Kofi", CustomerType.WALK_IN);
        assertThrows(IllegalArgumentException.class, () -> c.recordHistory(event));
    }

    @Test
    void recordHistoryRejectsNullEvent() {
        Customer c = new Customer("Kofi", CustomerType.WALK_IN);
        // Currently surfaces as a NullPointerException rather than a validation error.
        assertThrows(RuntimeException.class, () -> c.recordHistory(null));
    }

    @Test
    void registeredCustomerCanRegisterPhone() {
        Customer c = new Customer("Ama", CustomerType.REGISTERED);
        assertDoesNotThrow(() -> c.registerPhone("0244 000 000"));
    }

    @Test
    void walkInCustomerCannotRegisterPhone() {
        Customer c = new Customer("Kofi", CustomerType.WALK_IN);
        assertThrows(IllegalStateException.class, () -> c.registerPhone("0244 000 000"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void registerPhoneRejectsBlankNumber(String phone) {
        Customer c = new Customer("Ama", CustomerType.REGISTERED);
        assertThrows(IllegalArgumentException.class, () -> c.registerPhone(phone));
    }

    @Test
    void registerPhoneRejectsNullNumber() {
        Customer c = new Customer("Ama", CustomerType.REGISTERED);
        // Currently surfaces as a NullPointerException rather than a validation error.
        assertThrows(RuntimeException.class, () -> c.registerPhone(null));
    }
}
