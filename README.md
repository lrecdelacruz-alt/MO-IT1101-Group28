<h1>MotorPH Payroll System App</h1>


<h2>Project Overview</h2>

MotorPH Payroll System App is a Java Swing-based payroll and employee management prototype developed for Computer Programming 2.

The system applies Object-Oriented Programming (OOP) principles and event-driven programming to manage employee records, attendance data, payroll processing, and payslip generation through a graphical user interface (GUI).

This project follows the provided UML class diagram and milestone requirements focused on Java Swing implementation, backend integration, input validation, and interface navigation.
  
  
<h2>Features</h2>


Authentication
Username and password login with masked password input
Exit confirmation on login cancel
Invalid login detection with user-friendly error messages

<h2>Employee Management</h2>

Add, Update, Delete, and View employee records
Employee table display using JTable with real-time search/filter
Delete confirmation dialog before removal
View individual attendance records per employee

<h2>Attendance Management</h2>

Store and display sample attendance records
Compute total hours worked per employee
Link attendance records to employees by ID

<h2>Payroll Processing</h2>

Gross Pay, Deductions, and Net Pay computation
Deductions include SSS, PhilHealth, Pag-IBIG, and Withholding Tax
Individual payslip generation
Payroll summary report with totals and average net pay


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


**Class Diagram Reference**
https://docs.google.com/spreadsheets/d/1Jq32oJ2zFISkkz7ArMZi5tileXsol66STqpQaP1La8U/edit?usp=sharing

**Final Submission**
This repository contains the integrated group submission for the MotorPH Payroll System Milestone 1 prototype.
