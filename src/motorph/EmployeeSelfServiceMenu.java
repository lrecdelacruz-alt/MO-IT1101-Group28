package motorph;

import javax.swing.*;
import java.awt.*;
import java.util.List;

// ============================================================
// EMPLOYEE SELF-SERVICE MENU CLASS
// Restricted GUI window for the "employee" role.
//
// Responsibilities:
//   - Let an employee view their own payslip
//   - Let an employee view their own attendance records
//   - Provide logout/exit, matching the admin-side windows
//
// Employees cannot view any other employee's data — the Employee ID
// is locked in once at login (see Main.promptForOwnEmployeeID) and
// never re-prompted here, so there is no way to type a different ID
// and see someone else's payslip.
//
// Extracted out of Main.java into its own class so every GUI window
// in the app (EmployeeMenu, PayrollMenu, and now this one) follows
// the same one-class-per-window structure — consistent modularity
// across the whole codebase, not just the admin-facing windows.
// ============================================================
public class EmployeeSelfServiceMenu {

    private JFrame    frame;
    private JTextArea outputArea;
    private JLabel    viewingLabel;

    private final PayrollService payrollService;
    private final AuthService    authService;
    private final String         ownEmployeeID;

    public EmployeeSelfServiceMenu(PayrollService payrollService,
            AuthService authService, String ownEmployeeID) {
        this.payrollService = payrollService;
        this.authService    = authService;
        this.ownEmployeeID  = ownEmployeeID;
    }

