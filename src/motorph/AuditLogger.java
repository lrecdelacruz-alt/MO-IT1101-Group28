package motorph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for recording timestamped system actions
 * to the application's audit log.
 */
public class AuditLogger {

    // Resolves the log file location to keep it consistent
    // across different IDEs and execution environments.
    private static final String LOG_FILE = resolveLogFile();

    private static String resolveLogFile() {
        String fileName = "motorph_audit.log";
        try {
            File classLocation = new File(AuditLogger.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());
            return new File(classLocation.getParentFile(), fileName).getPath();
        } catch (Exception e) {
            return fileName; // fall back to working-directory-relative if resolution fails
        }
    }
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static String currentRole = "system";

    /**
     * Sets the currently logged-in role so log entries
     * are attributed correctly.
     */
    public static void setRole(String role) {
        currentRole = role != null ? role : "system";
    }

    /**
    * Writes a timestamped entry to the audit log.
    *
    * @param action  Description of the action performed.
    * @param details Additional information about the action.
    */
    public static void log(String action, String details) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String entry = "[" + timestamp + "] [" + currentRole.toUpperCase() + "] "
                     + action + " — " + details;

        System.out.println(entry);

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(new File(LOG_FILE), true))) {
            writer.write(entry);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("AuditLogger: could not write to log file — " + e.getMessage());
        }
    }
}