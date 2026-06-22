package motorph;

import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ============================================================
// CSV HANDLER CLASS
// Handles all CSV file reading and writing for the MotorPH system.
//
// Responsibilities:
//   - Read employee records from the MotorPH Employee Details CSV
//   - Read attendance and compute total hours worked per employee
//   - Read attendance records for a specific employee (for viewer)
//   - Write the full employee list back to CSV (update/delete)
//   - Append a single new employee to the CSV (add)
// ============================================================

public class CSVHandler {

    private static final String CSV_HEADER =
        "Employee #,Last Name,First Name,SSS #,PhilHealth #,"
        + "TIN #,Pag-IBIG #,Position,Hourly Rate,Basic Salary,"
        + "Hours Worked,Gross Pay,Total Deductions,Net Pay";

    // Formatter for attendance Log In / Log Out (e.g. "8:59", "18:31")
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("H:mm");

    // --------------------------------------------------------
    // READ EMPLOYEES
    // --------------------------------------------------------

    /**
     * Reads all employee records from the CSV file.
     * Automatically handles both the original 19-column MotorPH format
     * and our 14-column working format.
     *
     * @param filePath Path to the employee CSV file
     * @return List of Employee objects
     */
    public static List<Employee> readEmployees(String filePath) {

        lastParseWarningCount = 0;
        List<Employee> employees = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("Employee file not found: " + filePath);
            return employees;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {

                line = line.trim(); // handles Windows \r\n endings

                if (isFirstLine) { isFirstLine = false; continue; }
                if (line.isEmpty()) continue;

                String[] cols = parseCSVLine(line);

                try {
                    if (cols.length >= 19) {
                        // Original MotorPH Employee Details CSV (19 columns)
                        String employeeID        = cols[0].trim();
                        String lastName          = cols[1].trim();
                        String firstName         = cols[2].trim();
                        String sssNumber         = cols[6].trim();
                        String philHealthNumber  = cols[7].trim();
                        String tin               = cols[8].trim();
                        String pagIbigNumber     = cols[9].trim();
                        String position          = cols[11].trim();
                        double basicSalary       = parseFormattedDouble(cols[13]);
                        double hourlyRate        = parseDouble(cols[18]);

                        employees.add(new Employee(
                            employeeID, firstName, lastName,
                            sssNumber, philHealthNumber, tin, pagIbigNumber,
                            position, hourlyRate, basicSalary
                        ));

                    } else if (cols.length >= 14) {
                        // Our working 14-column format
                        String employeeID        = cols[0].trim();
                        String lastName          = cols[1].trim();
                        String firstName         = cols[2].trim();
                        String sssNumber         = cols[3].trim();
                        String philHealthNumber  = cols[4].trim();
                        String tin               = cols[5].trim();
                        String pagIbigNumber     = cols[6].trim();
                        String position          = cols[7].trim();
                        double hourlyRate        = parseDouble(cols[8]);
                        double basicSalary       = parseDouble(cols[9]);
                        double hoursWorked       = parseDouble(cols[10]);
                        double grossPay          = parseDouble(cols[11]);
                        double totalDeductions   = parseDouble(cols[12]);
                        double netPay            = parseDouble(cols[13]);

                        employees.add(new Employee(
                            employeeID, firstName, lastName,
                            sssNumber, philHealthNumber, tin, pagIbigNumber,
                            position, hourlyRate, basicSalary,
                            hoursWorked, grossPay, totalDeductions, netPay
                        ));

                    } else {
                        System.out.println("Skipping row with too few columns: " + line);
                    }

                } catch (Exception e) {
                    System.out.println("Skipping invalid row: " + line);
                }
            }

        } catch (IOException e) {
            System.out.println("Error reading employee file: " + e.getMessage());
        }

