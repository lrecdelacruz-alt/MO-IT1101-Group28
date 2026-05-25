import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

// ============================================================
// MAIN CLASS — Entry point and navigation handler
// ============================================================

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            // Create service objects
            PayrollService payrollService = new PayrollService();
            AuthService authService = new AuthService();

            // Load sample employee and attendance data
            payrollService.loadEmployees();
            payrollService.loadAttendance();

            // Login form
            String username = JOptionPane.showInputDialog(
                    null,
                    "Enter Username:",
                    "MotorPH Login",
                    JOptionPane.PLAIN_MESSAGE);

            String password = JOptionPane.showInputDialog(
                    null,
                    "Enter Password:",
                    "MotorPH Login",
                    JOptionPane.PLAIN_MESSAGE);

            // Validate login
            if (authService.login(username, password)) {

                JOptionPane.showMessageDialog(null, "Login Successful!");

                String[] options = {"Employee Menu", "Payroll Menu", "Generate Payslip", "Exit"};

                int choice;

                do {
                    choice = JOptionPane.showOptionDialog(
                            null,
                            "Choose an option:",
                            "MotorPH Payroll System",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            options,
                            options[0]);

                    switch (choice) {

                        case 0:
                            // Open Employee Menu
                            EmployeeMenu employeeMenu = new EmployeeMenu(payrollService);
                            employeeMenu.displayMenu();
                            break;

                        case 1:
                            // Open Payroll Menu
                            PayrollMenu payrollMenu = new PayrollMenu(payrollService);
                            payrollMenu.displayPayrollMenu();
                            break;

                        case 2:
                            // Generate Payslip
                            String employeeID = JOptionPane.showInputDialog("Enter Employee ID:");
                            String payslip = payrollService.generatePayslip(employeeID);
                            JTextArea textArea = new JTextArea(payslip);
                            textArea.setEditable(false);
                            JOptionPane.showMessageDialog(
                                    null,
                                    new JScrollPane(textArea),
                                    "Payslip",
                                    JOptionPane.INFORMATION_MESSAGE);
                            break;

                        default:
                            JOptionPane.showMessageDialog(null, "Exiting system...");
                    }

                } while (choice != 3);

            } else {
                JOptionPane.showMessageDialog(null, "Invalid username or password!");
            }
        });
    }
}

// ============================================================
// EMPLOYEE CLASS — Stores employee information
// ============================================================
class Employee {

    private String employeeID;
    private String firstName;
    private String lastName;
    private String position;
    private double hourlyRate;
    private double basicSalary;

    public Employee(String employeeID,
                    String firstName,
                    String lastName,
                    String position,
                    double hourlyRate,
                    double basicSalary) {

        this.employeeID = employeeID;
        this.firstName  = firstName;
        this.lastName   = lastName;
        this.position   = position;
        this.hourlyRate = hourlyRate;
        this.basicSalary = basicSalary;
    }

    // Getters and Setters

    public String getEmployeeID()              { return employeeID; }
    public void   setEmployeeID(String id)     { this.employeeID = id; }

    public String getFirstName()               { return firstName; }
    public void   setFirstName(String fn)      { this.firstName = fn; }

    public String getLastName()                { return lastName; }
    public void   setLastName(String ln)       { this.lastName = ln; }

    public String getPosition()                { return position; }
    public void   setPosition(String pos)      { this.position = pos; }

    public double getHourlyRate()              { return hourlyRate; }
    public void   setHourlyRate(double rate)   { this.hourlyRate = rate; }

    public double getBasicSalary()             { return basicSalary; }
    public void   setBasicSalary(double sal)   { this.basicSalary = sal; }

    // Returns the employee's full name
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Prints employee details to the console
    public void displayEmployeeInfo() {
        System.out.println("Employee ID  : " + employeeID);
        System.out.println("Name         : " + getFullName());
        System.out.println("Position     : " + position);
        System.out.println("Hourly Rate  : " + hourlyRate);
        System.out.println("Basic Salary : " + basicSalary);
    }

    @Override
    public String toString() {
        return employeeID + " - " + getFullName();
    }
}

