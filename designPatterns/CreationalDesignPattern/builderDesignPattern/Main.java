public class Main {
    public static void main(String[] args) {
        EmailBuilder builder = new EmailBuilder();
        Email email = builder
                            .setTo("contact@nailyourinterview.org")
                            .setSubject("Request for Java Multithreading Content")
                            .setBody("Hi Shubh, ....")
                            .build();
 
        // email is immutable because it does not have setters and the attributes are private.
        System.out.println(email);
    }
}