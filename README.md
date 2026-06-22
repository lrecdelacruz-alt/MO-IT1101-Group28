*Project Overview

MotorPH Payroll System App is a Java Swing-based payroll and employee management system developed for MO-IT103 - Computer Programming 2 | Group 28 | H1101.

The system applies Object-Oriented Programming (OOP) principles, event-driven programming, and the Observer design pattern to manage employee records, attendance data, payroll computation, and payslip generation through a graphical user interface (GUI).

This project fulfills Milestone 2 requirements by implementing Features 2-4 as a cohesive and modular system with CSV file persistence, input validation, and maintainable code architecture. The application also includes Feature 5 (Payroll Summary) as an enhancement.

⸻

Features

Authentication

Username and password login with masked password input (JPasswordField)
Role-based access control for admin and employee users
Employee identity verification using Employee ID
Invalid login detection with user-friendly error messages
Logout support with session refresh

Employee Management (Feature 2)

Load and display employee records from a CSV file in a JTable
Display Employee #, First Name, Last Name, Position, Hourly Rate, Hours Worked, Gross Pay, Total Deductions, and Net Pay
Real-time search and filtering by Employee #, First Name, or Last Name
Sortable table columns using TableRowSorter
Add new employees with input validation and duplicate ID prevention
Update existing employee records using pre-filled forms
Delete employee records with confirmation dialogs
View complete employee details
View attendance records linked by Employee ID
Add attendance entries with date and time validation

Salary Computation (Feature 3)

Compute gross pay, government deductions, and net pay using a dedicated SalaryComputationModule
Calculate SSS, PhilHealth, Pag-IBIG, and withholding tax deductions
Save computed values back to the employee CSV file
Apply monthly scaling for attendance records spanning multiple months
Provide clear success and error messages during computation

Update and Delete Records (Feature 4)

Update and delete employee records directly from the employee table
Validate all inputs before saving changes
Persist changes to the CSV file
Refresh open windows automatically using the Observer pattern
Display confirmation messages for update and delete actions

Payroll Summary (Feature 5 - Enhancement)

Process payroll for all employees
Generate formatted payslips by Employee ID
Generate payroll reports with gross pay, deductions, net pay, totals, and average net pay

Employee Self-Service

Restrict employees to viewing only their own information
View personal payslips
View personal attendance records

⸻

Technologies Used

Java
Java Swing
Java Collections Framework (ArrayList, HashMap, HashSet)
CSV file I/O (BufferedReader, BufferedWriter, RandomAccessFile)
java.time.LocalTime
Event-driven programming (ActionListener, DocumentListener, WindowAdapter)
Object-Oriented Programming principles
Observer Design Pattern (DataChangeListener)

⸻

System Structure

Main.java - Application entry point, login flow, and session management
Employee.java - Employee data model
Attendance.java - Attendance data model
AuthService.java - Authentication and role management
CSVHandler.java - CSV file reading and writing operations
PayrollService.java - Employee CRUD operations, payroll coordination, and Observer notifications
SalaryComputationModule.java - Salary and deduction calculations
EmployeeMenu.java - Employee management GUI
PayrollMenu.java - Payroll operations GUI
DataChangeListener.java - Observer interface for window synchronization
AppIcon.java - Shared application icon
UIHelper.java - Shared button styling utilities

⸻

CSV Files Required

Place the following files in the location expected by the application:

MotorPH_Employee Data - Employee Details.csv - Employee records
MotorPH_Employee Data - Attendance Record.csv - Attendance records

Update the file paths in the source code if your CSV files are stored in a different directory.

⸻

Running the Program

1. Clone or download the repository.
2. Place the required CSV files in the configured file location.
3. Compile and run Main.java.
4. Log in using the credentials below.

Default Login Credentials

Admin

Username: admin
Password: admin123

Employee

Username: employee
Password: employee123

Employees must enter their Employee ID after login to access their records.

⸻

Validation and Error Handling

Required field validation for all forms
Numeric validation for Employee #, Salary, and Hourly Rate
Positive value validation for Salary and Hourly Rate
Government ID validation for SSS, PhilHealth, TIN, and Pag-IBIG
Duplicate Employee ID prevention
Date validation using MM/DD/YYYY format
Time validation using H:mm format
Attendance log-out time must be later than log-in time
Confirmation dialogs for critical actions
Rollback on failed CSV writes to maintain data consistency
User-friendly success and error messages

⸻

GUI Design

Non-blocking JFrame navigation
Consistent color-coded actions for add, update, delete, and view operations
Non-editable and sortable JTable
Real-time search and filtering
Monospaced formatting for reports and payslips
Automatic table refresh through the Observer pattern
Prevention of duplicate menu windows
Proper handling of sorted table selections using convertRowIndexToModel()

⸻

Salary Computation Details

SSS: Official SSS contribution table
PhilHealth: 1.5% rate with floor and cap
Pag-IBIG: Percentage-based contribution with cap
Withholding Tax: BIR tax bracket table

Taxable income = Gross Pay - (SSS + PhilHealth + Pag-IBIG)

Net Pay = Gross Pay - Total Deductions
