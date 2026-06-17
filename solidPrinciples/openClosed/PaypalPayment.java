class PaypalPayment implements PaymentMethod {
    @Override
    public void pay() {
        System.out.println("Paying through PayPal");
    }
}