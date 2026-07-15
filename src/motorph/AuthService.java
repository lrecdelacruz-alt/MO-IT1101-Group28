package motorph;

/**
 * Handles user authentication and role management
 * for the MotorPH Payroll System.
 */

public class AuthService {

    private String adminUsername = "admin";
    private String adminPassword = "admin123";

    private String employeeUsername = "employee";
    private String employeePassword = "employee123";

    // Stores the current user's role so the
    // appropriate menu can be displayed.
    private String loggedInRole = "";

    /**
    * Authenticates a user using the provided credentials.
    *
    * @param inputUsername Username entered by the user.
    * @param inputPassword Password entered by the user.
    * @return true if the credentials are valid; otherwise false.
    */
    public boolean login(String inputUsername, String inputPassword) {
        if (!validateUser(inputUsername, inputPassword)) {
            return false;
        }

        if (inputUsername.equals(adminUsername) && inputPassword.equals(adminPassword)) {
        loggedInRole = "admin";
        AuditLogger.setRole("admin");
        AuditLogger.log("LOGIN", "Admin logged in");
        return true;
    }
    if (inputUsername.equals(employeeUsername) && inputPassword.equals(employeePassword)) {
        loggedInRole = "employee";
        AuditLogger.setRole("employee");
        AuditLogger.log("LOGIN", "Employee role logged in");
        return true;
    }

        return false;
    }

    /** @return "admin", "employee", or "" if no one is currently logged in */
    public String getLoggedInRole() {
        return loggedInRole;
    }

    /**
    * Validates that the username and password
    * are both non-null and non-empty.
    *
    * @return true if both inputs are valid.
    */
    public boolean validateUser(String inputUsername, String inputPassword) {
        return inputUsername != null && !inputUsername.trim().isEmpty()
                && inputPassword != null && !inputPassword.trim().isEmpty();
    }

    /**
    * Logs out the current user and resets
    * the active session information.
    *
    * Future milestones may extend this method
    * with additional session management.
    */
    public void logout() {
        AuditLogger.log("LOGOUT", "User logged out");
        loggedInRole = "";
        AuditLogger.setRole("system");
        System.out.println("User logged out successfully.");
    }
}
    
