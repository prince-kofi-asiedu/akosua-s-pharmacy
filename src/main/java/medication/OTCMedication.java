package medication;

import java.time.LocalDate;
import java.math.BigDecimal;

public class OTCMedication extends Medication {
    public OTCMedication(String name, BigDecimal price, int stock, int stockingThreshold, LocalDate expiryDate) {
        super(name, price, stock, stockingThreshold, expiryDate);
    }
}
