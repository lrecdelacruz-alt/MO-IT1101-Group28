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
                new LoginScreen(payrollService, authService).display();
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

    

    public static void launchMainMenu(PayrollService payrollService,
           AuthService authService) {

        JFrame menuFrame = new JFrame("MotorPH — Main Menu ("
                + authService.getLoggedInRole() + ")");
        menuFrame.setIconImage(AppIcon.create());
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

        employeeMenuButton.setToolTipText("Open the Employee Management window");
        payrollMenuButton.setToolTipText("Open the Payroll Operations window");
        logoutButton.setToolTipText("Log out and return to the login screen");
        exitButton.setToolTipText("Exit the application");

        // Employee/Payroll Menu buttons use the same neutral-gray
        // navigation style as View/Process buttons in EmployeeMenu and
        // PayrollMenu — keeps button language consistent across all windows.
        // Employee/Payroll Menu buttons use the same neutral-gray navigation
        // style as View/Process buttons elsewhere — all routed through
        // UIHelper.styleButton() for one single source of truth on button styling.
        UIHelper.styleButton(employeeMenuButton, UIHelper.GRAY, Color.BLACK,
                employeeMenuButton.getPreferredSize());
        UIHelper.styleButton(payrollMenuButton, UIHelper.GRAY, Color.BLACK,
                payrollMenuButton.getPreferredSize());
        UIHelper.styleButton(logoutButton, UIHelper.AMBER, Color.WHITE,
                logoutButton.getPreferredSize());
        UIHelper.styleButton(exitButton, UIHelper.RED, Color.WHITE,
                exitButton.getPreferredSize());

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
            new LoginScreen(payrollService, authService).display();
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

    /**
     * Launches the restricted self-service window for the "employee" role.
     * Delegates to EmployeeSelfServiceMenu, which is now its own class —
     * matching the same one-class-per-window structure used by
     * EmployeeMenu and PayrollMenu on the admin side.
     */
    public static void launchEmployeeMenu(PayrollService payrollService,
            AuthService authService, String ownEmployeeID) {
        new EmployeeSelfServiceMenu(payrollService, authService, ownEmployeeID).display();
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

    /**
     * Handles the employee role flow after login —
     * prompts for Employee ID then launches restricted menu.
     * Called by LoginScreen after successful employee login.
     */
    public static void launchEmployeeFlow(PayrollService payrollService,
            AuthService authService) {

        String ownEmployeeID = promptForOwnEmployeeID(payrollService);
        if (ownEmployeeID == null) {
            JOptionPane.showMessageDialog(null,
                    "Could not verify your Employee ID. The application will now close.",
                    "MotorPH", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }
        launchEmployeeMenu(payrollService, authService, ownEmployeeID);
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
            JTextField idField = new JTextField(15);
            idField.setFont(new Font("Arial", Font.PLAIN, 13));

            JLabel idLabel = new JLabel("Please enter your Employee ID:");
            idLabel.setFont(new Font("Arial", Font.PLAIN, 13));

            JLabel idHint = new JLabel("This confirms your identity for self-service access.");
            idHint.setFont(new Font("Arial", Font.ITALIC, 11));
            idHint.setForeground(Color.GRAY);

            SwingUtilities.invokeLater(idField::requestFocusInWindow);

            int result = JOptionPane.showConfirmDialog(
                    null,
                    new Object[]{idLabel, idHint, idField},
                    "Confirm Your Identity",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            String inputID = (result == JOptionPane.OK_OPTION)
                    ? idField.getText()
                    : null;

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