// ============================================================
// ATTENDANCE CLASS — Stores and computes attendance records
// ============================================================

class Attendance {

    private String attendanceID;
    private String employeeID;
    private String date;
    private double timeIn;
    private double timeOut;
    private double hoursWorked;

    public Attendance(String attendanceID,
                      String employeeID,
                      String date,
                      double timeIn,
                      double timeOut,
                      double hoursWorked) {

        this.attendanceID = attendanceID;
        this.employeeID   = employeeID;
        this.date         = date;
        this.timeIn       = timeIn;
        this.timeOut      = timeOut;
        this.hoursWorked  = hoursWorked;
    }

    // Getters and Setters

    public String getAttendanceID()             { return attendanceID; }
    public void   setAttendanceID(String id)    { this.attendanceID = id; }

    public String getEmployeeID()               { return employeeID; }
    public void   setEmployeeID(String id)      { this.employeeID = id; }

    public String getDate()                     { return date; }
    public void   setDate(String date)          { this.date = date; }

    public double getTimeIn()                   { return timeIn; }
    public void   setTimeIn(double t)           { this.timeIn = t; }

    public double getTimeOut()                  { return timeOut; }
    public void   setTimeOut(double t)          { this.timeOut = t; }

    public double getHoursWorked()              { return hoursWorked; }
    public void   setHoursWorked(double h)      { this.hoursWorked = h; }

    // Calculates total worked hours based on time in and time out
    public double calculateHoursWorked() {
        if (timeOut >= timeIn) {
            hoursWorked = timeOut - timeIn;
        } else {
            hoursWorked = 0;
        }
        return hoursWorked;
    }

    // Returns a formatted string of attendance details
    public String getAttendanceDetails() {
        return "Attendance ID : " + attendanceID
             + "\nEmployee ID   : " + employeeID
             + "\nDate          : " + date
             + "\nTime In       : " + timeIn
             + "\nTime Out      : " + timeOut
             + "\nHours Worked  : " + calculateHoursWorked();
    }
}

// ============================================================
// AUTH SERVICE CLASS — Handles user login validation
// ============================================================

class AuthService {

    // Default credentials — update as needed
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";

    public boolean login(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        return username.equals(DEFAULT_USERNAME)
            && password.equals(DEFAULT_PASSWORD);
    }
}

// ============================================================
// PAYROLL SERVICE CLASS — Manages employee data and payroll
// ============================================================

class PayrollService {

    private List<Employee>  employees      = new ArrayList<>();
    private List<Attendance> attendanceList = new ArrayList<>();

    // Loads sample employees (replace content with CSV reading logic later)
    public void loadEmployees() {
        employees.add(new Employee("10001", "Micah",  "Santos",    "HR Manager",          250, 50000));
        employees.add(new Employee("10002", "Juan",   "Dela Cruz", "Software Engineer",   300, 60000));
        employees.add(new Employee("10003", "Maria",  "Reyes",     "Accountant",          200, 40000));
    }

    // Loads sample attendance (replace content with CSV reading logic later)
    public void loadAttendance() {
        attendanceList.add(new Attendance("ATT001", "10001", "2026-05-25",  8.0, 17.0, 0));
        attendanceList.add(new Attendance("ATT002", "10002", "2026-05-25",  9.0, 18.0, 0));
        attendanceList.add(new Attendance("ATT003", "10003", "2026-05-25",  8.0, 17.0, 0));
    }

    public List<Employee> getEmployees() {
        return employees;
    }

    public List<Attendance> getAttendanceList() {
        return attendanceList;
    }

    // Finds an employee by their ID; returns null if not found
    public Employee findEmployeeByID(String employeeID) {
        for (Employee emp : employees) {
            if (emp.getEmployeeID().equals(employeeID)) {
                return emp;
            }
        }
        return null;
    }

    // Sums up all hours worked across attendance records for a given employee
    public double getTotalHoursWorked(String employeeID) {
        double total = 0;
        for (Attendance att : attendanceList) {
            if (att.getEmployeeID().equals(employeeID)) {
                total += att.calculateHoursWorked();
            }
        }
        return total;
    }

