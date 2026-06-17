package proxyDesignPattern;

public class Main {

	public static void main(String[] args) {

		UserService realService = new UserServiceImpl();

		UserService adminProxy = new UserServiceProxy(
				realService,
				"ADMIN");

		UserService userProxy = new UserServiceProxy(
				realService,
				"USER");

		System.out.println("ADMIN TRYING:");

		adminProxy.getUser("User123");
		adminProxy.deleteUser("User123");

		System.out.println();

		System.out.println("USER TRYING:");

		userProxy.getUser("User123");
		userProxy.deleteUser("User123");
	}
}