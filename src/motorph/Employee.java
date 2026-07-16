package motorph;

// ============================================================
// EMPLOYEE
//
// Represents an employee in the payroll system and stores
// personal, employment, and payroll information.
//
// ============================================================
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

    // ------------------------------------------------------------
    // CONSTRUCTORS
    // ------------------------------------------------------------
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

    // ------------------------------------------------------------
    // GETTERS AND SETTERS
    // ------------------------------------------------------------
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

    // ------------------------------------------------------------
    // UTILITY METHODS
    // ------------------------------------------------------------
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return employeeID + " - " + getFullName();
    }
}

