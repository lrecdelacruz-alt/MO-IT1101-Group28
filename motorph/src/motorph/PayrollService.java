package motorph;

import java.util.ArrayList;
import java.util.List;

// PAYROLL SERVICE CLASS
// Central backend service for the MotorPH Payroll System.
//
// Responsibilities:
//   - Manage the employee list (CRUD operations)
//   - Manage the attendance records list
//   - Compute gross pay, deductions, and net pay
//   - Generate formatted payslip strings
//   - Generate payroll summary data
//
// NOTE: For MS1, employee and attendance data are loaded via
//       hardcoded sample records. The parseEmployees(),
//       parseAttendance(), and readCSVFile() methods are
//       defined as stubs reserved for future CSV integration.
public class PayrollService {

    // DEDUCTION CONSTANTS
    // Rates aligned with PH government approximations for MS1.
    // These will be updated with official bracket tables in
    // future milestones.
    private static final double DEDUCTION_RATE_SSS = 0.045;  // 4.5% of gross pay
    private static final double DEDUCTION_RATE_PHILHEALTH = 0.020;  // 2.0% of gross pay
    private static final double TAX_RATE = 0.100;  // 10% withholding tax
    private static final double PAGIBIG_FIXED = 100.00; // Fixed Pag-IBIG contribution

    // Overtime multiplier (standard PH rate: 1.25x regular hourly rate)
    private static final double OVERTIME_RATE = 1.25;

    // ATTRIBUTES (aligned with class diagram inventory)
    private List<Employee> employees = new ArrayList<>();
    private List<Attendance> attendanceRecords = new ArrayList<>();

    // filePath is reserved for future CSV file integration
    private String filePath = "";

    // DATA LOADING — Sample Data for MS1
    /**
     * Loads hardcoded sample employee records for MS1 demonstration. Replace
     * the body of this method with parseEmployees() logic once CSV integration
     * is implemented in a future milestone.
     */
    public void loadEmployees() {
        employees.add(new Employee("10001", "Michael", "Jackson", "HR Manager", 250.00, 50000.00));
        employees.add(new Employee("10002", "Ryland", "Grace", "Software Engineer", 300.00, 60000.00));
        employees.add(new Employee("10003", "Severus", "Snape", "Accountant", 200.00, 40000.00));
    }

    /**
     * Loads hardcoded sample attendance records for MS1 demonstration. Replace
     * the body of this method with parseAttendance() logic once CSV integration
     * is implemented in a future milestone.
     */
    public void loadAttendance() {
        attendanceRecords.add(new Attendance("ATT001", "10001", "2026-05-25", 8.0, 17.0, 0));
        attendanceRecords.add(new Attendance("ATT002", "10002", "2026-05-25", 9.0, 18.0, 0));
        attendanceRecords.add(new Attendance("ATT003", "10003", "2026-05-25", 8.0, 17.0, 0));
    }

    // CSV STUBS — Reserved for Future Milestones
    /**
     * Reads raw CSV content from the specified file path. Reserved for future
     * CSV file integration.
     *
     * @param csvFilePath Path to the CSV file to be read
     */
    public void readCSVFile(String csvFilePath) {
        // TODO: Implement CSV file reading in a future milestone
        this.filePath = csvFilePath;
        System.out.println("readCSVFile() — reserved for future CSV integration.");
    }

    /**
     * Parses employee records from the loaded CSV file into Employee objects.
     * Reserved for future CSV file integration.
     */
    public void parseEmployees() {
        // TODO: Parse CSV rows and populate the employees list
        System.out.println("parseEmployees() — reserved for future CSV integration.");
    }

    /**
     * Parses attendance records from the loaded CSV file into Attendance
     * objects. Reserved for future CSV file integration.
     */
    public void parseAttendance() {
        // TODO: Parse CSV rows and populate the attendanceRecords list
        System.out.println("parseAttendance() — reserved for future CSV integration.");
    }

    // EMPLOYEE CRUD OPERATIONS
    /**
     * Returns the full list of employees.
     *
     * @return List of all Employee objects
     */
    public List<Employee> getEmployees() {
        return employees;
    }

    /**
     * Returns the full list of attendance records.
     *
     * @return List of all Attendance objects
     */
    public List<Attendance> getAttendanceList() {
        return attendanceRecords;
    }

