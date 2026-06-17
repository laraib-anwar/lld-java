// Handles only database-related operations.
public class UserRepository {
    public void save(User user) {
        System.out.println("Saving user to database: " + user.getName());
    }
 
    // Other methods like read, update, delete can go here.
}