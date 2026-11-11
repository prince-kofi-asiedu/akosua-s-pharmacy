package medication;
import java.time.LocalDate;

public abstract class Medication {
    private String name;
    private double price;
    private int stock;
    private String category; // "OTC" or "PRESCRIPTION"
    private int id;
    private LocalDate expiryDate;
    private int stockingThreshold;
    static int counter = 1;

    

    public Medication(String name, double price, int stock, String category, int stockingThreshold, LocalDate expiryDate) {
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

        this.id = counter;
        counter++;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.stockingThreshold = stockingThreshold;
        this.expiryDate = expiryDate;
    }
}
