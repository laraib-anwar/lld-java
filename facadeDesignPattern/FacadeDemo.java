package facadeDesignPattern;

public class FacadeDemo {

	public static void main(String[] args) {

		BankFacade bankFacade = new BankFacade();

		bankFacade.withdraw(
				"ACC123",
				"1234",
				500.0);
	}
}
