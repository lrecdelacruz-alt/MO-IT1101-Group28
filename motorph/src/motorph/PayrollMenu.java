package motorph;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;

// ============================================================
// PAYROLL MENU CLASS
// Provides the GUI window for all payroll operations.
//
// Responsibilities:
//   - Compute salaries for all employees (Feature 3 trigger)
//   - Process payroll — show hours and gross pay per employee
//   - View individual payslip by employee ID
//   - Generate full payroll report with totals and averages
//
// The "Compute Salaries" button is the main Feature 3 action.
// It must be clicked before payslips show accurate values.
// ============================================================

public class PayrollMenu implements DataChangeListener {

    // GUI components
    private JFrame    frame;
    private JTextArea outputArea;
    private JButton   computeSalariesButton;
    private JButton   processPayrollButton;
    private JButton   viewPayslipButton;
    private JButton   generateReportButton;

    private PayrollService payrollService;

    private Runnable onCloseCallback;

    /** Registers a callback to run when this window is closed (used by Main for the singleton guard). */
    public void onCloseClearReference(Runnable callback) {
        this.onCloseCallback = callback;
    }

    /** Brings this window to the front and focuses it if it's already open. */
    public void bringToFront() {
        if (frame != null) {
            frame.setState(Frame.NORMAL);
            frame.toFront();
            frame.requestFocus();
        }
    }

    @Override
    public void onDataChanged() {
        // No live table here — every button already pulls fresh data on
        // click. Implemented for architectural consistency with EmployeeMenu.
    }