    /**
     * Searches for an employee by their unique ID. Returns null if no matching
     * employee is found.
     *
     * @param employeeID The ID to search for
     * @return Matching Employee object, or null if not found
     */
    public Employee findEmployee(String employeeID) {
        for (Employee emp : employees) {
            if (emp.getEmployeeID().equals(employeeID)) {
                return emp;
            }
        }
        return null;
    }

    public void addEmployee(Employee emp) {
        employees.add(emp);
    }

    /**
     * Updates the editable fields of an existing employee identified by ID.
     * Only first name, last name, and position are editable to prevent
     * accidental ID or salary corruption.
     *
     * @param employeeID ID of the employee to update
     * @param firstName New first name
     * @param lastName New last name
     * @param position New job position
     * @return true if the update was successful; false if employee not found
     */
    public boolean updateEmployee(String employeeID,
            String firstName,
            String lastName,
            String position) {
        Employee emp = findEmployee(employeeID);
        if (emp != null) {
            emp.setFirstName(firstName);
            emp.setLastName(lastName);
            emp.setPosition(position);
            return true;
        }
        return false;
    }

    /**
     * Removes an employee from the list by their ID.
     *
     * @param employeeID ID of the employee to remove
     * @return true if deletion was successful; false if employee not found
     */
    public boolean deleteEmployee(String employeeID) {
        Employee emp = findEmployee(employeeID);
        if (emp != null) {
            employees.remove(emp);
            return true;
        }
        return false;
    }

    // PAYROLL COMPUTATION METHODS
    /**
     * Sums all hours worked across all attendance records for a given employee.
     * Uses calculateHoursWorked() from the Attendance class for each record.
     *
     * @param employeeID ID of the employee
     * @return Total hours worked as a double
     */
    public double getTotalHoursWorked(String employeeID) {
        double total = 0;
        for (Attendance att : attendanceRecords) {
            if (att.getEmployeeID().equals(employeeID)) {
                total += att.calculateHoursWorked();
            }
        }
        return total;
    }

    /**
     * Calculates gross pay for a given employee based on their total hours
     * worked and their hourly rate.
     *
     * @param emp Employee whose gross pay to compute
     * @param att Single attendance record (used for individual record pay)
     * @return Gross pay amount
     */
    public double calculateGrossPay(Employee emp, Attendance att) {
        return att.calculateHoursWorked() * emp.getHourlyRate();
    }

    /**
     * Calculates all government-mandated deductions from gross pay. Returns
     * total deductions (SSS + PhilHealth + Pag-IBIG + Tax).
     *
     * @param grossPay The computed gross pay amount
     * @return Total deductions amount
     */
    public double calculateDeductions(double grossPay) {
        return (grossPay * DEDUCTION_RATE_SSS)
                + (grossPay * DEDUCTION_RATE_PHILHEALTH)
                + PAGIBIG_FIXED
                + (grossPay * TAX_RATE);
    }

    /**
     * Calculates net pay by subtracting total deductions from gross pay.
     *
     * @param grossPay Computed gross pay
     * @param deductions Total deductions amount
     * @return Net pay amount
     */
    public double calculateNetPay(double grossPay, double deductions) {
        return grossPay - deductions;
    }

    /**
     * Calculates overtime pay using a standard 1.25x multiplier applied to the
     * employee's hourly rate.
     *
     * @param overtimeHours Number of overtime hours rendered
     * @param hourlyRate Employee's regular hourly rate
     * @return Computed overtime pay amount
     */
    public double calculateOvertimePay(double overtimeHours, double hourlyRate) {
        return overtimeHours * hourlyRate * OVERTIME_RATE;
    }

