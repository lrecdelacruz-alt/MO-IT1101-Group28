package motorph;

// EMPLOYEE CLASS
// Represents a single employee and stores their personal
// and salary-related information.
//
// Responsibilities:
//   - Store employee attributes (ID, name, position, pay)
//   - Provide getters and setters for each field
//   - Provide utility methods: getFullName(), displayEmployeeInfo()
public class Employee {

    // ATTRIBUTES (aligned with class diagram inventory)
    private String employeeID;
    private String firstName;
    private String lastName;
    private String sssNumber;
    private String philHealthNumber;
    private String tin;
    private String pagIbigNumber;
    private String position;
    private double hourlyRate;
    private double basicSalary;
    private double hoursWorked;
    private double grossPay;
    private double totalDeductions;
    private double netPay;

    // CONSTRUCTOR — full (used when loading from CSV, all fields known)
    public Employee(String employeeID,
            String firstName,
            String lastName,
            String sssNumber,
            String philHealthNumber,
            String tin,
            String pagIbigNumber,
            String position,
            double hourlyRate,
            double basicSalary,
            double hoursWorked,
            double grossPay,
            double totalDeductions,
            double netPay) {
        this.employeeID = employeeID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.sssNumber = sssNumber;
        this.philHealthNumber = philHealthNumber;
        this.tin = tin;
        this.pagIbigNumber = pagIbigNumber;
        this.position = position;
        this.hourlyRate = hourlyRate;
        this.basicSalary = basicSalary;
        this.hoursWorked = hoursWorked;
        this.grossPay = grossPay;
        this.totalDeductions = totalDeductions;
        this.netPay = netPay;
    }

    // CONSTRUCTOR — convenience (used when adding a brand-new employee via the
    // GUI; computed salary fields don't exist yet, so they default to 0)
    public Employee(String employeeID,
            String firstName,
            String lastName,
            String sssNumber,
            String philHealthNumber,
            String tin,
            String pagIbigNumber,
            String position,
            double hourlyRate,
            double basicSalary) {
        this(employeeID, firstName, lastName, sssNumber, philHealthNumber, tin,
                pagIbigNumber, position, hourlyRate, basicSalary, 0.0, 0.0, 0.0, 0.0);
    }

    // GETTERS AND SETTERS
    public String getEmployeeID() {
        return employeeID;
    }

    public void setEmployeeID(String id) {
        this.employeeID = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String fn) {
        this.firstName = fn;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String ln) {
        this.lastName = ln;
    }
    
    public String getSssNumber() {
        return sssNumber;
    }
    
    public void setSssNumber(String sssNumber) {
        this.sssNumber = sssNumber;
    }
    public String getPhilHealthNumber() {
        return philHealthNumber;
    }
    
    public void setPhilHealthNumber(String philHealthNumber) {
        this.philHealthNumber = philHealthNumber;
    }
    
    public String getTin() {
        return tin;
    }
    
    public void setTin(String tin) {
        this.tin = tin;
    }
    
    public String getPagIbigNumber() {
        return pagIbigNumber;
    }
    
    public void setPagIbigNumber(String pagIbigNumber) {
        this.pagIbigNumber = pagIbigNumber;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String pos) {
        this.position = pos;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double rate) {
        this.hourlyRate = rate;
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(double sal) {
        this.basicSalary = sal;
    }
    
    public double getHoursWorked() {
        return hoursWorked;
    }
    
    public void setHoursWorked(double hoursWorked) {
        this.hoursWorked = hoursWorked;
    }
    
    public double getGrossPay() {
        return grossPay;
    }
    
    public void setGrossPay(double grossPay) {
        this.grossPay = grossPay;
    }
    
    public double getTotalDeductions() {
        return totalDeductions;
    }
    
    public void setTotalDeductions(double totalDeductions) {
        this.totalDeductions = totalDeductions;
    }
    
    public double getNetPay() {
        return netPay;
    }
    
    public void setNetPay(double netPay) {
        this.netPay = netPay;
    }

    // UTILITY METHODS
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Prints a formatted summary of the employee's details to the console. Used
     * for debugging purposes; GUI display is handled by EmployeeMenu.
     */
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
