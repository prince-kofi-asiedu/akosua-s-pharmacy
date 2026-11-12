package customer;

import java.util.ArrayList;
import java.util.List;

public class Customer {
    private String name;
    private CustomerType kind;
    private String phone;
    private int id;
    private List<String> history = new ArrayList<>();
    static int counter = 1;

    public Customer(String name, CustomerType kind) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name cannot be empty");
        if (kind == null ) throw new IllegalArgumentException("Kind cannot be empty");
        this.id = counter;
        counter++;
        this.name = name;
        this.kind = kind;
    }

    public Customer(String name, CustomerType kind, String phone) {
        if (kind != CustomerType.REGISTERED) throw new IllegalStateException("Cannot register phone for WALK_INs");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name cannot be empty");
        if (kind == null ) throw new IllegalArgumentException("Kind cannot be empty");
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("Phone cannot be empty");
        this.id = counter;
        counter++;
        this.name = name;
        this.kind = kind;
    }

    public CustomerType getKind() {
        return kind;
    }

    public String getName() {
        return this.name;
    }

    public int getId() {
        return this.id;
    }
    
    public void recordHistory(String event) {
        if (event.isBlank()) throw new IllegalArgumentException("event cannot be empty");
        history.add(event);
    }

    public void registerPhone(String phoneNumber) {
        if (!kind.equals(CustomerType.REGISTERED)) {
            throw new IllegalStateException("Cannot register phone for a walk-in customer");
        }
        if (phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        this.phone = phoneNumber;
    }
}
