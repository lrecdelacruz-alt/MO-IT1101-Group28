package motorph;

// AUTH SERVICE CLASS
// Handles user authentication for the MotorPH Payroll System.
//
// Responsibilities:
//   - Validate login credentials against stored values
//   - Provide logout and user validation utility methods
//
// NOTE: For Milestone 1, credentials are hardcoded.
//       Future milestones may integrate a user database or file.

public class AuthService {

    // ATTRIBUTES

    // Stored credentials for the default admin account
    private String username = "admin";
    private String password = "admin123";

    // AUTHENTICATION METHODS

    /**
     * Validates the provided credentials against the stored username
     * and password. Returns true only if both match exactly.
     *
     * @param inputUsername Username entered by the user
     * @param inputPassword Password entered by the user
     * @return true if credentials are valid, false otherwise
     */
    public boolean login(String inputUsername, String inputPassword) {
        if (!validateUser(inputUsername, inputPassword)) {
            return false;
        }
        return inputUsername.equals(username)
            && inputPassword.equals(password);
    }

    /**
     * Checks that the provided username and password are both
     * non-null and non-empty. Used as a pre-check before login.
     *
     * @param inputUsername Username to validate
     * @param inputPassword Password to validate
     * @return true if both fields are non-null and non-empty
     */
    public boolean validateUser(String inputUsername, String inputPassword) {
        return inputUsername != null && !inputUsername.trim().isEmpty()
            && inputPassword != null && !inputPassword.trim().isEmpty();
    }

    /**
     * Logs out the current user.
     * For MS1, this prints a confirmation to the console.
     * Future milestones may clear session tokens or user state here.
     */
    public void logout() {
        System.out.println("User logged out successfully.");
    }
}
