<h1>MotorPH Payroll System App</h1>


<h2>Project Overview</h2>

MotorPH Payroll System App is a Java Swing-based payroll and employee management system developed for MO-IT103 - Computer Programming 2 | Group 28 | H1101.

The system applies Object-Oriented Programming (OOP) principles, event-driven programming, and the Observer design pattern to manage employee records, attendance data, payroll computation, and payslip generation through a graphical user interface (GUI).

This project fulfills Milestone 2 requirements by implementing Features 2-4 as a cohesive and modular system with CSV file persistence, input validation, and maintainable code architecture. The application also includes Feature 5 (Payroll Summary) as an enhancement.
  
<h2>Features</h2>


<h3>Authentication</h3>

* Username and password login with masked password input (JPasswordField)
* Role-based access control for admin and employee users
* Employee identity verification using Employee ID
* Invalid login detection with user-friendly error messages
* Logout support with session refresh

<h3>Employee Management (Feature 2)</h3>

* Load and display employee records from a CSV file in a JTable
* Display Employee #, First Name, Last Name, Position, Hourly Rate, Hours Worked, Gross Pay, Total Deductions, and Net Pay
* Real-time search and filtering by Employee #, First Name, or Last Name
* Sortable table columns using TableRowSorter
* Add new employees with input validation and duplicate ID prevention
* Update existing employee records using pre-filled forms
* Delete employee records with confirmation dialogs
* View complete employee details
* View attendance records linked by Employee ID
* Add attendance entries with date and time validation

<h3>Salary Computation (Feature 3)</h3>

* Compute gross pay, government deductions, and net pay using a dedicated SalaryComputationModule
* Calculate SSS, PhilHealth, Pag-IBIG, and withholding tax deductions
* Save computed values back to the employee CSV file
* Apply monthly scaling for attendance records spanning multiple months
* Provide clear success and error messages during computation

<h3>Update and Delete Records (Feature 4)</h3>

* Update and delete employee records directly from the employee table
* Validate all inputs before saving changes
* Persist changes to the CSV file
* Refresh open windows automatically using the Observer pattern
* Display confirmation messages for update and delete actions

<h3>Payroll Summary (Feature 5 - Enhancement)</h3>

* Process payroll for all employees
* Generate formatted payslips by Employee ID
* Generate payroll reports with gross pay, deductions, net pay, totals, and average net pay

<h3>Employee Self-Service</h3>

* Restrict employees to viewing only their own information
* View personal payslips
* View personal attendance records


<h3>Employee Self-Service</h3>

* Restrict employees to viewing only their own information
* View personal payslips
* View personal attendance records


<h2>Technologies Used</h2>

* Java
* Java Swing
* Java Collections Framework (ArrayList)
* Event-Driven Programming
* Object-Oriented Programming (OOP)


<h2>System Structure</h2>

* Main.java — Entry point, login flow, main navigation menu
* Employee.java — Employee attributes and utility methods
* Attendance.java — Attendance records and hours worked computation
* PayrollService.java — Backend logic for CRUD, payroll computation, and report generation
* AuthService.java — Login validation and logout handling
* EmployeeMenu.java — Employee management GUI and event handling
* PayrollMenu.java — Payroll operations GUI and event handling


<h2>Running the Program</h2>

Default Login Credentials
* Username: admin
* Password: admin123

Sample Employee IDs for Testing
* 10001
* 10002
* 10003


<h2>Validation and Error Handling</h2>

* Empty field detection on all input forms
* Numeric validation for salary and hourly rate fields
* Positive value validation for salary and hourly rate
* Duplicate Employee ID prevention
* Empty selection handling for table actions
* Invalid login detection
* Employee not found handling during payslip generation
* Delete confirmation before record removal
* Friendly error messages throughout using JOptionPane


<h2>GUI Improvements</h2>

* Non-blocking JFrame main menu so sub-windows can be opened freely
* Real-time search/filter field in Employee Management
* JTable with non-editable cells and auto-refresh after changes
* Monospaced font in payroll output area for aligned payslip formatting
* Exit button placed on the right following standard UI conventions
* Consistent spacing, layout, and font across all windows
* User feedback dialogs for all major actions


<h2>Notes</h2>

* Sample employee and attendance records are hardcoded for MS1 demonstration purposes
* Default login credentials are predefined in AuthService.java
* Payroll deduction rates are approximations for MS1; official bracket tables will be applied in future milestones
* CSV stubs (readCSVFile, parseEmployees, parseAttendance) are defined in PayrollService.java and reserved for future milestone integration


<h2>Class Diagram Reference</h2>
https://docs.google.com/spreadsheets/d/1Jq32oJ2zFISkkz7ArMZi5tileXsol66STqpQaP1La8U/edit?usp=sharing

<h2>Final Submission</h2>
This repository contains the integrated group submission for the MotorPH Payroll System Milestone 1 prototype.
