package medication;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OTCMedication extends Medication {

    public OTCMedication(String name, BigDecimal price, int stock, int stockingThreshold, LocalDate expiryDate) {
        super(name, price, stock, stockingThreshold, expiryDate);
    }

    @Override
    public String category() {
        return "OTC";
    }
}
