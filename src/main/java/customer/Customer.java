package customer;

import java.util.ArrayList;
import java.util.List;

// Original
// class Customer {
//     public String name;
//     public String kind; // "WALK_IN" or "REGISTERED"
//     public String phone;
//     public int id;
//     public List<String> history = new ArrayList<>();
//     static int counter = 1;

//     public Customer(String name, String kind) {
//         this.id = counter;
//         counter++;
//         this.name = name;
//         this.kind = kind;
//     }
// }

public class Customer {
    private String name;
    private CustomerType kind; // "WALK_IN" or "REGISTERED"
    private String phone;
    private int id;
    private List<String> history = new ArrayList<>();
    static int counter = 1;

    public Customer(String name, CustomerType kind) {
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
    
    public void recordHistory(String event) {
        if (event.isBlank()) throw new IllegalArgumentException("event cannot be empty");
        history.add(event);
    }
}
