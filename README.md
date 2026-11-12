# Akosua's Pharmacy — Stock and Dispensing Tracker

A small in-house program that Akosua's pharmacy uses to track what is in stock
and what gets dispensed. This rebuild keeps what the original program did, but
makes it safe to change and impossible to leave in an invalid state.

## Running it

You need JDK 21 and Maven. The program itself has no external dependencies;
JUnit 5 is used for the tests only.

**Run the program:**

```bash
mvn compile exec:java -Dexec.mainClass=PharmacyTracker
```

**Run the tests:**

```bash
mvn test
```

## Most dangerous defect from the diagnosis

**Dispensing more than is in stock.** The original program "dispensed" 100 Amoxicillin when only 40 existed, charged GHS 3000 and left stock at -61. For a pharmacy this is the worst defect: a patient can leave with an incomplete antibiotic course while the record says it was full, and prescription-only stock that doesn't reconcile can cost the pharmacy its licence at inspection.

# Rebuild

The class diagram of the rebuilt program.

## Class Diagram

```mermaid
classDiagram

class Pharmacy {
  -name: String
  -medications: List~Medication~
  -customers: List~Customer~
  -transactions: List~Transaction~
  -clock: Clock
  +addMedication(Medication)
  +addCustomer(Customer)
  +dispense(Customer, Medication, int)
  +dispense(Customer, Medication, int, String)
  +restock(Medication, int)
  +getTransactions() List~Transaction~
  +printReport()
}

class Medication {
  <<abstract>>
  -id: int
  -name: String
  -price: BigDecimal
  -stock: int
  -expiryDate: LocalDate
  -stockingThreshold: int
  +getId() int
  +getName() String
  +getPrice() BigDecimal
  +getStock() int
  +getExpiryDate() LocalDate
  +reduceStockBy(int)
  +increaseStockBy(int)
  +isExpiredOn(LocalDate) boolean
  +expiresWithin(int, LocalDate) boolean
  +checkCanDispense(String)
  +category()* String
}

class OTCMedication {
  +category() String
}

class PrescriptionMedication {
  +checkCanDispense(String)
  +category() String
}

class Customer {
  -id: int
  -name: String
  -phone: String
  -kind: CustomerType
  -history: List~String~
  +getId() int
  +getName() String
  +getKind() CustomerType
  +recordHistory(String)
  +registerPhone(String)
}

class Transaction {
  <<record>>
  id: int
  customerName: String
  customerId: int
  medName: String
  medId: int
  qty: int
  total: BigDecimal
  type: TransactionType
  timestamp: LocalDateTime
}

class CustomerType {
  <<enumeration>>
  WALK_IN
  REGISTERED
}

class TransactionType {
  <<enumeration>>
  DISPENSE
  RESTOCK
}

class InsufficientStockException
class ExpiredMedicationException

Pharmacy "1" o-- "0..*" Medication
Pharmacy "1" o-- "0..*" Customer
Pharmacy "1" *-- "0..*" Transaction
Medication <|-- OTCMedication
Medication <|-- PrescriptionMedication
Customer --> CustomerType
Transaction --> TransactionType
Medication ..> InsufficientStockException : throws
Pharmacy ..> ExpiredMedicationException : throws
```

## Design reasoning

**Key decisions, and what I chose them over**

- **Subclasses instead of a `category` string.** `OTCMedication` and `PrescriptionMedication` extend an abstract `Medication`, rather than one class holding `"OTC"`/`"PRESCRIPTION"`. A typo such as `"Otc"` can no longer silently skip every rule, and each subclass carries its own dispensing rule instead of `Pharmacy` branching on the type.
- **Enums instead of free-text strings.** `CustomerType` and `TransactionType` limit the values to ones the program actually handles, so a fake category can't get in.
- **Private fields with guarded methods instead of public fields.** Stock only changes through `reduceStockBy`/`increaseStockBy`, which reject zero or negative amounts and over-dispensing. Objects are validated in their constructors, so none can exist in an invalid state, and stock can no longer reach -61.
- **`BigDecimal` instead of `double` for money,** so totals like 12.50 × 0.95 don't pick up rounding errors.
- **Validate everything before changing anything.** `dispense` and `restock` build the `Transaction` record (which validates quantity and total) *before* touching stock, and `reduceStockBy`/`increaseStockBy` reject zero as well as negative quantities. A rejected operation therefore leaves stock, the transaction log and the customer's history exactly as they were, instead of changing stock and then failing to record it.
- **`Transaction` as an immutable `record`, and `getTransactions()` returning a copy,** instead of handing out the live list for callers to edit.

