// Open for extension, closed for modification
public class Payment {
    private final PaymentMethod paymentMethod;
 
    // constructor based dependency injection
    public Payment(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
 
    public void pay() {
        paymentMethod.pay();
    }
}