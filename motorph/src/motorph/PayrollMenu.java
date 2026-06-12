package motorph;

import javax.swing.*;
import java.awt.*;

// PAYROLL MENU CLASS
// Provides the GUI window for payroll operations.

// Responsibilities:
//   - Process payroll (show hours + gross pay per employee)
//   - View individual employee payslip by ID
//   - Generate a full payroll report with totals and averages

public class PayrollMenu {

    // GUI COMPONENTS

    private JFrame    frame;
    private JTextArea outputArea;
    private JButton   processPayrollButton;
    private JButton   viewPayslipButton;
    private JButton   generateReportButton;

    // SERVICE REFERENCE

    private PayrollService payrollService;

    // CONSTRUCTOR

    public PayrollMenu(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    // MAIN DISPLAY METHOD

    /**
     * Builds and displays the Payroll Menu window.
     * Title and buttons appear at the top (NORTH panel);
     * the output area fills the center.
     */
    public void displayPayrollMenu() {

        frame = new JFrame("MotorPH — Payroll Menu");
        frame.setSize(550, 450);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        // --- Title ---
        JLabel titleLabel = new JLabel("Payroll Management", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));

        // --- Buttons ---
        processPayrollButton = new JButton("Process Payroll");
        viewPayslipButton    = new JButton("View Payslip");
        generateReportButton = new JButton("Generate Payroll Report");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(processPayrollButton);
        buttonPanel.add(viewPayslipButton);
        buttonPanel.add(generateReportButton);

        // --- North Panel: title + buttons stacked together ---
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(titleLabel,  BorderLayout.NORTH);
        northPanel.add(buttonPanel, BorderLayout.SOUTH);

        // --- Output Area ---
        // Read-only text area for payslips, summaries, and status messages
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setBorder(BorderFactory.createTitledBorder("Output"));
        JScrollPane outputScroll = new JScrollPane(outputArea);

        // --- Layout Assembly ---
        frame.add(northPanel,   BorderLayout.NORTH);
        frame.add(outputScroll, BorderLayout.CENTER);

        // --- Button Event Listeners ---
        processPayrollButton.addActionListener(e  -> processPayroll());
        viewPayslipButton.addActionListener(e     -> viewPayslip());
        generateReportButton.addActionListener(e  -> generatePayrollReport());

        frame.setVisible(true);
    }

    // PAYROLL OPERATIONS

    /**
     * Processes payroll for all employees and displays the result.
     * Shows each employee's name, hours worked, and gross pay.
     * Delegates computation to PayrollService.processPayroll().
     */
    public void processPayroll() {
        outputArea.setText(payrollService.processPayroll());
    }

    /**
     * Prompts the user for an employee ID and displays their full payslip.
     * Shows a warning if the ID field is empty or cancelled.
     * Delegates payslip generation to PayrollService.generatePayslip().
     */
    public void viewPayslip() {

        String empID = JOptionPane.showInputDialog(
                frame,
                "Enter Employee ID:",
                "View Payslip",
                JOptionPane.PLAIN_MESSAGE);

        // User clicked Cancel
        if (empID == null) return;

        // Empty input validation
        if (empID.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Employee ID is required.\nPlease enter a valid Employee ID.",
                    "Missing Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Display payslip (PayrollService returns an error message if not found)
        outputArea.setText(payrollService.generatePayslip(empID.trim()));
    }

    /**
     * Generates and displays a full payroll report for all employees.
     * Includes individual gross pay, deductions, net pay, totals,
     * and average net pay. Delegates to PayrollService.generateSummary().
     */
    public void generatePayrollReport() {
        outputArea.setText(payrollService.generateSummary());
    }
}
