// Orchestrates all logic for registering a user.
public class UserService {
    private UserRepository repository = new UserRepository();
    private EmailService emailService = new EmailService();
    private LoggerService logger = new LoggerService();
 
    public void registerUser(String name, String email, String password) {
        // 1. Hash the password
        String hashed = Integer.toHexString(password.hashCode());
        User user = new User(name, email, hashed);
 
        // 2. Save to database
        repository.save(user);
 
        // 3. Send welcome email
        emailService.sendWelcomeEmail(user.getEmail());
 
        // 4. Log the event
        logger.log("New user registered: " + user.getName());
    }
}