**How the coming change fits in**

- Each `Medication` subclass carries its own dispensing rule by overriding `checkCanDispense(prescriber)`: `OTCMedication` adds none, and `PrescriptionMedication` requires a prescriber. `Pharmacy` has a single `dispense(Customer, Medication, qty, prescriber)` that calls the rule polymorphically, then applies the shared expiry, stock and pricing steps.
- To add a new kind of medication (e.g. controlled drugs), you write one new subclass that overrides `checkCanDispense` and `category()`. `Pharmacy`, the existing medication classes, `Customer` and `Transaction` are not modified. `PharmacyTest.newMedicationTypeWorksWithoutChangingPharmacy` proves this with a controlled-drug subclass defined only in the test.

**What the old version would have forced you to touch**

- `dispense()` was one `if (m.category.equals("OTC")) … else if (…"PRESCRIPTION")` block, so the new kind meant another branch in the middle of working code. That branch had to copy the price calculation, the hardcoded `0.95` discount and the unguarded `m.stock = m.stock - qty` again.
- Nothing stopped code elsewhere from writing to the public `stock`/`price` fields directly. And a misspelled category string would have fallen through every branch while still recording a dispense transaction.

## One improvement beyond the brief

**Expiry checks.** Medicine was only checked for expiry when it was first registered, so a box that expired on the shelf could still be dispensed.

- **What it does:**
  - `dispense` refuses expired stock with an `ExpiredMedicationException`, before any stock is touched.
  - The end-of-day report lists anything expired ("remove from shelf") or expiring within 30 days.
- **Why it matters to the business:** giving a patient expired medicine is a patient-safety and regulatory failure, the same kind of risk as the most dangerous defect above. The 30-day warning lets Akosua sell the stock or return it to the supplier before it becomes a write-off.
- **Why it helps the next developer:**
  - The expiry rules live on `Medication` (`isExpiredOn`, `expiresWithin`), so any new medication type inherits them.
  - `Pharmacy` takes a `Clock`, so tests can pin "today" to a fixed date.
  - 19 unit tests cover the feature, including the expiry day itself and the 30-day boundary.


## Code run output

### Before

```text
Dispensed 2 x Paracetamol 500mg to Kofi Mensah for GHS 25.0
Dispensed 1 x Amoxicillin 250mg to Ama Owusu (prescribed by Dr. Boateng) for GHS 28.5
Restocked 50 x Paracetamol 500mg
Dispensed 100 x Amoxicillin 250mg to Kofi Mensah (prescribed by Dr. Boateng) for GHS 3000.0

=== Akosua's Pharmacy - end of day ===
Stock levels:
  Paracetamol 500mg (OTC): 148 in stock
  Amoxicillin 250mg (PRESCRIPTION): -61 in stock
Transactions today: 4
```

### After

```text
Dispensed 2 x Paracetamol 500mg to Kofi Mensah for GHS 25.0
Dispensed 1 x Amoxicillin 250mg to Ama Owusu (prescribed by Dr. Boateng) for GHS 28.5
Restocked 50 x Paracetamol 500mg
2026-11-12 13:52:55 WARNING PharmacyTracker - InsufficientStockException at Pharmacy.dispense(PharmacyTracker.java:132): Insufficient stock for Amoxicillin 250mg (id 2)  : requested 100, available 39

=== Akosua's Pharmacy - end of day ===
Stock levels:
  Paracetamol 500mg (OTC): 148 in stock
  Amoxicillin 250mg (PRESCRIPTION): 39 in stock
Transactions today: 3
Expiry check:
  Expiring within 30 days: Paracetamol 500mg (expires 2026-12-12, 148 in stock)
```
