## Diagnosis.md
A detailed account of defects I found in the program ranked by severity level and business risk.

No.| Issue   | Where | Root cause | Business Risk | Severity |
|--|--------|----------|-----|--------|--------|
|1. | No validation of class properties on creation | Medication class | No check for price and other medication properties in the medication object creation process. | Inventory corruption and loss of revenue. | 1 |
|2. | No verification of restock quantity | restock() method of Pharmacy class | No check for quantity validity in the dispense function. Quantity can be negative or 0. | Incorrect transactions in the business and incorrect revenue calculations. | 1 |
|3. | Dispensing of out-of-stock drugs | in the dispense() method; Pharmacy class | No check for quantity of medication left before dispensing. | Customers pay for drugs that are not available. This leads to reduced trust in the pharmacy and its systems. | 1 |
|4.| Class fields are accessible by all code | Customer, Transaction, Medication and Pharmacy classes | Class fields are defined as public  | Modifications can be made to the data without verification. This leads to falsification of data and unauthorized access to Personal Identifiable data which is against the law | 1 |
|5.| Medication, Customers and Transactions are not standardized into their categories. Fake categories can be input without checks | Medication, Customer and Transaction classes | No predefined list of categories for CUSTOMER , TRANSACTIONS and MEDICATION classes. |  Incorrect data logging of dispenser events at the pharmacy. This leads to the occurrence of unacceptable events at the pharmacy. | 1 |
|6.| Data integrity issue: Returning a mutable list of company sensitive information | getTransactions() method in Pharmacy class | Direct return of the transactions list | Exposure of modifiable company data. | 1 |
|7. | Incomplete Transaction information | TRANSACTION class | Transaction class is missing vital information such as timestamp, medication ID and customer IDs | Hinders effective auditing of activities | 2 |
|8. | Potential drift of discount amounts | dispense() method in Pharmacy class| Hardcoding of discount amount in different places | Application of incorrect discount prices to customers as code changes. | 3 |





After running the program for the first time, the following output was obtained:
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
