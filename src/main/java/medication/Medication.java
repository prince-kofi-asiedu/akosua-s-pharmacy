package medication;
import java.time.LocalDate;

import customer.Customer;

public abstract class Medication {
    private String name;
    private double price;
    private int stock;
    private int id;
    private LocalDate expiryDate;
    private int stockingThreshold;
    static int counter = 1;


    public Medication(String name, double price, int stock, int stockingThreshold, LocalDate expiryDate) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Medication name must not be blank");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be positive, got " + price);
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative, got " + stock);
        }
        if (expiryDate == null) {
            throw new IllegalArgumentException("Expiry date is required");
        }

        if (expiryDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot register expired goodds");
        }

        this.id = counter;
        counter++;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.stockingThreshold = stockingThreshold;
        this.expiryDate = expiryDate;
    }

    public double getPrice() {
        return this.price;
    };

    public int getStock() {
        return this.stock;
    }

    public void reduceStockBy(int n) {
        if (n < 0) throw new IllegalArgumentException("Stock cannot be negative, got " + n);
        this.stock -= n;
    }

    public void increaseStockBy(int n) {
         if (n < 0) throw new IllegalArgumentException("Cannot increase stock by " + n);
         this.stock += n;
    }

    public String getName() {
        return this.name;
    }
    
}
