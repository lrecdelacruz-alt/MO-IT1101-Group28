package motorph;

// ============================================================
// ATTENDANCE CLASS
// Represents a single attendance record for an employee.
// ============================================================

public class Attendance {

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

    public String getAttendanceID()            { return attendanceID; }
    public void   setAttendanceID(String id)   { this.attendanceID = id; }

    public String getEmployeeID()              { return employeeID; }
    public void   setEmployeeID(String id)     { this.employeeID = id; }

    public String getDate()                    { return date; }
    public void   setDate(String date)         { this.date = date; }

    public double getTimeIn()                  { return timeIn; }
    public void   setTimeIn(double t)          { this.timeIn = t; }

    public double getTimeOut()                 { return timeOut; }
    public void   setTimeOut(double t)         { this.timeOut = t; }

    public double getHoursWorked()             { return hoursWorked; }
    public void   setHoursWorked(double h)     { this.hoursWorked = h; }

    /**
     * Utility method — calculates hours worked from timeIn and timeOut.
     * Reserved for future use; actual hours are computed by CSVHandler
     * using LocalTime parsing for accuracy.
     */
    public double calculateHoursWorked() {
        return (timeOut >= timeIn) ? timeOut - timeIn : 0;
    }

    public String getAttendanceDetails() {
        return "Attendance ID : " + attendanceID
             + "\nEmployee ID   : " + employeeID
             + "\nDate          : " + date
             + "\nTime In       : " + timeIn
             + "\nTime Out      : " + timeOut
             + "\nHours Worked  : " + calculateHoursWorked();
    }
}