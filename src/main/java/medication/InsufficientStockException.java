package medication;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String med, int medId, int requested, int avail) {
       super("Insufficient stock for " + medId + " " + med + ": requested " + requested + ", available " + avail);
    }
}
