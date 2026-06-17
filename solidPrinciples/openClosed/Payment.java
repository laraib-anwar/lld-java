class GooglePayPayment implements PaymentMethod {
    @Override
    public void pay() {
        System.out.println("Paying through Google Pay");
    }
}