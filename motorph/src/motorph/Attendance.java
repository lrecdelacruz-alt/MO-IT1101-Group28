package motorph;

// ATTENDANCE CLASS
// Represents a single attendance record for an employee.
//
// Responsibilities:
//   - Store attendance attributes (ID, employee ID, date, times)
//   - Calculate hours worked from time-in and time-out values
//   - Provide a formatted attendance detail string
//
// NOTE: timeIn and timeOut are stored as doubles representing
//       the 24-hour clock (e.g., 8.0 = 8:00 AM, 17.5 = 5:30 PM).

public class Attendance {

    // ATTRIBUTES

    private String attendanceID;
    private String employeeID;
    private String date;
    private double timeIn;
    private double timeOut;
    private double hoursWorked;

    // CONSTRUCTOR

    /**
     * Creates a new Attendance record.
     *
     * @param attendanceID Unique identifier for this record
     * @param employeeID   ID of the employee this record belongs to
     * @param date         Date of attendance (e.g., "2026-05-25")
     * @param timeIn       Time the employee clocked in (24-hour decimal)
     * @param timeOut      Time the employee clocked out (24-hour decimal)
     * @param hoursWorked  Pre-stored hours worked (0 if not yet calculated)
     */
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

    // GETTERS AND SETTERS

    public String getAttendanceID()              { return attendanceID; }
    public void   setAttendanceID(String id)     { this.attendanceID = id; }

    public String getEmployeeID()                { return employeeID; }
    public void   setEmployeeID(String id)       { this.employeeID = id; }

    public String getDate()                      { return date; }
    public void   setDate(String date)           { this.date = date; }

    public double getTimeIn()                    { return timeIn; }
    public void   setTimeIn(double timeIn)       { this.timeIn = timeIn; }

    public double getTimeOut()                   { return timeOut; }
    public void   setTimeOut(double timeOut)     { this.timeOut = timeOut; }

    public double getHoursWorked()               { return hoursWorked; }
    public void   setHoursWorked(double hours)   { this.hoursWorked = hours; }

    // CALCULATION METHOD

    /**
     * Calculates and returns the total hours worked based on
     * timeIn and timeOut. Returns 0 if timeOut is before timeIn
     * (treats invalid time ranges as no hours worked).
     *
     * NOTE: This method does NOT modify the stored hoursWorked field.
     *       Use setHoursWorked() separately if you need to persist the result.
     *
     * @return Computed hours worked as a double
     */
    public double calculateHoursWorked() {
        if (timeOut >= timeIn) {
            return timeOut - timeIn;
        }
        return 0;
    }

    // UTILITY METHOD

    /**
     * Returns a formatted multi-line string of all attendance details.
     * Useful for display in dialogs or console debugging.
     *
     * @return Formatted attendance record string
     */
    public String getAttendanceDetails() {
        return "Attendance ID : " + attendanceID
             + "\nEmployee ID   : " + employeeID
             + "\nDate          : " + date
             + "\nTime In       : " + timeIn
             + "\nTime Out      : " + timeOut
             + "\nHours Worked  : " + calculateHoursWorked();
    }
}
