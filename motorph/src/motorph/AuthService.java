package motorph;

// AUTH SERVICE CLASS
// Handles user authentication for the MotorPH Payroll System.
//
// Responsibilities:
//   - Validate login credentials against stored values
//   - Provide logout and user validation utility methods
//

public class AuthService {

    // ATTRIBUTES
    // Stored credentials for the default admin account
    private String adminUsername = "admin";
    private String adminPassword = "admin123";

    private String employeeUsername = "employee";
    private String employeePassword = "employee123";

    // Tracks which role logged in most recently, so Main can decide
    // which menu to show. "" means no one is logged in yet.
    private String loggedInRole = "";

    // AUTHENTICATION METHODS
    /**
     * Validates the provided credentials against the stored username and
     * password. Returns true only if both match exactly.
     *
     * @param inputUsername Username entered by the user
     * @param inputPassword Password entered by the user
     * @return true if credentials are valid, false otherwise
     */
    public boolean login(String inputUsername, String inputPassword) {
        if (!validateUser(inputUsername, inputPassword)) {
            return false;
        }

        if (inputUsername.equals(adminUsername) && inputPassword.equals(adminPassword)) {
            loggedInRole = "admin";
            return true;
        }

        if (inputUsername.equals(employeeUsername) && inputPassword.equals(employeePassword)) {
            loggedInRole = "employee";
            return true;
        }

        return false;
    }

    /** @return "admin", "employee", or "" if no one is currently logged in */
    public String getLoggedInRole() {
        return loggedInRole;
    }

    /**
     * Checks that the provided username and password are both non-null and
     * non-empty. Used as a pre-check before login.
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
     * Logs out the current user. For MS1, this prints a confirmation to the
     * console. Future milestones may clear session tokens or user state here.
     */
    public void logout() {
        loggedInRole = "";
        System.out.println("User logged out successfully.");
    }
}
    
