package medication;

import java.time.LocalDate;

public class PrescriptionMedication extends Medication {
    public PrescriptionMedication(String name, double price, int stock, int stockingThreshold, LocalDate expiryDate) {
        super(name, price, stock, stockingThreshold, expiryDate);
    } 
}
