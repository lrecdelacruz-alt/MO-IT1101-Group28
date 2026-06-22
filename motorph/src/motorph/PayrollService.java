package motorph;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// ============================================================
// PAYROLL SERVICE CLASS
// Central backend service for the MotorPH Payroll System.
//
// Responsibilities:
//   - Load employee records from the MotorPH Employee Details CSV
//   - Perform CRUD operations (add, update, delete) and sync to CSV
//   - Compute salaries for all employees using attendance data
//   - Generate payslips and payroll reports
//   - Notify registered listeners (e.g. open EmployeeMenu windows)
//     whenever employee data actually changes (Observer pattern)
//
// All file operations delegate to CSVHandler.
// All salary computations delegate to SalaryComputationModule.
// This class only coordinates data flow between layers.
// ============================================================

public class PayrollService {

    private static final String PROJECT_ROOT = 
    new java.io.File("").getAbsolutePath();
    // CSV file paths — public so EmployeeMenu can reference them
    public static final String EMPLOYEE_FILE   =
    PROJECT_ROOT + java.io.File.separator + "data" 
     + java.io.File.separator +     "MotorPH_Employee Data - Employee Details.csv";
    public static final String ATTENDANCE_FILE =
        PROJECT_ROOT + java.io.File.separator + "data" 
         + java.io.File.separator +     "MotorPH_Employee Data - Attendance Record.csv";

    private List<Employee> employees = new ArrayList<>();

    // Observer pattern: windows register here to be notified whenever
    // employee data changes from anywhere in the app.
    private final List<DataChangeListener> listeners = new ArrayList<>();

    // --------------------------------------------------------
    // DATA LOADING
    // --------------------------------------------------------

    /**
     * Loads employee records from the MotorPH Employee Details CSV.
     * Handles both the original 19-column format and our 14-column
     * working format automatically via CSVHandler.
     *
     * BUG FIX: previously this returned void, so a missing CSV file
     * loaded silently (CSVHandler only printed to the console) and
     * the app opened with an empty employee list and no explanation.
     * Now returns a warning string the caller (Main) can show in a
     * dialog. An empty string means everything loaded fine.
     *
     * @return warning message if a file is missing, or "" if all good
     */
    public String loadEmployees() {

        System.out.println("Employee file: " + EMPLOYEE_FILE);
        StringBuilder warnings = new StringBuilder();

        if (!new File(EMPLOYEE_FILE).exists()) {
            warnings.append("Employee file not found:\n\"")
                    .append(EMPLOYEE_FILE).append("\"\n\n");
        }
        if (!new File(ATTENDANCE_FILE).exists()) {
            warnings.append("Attendance file not found:\n\"")
                    .append(ATTENDANCE_FILE).append("\"\n\n");
        }

        employees = CSVHandler.readEmployees(EMPLOYEE_FILE);

        int badValues = CSVHandler.getLastParseWarningCount();
        if (badValues > 0) {
            warnings.append(badValues)
                    .append(" numeric value(s) in the employee CSV could not be read "
                          + "and were treated as 0. Check the file for corrupted "
                          + "rate or salary data.\n\n");
        }

        return warnings.toString();
    }

    // --------------------------------------------------------
    // OBSERVER PATTERN — keep every open window in sync
    // --------------------------------------------------------

