package motorph;

import javax.swing.*;
import java.awt.*;
import java.util.List;

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
    private JLabel    computeHintLabel;
    private JButton   computeSalariesButton;
    private JButton   processPayrollButton;
    private JButton   viewPayslipButton;
    private JButton   generateReportButton;
    private JButton   generateSummaryButton;
    private JButton   exportButton;

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
        frame.setSize(780, 500);
        frame.setMinimumSize(new Dimension(700, 420));

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
        generateSummaryButton = new JButton("Generate Summary");
        exportButton          = new JButton("Export ▾");

        Dimension payrollButtonSize = new Dimension(200, 32);

        // Use UIHelper for consistent styling across all windows
        UIHelper.styleButton(computeSalariesButton, UIHelper.COMPUTE, Color.WHITE,
        payrollButtonSize);
        UIHelper.styleButton(processPayrollButton,  UIHelper.GRAY,    Color.BLACK,
                payrollButtonSize);
        UIHelper.styleButton(viewPayslipButton,     UIHelper.GRAY,    Color.BLACK,
                payrollButtonSize);
        UIHelper.styleButton(generateReportButton,  UIHelper.GRAY,    Color.BLACK,
        payrollButtonSize);
        UIHelper.styleButton(generateSummaryButton, UIHelper.GRAY,    Color.BLACK,
                payrollButtonSize);
        UIHelper.styleButton(exportButton,          UIHelper.GREEN,   Color.WHITE,
                payrollButtonSize);

        // Tooltips
        computeSalariesButton.setToolTipText(
                "Compute gross pay, deductions, and net pay for all employees using attendance data");
        processPayrollButton.setToolTipText(
                "View a quick summary of all employees' hours worked and gross pay");
        viewPayslipButton.setToolTipText(
                "View the full payslip for a selected employee");
        generateReportButton.setToolTipText(
                "Generate a full payroll report with totals and average net pay");
        generateSummaryButton.setToolTipText(
                "Show a quick popup summary: total employees, gross pay, deductions, and average net pay");
        exportButton.setToolTipText(
                "Export a payslip, payroll report, or summary CSV to a file");

        JPanel topButtonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        topButtonRow.add(generateReportButton);
        topButtonRow.add(processPayrollButton);
        topButtonRow.add(viewPayslipButton);

        JPanel bottomButtonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        bottomButtonRow.add(computeSalariesButton);
        bottomButtonRow.add(generateSummaryButton);
        bottomButtonRow.add(exportButton);

        JPanel buttonPanel = new JPanel(new java.awt.GridLayout(2, 1, 0, 0));
        buttonPanel.add(topButtonRow);
        buttonPanel.add(bottomButtonRow);

        boolean alreadyComputed = payrollService.getEmployees().stream()
                .anyMatch(e -> e.getGrossPay() > 0);
        processPayrollButton.setEnabled(alreadyComputed);
        viewPayslipButton.setEnabled(alreadyComputed);
        generateReportButton.setEnabled(alreadyComputed);
        generateSummaryButton.setEnabled(alreadyComputed);
        exportButton.setEnabled(alreadyComputed);

        // Hint label shown below buttons when salaries not yet computed
        computeHintLabel = new JLabel(
            alreadyComputed
                ? " "
                : "⚠  Run 'Compute Salaries' first to unlock payslip and report buttons.",
            SwingConstants.CENTER);
        computeHintLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        computeHintLabel.setForeground(new Color(150, 100, 0));
        computeHintLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(titleLabel,  BorderLayout.NORTH);
        northPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel northWrapper = new JPanel(new BorderLayout());
        northWrapper.add(northPanel,      BorderLayout.CENTER);
        northWrapper.add(computeHintLabel, BorderLayout.SOUTH);

        // Output area — monospaced font keeps payslip columns aligned
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        outputArea.setBorder(BorderFactory.createTitledBorder("Payroll Output"));
        outputArea.setText(
            "Welcome to Payroll Management.\n\n"
        + "Getting Started:\n"
        + "  1. Click 'Compute Salaries' to calculate pay for all employees.\n"
        + "  2. View individual payslips using 'View Payslip'.\n"
        + "  3. Generate a full report using 'Generate Payroll Report'.\n"
        + "  4. Click 'Export ▾' to save a payslip, report, or summary CSV to a file.\n\n"
        + "Note: Compute Salaries must be run first before viewing payslips or reports.");
        JScrollPane outputScroll = new JScrollPane(outputArea);

        frame.add(northWrapper, BorderLayout.NORTH);
        frame.add(outputScroll, BorderLayout.CENTER);

        // Event listeners
        computeSalariesButton.addActionListener(e -> computeSalaries());
        processPayrollButton.addActionListener(e  -> processPayroll());
        viewPayslipButton.addActionListener(e     -> viewPayslip());
        generateReportButton.addActionListener(e  -> generatePayrollReport());
        generateSummaryButton.addActionListener(e -> generateSummaryPopup());
        exportButton.addActionListener(e          -> showExportMenu());

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

        if (result.startsWith("Salary computation complete!")) {
            int empCount2 = payrollService.getEmployees().size();
            outputArea.setText(
                "================================\n"
            + "   SALARY COMPUTATION COMPLETE  \n"
            + "================================\n"
            + "Employees Processed : " + empCount2 + "\n"
            + "Status              : Saved to CSV\n"
            + "--------------------------------\n"
            + "Next Steps:\n"
            + "  → View Payslip — individual payslip\n"
            + "  → Process Payroll — quick summary\n"
            + "  → Generate Report — full breakdown\n"
            + "================================\n");
        } else {
            outputArea.setText(result);
        }

        if (result.startsWith("Salary computation complete!")) {
            // Unlock all read buttons now that data exists
            processPayrollButton.setEnabled(true);
            viewPayslipButton.setEnabled(true);
            generateReportButton.setEnabled(true);
            generateSummaryButton.setEnabled(true);
            exportButton.setEnabled(true);
            computeHintLabel.setText(" ");
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
     * Generates and displays the full payroll report for all employees.
     * Includes gross pay, deductions, net pay, totals, and average.
     */
    public void generatePayrollReport() {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter
                        .ofPattern("MMMM dd, yyyy  hh:mm a"));
        outputArea.setText(
            "Report generated: " + timestamp + "\n\n"
            + payrollService.generateSummary());
    }

    /**
     * Shows a quick popup summary of the four MPHCR05 metrics — total
     * employees, total gross pay, total deductions, and average net pay —
     * matching the JOptionPane popup pattern from the course's sample
     * generateSalesSummary() method. Kept separate from "Generate Payroll
     * Report" (which shows a detailed per-employee breakdown in the output
     * area) so this stays the simple, at-a-glance company-wide view that
     * MPHCR05 specifically asks for.
     */
    public void generateSummaryPopup() {

        List<Employee> employees = payrollService.getEmployees();

        if (employees.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No employee records found! Please add employees first.",
                    "Payroll Summary",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        double totalGross = 0, totalDeduct = 0, totalNet = 0;
        for (Employee emp : employees) {
            totalGross  += emp.getGrossPay();
            totalDeduct += emp.getTotalDeductions();
            totalNet    += emp.getNetPay();
        }

        int employeeCount = employees.size();
        double averageNetPay = totalNet / employeeCount;

        String summary = String.format(
                    "%-17s: %d%n"
                    + "%-17s: PHP %,.2f%n"
                    + "%-17s: PHP %,.2f%n"
                    + "%-17s: PHP %,.2f",
                    "Total Employees",  employeeCount,
                    "Total Gross Pay",  totalGross,
                    "Total Deductions", totalDeduct,
                    "Average Net Pay",  averageNetPay);

        JTextArea summaryArea = new JTextArea(summary);
        summaryArea.setEditable(false);
        summaryArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        summaryArea.setBackground(new Color(250, 250, 252));
        summaryArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JOptionPane.showMessageDialog(frame, summaryArea,
                "Payroll Summary",
                JOptionPane.INFORMATION_MESSAGE);

        AuditLogger.log("GENERATE SUMMARY", "Displayed payroll summary popup for "
                + employeeCount + " employee(s)");
    }

    /**
     * Shows a searchable dropdown of all employees.
     * User picks a name instead of typing an ID — no wrong-ID errors possible.
     */
    public void viewPayslip() {

        List<Employee> employees = payrollService.getEmployees();

        if (employees.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No employees loaded. Please check the CSV file.",
                    "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Build dropdown entries as "10001 — Juan dela Cruz"
        String[] options = employees.stream()
                .map(e -> e.getEmployeeID() + " — " + e.getFullName())
                .toArray(String[]::new);

        JComboBox<String> dropdown = new JComboBox<>(options);
        dropdown.setEditable(true); // allows typing to filter

        Object[] fields = { "Select Employee:", dropdown };

        int option = JOptionPane.showConfirmDialog(
                frame, fields, "View Payslip", JOptionPane.OK_CANCEL_OPTION);

        if (option != JOptionPane.OK_OPTION) return;

        String selected = (String) dropdown.getSelectedItem();
        if (selected == null || selected.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Please select an employee.",
                    "Missing Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Extract the ID from "10001 — Juan dela Cruz"
        String empID = selected.split(" — ")[0].trim();

        if (payrollService.findEmployee(empID) == null) {
            JOptionPane.showMessageDialog(frame,
                    "Employee not found. Please select from the list.",
                    "Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        outputArea.setText(payrollService.generatePayslip(empID));
    }

    /**
     * Shows a small choice dialog letting the user pick which kind of
     * export they want, instead of having three separate buttons. Keeps
     * the toolbar at a clean, consistent button count while still
     * exposing the same three export options underneath.
     */
    private void showExportMenu() {
        String[] options = {
            "Payslip (.txt)",
            "Full Payroll Report (.txt)",
            "Payroll Summary (.csv)"
        };
        int choice = JOptionPane.showOptionDialog(
                frame,
                "What would you like to export?",
                "Export",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        switch (choice) {
            case 0 -> savePayslipToFile();
            case 1 -> saveReportToFile();
            case 2 -> saveSummaryCsv();
            default -> { /* dialog cancelled — do nothing */ }
        }
    }

    /**
     * Prompts for an Employee ID, generates their payslip, and saves
     * it to a .txt file in the payslips/ folder.
     * Creates the folder automatically if it doesn't exist.
     */
    public void savePayslipToFile() {

        List<Employee> employees = payrollService.getEmployees();
        if (employees.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No employees loaded.", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] options = employees.stream()
                .map(e -> e.getEmployeeID() + " — " + e.getFullName())
                .toArray(String[]::new);

        JComboBox<String> dropdown = new JComboBox<>(options);
        dropdown.setEditable(true);

        int option = JOptionPane.showConfirmDialog(
                frame, new Object[]{"Select Employee:", dropdown},
                "Save Payslip to File", JOptionPane.OK_CANCEL_OPTION);

        if (option != JOptionPane.OK_OPTION) return;

        String selected = (String) dropdown.getSelectedItem();
        if (selected == null || selected.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Please select an employee.",
                    "Missing Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String empID = selected.split(" — ")[0].trim();

        if (payrollService.findEmployee(empID) == null) {
            JOptionPane.showMessageDialog(frame,
                    "Employee not found. Please select from the list.",
                    "Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String payslip = payrollService.generatePayslip(empID);

        // Create payslips/ folder if it doesn't exist
        java.io.File folder = new java.io.File("payslips");
        if (!folder.exists()) folder.mkdirs();

        String fileName = "payslips/Payslip_" + empID + ".txt";
        String absolutePath = new java.io.File(fileName).getAbsolutePath();

        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                new java.io.FileWriter(fileName))) {
            writer.write(payslip);
            JOptionPane.showMessageDialog(frame,
                    "Payslip saved successfully!\n\nSaved to:\n" + absolutePath,
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
            outputArea.setText("Payslip exported to:\n" + absolutePath + "\n\n" + payslip);
            AuditLogger.log("EXPORT PAYSLIP", "Employee #" + empID
                + " | Saved to " + fileName);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Failed to save payslip.\nPlease check file permissions.",
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Saves the full payroll report to a timestamped .txt file
     * in the reports/ folder.
     */
    public void saveReportToFile() {

        if (payrollService.getEmployees().isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No employee data loaded.",
                    "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String report = payrollService.generateSummary();

        java.io.File folder = new java.io.File("reports");
        if (!folder.exists()) folder.mkdirs();

        // Timestamped filename so each report is unique and never overwrites
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String fileName = "reports/PayrollReport_" + timestamp + ".txt";

        String absolutePath = new java.io.File(fileName).getAbsolutePath();

        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                new java.io.FileWriter(fileName))) {
            writer.write(report);
            JOptionPane.showMessageDialog(frame,
                    "Payroll report saved successfully!\n\nSaved to:\n" + absolutePath,
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
            outputArea.setText("Report exported to:\n" + absolutePath + "\n\n" + report);
            AuditLogger.log("EXPORT REPORT", "Saved to " + fileName);
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Failed to save report.\nPlease check file permissions.",
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Exports the payroll summary as a properly structured CSV file with
     * "Metric,Value" labeled rows — distinct from saveReportToFile(),
     * which exports a human-readable narrative .txt report. This is the
     * machine-readable format MS2's QA checklist specifically calls out
     * ("a correctly formatted CSV file with complete and labeled values,
     * e.g., 'Metric, Value'").
     */
    public void saveSummaryCsv() {

        List<Employee> employees = payrollService.getEmployees();
        if (employees.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No employee data loaded.",
                    "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double totalGross = 0, totalDeduct = 0, totalNet = 0;
        for (Employee emp : employees) {
            totalGross  += emp.getGrossPay();
            totalDeduct += emp.getTotalDeductions();
            totalNet    += emp.getNetPay();
        }
        double averageNet = employees.size() > 0 ? totalNet / employees.size() : 0.0;

        java.io.File folder = new java.io.File("reports");
        if (!folder.exists()) folder.mkdirs();

        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String fileName = "reports/PayrollSummary_" + timestamp + ".csv";
        String absolutePath = new java.io.File(fileName).getAbsolutePath();

        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                new java.io.FileWriter(fileName))) {
            writer.write("Metric,Value");
            writer.newLine();
            writer.write("Total Employees," + employees.size());
            writer.newLine();
            writer.write("Total Gross Pay," + String.format("%.2f", totalGross));
            writer.newLine();
            writer.write("Total Deductions," + String.format("%.2f", totalDeduct));
            writer.newLine();
            writer.write("Total Net Pay," + String.format("%.2f", totalNet));
            writer.newLine();
            writer.write("Average Net Pay," + String.format("%.2f", averageNet));
            writer.newLine();

            JOptionPane.showMessageDialog(frame,
                    "Payroll summary CSV saved successfully!\n\nSaved to:\n" + absolutePath,
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
            outputArea.setText("Summary CSV exported to:\n" + absolutePath
                    + "\n\nMetric,Value\n"
                    + "Total Employees," + employees.size() + "\n"
                    + "Total Gross Pay," + String.format("%.2f", totalGross) + "\n"
                    + "Total Deductions," + String.format("%.2f", totalDeduct) + "\n"
                    + "Total Net Pay," + String.format("%.2f", totalNet) + "\n"
                    + "Average Net Pay," + String.format("%.2f", averageNet));
            AuditLogger.log("EXPORT SUMMARY CSV", "Saved to " + fileName);

        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Failed to save summary CSV.\nPlease check file permissions.",
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}