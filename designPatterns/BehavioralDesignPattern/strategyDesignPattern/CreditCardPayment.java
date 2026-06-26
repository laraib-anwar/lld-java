public class CreditCardPayment implements PaymentStrategy {
    private String name;
    private String cardNumber;
    private String cvv;
    private String expiryDate;
 
    public CreditCardPayment(String name, String cardNumber, String cvv, String expiryDate) {
        this.name = name;
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.expiryDate = expiryDate;
    }
 
    @Override
    public void pay(double amount) {
        System.out.println("Processing $" + amount + " payment with Credit Card belonging to " + name + ".");
    }
}