package medication;

import java.time.LocalDate;

import customer.Customer;

public class PrescriptionMedication extends Medication {
    public PrescriptionMedication(String name, double price, int stock, int stockingThreshold, LocalDate expiryDate) {
        super(name, price, stock, stockingThreshold, expiryDate);
    }

    public void dispense(Customer c, int qty, String prescriber) {
        // do something
    }  
}
