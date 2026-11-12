package medication;

import java.math.BigDecimal;
import java.time.LocalDate;

public abstract class Medication {

    private String name;
    private BigDecimal price;
    private int stock;
    private int id;
    private LocalDate expiryDate;
    private int stockingThreshold;
    static int counter = 1;

    public Medication(String name, BigDecimal price, int stock, int stockingThreshold, LocalDate expiryDate) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Medication name must not be blank");
        }
        if (price == null) {
            throw new IllegalArgumentException("Price is required");
        }
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("Price must be positive, got " + price);
        }
        if (price.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Price cannot have more than 2 decimal places, got " + price);
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative, got " + stock);
        }
        if (expiryDate == null) {
            throw new IllegalArgumentException("Expiry date is required");
        }

        if (expiryDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot register expired goods");
        }

        this.id = counter;
        counter++;
        this.name = name;
        this.price = price.setScale(1);
        this.stock = stock;
        this.stockingThreshold = stockingThreshold;
        this.expiryDate = expiryDate;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public int getStock() {
        return this.stock;
    }

    public void reduceStockBy(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Stock cannot be negative, got " + n); // is a negative number

        }
        if (n > this.stock) {
            throw new InsufficientStockException(this.name, this.id, n, this.stock);  // is more than we have

        }
        this.stock -= n;
    }

    public void increaseStockBy(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Cannot increase stock by " + n); // is a negative number

        }
        this.stock += n;
    }

    public String getName() {
        return this.name;
    }

    public int getId() {
        return this.id;
    }

    public void checkCanDispense(String prescriber) {}

    public abstract String category();

    public LocalDate getExpiryDate() {
        return this.expiryDate;
    }

    public boolean isExpiredOn(LocalDate date) {
        return date.isAfter(this.expiryDate);
    }

    public boolean expiresWithin(int days, LocalDate date) {
        if (days < 0) {
            throw new IllegalArgumentException("days cannot be negative, got " + days);
        }
        return !isExpiredOn(date) && !this.expiryDate.isAfter(date.plusDays(days));
    }

}
