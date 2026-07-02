import java.util.Map;
import java.util.HashMap;

// ==========================================
// 1. State Design Pattern Interfaces
// ==========================================
interface ATMState {
    void insertCard();
    void ejectCard();
    void insertPin(int pin);
    void withdrawCash(int amount);
}

// ==========================================
// 2. Context Class (The ATM Machine)
// ==========================================
class ATMMachine {
    private final ATMState idleState;
    private final ATMState hasCardState;
    private final ATMState authenticatedState;
    private final ATMState outOfCashState;

    private ATMState currentState;
    private int cashInventory;

    public ATMMachine(int initialCash) {
        this.idleState = new IdleState(this);
        this.hasCardState = new HasCardState(this);
        this.authenticatedState = new AuthenticatedState(this);
        this.outOfCashState = new OutOfCashState(this);

        this.cashInventory = initialCash;
        this.currentState = (initialCash > 0) ? idleState : outOfCashState;
    }

    // State Mutators
    public void setATMState(ATMState newState) { this.currentState = newState; }
    public void deductCash(int amount) { this.cashInventory -= amount; }
    
    // Getters for individual states
    public ATMState getIdleState() { return idleState; }
    public ATMState getHasCardState() { return hasCardState; }
    public ATMState getAuthenticatedState() { return authenticatedState; }
    public ATMState getOutOfCashState() { return outOfCashState; }
    public int getCashInventory() { return cashInventory; }

    // Public Operations delegating behavior directly to the current State
    public void insertCard() { currentState.insertCard(); }
    public void ejectCard() { currentState.ejectCard(); }
    public void insertPin(int pin) { currentState.insertPin(pin); }
    public void withdrawCash(int amount) { currentState.withdrawCash(amount); }
}

// ==========================================
// 3. Concrete State Implementations
// ==========================================

class IdleState implements ATMState {
    private final ATMMachine atm;
    public IdleState(ATMMachine atm) { this.atm = atm; }

    @Override
    public void insertCard() {
        System.out.println("💳 Card Inserted successfully.");
        atm.setATMState(atm.getHasCardState());
    }
    @Override
    public void ejectCard() { System.out.println("❌ No card found in the machine."); }
    @Override
    public void insertPin(int pin) { System.out.println("❌ Please insert your card first."); }
    @Override
    public void withdrawCash(int amount) { System.out.println("❌ Insert card to process transaction."); }
}

class HasCardState implements ATMState {
    private final ATMMachine atm;
    public HasCardState(ATMMachine atm) { this.atm = atm; }

    @Override
    public void insertCard() { System.out.println("❌ A card is already present in the slot."); }
    @Override
    public void ejectCard() {
        System.out.println("🔓 Card ejected. Please collect your card.");
        atm.setATMState(atm.getIdleState());
    }
    @Override
    public void insertPin(int pin) {
        if (pin == 1234) { // Mock verification logic
            System.out.println("✅ PIN Authenticated successfully.");
            atm.setATMState(atm.getAuthenticatedState());
        } else {
            System.out.println("❌ Incorrect PIN entered. Try again.");
            ejectCard();
        }
    }
    @Override
    public void withdrawCash(int amount) { System.out.println("❌ Enter your PIN first."); }
}

class AuthenticatedState implements ATMState {
    private final ATMMachine atm;
    public AuthenticatedState(ATMMachine atm) { this.atm = atm; }

    @Override
    public void insertCard() { System.out.println("❌ Card already present."); }
    @Override
    public void ejectCard() {
        System.out.println("🔓 Card ejected. Thank you for visiting.");
        atm.setATMState(atm.getIdleState());
    }
    @Override
    public void insertPin(int pin) { System.out.println("❌ Already authenticated."); }
    @Override
    public void withdrawCash(int amount) {
        if (amount > atm.getCashInventory()) {
            System.out.println("❌ Transaction Failed: Insufficient cash in the ATM vault.");
            ejectCard();
        } else {
            atm.deductCash(amount);
            System.out.println("💵 Success! Dispensing INR " + amount + " in cash.");
            System.out.println("Remaining ATM balance: " + atm.getCashInventory());
            
            if (atm.getCashInventory() <= 0) {
                atm.setATMState(atm.getOutOfCashState());
                System.out.println("🚨 Notice: ATM is now out of service (Out of Cash).");
            } else {
                ejectCard(); // Auto eject card after successful transaction
            }
        }
    }
}

class OutOfCashState implements ATMState {
    private final ATMMachine atm;
    public OutOfCashState(ATMMachine atm) { this.atm = atm; }

    @Override public void insertCard() { System.out.println("🚨 Out of Service: No cash available in this machine."); }
    @Override public void ejectCard() { System.out.println("❌ No card present."); }
    @Override public void insertPin(int pin) { System.out.println("🚨 Machine is empty."); }
    @Override public void withdrawCash(int amount) { System.out.println("🚨 Machine is empty."); }
}

// ==========================================
// 4. Main Execution Driver
// ==========================================
public class AtmMachineDriver{
    public static void main(String[] args) {
        // Step A: Initialize an ATM Machine with INR 5,000 vault balance
        System.out.println("=== INITIALIZING ATM SYSTEM ===");
        ATMMachine hybridATM = new (5000); 

        // Step B: Execution Flow 1 - Successful WATMMachineithdrawal
        System.out.println("\n--- RUNNING TRANSACTION 1 ---");
        hybridATM.insertCard();
        hybridATM.insertPin(1234);
        hybridATM.withdrawCash(2000);

        // Step C: Execution Flow 2 - Attempting an operation out of sequence
        System.out.println("\n--- RUNNING TRANSACTION 2 (Error Handling Test) ---");
        hybridATM.insertPin(1234); // Should complain because card isn't inserted
        
        // Step D: Execution Flow 3 - Running the ATM to empty limit
        System.out.println("\n--- RUNNING TRANSACTION 3 (Vault Depletion Test) ---");
        hybridATM.insertCard();
        hybridATM.insertPin(1234);
        hybridATM.withdrawCash(4000); // Tries to grab 4000, but only 3000 remains
    }
}