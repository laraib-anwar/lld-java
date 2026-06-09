package proxyDesignPattern;

public class UserServiceProxy implements UserService {

	private UserService userService;
	private String currentUserRole;

	public UserServiceProxy(
			UserService userService,
			String currentUserRole) {

		this.userService = userService;
		this.currentUserRole = currentUserRole;
	}

	@Override
	public void getUser(String id) {
		userService.getUser(id);
	}

	@Override
	public void deleteUser(String id) {

		if ("ADMIN".equals(currentUserRole)) {
			userService.deleteUser(id);
		} else {
			System.out.println(
					"Access Denied: Only Admins can delete users.");
		}
	}
}