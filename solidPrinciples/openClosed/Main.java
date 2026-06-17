public class Main {
    public static void main(String[] args) {
        Payment paymentObj = new Payment(new GooglePayPayment());
        paymentObj.pay();
    }
}