    // Generates a formatted payslip string for a given employee ID
    public String generatePayslip(String employeeID) {

        Employee emp = findEmployeeByID(employeeID);

        if (emp == null) {
            return "Employee with ID \"" + employeeID + "\" not found.";
        }

        double totalHours = getTotalHoursWorked(employeeID);
        double grossPay   = totalHours * emp.getHourlyRate();

        // Deduction rates (update with actual PH government rates as needed)
        double sssDeduction        = grossPay * 0.045;
        double philHealthDeduction = grossPay * 0.020;
        double pagIbigDeduction    = 100.00;
        double withholdingTax      = grossPay * 0.100;
        double totalDeductions     = sssDeduction + philHealthDeduction
                                   + pagIbigDeduction + withholdingTax;
        double netPay = grossPay - totalDeductions;

        return  "================================\n"
              + "        MOTORPH PAYSLIP         \n"
              + "================================\n"
              + "Employee ID  : " + emp.getEmployeeID()                         + "\n"
              + "Name         : " + emp.getFullName()                           + "\n"
              + "Position     : " + emp.getPosition()                           + "\n"
              + "Hourly Rate  : PHP " + String.format("%.2f", emp.getHourlyRate()) + "\n"
              + "Total Hours  : "     + String.format("%.2f", totalHours)       + "\n"
              + "--------------------------------\n"
              + "Gross Pay    : PHP " + String.format("%.2f", grossPay)         + "\n"
              + "--------------------------------\n"
              + "SSS          : PHP " + String.format("%.2f", sssDeduction)     + "\n"
              + "PhilHealth   : PHP " + String.format("%.2f", philHealthDeduction) + "\n"
              + "Pag-IBIG     : PHP " + String.format("%.2f", pagIbigDeduction) + "\n"
              + "Withholding  : PHP " + String.format("%.2f", withholdingTax)   + "\n"
              + "Total Deduct : PHP " + String.format("%.2f", totalDeductions)  + "\n"
              + "--------------------------------\n"
              + "NET PAY      : PHP " + String.format("%.2f", netPay)           + "\n"
              + "================================\n";
    }

    // Adds a new employee to the list
    public void addEmployee(Employee emp) {
        employees.add(emp);
    }

    // Updates an existing employee's editable fields; returns false if not found
    public boolean updateEmployee(String employeeID, String firstName,
                                  String lastName,  String position) {
        Employee emp = findEmployeeByID(employeeID);
        if (emp != null) {
            emp.setFirstName(firstName);
            emp.setLastName(lastName);
            emp.setPosition(position);
            return true;
        }
        return false;
    }

    // Removes an employee from the list; returns false if not found
    public boolean deleteEmployee(String employeeID) {
        Employee emp = findEmployeeByID(employeeID);
        if (emp != null) {
            employees.remove(emp);
            return true;
        }
        return false;
    }
}

// ============================================================
// EMPLOYEE MENU CLASS — GUI for employee management (CRUD)
// ============================================================

class EmployeeMenu {

    private JFrame             frame;
    private JTable             employeeTable;
    private DefaultTableModel  tableModel;
    private JButton            addButton, updateButton, deleteButton, viewButton;
    private JTextArea          outputArea;
    private PayrollService     payrollService;

