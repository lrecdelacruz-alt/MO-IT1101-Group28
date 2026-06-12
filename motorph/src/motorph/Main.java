package motorph;

import javax.swing.*;
import java.awt.*;

// MAIN CLASS
// Entry point for the MotorPH Payroll System.
//
// Responsibilities:
//   - Initialize all service objects (PayrollService, AuthService)
//   - Load sample employee and attendance data
//   - Run the login authentication loop
//   - Display the main navigation menu

public class Main {

    public static void main(String[] args) {

        // Run the entire GUI on the Event Dispatch Thread (EDT)
        // as required by Java Swing thread safety rules
        SwingUtilities.invokeLater(() -> {

            // SERVICE INITIALIZATION

            PayrollService payrollService = new PayrollService();
            AuthService    authService    = new AuthService();

            // Load sample data for MS1 demonstration
            payrollService.loadEmployees();
            payrollService.loadAttendance();

            // AUTHENTICATION LOOP
            // Keeps prompting until valid credentials are entered
            // or the user cancels (which exits the application).

            boolean authenticated = false;

            while (!authenticated) {

                // --- Username Input ---
                String username = JOptionPane.showInputDialog(
                        null,
                        "Enter Username:",
                        "MotorPH Login",
                        JOptionPane.PLAIN_MESSAGE);

                // null means the user clicked Cancel or closed the dialog
                if (username == null) {
                    int confirm = JOptionPane.showConfirmDialog(
                            null,
                            "Are you sure you want to exit?",
                            "Exit Confirmation",
                            JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                    continue;
                }

                if (username.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(null,
                            "Username cannot be empty. Please try again.",
                            "Missing Input", JOptionPane.WARNING_MESSAGE);
                    continue;
                }

                // --- Password Input (masked via JPasswordField) ---
                JPasswordField passwordField = new JPasswordField();
                int passOption = JOptionPane.showConfirmDialog(
                        null,
                        new Object[]{"Enter Password:", passwordField},
                        "MotorPH Login",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);

                if (passOption != JOptionPane.OK_OPTION) {
                    int confirm = JOptionPane.showConfirmDialog(
                            null,
                            "Are you sure you want to exit?",
                            "Exit Confirmation",
                            JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                    continue;
                }

                String password = new String(passwordField.getPassword());

                if (password.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(null,
                            "Password cannot be empty. Please try again.",
                            "Missing Input", JOptionPane.WARNING_MESSAGE);
                    continue;
                }

                // --- Credential Validation via AuthService ---
                if (authService.login(username.trim(), password)) {
                    authenticated = true;
                    JOptionPane.showMessageDialog(null,
                            "Welcome! Login successful.",
                            "MotorPH Login", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Invalid username or password.\nPlease try again.",
                            "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
            }

            // MAIN NAVIGATION MENU
            // A non-blocking JFrame replaces the previous JOptionPane menu.
            //
            // WHY JFrame instead of JOptionPane here:
            //   JOptionPane.showOptionDialog() is a BLOCKING modal dialog.
            //   When the user opens EmployeeMenu or PayrollMenu (both JFrames),
            //   the JOptionPane stays frozen underneath, stealing focus and
            //   preventing interaction with the sub-windows.
            //   A JFrame-based menu is non-blocking: it stays open and visible
            //   while sub-windows are open, so users can freely switch between
            //   them and return to the main menu without losing their session.

            showMainMenu(payrollService, authService);
        });
    }

    // MAIN MENU WINDOW

    /**
     * Builds and displays the main navigation menu as a JFrame.
     * Stays open while sub-windows (EmployeeMenu, PayrollMenu) are open.
     * Exit button logs out and closes the entire application.
     *
     * Uses pack() instead of setSize() so the window auto-sizes to
     * fit its contents exactly — no clipped buttons regardless of
     * screen scaling or OS.
     *
     * @param payrollService Shared PayrollService instance
     * @param authService    Shared AuthService instance
     */
    private static void showMainMenu(PayrollService payrollService,
                                     AuthService authService) {

        JFrame menuFrame = new JFrame("MotorPH — Main Menu");
        menuFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        menuFrame.setLayout(new BorderLayout(10, 10));

        // Intercept the window X button to show an exit confirmation
        // instead of silently closing without logging out
        menuFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                confirmExit(menuFrame, authService);
            }
        });

        // --- Title ---
        JLabel titleLabel = new JLabel("MotorPH Payroll System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(18, 0, 5, 0));

        JLabel subtitleLabel = new JLabel("What would you like to do?", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitleLabel.setForeground(Color.GRAY);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(titleLabel,    BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);

        // --- Navigation Buttons ---
        JButton employeeMenuButton = new JButton("Employee Menu");
        JButton payrollMenuButton  = new JButton("Payroll Menu");
        JButton exitButton         = new JButton("Exit");

        employeeMenuButton.setPreferredSize(new Dimension(160, 36));
        payrollMenuButton.setPreferredSize(new Dimension(160, 36));
        exitButton.setPreferredSize(new Dimension(80, 36));

        // All three buttons in one FlowLayout row.
        // Exit is added last so it naturally sits on the right,
        // following standard UI conventions to prevent accidental clicks.
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 18, 14));
        buttonPanel.add(employeeMenuButton);
        buttonPanel.add(payrollMenuButton);
        buttonPanel.add(exitButton);

        // --- Layout Assembly ---
        menuFrame.add(titlePanel,  BorderLayout.NORTH);
        // Empty strut in CENTER gives the layout vertical breathing room
        // so pack() calculates the correct full height
        menuFrame.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        menuFrame.add(buttonPanel, BorderLayout.SOUTH);

        // --- Button Event Listeners ---

        employeeMenuButton.addActionListener(e -> {
            // Opens Employee Management as a separate JFrame.
            // The main menu stays open and accessible.
            EmployeeMenu empMenu = new EmployeeMenu(payrollService);
            empMenu.displayMenu();
        });

        payrollMenuButton.addActionListener(e -> {
            // Opens Payroll Menu as a separate JFrame.
            // The main menu stays open and accessible.
            PayrollMenu payMenu = new PayrollMenu(payrollService);
            payMenu.displayPayrollMenu();
        });

        exitButton.addActionListener(e -> confirmExit(menuFrame, authService));

        // pack() auto-sizes the window to fit all contents exactly.
        // Must run BEFORE setLocationRelativeTo() and setResizable()
        // so centering and sizing are based on the final window dimensions.
        menuFrame.pack();
        menuFrame.setMinimumSize(new Dimension(420, 220));
        menuFrame.setResizable(false);
        menuFrame.setLocationRelativeTo(null);
        menuFrame.setVisible(true);
    }

    // HELPER — Exit Confirmation

    private static void confirmExit(JFrame parent, AuthService authService) {
        int confirm = JOptionPane.showConfirmDialog(
                parent,
                "Are you sure you want to exit?",
                "Exit Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            authService.logout();
            JOptionPane.showMessageDialog(parent,
                    "You have been logged out. Goodbye!",
                    "MotorPH", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }
}
