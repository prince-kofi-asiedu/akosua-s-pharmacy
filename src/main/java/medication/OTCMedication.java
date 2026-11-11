package medication;

import java.time.LocalDate;

import customer.Customer;

public class OTCMedication extends Medication {
    public OTCMedication(String name, double price, int stock, int stockingThreshold, LocalDate expiryDate) {
        super(name, price, stock, stockingThreshold, expiryDate);
    }

    @Override
    public void dispense(Customer c, int qty, String prescriber) {
        // do something
    }
}
