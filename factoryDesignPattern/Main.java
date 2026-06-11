
package factoryDesignPattern;

public class Main {

	public static void main(String[] args) {

		Notification notification1 = NotificationFactory.createNotification("EMAIL");

		notification1.send();

		Notification notification2 = NotificationFactory.createNotification("SMS");

		notification2.send();

		Notification notification3 = NotificationFactory.createNotification("PUSH");

		notification3.send();
	}
}