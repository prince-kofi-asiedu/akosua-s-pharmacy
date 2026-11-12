package medication;

import java.time.LocalDate;

public class ExpiredMedicationException extends RuntimeException {
    public ExpiredMedicationException(String med, int medId, LocalDate expiryDate) {
        super("Cannot dispense " + medId + " " + med + ": expired on " + expiryDate);
    }
}