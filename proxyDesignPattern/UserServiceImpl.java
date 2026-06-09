package proxyDesignPattern;

public class UserServiceImpl implements UserService {
	@Override
	public void getUser(String id){
     System.out.println("The user with id "+ id);
	}
	@Override
	public void deleteUser(String id){
     System.out.println("The user with "+ id + " is deleted successfully");
	}
}
