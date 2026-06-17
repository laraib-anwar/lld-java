package facadeDesignPattern;

class Funds {

	public boolean isSufficientBalance(double amount) {
		System.out.println("Funds sufficient.");
		return true;
	}

	public void debit(double amount) {
		System.out.println("Amount " + amount + " debited.");
	}
}