    public EmployeeMenu(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    // Builds and shows the Employee Management window
    public void displayMenu() {

        frame = new JFrame("Employee Management System");

        // Title label
        JLabel titleLabel = new JLabel(
                "Employee Management System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));

        // Action buttons
        addButton    = new JButton("Add Employee");
        updateButton = new JButton("Update Employee");
        deleteButton = new JButton("Delete Employee");
        viewButton   = new JButton("View Employee");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(viewButton);

        // Table setup
        String[] columns = {
            "First Name", "Last Name", "Employee ID",
            "Position", "Basic Salary", "Hourly Rate"
        };
        tableModel    = new DefaultTableModel(columns, 0);
        employeeTable = new JTable(tableModel);

        // Populate table from PayrollService data
        for (Employee emp : payrollService.getEmployees()) {
            tableModel.addRow(new Object[]{
                emp.getFirstName(),
                emp.getLastName(),
                emp.getEmployeeID(),
                emp.getPosition(),
                emp.getBasicSalary(),
                emp.getHourlyRate()
            });
        }

        JScrollPane tableScrollPane = new JScrollPane(employeeTable);

        // Output area for status messages
        outputArea = new JTextArea(5, 30);
        outputArea.setEditable(false);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);

        // Layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.add(titleLabel,       BorderLayout.NORTH);
        mainPanel.add(tableScrollPane,  BorderLayout.CENTER);
        mainPanel.add(buttonPanel,      BorderLayout.SOUTH);

        frame.setLayout(new BorderLayout());
        frame.add(mainPanel,        BorderLayout.CENTER);
        frame.add(outputScrollPane, BorderLayout.SOUTH);
        frame.setSize(900, 500);
        // DISPOSE_ON_CLOSE so closing this window does not shut down the whole app
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        // Wire buttons
        addButton.addActionListener(e    -> addEmployee());
        updateButton.addActionListener(e -> updateEmployee());
        deleteButton.addActionListener(e -> deleteEmployee());
        viewButton.addActionListener(e   -> viewEmployee());

        frame.setVisible(true);
    }

    // Prompts user for new employee details and adds to both table and service
    public void addEmployee() {

        JTextField firstNameField  = new JTextField();
        JTextField lastNameField   = new JTextField();
        JTextField idField         = new JTextField();
        JTextField positionField   = new JTextField();
        JTextField salaryField     = new JTextField();
        JTextField hourlyRateField = new JTextField();

        Object[] fields = {
            "First Name:",  firstNameField,
            "Last Name:",   lastNameField,
            "Employee ID:", idField,
            "Position:",    positionField,
            "Basic Salary:", salaryField,
            "Hourly Rate:", hourlyRateField
        };

        int option = JOptionPane.showConfirmDialog(
                frame, fields, "Add Employee", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try {
                double salary     = Double.parseDouble(salaryField.getText());
                double hourlyRate = Double.parseDouble(hourlyRateField.getText());

                Employee newEmp = new Employee(
                        idField.getText(),
                        firstNameField.getText(),
                        lastNameField.getText(),
                        positionField.getText(),
                        hourlyRate,
                        salary);

                payrollService.addEmployee(newEmp);

                tableModel.addRow(new Object[]{
                        firstNameField.getText(),
                        lastNameField.getText(),
                        idField.getText(),
                        positionField.getText(),
                        salary,
                        hourlyRate
                });

                outputArea.setText("Employee added successfully.");

            } catch (NumberFormatException ex) {
                outputArea.setText("Error: Salary and Hourly Rate must be numbers.");
            }
        }
    }

    // Updates the name and position of the selected employee row
    public void updateEmployee() {

        int selectedRow = employeeTable.getSelectedRow();

        if (selectedRow == -1) {
            outputArea.setText("Please select an employee first.");
            return;
        }

        String firstName = JOptionPane.showInputDialog(
                frame, "Enter New First Name:", tableModel.getValueAt(selectedRow, 0));
        String lastName  = JOptionPane.showInputDialog(
                frame, "Enter New Last Name:",  tableModel.getValueAt(selectedRow, 1));
        String position  = JOptionPane.showInputDialog(
                frame, "Enter New Position:",   tableModel.getValueAt(selectedRow, 3));

        String employeeID = (String) tableModel.getValueAt(selectedRow, 2);
        payrollService.updateEmployee(employeeID, firstName, lastName, position);

        tableModel.setValueAt(firstName, selectedRow, 0);
        tableModel.setValueAt(lastName,  selectedRow, 1);
        tableModel.setValueAt(position,  selectedRow, 3);

        outputArea.setText("Employee updated successfully.");
    }

    // Removes the selected employee row from both the table and the service
    public void deleteEmployee() {

        int selectedRow = employeeTable.getSelectedRow();

        if (selectedRow == -1) {
            outputArea.setText("Please select an employee first.");
            return;
        }

        String employeeID = (String) tableModel.getValueAt(selectedRow, 2);
        payrollService.deleteEmployee(employeeID);
        tableModel.removeRow(selectedRow);

        outputArea.setText("Employee deleted.");
    }

    // Displays the selected employee's details in the output area
    public void viewEmployee() {

        int selectedRow = employeeTable.getSelectedRow();

        if (selectedRow == -1) {
            outputArea.setText("Please select an employee first.");
            return;
        }

        String info =
              "First Name   : " + tableModel.getValueAt(selectedRow, 0)
            + "\nLast Name    : " + tableModel.getValueAt(selectedRow, 1)
            + "\nEmployee ID  : " + tableModel.getValueAt(selectedRow, 2)
            + "\nPosition     : " + tableModel.getValueAt(selectedRow, 3)
            + "\nBasic Salary : " + tableModel.getValueAt(selectedRow, 4)
            + "\nHourly Rate  : " + tableModel.getValueAt(selectedRow, 5);

        outputArea.setText(info);
    }
}

// ============================================================
// PAYROLL MENU CLASS — GUI for payroll operations
// ============================================================

class PayrollMenu {