    // PAYSLIP AND REPORT GENERATION
    /**
     * Generates a formatted payslip string for the employee with the given ID.
     * Shows gross pay, itemized deductions, and net pay. Returns an error
     * message string if the employee is not found.
     *
     * NOTE: Deduction rates used are MS1 approximations. SSS: 4.5%, PhilHealth:
     * 2.0%, Pag-IBIG: PHP 100 fixed, Tax: 10% These will be updated with
     * official PH bracket tables in future milestones.
     *
     * @param employeeID ID of the employee whose payslip to generate
     * @return Formatted payslip string, or an error message if not found
     */
    public String generatePayslip(String employeeID) {

        Employee emp = findEmployee(employeeID);

        if (emp == null) {
            return "Employee with ID \"" + employeeID + "\" was not found.\n"
                    + "Please check the ID and try again.";
        }

        double totalHours = getTotalHoursWorked(employeeID);
        double grossPay = totalHours * emp.getHourlyRate();
        double sss = grossPay * DEDUCTION_RATE_SSS;
        double philHealth = grossPay * DEDUCTION_RATE_PHILHEALTH;
        double pagIbig = PAGIBIG_FIXED;
        double tax = grossPay * TAX_RATE;
        double totalDeduct = calculateDeductions(grossPay);
        double netPay = calculateNetPay(grossPay, totalDeduct);

        return "================================\n"
                + "        MOTORPH PAYSLIP         \n"
                + "================================\n"
                + "Employee ID  : " + emp.getEmployeeID() + "\n"
                + "Name         : " + emp.getFullName() + "\n"
                + "Position     : " + emp.getPosition() + "\n"
                + "Hourly Rate  : PHP " + String.format("%.2f", emp.getHourlyRate()) + "\n"
                + "Total Hours  : " + String.format("%.2f", totalHours) + "\n"
                + "--------------------------------\n"
                + "Gross Pay    : PHP " + String.format("%.2f", grossPay) + "\n"
                + "--------------------------------\n"
                + "SSS          : PHP " + String.format("%.2f", sss) + "\n"
                + "PhilHealth   : PHP " + String.format("%.2f", philHealth) + "\n"
                + "Pag-IBIG     : PHP " + String.format("%.2f", pagIbig) + "\n"
                + "Withholding  : PHP " + String.format("%.2f", tax) + "\n"
                + "Total Deduct : PHP " + String.format("%.2f", totalDeduct) + "\n"
                + "--------------------------------\n"
                + "NET PAY      : PHP " + String.format("%.2f", netPay) + "\n"
                + "================================\n";
    }

    /**
     * Generates a payroll summary covering all employees. Shows each employee's
     * hours worked, gross pay, total deductions, and net pay, plus a running
     * total at the bottom.
     *
     * Validation: Returns an error message if no employees are loaded.
     *
     * @return Formatted payroll summary string
     */
    public String generateSummary() {

        if (employees.isEmpty()) {
            return "No employee data loaded. Please add employees first.";
        }

        StringBuilder sb = new StringBuilder();
        double totalGross = 0;
        double totalDeduct = 0;
        double totalNet = 0;

        sb.append("================================\n");
        sb.append("     MOTORPH PAYROLL SUMMARY    \n");
        sb.append("================================\n");
        sb.append(String.format("%-6s %-20s %10s %12s %10s%n",
                "ID", "Name", "Gross", "Deductions", "Net Pay"));
        sb.append("------------------------------------------------\n");

        for (Employee emp : employees) {
            double hours = getTotalHoursWorked(emp.getEmployeeID());
            double gross = hours * emp.getHourlyRate();
            double deduct = calculateDeductions(gross);
            double net = calculateNetPay(gross, deduct);

            totalGross += gross;
            totalDeduct += deduct;
            totalNet += net;

            sb.append(String.format("%-6s %-20s %10.2f %12.2f %10.2f%n",
                    emp.getEmployeeID(),
                    emp.getFullName(),
                    gross,
                    deduct,
                    net));
        }

        sb.append("------------------------------------------------\n");
        sb.append(String.format("%-27s %10.2f %12.2f %10.2f%n",
                "TOTALS:", totalGross, totalDeduct, totalNet));
        sb.append("================================\n");
        sb.append("Total Employees : ").append(employees.size()).append("\n");
        sb.append(String.format("Average Net Pay : PHP %.2f%n",
                employees.isEmpty() ? 0 : totalNet / employees.size()));

        return sb.toString();
    }

    /**
     * Processes payroll for all employees and returns a simple summary showing
     * name, hours worked, and gross pay for each employee. Used by the
     * PayrollMenu "Process Payroll" button.
     *
     * @return Formatted payroll processing result string
     */
    public String processPayroll() {

        if (employees.isEmpty()) {
            return "No employee data loaded. Please add employees first.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== PAYROLL PROCESSED ===\n");

        for (Employee emp : employees) {
            double hours = getTotalHoursWorked(emp.getEmployeeID());
            double gross = hours * emp.getHourlyRate();
            sb.append(String.format("%-22s | Hours: %5.2f | Gross: PHP %,.2f%n",
                    emp.getFullName(), hours, gross));
        }

        sb.append("=========================\n");
        sb.append("Computation complete. View individual payslips for full breakdown.");
        return sb.toString();
    }
}
