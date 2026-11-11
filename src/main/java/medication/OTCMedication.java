package medication;

import java.time.LocalDate;


public class OTCMedication extends Medication {
    public OTCMedication(String name, double price, int stock, int stockingThreshold, LocalDate expiryDate) {
        super(name, price, stock, stockingThreshold, expiryDate);
    }
}
