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
    private String position;
    private double hourlyRate;
    private double basicSalary;

    // CONSTRUCTOR

    /**
     * Creates a new Employee with all required fields.
     *
     * @param employeeID  Unique identifier for the employee
     * @param firstName   Employee's first name
     * @param lastName    Employee's last name
     * @param position    Job title or position
     * @param hourlyRate  Pay per hour (used in gross pay computation)
     * @param basicSalary Monthly basic salary
     */
    public Employee(String employeeID,
                    String firstName,
                    String lastName,
                    String position,
                    double hourlyRate,
                    double basicSalary) {

        this.employeeID  = employeeID;
        this.firstName   = firstName;
        this.lastName    = lastName;
        this.position    = position;
        this.hourlyRate  = hourlyRate;
        this.basicSalary = basicSalary;
    }

    // GETTERS AND SETTERS

    public String getEmployeeID()            { return employeeID; }
    public void   setEmployeeID(String id)   { this.employeeID = id; }

    public String getFirstName()             { return firstName; }
    public void   setFirstName(String fn)    { this.firstName = fn; }

    public String getLastName()              { return lastName; }
    public void   setLastName(String ln)     { this.lastName = ln; }

    public String getPosition()              { return position; }
    public void   setPosition(String pos)    { this.position = pos; }

    public double getHourlyRate()            { return hourlyRate; }
    public void   setHourlyRate(double rate) { this.hourlyRate = rate; }

    public double getBasicSalary()           { return basicSalary; }
    public void   setBasicSalary(double sal) { this.basicSalary = sal; }

    // UTILITY METHODS

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Prints a formatted summary of the employee's details to the console.
     * Used for debugging purposes; GUI display is handled by EmployeeMenu.
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
