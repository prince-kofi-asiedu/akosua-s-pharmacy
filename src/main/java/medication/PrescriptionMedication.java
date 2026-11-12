package medication;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PrescriptionMedication extends Medication {

    public PrescriptionMedication(String name, BigDecimal price, int stock, int stockingThreshold, LocalDate expiryDate) {
        super(name, price, stock, stockingThreshold, expiryDate);
    }

    @Override
    public String category() {
        return "PRESCRIPTION";
    }

    @Override
    public void checkCanDispense(String prescriber) {
        if (prescriber == null || prescriber.isBlank()) {
            throw new IllegalArgumentException("Prescription required for " + getName());
        }
    }

}