    /** Builds and displays the Employee Self-Service window. */
    public void display() {

        frame = new JFrame("MotorPH — Employee Self-Service (#" + ownEmployeeID + ")");
        frame.setIconImage(AppIcon.create());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                confirmExit();
            }
        });

        JLabel titleLabel = new JLabel("Employee Self-Service", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(18, 0, 5, 0));

        Employee self = payrollService.findEmployee(ownEmployeeID);
        String selfName = (self != null) ? self.getFullName() : "";
        JLabel subtitleLabel = new JLabel(
                "Logged in as: " + selfName + " (Employee #" + ownEmployeeID + ")",
                SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitleLabel.setForeground(Color.GRAY);

        // Shows which result is currently displayed below — gives a clear
        // visual cue (separate from the output text itself) of what the
        // user is looking at, instead of relying on reading the content.
        viewingLabel = new JLabel(" ", SwingConstants.CENTER);
        viewingLabel.setFont(new Font("Arial", Font.BOLD, 12));
        viewingLabel.setForeground(new Color(21, 101, 192));
        viewingLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(titleLabel,    BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.CENTER);
        titlePanel.add(viewingLabel,  BorderLayout.SOUTH);

        outputArea = new JTextArea(14, 40);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setBorder(BorderFactory.createTitledBorder("Output"));

        showHome();

        JScrollPane outputScroll = new JScrollPane(outputArea);

        JButton homeButton           = new JButton("Home");
        JButton viewPayslipButton    = new JButton("View My Payslip");
        JButton viewAttendanceButton = new JButton("View My Attendance");
        JButton logoutButton         = new JButton("Logout");
        JButton exitButton           = new JButton("Exit");
        homeButton.setPreferredSize(new Dimension(90, 32));
        viewPayslipButton.setPreferredSize(new Dimension(160, 32));
        viewAttendanceButton.setPreferredSize(new Dimension(180, 32));
        logoutButton.setPreferredSize(new Dimension(90, 32));
        exitButton.setPreferredSize(new Dimension(100, 32));

        homeButton.setToolTipText("Return to the welcome screen");
        viewPayslipButton.setToolTipText("View your personal payslip");
        viewAttendanceButton.setToolTipText("View your attendance records");
        logoutButton.setToolTipText("Log out and return to the login screen");
        exitButton.setToolTipText("Exit the application");

        UIHelper.styleButton(homeButton, UIHelper.GRAY, Color.BLACK, homeButton.getPreferredSize());
        Dimension navButtonSize = viewPayslipButton.getPreferredSize();
        UIHelper.styleButton(viewPayslipButton, UIHelper.GRAY, Color.BLACK, navButtonSize);
        UIHelper.styleButton(viewAttendanceButton, UIHelper.GRAY, Color.BLACK,
                viewAttendanceButton.getPreferredSize());

        UIHelper.styleButton(logoutButton, UIHelper.AMBER, Color.WHITE,
                logoutButton.getPreferredSize());
        UIHelper.styleButton(exitButton, UIHelper.RED, Color.WHITE,
                exitButton.getPreferredSize());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttonPanel.add(homeButton);
        buttonPanel.add(viewPayslipButton);
        buttonPanel.add(viewAttendanceButton);
        buttonPanel.add(logoutButton);
        buttonPanel.add(exitButton);

        frame.add(titlePanel,   BorderLayout.NORTH);
        frame.add(outputScroll, BorderLayout.CENTER);
        frame.add(buttonPanel,  BorderLayout.SOUTH);

        homeButton.addActionListener(e -> showHome());
        viewAttendanceButton.addActionListener(e -> showAttendance());
        viewPayslipButton.addActionListener(e -> showPayslip());

        logoutButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    frame,
                    "Are you sure you want to logout?\nYou will be returned to the login screen.",
                    "Confirm Logout",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            authService.logout();
            frame.dispose();
            JOptionPane.showMessageDialog(null,
                    "You have been logged out successfully.\nPlease log in again to continue.",
                    "Logged Out", JOptionPane.INFORMATION_MESSAGE);
            payrollService.loadEmployees();
            new LoginScreen(payrollService, authService).display();
        });

        exitButton.addActionListener(e -> confirmExit());

        frame.pack();
        frame.setMinimumSize(new Dimension(480, 440));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void showAttendance() {
        List<String[]> records = CSVHandler.readAttendanceForEmployee(
                PayrollService.ATTENDANCE_FILE, ownEmployeeID);
        AuditLogger.log("VIEW ATTENDANCE", "Employee #" + ownEmployeeID + " viewed their attendance");

        StringBuilder sb = new StringBuilder();
        sb.append("Attendance Records — Employee #").append(ownEmployeeID).append("\n");
        sb.append("---------------------------------------------------\n");
        sb.append(String.format("%-12s %-10s %-10s %s%n", "Date", "Log In", "Log Out", "Hours"));
        sb.append("---------------------------------------------------\n");

        if (records.isEmpty()) {
            sb.append("No attendance records found.");
        } else {
            double totalHours = 0;
            for (String[] r : records) {
                sb.append(String.format("%-12s %-10s %-10s %s%n",
                        r[0], r[1], r[2], r[3]));
                try {
                    totalHours += Double.parseDouble(r[3]);
                } catch (NumberFormatException ignored) {}
            }
            sb.append("---------------------------------------------------\n");
            sb.append(String.format("Total Records : %d%n", records.size()));
            sb.append(String.format("Total Hours   : %.2f hrs%n", totalHours));
        }

        viewingLabel.setText("Currently viewing: Attendance Records");
        outputArea.setText(sb.toString());
    }

    private void showPayslip() {
        viewingLabel.setText("Currently viewing: Payslip");
        outputArea.setText(payrollService.generatePayslip(ownEmployeeID));
        AuditLogger.log("VIEW PAYSLIP", "Employee #" + ownEmployeeID + " viewed their payslip");
    }

    private void showHome() {
        Employee self = payrollService.findEmployee(ownEmployeeID);
        String selfName = (self != null) ? self.getFullName() : "";

        viewingLabel.setText(" ");
        outputArea.setText(
            "Welcome, " + selfName + "!\n\n"
          + "Getting Started:\n"
          + "  1. Click 'View My Payslip' to see your latest computed pay.\n"
          + "  2. Click 'View My Attendance' to see your logged hours.\n\n"
          + "Note: If your payslip isn't available yet, your administrator\n"
          + "may not have run payroll computation yet. Please check back\n"
          + "later or contact HR.");
    }

    private void confirmExit() {
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Are you sure you want to exit?",
                "Exit Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            authService.logout();
            JOptionPane.showMessageDialog(frame,
                    "You have been logged out. Goodbye!",
                    "MotorPH", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }
}
