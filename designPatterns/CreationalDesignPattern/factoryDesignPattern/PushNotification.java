package factoryDesignPattern;

public class PushNotification implements Notification {

	@Override
	public void send() {
		System.out.println("Sending Push Notification");
	}
}