        return employees;
    }

    // --------------------------------------------------------
    // READ ATTENDANCE — total hours per employee
    // --------------------------------------------------------

    /**
     * Reads the attendance CSV and returns a map of
     * employeeID → total hours worked (summed across all records).
     *
     * @param filePath Path to the attendance CSV file
     * @return Map of employeeID to total hours worked
     */
    public static Map<String, Double> readTotalHoursWorked(String filePath) {

        Map<String, Double> hoursMap = new HashMap<>();
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("Attendance file not found: " + filePath);
            return hoursMap;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {

                line = line.trim();
                if (isFirstLine) { isFirstLine = false; continue; }
                if (line.isEmpty()) continue;

                // Attendance columns: Employee #, Last Name, First Name, Date, Log In, Log Out
                String[] cols = parseCSVLine(line);
                if (cols.length < 6) continue;

                try {
                    String employeeID = cols[0].trim();
                    String logInStr   = cols[4].trim();
                    String logOutStr  = cols[5].trim();

                    double hours = calculateHoursBetween(logInStr, logOutStr);

                    hoursMap.put(employeeID,

                        hoursMap.getOrDefault(employeeID, 0.0) + hours);

                } catch (Exception e) {
                    System.out.println("Skipping invalid attendance row: " + line);
                }
            }

        } catch (IOException e) {
            System.out.println("Error reading attendance file: " + e.getMessage());
        }

        return hoursMap;
    }

    /**
     * Counts the number of distinct calendar months (MM/YYYY) present in
     * the attendance CSV for each employee.
     *
     * Why this exists: SSS/PhilHealth/Pag-IBIG/withholding-tax tables in
     * SalaryComputationModule are published as MONTHLY rates. The
     * attendance file spans multiple months of records per employee, so
     * running the full multi-month gross pay through those tables would
     * push every employee into the same maxed-out bracket regardless of
     * salary. PayrollService uses this count to convert cumulative hours
     * into an average MONTHLY gross pay before computing deductions, then
     * scales the result back up to match the cumulative total.
     *
     * @param filePath Path to the attendance CSV file
     * @return Map of employeeID to number of distinct months with at least one record
     */
    public static Map<String, Integer> countMonthsWorked(String filePath) {

        Map<String, java.util.Set<String>> monthsByEmployee = new HashMap<>();
        File file = new File(filePath);

        if (!file.exists()) {
            return new HashMap<>();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {

                line = line.trim();
                if (isFirstLine) { isFirstLine = false; continue; }
                if (line.isEmpty()) continue;

                String[] cols = line.split(",", -1);
                if (cols.length < 6) continue;

                try {
                    String employeeID = cols[0].trim();
                    String dateStr    = cols[3].trim(); // MM/DD/YYYY

                    if (dateStr.length() < 10) continue;

                    String monthKey = dateStr.substring(0, 2) + "/" + dateStr.substring(6);

                    monthsByEmployee
                        .computeIfAbsent(employeeID, k -> new java.util.HashSet<>())
                        .add(monthKey);

                } catch (Exception e) {
                    System.out.println("Skipping invalid attendance row while counting months: " + line);
                }
            }

        } catch (IOException e) {
            System.out.println("Error reading attendance file: " + e.getMessage());
        }

        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, java.util.Set<String>> entry : monthsByEmployee.entrySet()) {
            result.put(entry.getKey(), entry.getValue().size());
        }
        return result;
    }

    // --------------------------------------------------------
    // READ ATTENDANCE — records for one specific employee
    // --------------------------------------------------------

    /**
     * Reads attendance records for a single employee and returns
     * them as a list of String arrays for display in the GUI.
     * Each array contains: [date, logIn, logOut, hoursFormatted]
     *
     * @param filePath   Path to the attendance CSV file
     * @param employeeID ID of the employee to filter by
     * @return List of attendance rows as String arrays
     */
    public static List<String[]> readAttendanceForEmployee(String filePath, String employeeID) {

        List<String[]> records = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists()) return records;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {

                line = line.trim();
                if (isFirstLine) { isFirstLine = false; continue; }
                if (line.isEmpty()) continue;

                String[] cols = parseCSVLine(line);
                if (cols.length < 6) continue;

                if (!cols[0].trim().equals(employeeID)) continue;

                try {
                    String date      = cols[3].trim();
                    String logInStr  = cols[4].trim();
                    String logOutStr = cols[5].trim();

                    double hours = calculateHoursBetween(logInStr, logOutStr);

                    records.add(new String[]{
                        date,
                        logInStr,
                        logOutStr,
                        String.format("%.2f", hours)
                    });

                } catch (Exception e) {
                    System.out.println("Skipping invalid attendance row: " + line);
                }
            }

        } catch (IOException e) {
            System.out.println("Error reading attendance file: " + e.getMessage());
        }

        return records;
    }

    // --------------------------------------------------------
    // WRITE — overwrite entire file (update / delete)
    // --------------------------------------------------------

    /**
     * Overwrites the CSV file with the full current employee list.
     * Always writes in our 14-column working format.
     *
     * BUG FIX: now returns boolean instead of void, so callers (e.g.
     * PayrollService) can tell whether the write actually succeeded
     * before reporting "successfully" to the user.
     *
     * @param filePath  Path to the CSV file
     * @param employees Current list of all employees
     * @return true if the file was written successfully, false otherwise
     */
    public static boolean writeEmployees(String filePath, List<Employee> employees) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {

            writer.write(CSV_HEADER);
            writer.newLine();

            for (Employee emp : employees) {
                writer.write(toCSVLine(emp));
                writer.newLine();
            }

            return true;

        } catch (IOException e) {
            System.out.println("Error writing employee file: " + e.getMessage());
            return false;
        }
    }

    // --------------------------------------------------------
    // APPEND — add one new record (add employee)
    // --------------------------------------------------------

    /**
     * Appends a single new employee record to the CSV.
     * Creates the file with headers if it does not yet exist.
     *
     * BUG FIX: now returns boolean instead of void, for the same
     * reason as writeEmployees() above.
     *
     * @param filePath Path to the CSV file
     * @param emp      The new Employee to append
     * @return true if the record was appended successfully, false otherwise
     */
    public static boolean appendEmployee(String filePath, Employee emp) {

        File file = new File(filePath);
        boolean needsHeader  = !file.exists() || file.length() == 0;
        boolean needsNewline = needsNewlineBeforeAppend(file);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {

            if (needsNewline) {
                writer.newLine();
            }

            if (needsHeader) {
                writer.write(CSV_HEADER);
                writer.newLine();
            }

            writer.write(toCSVLine(emp));
            writer.newLine();

            return true;

        } catch (IOException e) {
            System.out.println("Error appending to employee file: " + e.getMessage());
            return false;
        }
    }

    // --------------------------------------------------------
    // APPEND — add one new attendance record
    // --------------------------------------------------------

    /**
     * Appends a single new attendance record to the Attendance CSV.
     * Creates the file with headers if it does not yet exist.
     *
     * Uses the same 6-column format already read by readTotalHoursWorked()
     * and readAttendanceForEmployee(): Employee #, Last Name, First Name,
     * Date, Log In, Log Out.
     *
     * @return true if the record was appended successfully, false otherwise
     */
    public static boolean appendAttendance(String filePath, String employeeID,
            String lastName, String firstName, String date,
            String logIn, String logOut) {

        File file = new File(filePath);
        boolean needsHeader  = !file.exists() || file.length() == 0;
        boolean needsNewline = needsNewlineBeforeAppend(file);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {

            if (needsNewline) {
                writer.newLine();
            }

            if (needsHeader) {
                writer.write("Employee #,Last Name,First Name,Date,Log In,Log Out");
                writer.newLine();
            }

            writer.write(employeeID + "," + lastName + "," + firstName + ","
                    + date + "," + logIn + "," + logOut);
            writer.newLine();

            return true;

        } catch (IOException e) {
            System.out.println("Error appending to attendance file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks whether the file's last byte is NOT a newline.
     *
     * BUG FIX: FileWriter in append mode writes immediately from
     * wherever the file currently ends. If the file (e.g. one originally
     * exported from Excel/Sheets) doesn't already end with a newline,
     * the next appended row gets silently glued onto the previous line
     * instead of starting fresh — corrupting both records with no error.
     */
    private static boolean needsNewlineBeforeAppend(File file) {
        if (!file.exists() || file.length() == 0) {
            return false;
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(file.length() - 1);
            int lastByte = raf.read();
            return lastByte != '\n' && lastByte != '\r';
        } catch (IOException e) {
            return false;
        }
    }

    // --------------------------------------------------------
    // PRIVATE HELPERS
    // --------------------------------------------------------

    /**
     * Computes hours worked between a log-in and log-out time string.
     * Shared by readTotalHoursWorked() and readAttendanceForEmployee()
     * so this parsing logic isn't duplicated in both places.
     *
     * @param logInStr  Log-in time string (e.g. "8:59")
     * @param logOutStr Log-out time string (e.g. "18:31")
     * @return Hours worked, never negative
     */
    private static double calculateHoursBetween(String logInStr, String logOutStr) {
        LocalTime logIn  = LocalTime.parse(logInStr,  TIME_FORMAT);
        LocalTime logOut = LocalTime.parse(logOutStr, TIME_FORMAT);
        double hours = (logOut.toSecondOfDay() - logIn.toSecondOfDay()) / 3600.0;
        return Math.max(hours, 0);
    }

    /**
     * Parses a CSV line into fields, correctly handling values
     * that are wrapped in double quotes (e.g. addresses, "90,000").
     */
    private static String[] parseCSVLine(String line) {

        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                insideQuotes = !insideQuotes;
            } else if (c == ',' && !insideQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    /**
     * Converts an Employee to a 14-column CSV line.
     * Wraps fields containing commas in double quotes.
     */
    private static String toCSVLine(Employee emp) {
        return emp.getEmployeeID()                             + ","
             + quoteIfNeeded(emp.getLastName())                + ","
             + quoteIfNeeded(emp.getFirstName())               + ","
             + emp.getSssNumber()                              + ","
             + emp.getPhilHealthNumber()                       + ","
             + emp.getTin()                                    + ","
             + emp.getPagIbigNumber()                          + ","
             + quoteIfNeeded(emp.getPosition())                + ","
             + String.format("%.2f", emp.getHourlyRate())      + ","
             + String.format("%.2f", emp.getBasicSalary())     + ","
             + String.format("%.2f", emp.getHoursWorked())     + ","
             + String.format("%.2f", emp.getGrossPay())        + ","
             + String.format("%.2f", emp.getTotalDeductions()) + ","
             + String.format("%.2f", emp.getNetPay());
    }

    private static String quoteIfNeeded(String value) {
        if (value != null && value.contains(",")) {
            return "\"" + value + "\"";
        }
        return value != null ? value : "";
    }

    // Tracks how many numeric values failed to parse during the most
    // recent readEmployees() call, so PayrollService can warn the user
    // instead of silently treating corrupted data as zero.
    private static int lastParseWarningCount = 0;

    /** @return number of values that failed numeric parsing in the last readEmployees() call */
    public static int getLastParseWarningCount() {
        return lastParseWarningCount;
    }

    // Parses a plain numeric string (e.g. "535.71")
    private static double parseDouble(String value) {
        try {
            String cleaned = value.trim();
            return cleaned.isEmpty() ? 0.0 : Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            lastParseWarningCount++;
            System.out.println("Warning: could not parse number \"" + value + "\" — defaulted to 0.0");
            return 0.0;
        }
    }

    // Parses a formatted numeric string (e.g. "90,000" from original CSV)
    private static double parseFormattedDouble(String value) {
        try {
            String cleaned = value.trim().replace(",", "");
            return cleaned.isEmpty() ? 0.0 : Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            lastParseWarningCount++;
            System.out.println("Warning: could not parse number \"" + value + "\" — defaulted to 0.0");
            return 0.0;
        }
    }
}