    /**
     * Registers a listener to be notified whenever employee data changes.
     * EmployeeMenu calls this when it opens so it can refresh itself
     * even when the change happened from a different window (e.g.
     * PayrollMenu's "Compute Salaries").
     */
    public void addDataChangeListener(DataChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Unregisters a listener. EmployeeMenu calls this when its window
     * closes, so a disposed window is never notified again.
     */
    public void removeDataChangeListener(DataChangeListener listener) {
        listeners.remove(listener);
    }

    /** Notifies every registered listener that employee data has changed. */
    private void notifyDataChanged() {
        for (DataChangeListener listener : listeners) {
            listener.onDataChanged();
        }
    }

    // --------------------------------------------------------
    // EMPLOYEE ACCESS
    // --------------------------------------------------------

    public List<Employee> getEmployees() {
        return employees;
    }

    /**
     * Searches for an employee by their unique ID.
     * Returns null if not found.
     */
    public Employee findEmployee(String employeeID) {
        for (Employee emp : employees) {
            if (emp.getEmployeeID().equals(employeeID)) return emp;
        }
        return null;
    }

    // --------------------------------------------------------
    // CRUD — each operation syncs to the CSV file
    // --------------------------------------------------------

    /**
     * Adds a new employee to the list and appends them to the CSV.
     * If the CSV write fails, the in-memory add is rolled back so
     * the app never shows a record that isn't actually saved.
     *
     * @return true if added and saved successfully, false otherwise
     */
    public boolean addEmployee(Employee emp) {
        employees.add(emp);
        boolean saved = CSVHandler.appendEmployee(EMPLOYEE_FILE, emp);

        if (!saved) {
            employees.remove(emp); // roll back — keep memory and disk consistent
            return false;
        }

        notifyDataChanged();
        return true;
    }

    /**
     * Updates all editable fields of an existing employee and
     * rewrites the CSV to reflect the change.
     * Employee ID and computed fields are not updated here.
     *
     * If the CSV write fails, the previous field values are restored
     * so the in-memory data still matches what's actually on disk.
     *
     * @return true if the employee was found, updated, and saved; false otherwise
     */
    public boolean updateEmployee(String employeeID,
                                  String firstName,    String lastName,
                                  String sssNumber,    String philHealthNumber,
                                  String tin,          String pagIbigNumber,
                                  String position,
                                  double hourlyRate,   double basicSalary) {

        Employee emp = findEmployee(employeeID);
        if (emp == null) return false;

        // Snapshot old values in case the write fails and we need to roll back
        String oldFirstName   = emp.getFirstName();
        String oldLastName    = emp.getLastName();
        String oldSss         = emp.getSssNumber();
        String oldPhilHealth  = emp.getPhilHealthNumber();
        String oldTin         = emp.getTin();
        String oldPagIbig     = emp.getPagIbigNumber();
        String oldPosition    = emp.getPosition();
        double oldHourlyRate  = emp.getHourlyRate();
        double oldBasicSalary = emp.getBasicSalary();

        emp.setFirstName(firstName);
        emp.setLastName(lastName);
        emp.setSssNumber(sssNumber);
        emp.setPhilHealthNumber(philHealthNumber);
        emp.setTin(tin);
        emp.setPagIbigNumber(pagIbigNumber);
        emp.setPosition(position);
        emp.setHourlyRate(hourlyRate);
        emp.setBasicSalary(basicSalary);

        boolean saved = CSVHandler.writeEmployees(EMPLOYEE_FILE, employees);

        if (!saved) {
            // Roll back so in-memory data still matches what's on disk
            emp.setFirstName(oldFirstName);
            emp.setLastName(oldLastName);
            emp.setSssNumber(oldSss);
            emp.setPhilHealthNumber(oldPhilHealth);
            emp.setTin(oldTin);
            emp.setPagIbigNumber(oldPagIbig);
            emp.setPosition(oldPosition);
            emp.setHourlyRate(oldHourlyRate);
            emp.setBasicSalary(oldBasicSalary);
            return false;
        }

        notifyDataChanged();
        return true;
    }

    /**
     * Removes an employee from the list and rewrites the CSV.
     * If the write fails, the employee is restored to the in-memory
     * list so memory and disk stay consistent.
     *
     * @return true if the employee was found, deleted, and saved; false otherwise
     */
    public boolean deleteEmployee(String employeeID) {
        Employee emp = findEmployee(employeeID);
        if (emp == null) return false;

        employees.remove(emp);
        boolean saved = CSVHandler.writeEmployees(EMPLOYEE_FILE, employees);

        if (!saved) {
            employees.add(emp); // roll back
            return false;
        }

        notifyDataChanged();
        return true;
    }

    // --------------------------------------------------------
    // SALARY COMPUTATION (Feature 3)
    // --------------------------------------------------------

    /**
     * Computes salaries for all employees using attendance records.
     * For each employee:
     *   1. Gets total hours worked from the Attendance CSV
     *   2. Computes gross pay, deductions, and net pay
     *   3. Updates the Employee object with computed values
     *   4. Saves all results back to the Employee CSV
     *
     * This is triggered by the "Compute Salaries" button in PayrollMenu.
     *
     * BUG FIX: previously this always returned a "complete" message,
     * even when the attendance file was missing/empty (every employee
     * silently got 0 hours) or when the CSV write itself failed. It now:
     *   - aborts immediately if there is no attendance data at all
     *   - reports how many employees had no matching attendance record
     *   - clearly distinguishes "computed but not saved" from genuine success
     * The result string always starts with "Salary computation complete!"
     * ONLY when computation AND saving both genuinely succeeded — PayrollMenu
     * relies on this to decide which dialog to show.
     *
     * @return Status message shown to the user after computation
     */
    public String computeAllSalaries() {

        if (employees.isEmpty()) {
            return "No employee records found.\n"
                 + "Please check that the CSV file exists and is not empty.";
        }

        Map<String, Double> hoursMap =
            CSVHandler.readTotalHoursWorked(ATTENDANCE_FILE);

        if (hoursMap.isEmpty()) {
            return "Salary computation aborted.\n"
                 + "No attendance records were found in:\n\"" + ATTENDANCE_FILE + "\"\n"
                 + "Please make sure the attendance file exists and is in the "
                 + "correct location, then try again.";
        }

        Map<String, Integer> monthsMap =
            CSVHandler.countMonthsWorked(ATTENDANCE_FILE);

        int missingCount = 0;

        for (Employee emp : employees) {
            if (!hoursMap.containsKey(emp.getEmployeeID())) {
                missingCount++;
            }

            double hours = hoursMap.getOrDefault(emp.getEmployeeID(), 0.0);
            int monthsWorked = monthsMap.getOrDefault(emp.getEmployeeID(), 1);
            if (monthsWorked < 1) monthsWorked = 1;

            double gross = SalaryComputationModule.computeGrossPay(emp.getHourlyRate(), hours);

            // Deduction brackets are monthly-scale — compute on the average
            // monthly gross, then scale back up to match the cumulative
            // gross/hours already shown elsewhere.
            double monthlyGross      = gross / monthsWorked;
            double monthlyDeductions = SalaryComputationModule.computeDeductions(monthlyGross);
            double deduct = monthlyDeductions * monthsWorked;

            double net = SalaryComputationModule.computeNetPay(gross, deduct);

            emp.setHoursWorked(hours);
            emp.setGrossPay(gross);
            emp.setTotalDeductions(deduct);
            emp.setNetPay(net);
        }

        boolean saved = CSVHandler.writeEmployees(EMPLOYEE_FILE, employees);

        // Notify listeners regardless of save outcome — the in-memory values
        // changed either way, so any open table should reflect them.
        notifyDataChanged();

        StringBuilder result = new StringBuilder();

        if (saved) {
            result.append("Salary computation complete!\n");
        } else {
            result.append("Salaries were computed but COULD NOT be saved to the CSV file.\n")
                  .append("Check file permissions and try again.\n");
        }

        result.append("Processed ").append(employees.size()).append(" employee(s).\n");

        if (missingCount > 0) {
            result.append(missingCount)
                  .append(" employee(s) had no attendance records and were given 0 hours.\n");
        }

        if (saved) {
            result.append("Results saved to CSV and visible in the table.");
        }

        return result.toString();
    }

    // --------------------------------------------------------
    // PAYROLL DISPLAY METHODS
    // --------------------------------------------------------

    /**
     * Generates a formatted payslip for the given employee ID.
     *
     * If Compute Salaries has not been run yet (grossPay is 0),
     * returns a prompt to run it first instead of showing zeros.
     *
     * @param employeeID The ID of the employee
     * @return Formatted payslip string, or an error/prompt message
     */
    public String generatePayslip(String employeeID) {

        Employee emp = findEmployee(employeeID);

        if (emp == null) {
            return "Employee with ID \"" + employeeID + "\" was not found.\n"
                 + "Please check the ID and try again.";
        }

        if (emp.getGrossPay() == 0) {
            return "Payslip for " + emp.getFullName() + " is not yet available.\n\n"
                 + "Please click 'Compute Salaries' in the Payroll Menu first\n"
                 + "to generate salary data from attendance records.";
        }

        double gross = emp.getGrossPay();

        // Recreate the same monthly-scale breakdown used in
        // computeAllSalaries() so SSS/PhilHealth/Pag-IBIG/Tax lines here
        // add up to the same Total Deductions already saved on the record.
        Map<String, Integer> monthsMap = CSVHandler.countMonthsWorked(ATTENDANCE_FILE);
        int monthsWorked = monthsMap.getOrDefault(employeeID, 1);
        if (monthsWorked < 1) monthsWorked = 1;

        double monthlyGross = gross / monthsWorked;
        double[] breakdown  = SalaryComputationModule.computeDeductionBreakdown(monthlyGross);
        double sss          = breakdown[0] * monthsWorked;
        double philHealth   = breakdown[1] * monthsWorked;
        double pagIbig      = breakdown[2] * monthsWorked;
        double tax          = breakdown[3] * monthsWorked;
        double totalDeduct  = emp.getTotalDeductions();
        double netPay       = emp.getNetPay();

        return  "================================\n"
              + "        MOTORPH PAYSLIP         \n"
              + "================================\n"
              + "Employee #   : " + emp.getEmployeeID()                              + "\n"
              + "Name         : " + emp.getFullName()                                + "\n"
              + "Position     : " + emp.getPosition()                                + "\n"
              + "SSS #        : " + emp.getSssNumber()                               + "\n"
              + "PhilHealth # : " + emp.getPhilHealthNumber()                        + "\n"
              + "TIN #        : " + emp.getTin()                                     + "\n"
              + "Pag-IBIG #   : " + emp.getPagIbigNumber()                           + "\n"
              + "Hourly Rate  : PHP " + String.format("%.2f", emp.getHourlyRate())   + "\n"
              + "Total Hours  : "     + String.format("%.2f", emp.getHoursWorked())  + "\n"
              + "--------------------------------\n"
              + "Gross Pay    : PHP " + String.format("%.2f", gross)                 + "\n"
              + "--------------------------------\n"
              + "SSS          : PHP " + String.format("%.2f", sss)                   + "\n"
              + "PhilHealth   : PHP " + String.format("%.2f", philHealth)            + "\n"
              + "Pag-IBIG     : PHP " + String.format("%.2f", pagIbig)              + "\n"
              + "Withholding  : PHP " + String.format("%.2f", tax)                   + "\n"
              + "Total Deduct : PHP " + String.format("%.2f", totalDeduct)           + "\n"
              + "--------------------------------\n"
              + "NET PAY      : PHP " + String.format("%.2f", netPay)               + "\n"
              + "================================\n";
    }

    /**
     * Returns a quick summary of all employees' hours and gross pay.
     * Used by the "Process Payroll" button in PayrollMenu.
     */
    public String processPayroll() {

        if (employees.isEmpty()) {
            return "No employee data loaded. Please check the CSV file.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== PAYROLL SUMMARY ===\n");
        sb.append(String.format("%-25s %10s %15s%n", "Name", "Hours", "Gross Pay"));
        sb.append("-------------------------------------------\n");

        for (Employee emp : employees) {
            sb.append(String.format("%-25s %10.2f  PHP %,.2f%n",
                      emp.getFullName(),
                      emp.getHoursWorked(),
                      emp.getGrossPay()));
        }

        sb.append("-------------------------------------------\n");
        sb.append("NOTE: Click 'Compute Salaries' for accurate data.");
        return sb.toString();
    }

    /**
     * Returns a full payroll report with gross pay, deductions,
     * net pay per employee, plus totals and average net pay.
     * Used by the "Generate Payroll Report" button in PayrollMenu.
     */
    public String generateSummary() {

        if (employees.isEmpty()) {
            return "No employee data loaded. Please check the CSV file.";
        }

        StringBuilder sb = new StringBuilder();
        double totalGross = 0, totalDeduct = 0, totalNet = 0;

        sb.append("================================\n");
        sb.append("    MOTORPH PAYROLL REPORT      \n");
        sb.append("================================\n");
        sb.append(String.format("%-6s %-22s %10s %12s %10s%n",
                  "ID", "Name", "Gross", "Deductions", "Net Pay"));
        sb.append("----------------------------------------------------------\n");

        for (Employee emp : employees) {
            double gross  = emp.getGrossPay();
            double deduct = emp.getTotalDeductions();
            double net    = emp.getNetPay();

            totalGross  += gross;
            totalDeduct += deduct;
            totalNet    += net;

            sb.append(String.format("%-6s %-22s %10.2f %12.2f %10.2f%n",
                      emp.getEmployeeID(), emp.getFullName(),
                      gross, deduct, net));
        }

        sb.append("----------------------------------------------------------\n");
        sb.append(String.format("%-29s %10.2f %12.2f %10.2f%n",
                  "TOTALS:", totalGross, totalDeduct, totalNet));
        sb.append("================================\n");
        sb.append("Total Employees : ").append(employees.size()).append("\n");
        sb.append(String.format("Average Net Pay : PHP %,.2f%n",
                  employees.size() > 0 ? totalNet / employees.size() : 0.0));
        sb.append("\nNOTE: Click 'Compute Salaries' first for accurate results.");

        return sb.toString();
    }
}