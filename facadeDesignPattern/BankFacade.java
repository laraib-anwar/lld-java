package facadeDesignPattern;

class BankFacade {

	private Account account = new Account();
	private Security security = new Security();
	private Funds funds = new Funds();
	private Notification notification = new Notification();

	public void withdraw(String accountId,
			String pin,
			double amount) {

		if (account.isAccountPresent(accountId)
				&& security.isPinValid(pin)
				&& funds.isSufficientBalance(amount)) {

			funds.debit(amount);
			notification.sendNotification();

		} else {
			System.out.println("Withdrawal failed.");
		}
	}
}
