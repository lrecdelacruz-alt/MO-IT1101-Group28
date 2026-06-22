package motorph;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.util.List;

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
            AuthService authService = new AuthService();

            // Load employee data from CSV. If a required file is missing,
            // tell the user immediately instead of opening with silent,
            // unexplained empty data.
            String loadWarning = payrollService.loadEmployees();
            if (!loadWarning.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                        loadWarning + "The app will still open, but employee "
                        + "data may be missing or incomplete.",
                        "File Not Found", JOptionPane.WARNING_MESSAGE);
            }

            // AUTHENTICATION LOOP
            // Keeps prompting until valid credentials are entered
            // or the user cancels (which exits the application).
            showLoginScreen(payrollService, authService);
    });
}

    // MAIN MENU WINDOW
    /**
     * Builds and displays the main navigation menu as a JFrame. Stays open
     * while sub-windows (EmployeeMenu, PayrollMenu) are open. Exit button logs
     * out and closes the entire application.
     *
     * Uses pack() instead of setSize() so the window auto-sizes to fit its
     * contents exactly — no clipped buttons regardless of screen scaling or OS.
     *
     * @param payrollService Shared PayrollService instance
     * @param authService Shared AuthService instance
     */

    /**
     * Shows the login dialogs and routes to the correct menu.
     * Called on first launch and again after every logout — this is
     * what replaces the while(true) + latch approach. Instead of
     * blocking the EDT, logout simply calls this method again directly.
     */
    private static void showLoginScreen(PayrollService payrollService,
            AuthService authService) {

        // --- Username ---
        String username = JOptionPane.showInputDialog(
                null,
                "Enter Username:",
                "MotorPH Login",
                JOptionPane.PLAIN_MESSAGE);

        if (username == null) {
            int confirm = JOptionPane.showConfirmDialog(null,
                    "Are you sure you want to exit?",
                    "Exit Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) System.exit(0);
            showLoginScreen(payrollService, authService); // try again
            return;
        }

        if (username.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Username cannot be empty. Please try again.",
                    "Missing Input", JOptionPane.WARNING_MESSAGE);
            showLoginScreen(payrollService, authService);
            return;
        }

        // --- Password ---
        JPasswordField passwordField = new JPasswordField();
        int passOption = JOptionPane.showConfirmDialog(
                null,
                new Object[]{"Enter Password:", passwordField},
                "MotorPH Login",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (passOption != JOptionPane.OK_OPTION) {
            int confirm = JOptionPane.showConfirmDialog(null,
                    "Are you sure you want to exit?",
                    "Exit Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) System.exit(0);
            showLoginScreen(payrollService, authService);
            return;
        }

        String password = new String(passwordField.getPassword());

        if (password.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Password cannot be empty. Please try again.",
                    "Missing Input", JOptionPane.WARNING_MESSAGE);
            showLoginScreen(payrollService, authService);
            return;
        }

        // --- Validate ---
        if (!authService.login(username.trim(), password)) {
            JOptionPane.showMessageDialog(null,
                    "Invalid username or password.\nPlease try again.",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            showLoginScreen(payrollService, authService);
            return;
        }

        JOptionPane.showMessageDialog(null,
                "Welcome! Login successful.",
                "MotorPH Login", JOptionPane.INFORMATION_MESSAGE);

        // --- Route by role ---
        String role = authService.getLoggedInRole();
        if (role.equals("admin")) {
            showMainMenu(payrollService, authService);
        } else {
            String ownEmployeeID = promptForOwnEmployeeID(payrollService);
            if (ownEmployeeID == null) {
                JOptionPane.showMessageDialog(null,
                        "Could not verify your Employee ID. The application will now close.",
                        "MotorPH", JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            }
            showEmployeeRestrictedMenu(payrollService, authService, ownEmployeeID);
        }
    }
    private static void showMainMenu(PayrollService payrollService,
            AuthService authService) {

        JFrame menuFrame = new JFrame("MotorPH — Main Menu");
        menuFrame.setIconImage(createAppIcon());
        final EmployeeMenu[] openEmployeeMenu = new EmployeeMenu[1];
        final PayrollMenu[] openPayrollMenu = new PayrollMenu[1];
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
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);

        // --- Navigation Buttons ---
        JButton employeeMenuButton = new JButton("Employee Menu");
        JButton payrollMenuButton = new JButton("Payroll Menu");
        JButton logoutButton = new JButton("Logout");
        JButton exitButton = new JButton("Exit");

        employeeMenuButton.setPreferredSize(new Dimension(160, 36));
        payrollMenuButton.setPreferredSize(new Dimension(160, 36));
        logoutButton.setPreferredSize(new Dimension(90, 36));
        exitButton.setPreferredSize(new Dimension(80, 36));

        // Employee/Payroll Menu buttons use the same neutral-gray
        // navigation style as View/Process buttons in EmployeeMenu and
        // PayrollMenu — keeps button language consistent across all windows.
        Color neutralGray = new Color(190, 195, 200); // gray

        employeeMenuButton.setFont(new Font("Arial", Font.PLAIN, 13));
        employeeMenuButton.setForeground(Color.BLACK);
        employeeMenuButton.setBackground(neutralGray);
        employeeMenuButton.setBorderPainted(true);
        employeeMenuButton.setFocusPainted(false);
        employeeMenuButton.setOpaque(true);
        employeeMenuButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, neutralGray.brighter(), neutralGray.darker()));

        payrollMenuButton.setFont(new Font("Arial", Font.PLAIN, 13));
        payrollMenuButton.setForeground(Color.BLACK);
        payrollMenuButton.setBackground(neutralGray);
        payrollMenuButton.setBorderPainted(true);
        payrollMenuButton.setFocusPainted(false);
        payrollMenuButton.setOpaque(true);
        payrollMenuButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, neutralGray.brighter(), neutralGray.darker()));

        // Logout — amber/orange to distinguish from Exit (red) and nav buttons (gray)
        Color logoutColor = new Color(230, 120, 0);
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setBackground(logoutColor);
        logoutButton.setBorderPainted(true);
        logoutButton.setFocusPainted(false);
        logoutButton.setOpaque(true);
        logoutButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, logoutColor.brighter(), logoutColor.darker()));

        // Exit ends the session completely — styled red
        Color exitColor = new Color(183, 28, 28);
        exitButton.setForeground(Color.WHITE);
        exitButton.setBackground(exitColor);
        exitButton.setBorderPainted(true);
        exitButton.setFocusPainted(false);
        exitButton.setOpaque(true);
        exitButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, exitColor.brighter(), exitColor.darker()));

        // All three buttons in one FlowLayout row.
        // Exit is added last so it naturally sits on the right,
        // following standard UI conventions to prevent accidental clicks.
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 18, 14));
        buttonPanel.add(employeeMenuButton);
        buttonPanel.add(payrollMenuButton);
        buttonPanel.add(logoutButton);
        buttonPanel.add(exitButton);

        // --- Layout Assembly ---
        menuFrame.add(titlePanel, BorderLayout.NORTH);
        // Empty strut in CENTER gives the layout vertical breathing room
        // so pack() calculates the correct full height
        menuFrame.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        menuFrame.add(buttonPanel, BorderLayout.SOUTH);

        // --- Button Event Listeners ---
        employeeMenuButton.addActionListener(e -> {
            if (openEmployeeMenu[0] == null) {
                openEmployeeMenu[0] = new EmployeeMenu(payrollService);
                openEmployeeMenu[0].displayMenu();
                openEmployeeMenu[0].onCloseClearReference(() -> openEmployeeMenu[0] = null);
            } else {
                openEmployeeMenu[0].bringToFront();
            }
        });

        payrollMenuButton.addActionListener(e -> {
            if (openPayrollMenu[0] == null) {
                openPayrollMenu[0] = new PayrollMenu(payrollService);
                openPayrollMenu[0].displayPayrollMenu();
                openPayrollMenu[0].onCloseClearReference(() -> openPayrollMenu[0] = null);
            } else {
                openPayrollMenu[0].bringToFront();
            }
        });

        logoutButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    menuFrame,
                    "Are you sure you want to logout?\nYou will be returned to the login screen.",
                    "Confirm Logout",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            // Close any open sub-windows cleanly
            if (openEmployeeMenu[0] != null) {
                openEmployeeMenu[0].bringToFront();
            }
            if (openPayrollMenu[0] != null) {
                openPayrollMenu[0].bringToFront();
            }

            authService.logout();
            menuFrame.dispose();
            JOptionPane.showMessageDialog(null,
                    "You have been logged out successfully.\nPlease log in again to continue.",
                    "Logged Out", JOptionPane.INFORMATION_MESSAGE);
            payrollService.loadEmployees();
            showLoginScreen(payrollService, authService);
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

    // EMPLOYEE-ROLE MENU (restricted)
    /**
     * Restricted menu for the "employee" role. Employees can only look up
     * and view their own payslip — no access to the full employee table,
     * no add/update/delete, no other employees' data. Satisfies the
     * real-world requirement that employees cannot view each other's records.
     */
    private static void showEmployeeRestrictedMenu(PayrollService payrollService,
            AuthService authService, String ownEmployeeID) {

        JFrame menuFrame = new JFrame("MotorPH — Employee Self-Service");
        menuFrame.setIconImage(createAppIcon());
        menuFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        menuFrame.setLayout(new BorderLayout(10, 10));

        menuFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                confirmExit(menuFrame, authService);
            }
        });

        JLabel titleLabel = new JLabel("Employee Self-Service", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(18, 0, 5, 0));

        JLabel subtitleLabel = new JLabel("Logged in as Employee #" + ownEmployeeID, SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitleLabel.setForeground(Color.GRAY);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);

        JTextArea outputArea = new JTextArea(14, 40);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane outputScroll = new JScrollPane(outputArea);

        JButton viewPayslipButton = new JButton("View My Payslip");
        JButton viewAttendanceButton = new JButton("View My Attendance");
        JButton logoutButton = new JButton("Logout");
        JButton exitButton = new JButton("Exit");
        viewPayslipButton.setPreferredSize(new Dimension(160, 32));
        viewAttendanceButton.setPreferredSize(new Dimension(180, 32));
        logoutButton.setPreferredSize(new Dimension(90, 32));
        exitButton.setPreferredSize(new Dimension(100, 32));

        Color neutralGray = new Color(190, 195, 200); // gray
        viewPayslipButton.setFont(new Font("Arial", Font.PLAIN, 13));
        viewPayslipButton.setForeground(Color.BLACK);
        viewPayslipButton.setBackground(neutralGray);
        viewPayslipButton.setBorderPainted(true);
        viewPayslipButton.setFocusPainted(false);
        viewPayslipButton.setOpaque(true);
        viewPayslipButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, neutralGray.brighter(), neutralGray.darker()));
                
        viewAttendanceButton.setFont(new Font("Arial", Font.PLAIN, 13));
        viewAttendanceButton.setForeground(Color.BLACK);
        viewAttendanceButton.setBackground(neutralGray);
        viewAttendanceButton.setBorderPainted(true);
        viewAttendanceButton.setFocusPainted(false);
        viewAttendanceButton.setOpaque(true);
        viewAttendanceButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, neutralGray.brighter(), neutralGray.darker()));

        Color logoutColor = new Color(230, 120, 0);
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setBackground(logoutColor);
        logoutButton.setBorderPainted(true);
        logoutButton.setFocusPainted(false);
        logoutButton.setOpaque(true);
        logoutButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, logoutColor.brighter(), logoutColor.darker()));

        Color exitColor = new Color(183, 28, 28);
        exitButton.setForeground(Color.WHITE);
        exitButton.setBackground(exitColor);
        exitButton.setBorderPainted(true);
        exitButton.setFocusPainted(false);
        exitButton.setOpaque(true);
        exitButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, exitColor.brighter(), exitColor.darker()));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttonPanel.add(viewPayslipButton);
        buttonPanel.add(viewAttendanceButton);
        buttonPanel.add(logoutButton);
        buttonPanel.add(exitButton);

        menuFrame.add(titlePanel, BorderLayout.NORTH);
        menuFrame.add(outputScroll, BorderLayout.CENTER);
        menuFrame.add(buttonPanel, BorderLayout.SOUTH);

        viewAttendanceButton.addActionListener(e -> {
            List<String[]> records = CSVHandler.readAttendanceForEmployee(
                    PayrollService.ATTENDANCE_FILE, ownEmployeeID);

            StringBuilder sb = new StringBuilder();
            sb.append("Attendance Records — Employee #").append(ownEmployeeID).append("\n");
            sb.append("---------------------------------------------------\n");
            sb.append(String.format("%-12s %-10s %-10s %s%n", "Date", "Log In", "Log Out", "Hours"));
            sb.append("---------------------------------------------------\n");

            if (records.isEmpty()) {
                sb.append("No attendance records found.");
            } else {
                for (String[] r : records) {
                    sb.append(String.format("%-12s %-10s %-10s %s%n",
                            r[0], r[1], r[2], r[3]));
                }
                sb.append("---------------------------------------------------\n");
                sb.append("Total Records : ").append(records.size());
            }

            outputArea.setText(sb.toString());
        });

        viewPayslipButton.addActionListener(e -> {
            // No ID prompt here — always shows the ID locked in at login,
            // so this screen can never display anyone else's payslip.
            outputArea.setText(payrollService.generatePayslip(ownEmployeeID));
        });

        logoutButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    menuFrame,
                    "Are you sure you want to logout?\nYou will be returned to the login screen.",
                    "Confirm Logout",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            authService.logout();
            menuFrame.dispose();
            JOptionPane.showMessageDialog(null,
                    "You have been logged out successfully.\nPlease log in again to continue.",
                    "Logged Out", JOptionPane.INFORMATION_MESSAGE);
            payrollService.loadEmployees();
            showLoginScreen(payrollService, authService);
        });

        exitButton.addActionListener(e -> confirmExit(menuFrame, authService));

        menuFrame.pack();
        menuFrame.setMinimumSize(new Dimension(480, 420));
        menuFrame.setLocationRelativeTo(null);
        menuFrame.setVisible(true);
    }

    /**
         * Builds a simple programmatic icon (no external image file needed)
         * so every window shares one consistent identity.
         */
        private static Image createAppIcon() {
            java.awt.image.BufferedImage icon =
                new java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = icon.createGraphics();
            g.setColor(new Color(21, 101, 192)); // same blue as Compute Salaries button
            g.fillOval(2, 2, 28, 28);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString("M", 10, 22);
            g.dispose();
            return icon;
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
    // EMPLOYEE ID VERIFICATION (login-time security gate)
/**
 * Asks the logged-in "employee" role user to confirm their own Employee ID.
 * Verifies the ID actually exists in the system before accepting it.
 *
 * This exists because the "employee" role is a single shared login, not
 * an individual account per person — without this step, anyone logged in
 * as "employee" could view any other employee's payslip just by typing
 * a different ID into the payslip lookup field. Locking the ID in once,
 * here, closes that gap: the self-service screen never asks for an ID
 * again, it only ever shows the one confirmed at login.
 *
 * @return the verified Employee ID, or null if the user cancelled or
 *         could not provide a valid ID after repeated attempts
 */
private static String promptForOwnEmployeeID(PayrollService payrollService) {

    while (true) {
        String inputID = JOptionPane.showInputDialog(
                null,
                "Please enter your Employee ID to continue:",
                "Confirm Your Identity",
                JOptionPane.PLAIN_MESSAGE);

        if (inputID == null) {
            // User clicked Cancel — ask if they really want to exit
            int confirm = JOptionPane.showConfirmDialog(
                    null,
                    "You must confirm your Employee ID to continue.\n"
                    + "Do you want to exit instead?",
                    "Confirmation Required",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                return null;
            }
            continue;
        }

        inputID = inputID.trim();

        if (inputID.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Employee ID cannot be empty.",
                    "Missing Input", JOptionPane.WARNING_MESSAGE);
            continue;
        }

        if (payrollService.findEmployee(inputID) == null) {
            JOptionPane.showMessageDialog(null,
                    "Employee ID \"" + inputID + "\" was not found.\n"
                    + "Please check the number and try again.",
                    "Employee Not Found", JOptionPane.ERROR_MESSAGE);
            continue;
        }

        return inputID;
    }
}
}