    public PayrollMenu(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    // --------------------------------------------------------
    // MAIN DISPLAY METHOD
    // --------------------------------------------------------

    /**
     * Builds and displays the Payroll Menu window.
     * "Compute Salaries" is accented in blue to indicate it is
     * the primary action that must be run before others.
     */
    public void displayPayrollMenu() {

        frame = new JFrame("MotorPH — Payroll Menu");
        frame.setIconImage(AppIcon.create());
        frame.setSize(990, 500);
        frame.setMinimumSize(new Dimension(960, 380));

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                payrollService.removeDataChangeListener(PayrollMenu.this);
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                }
            }
        });

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));
        payrollService.addDataChangeListener(this);

        // Title
        JLabel titleLabel = new JLabel("Payroll Management", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));

        // Buttons
        computeSalariesButton = new JButton("Compute Salaries");
        processPayrollButton  = new JButton("Process Payroll");
        viewPayslipButton     = new JButton("View Payslip");
        generateReportButton  = new JButton("Generate Payroll Report");

        Dimension payrollButtonSize = new Dimension(220, 32);

        // Style every button the same way (font, flat look, opacity) so
        // the whole row reads as one consistent family — only the color
        // differs, to mark "Compute Salaries" as the primary action.
        JButton[] payrollButtons = {
            computeSalariesButton, processPayrollButton, viewPayslipButton, generateReportButton
        };
        for (JButton b : payrollButtons) {
            b.setPreferredSize(payrollButtonSize);
            b.setFont(new Font("Arial", Font.PLAIN, 13));
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setContentAreaFilled(true);
            b.setOpaque(true);
        }

        // Compute Salaries is the only button here that writes/saves data,
        // so it keeps the app's signature blue. The other three are
        // read-only displays and stay gray — same write-vs-read rule used
        // in EmployeeMenu. All four get a raised bevel border for a subtle
        // 3D look, shaded from their own base color.
        Color computeColor = new Color(21, 101, 192); // blue
        Color neutralGray  = new Color(190, 195, 200); // gray

        computeSalariesButton.setBackground(computeColor);
        computeSalariesButton.setForeground(Color.WHITE);
        computeSalariesButton.setBorderPainted(true);
        computeSalariesButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, computeColor.brighter(), computeColor.darker()));

        processPayrollButton.setBackground(neutralGray);
        processPayrollButton.setForeground(Color.BLACK);
        processPayrollButton.setBorderPainted(true);
        processPayrollButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, neutralGray.brighter(), neutralGray.darker()));

        viewPayslipButton.setBackground(neutralGray);
        viewPayslipButton.setForeground(Color.BLACK);
        viewPayslipButton.setBorderPainted(true);
        viewPayslipButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, neutralGray.brighter(), neutralGray.darker()));

        generateReportButton.setBackground(neutralGray);
        generateReportButton.setForeground(Color.BLACK);
        generateReportButton.setBorderPainted(true);
        generateReportButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, neutralGray.brighter(), neutralGray.darker()));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(computeSalariesButton);
        buttonPanel.add(processPayrollButton);
        buttonPanel.add(viewPayslipButton);
        buttonPanel.add(generateReportButton);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(titleLabel,  BorderLayout.NORTH);
        northPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Output area — monospaced font keeps payslip columns aligned
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setBorder(BorderFactory.createTitledBorder("Output"));
        JScrollPane outputScroll = new JScrollPane(outputArea);

        frame.add(northPanel,   BorderLayout.NORTH);
        frame.add(outputScroll, BorderLayout.CENTER);

        // Event listeners
        computeSalariesButton.addActionListener(e -> computeSalaries());
        processPayrollButton.addActionListener(e  -> processPayroll());
        viewPayslipButton.addActionListener(e     -> viewPayslip());
        generateReportButton.addActionListener(e  -> generatePayrollReport());

        frame.setVisible(true);
    }

    // --------------------------------------------------------
    // PAYROLL OPERATIONS
    // --------------------------------------------------------

    /**
     * Triggers salary computation for all employees.
     * Reads total hours from the Attendance CSV, computes
     * gross pay, deductions, and net pay for each employee,
     * then saves the results back to the Employee CSV.
     *
     * This must be run before payslips show accurate values.
     *
     * BUG FIX: the dialog shown now reflects whether computation and
     * saving actually succeeded. PayrollService.computeAllSalaries()
     * only returns a string starting with "Salary computation complete!"
     * when both genuinely succeeded — anything else (missing attendance
     * data, failed save, no employees loaded) now shows a warning dialog
     * instead of a false success message.
     */
    public void computeSalaries() {

        int empCount = payrollService.getEmployees().size();
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "This will compute salaries for " + empCount + " employee(s)\n"
                + "using attendance records from the CSV file.\n\nContinue?",
                "Compute Salaries",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        String result = payrollService.computeAllSalaries();
        outputArea.setText(result);

        if (result.startsWith("Salary computation complete!")) {
            JOptionPane.showMessageDialog(frame,
                    "Salary computation complete!\nResults have been saved to the CSV file.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(frame,
                    "Salary computation did not finish successfully.\n"
                    + "See the details below for what happened.",
                    "Computation Issue", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Displays a quick summary of all employees'
     * hours worked and gross pay.
     */
    public void processPayroll() {
        outputArea.setText(payrollService.processPayroll());
    }

    /**
     * Prompts for an employee ID and displays their full payslip.
     * If Compute Salaries has not been run yet, a prompt is shown.
     */
    public void viewPayslip() {

        JTextField empIdField = new JTextField(15);
        Object[] fields = { "Employee #:", empIdField };

        int option = JOptionPane.showConfirmDialog(
                frame, fields, "View Payslip", JOptionPane.OK_CANCEL_OPTION);

        if (option != JOptionPane.OK_OPTION) return;

        String empID = empIdField.getText();

        if (empID.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Employee ID is required.\nPlease enter a valid Employee ID.",
                    "Missing Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!empID.trim().matches("\\d+")) {
            JOptionPane.showMessageDialog(frame,
                    "Employee ID must contain numbers only (e.g., 10001).",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (payrollService.findEmployee(empID.trim()) == null) {
            JOptionPane.showMessageDialog(frame,
                    "Employee ID \"" + empID.trim() + "\" was not found.\nPlease check the ID and try again.",
                    "Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        outputArea.setText(payrollService.generatePayslip(empID.trim()));
    }

    /**
     * Generates and displays the full payroll report for all employees.
     * Includes gross pay, deductions, net pay, totals, and average.
     */
    public void generatePayrollReport() {
        outputArea.setText(payrollService.generateSummary());
    }
}