    private JFrame    frame;
    private JTextArea outputArea;
    private JButton   processPayrollButton, viewPayslipButton, generateReportButton;
    private PayrollService payrollService;

    public PayrollMenu(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    // Builds and shows the Payroll Menu window
    public void displayPayrollMenu() {

        frame = new JFrame("Payroll Menu");
        frame.setSize(450, 400);
        frame.setLayout(new FlowLayout());
        // DISPOSE_ON_CLOSE so closing this window does not shut down the whole app
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        outputArea = new JTextArea(12, 35);
        outputArea.setEditable(false);

        processPayrollButton  = new JButton("Process Payroll");
        viewPayslipButton     = new JButton("View Payslip");
        generateReportButton  = new JButton("Generate Payroll Report");

        frame.add(processPayrollButton);
        frame.add(viewPayslipButton);
        frame.add(generateReportButton);
        frame.add(new JScrollPane(outputArea));

        processPayrollButton.addActionListener(e  -> processPayroll());
        viewPayslipButton.addActionListener(e     -> viewPayslip());
        generateReportButton.addActionListener(e  -> generatePayrollReport());

        frame.setVisible(true);
    }

    // Lists each employee's total hours and gross pay
    public void processPayroll() {

        StringBuilder sb = new StringBuilder("=== PAYROLL SUMMARY ===\n");

        for (Employee emp : payrollService.getEmployees()) {
            double hours = payrollService.getTotalHoursWorked(emp.getEmployeeID());
            double gross = hours * emp.getHourlyRate();
            sb.append(emp.getFullName())
              .append("  |  Hours: ").append(String.format("%.2f", hours))
              .append("  |  Gross: PHP ").append(String.format("%.2f", gross))
              .append("\n");
        }

        outputArea.setText(sb.toString());
    }

    // Prompts for an employee ID and shows their payslip
    public void viewPayslip() {

        String empID = JOptionPane.showInputDialog(frame, "Enter Employee ID:");

        if (empID != null && !empID.trim().isEmpty()) {
            outputArea.setText(payrollService.generatePayslip(empID.trim()));
        }
    }

    // Generates a total payroll report with a running total
    public void generatePayrollReport() {

        StringBuilder report    = new StringBuilder("=== PAYROLL REPORT ===\n");
        double        totalGross = 0;

        for (Employee emp : payrollService.getEmployees()) {
            double hours = payrollService.getTotalHoursWorked(emp.getEmployeeID());
            double gross = hours * emp.getHourlyRate();
            totalGross  += gross;
            report.append(emp.getEmployeeID())
                  .append(" - ").append(emp.getFullName())
                  .append("  |  PHP ").append(String.format("%.2f", gross))
                  .append("\n");
        }

        report.append("-------------------------\n");
        report.append("Total Payroll : PHP ").append(String.format("%.2f", totalGross));

        outputArea.setText(report.toString());
    }
}
