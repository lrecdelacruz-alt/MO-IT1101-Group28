package motorph;

import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ============================================================
// CSV HANDLER
// Centralizes reading and writing of employee and attendance CSV files.
// ============================================================

public class CSVHandler {

    private static final String CSV_HEADER =
        "Employee #,Last Name,First Name,SSS #,PhilHealth #,"
        + "TIN #,Pag-IBIG #,Position,Hourly Rate,Basic Salary,"
        + "Hours Worked,Gross Pay,Total Deductions,Net Pay";

    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("H:mm");

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

                line = line.trim();

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
     * Counts the number of distinct calendar months (MM/YYYY)
     * represented in the attendance records for each employee.
     *
     * This information is used when computing payroll deductions.
     * Since government deductions are based on monthly salary,
     * PayrollService derives an average monthly gross pay from
     * cumulative attendance records before applying deduction tables.
     *
     * @param filePath Path to the attendance CSV file
     * @return Map of employeeID to the number of months worked
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

                String[] cols = parseCSVLine(line);
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
     * Overwrites the employee CSV file with the current list of employees.
     * Data is always written using the 14-column working format.
     *
     * Returning a boolean allows the caller to determine whether the
     * write operation completed successfully.
     *
     * @param filePath Path to the CSV file
     * @param employees Current list of employees to write
     * @return true if the file was written successfully; otherwise false
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
     * Appends a single employee record to the CSV file.
     * If the file does not exist or is empty, the header row is written first.
     *
     * Returning a boolean allows the caller to verify whether the
     * append operation completed successfully.
     *
     * @param filePath Path to the CSV file
     * @param emp Employee to append
     * @return true if the record was appended successfully; otherwise false
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
     * Appends a single attendance record to the attendance CSV file.
     * If the file does not exist or is empty, the header row is written first.
     *
     * The record follows the same six-column format used by the
     * attendance-reading methods:
     * Employee #, Last Name, First Name, Date, Log In, Log Out.
     *
     * @return true if the record was appended successfully; otherwise false
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
     * Updates a specific attendance record, identified by the unique
     * (employeeID, original date) pair — matching the same uniqueness
     * rule already enforced when adding attendance (one record per
     * employee per date). Rewrites the entire file with the matching
     * row replaced by the new values.
     *
     * @return true if a matching row was found and the file was
     *         rewritten successfully, false otherwise
     */
    public static boolean updateAttendance(String filePath, String employeeID,
            String originalDate, String newDate, String newLogIn, String newLogOut) {

        File file = new File(filePath);
        if (!file.exists()) return false;

        List<String[]> allRows = new ArrayList<>();
        String header = null;
        boolean found = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (isFirstLine) {
                    header = line;
                    isFirstLine = false;
                    continue;
                }
                if (line.isEmpty()) continue;

                String[] cols = parseCSVLine(line);
                if (cols.length < 6) continue;

                if (cols[0].trim().equals(employeeID) && cols[3].trim().equals(originalDate)) {
                    allRows.add(new String[]{
                        cols[0].trim(), cols[1].trim(), cols[2].trim(),
                        newDate, newLogIn, newLogOut
                    });
                    found = true;
                } else {
                    allRows.add(cols);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading attendance file for update: " + e.getMessage());
            return false;
        }

        if (!found) return false;

        return rewriteAttendanceFile(filePath, header, allRows);
    }

    /**
     * Deletes a specific attendance record, identified by the unique
     * (employeeID, date) pair. Rewrites the entire file without that row.
     *
     * @return true if a matching row was found and removed, false otherwise
     */
    public static boolean deleteAttendance(String filePath, String employeeID, String date) {

        File file = new File(filePath);
        if (!file.exists()) return false;

        List<String[]> remainingRows = new ArrayList<>();
        String header = null;
        boolean found = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (isFirstLine) {
                    header = line;
                    isFirstLine = false;
                    continue;
                }
                if (line.isEmpty()) continue;

                String[] cols = parseCSVLine(line);
                if (cols.length < 6) continue;

                if (cols[0].trim().equals(employeeID) && cols[3].trim().equals(date)) {
                    found = true;
                } else {
                    remainingRows.add(cols);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading attendance file for delete: " + e.getMessage());
            return false;
        }

        if (!found) return false;

        return rewriteAttendanceFile(filePath, header, remainingRows);
    }

    /**
     * Rewrites the attendance CSV using the supplied header and records.
     *
     * Each field is processed using quoteIfNeeded() so values containing
     * commas are enclosed in quotation marks and remain valid CSV fields.
     */
    private static boolean rewriteAttendanceFile(String filePath, String header, List<String[]> rows) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
            writer.write(header != null ? header : "Employee #,Last Name,First Name,Date,Log In,Log Out");
            writer.newLine();
            for (String[] row : rows) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) line.append(",");
                    line.append(quoteIfNeeded(row[i]));
                }
                writer.write(line.toString());
                writer.newLine();
            }
            return true;
        } catch (IOException e) {
            System.out.println("Error writing attendance file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Determines whether a newline should be written before
     * appending another record to the file.
     *
     * This prevents a newly appended record from being placed
     * on the same line as the previous record when the existing
     * file does not end with a newline character.
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

    /**
     * Wraps a value in double quotes if it contains commas so it
     * remains a valid CSV field.
     */
    private static String quoteIfNeeded(String value) {
        if (value != null && value.contains(",")) {
            return "\"" + value + "\"";
        }
        return value != null ? value : "";
    }

    // Stores the number of numeric parsing failures encountered during
    // the most recent call to readEmployees().
    private static int lastParseWarningCount = 0;

    /**
     * Returns the number of numeric parsing failures encountered
     * during the most recent call to readEmployees().
     *
     * @return parsing warning count
     */
    public static int getLastParseWarningCount() {
        return lastParseWarningCount;
    }

    /**
     * Converts a string into a double value.
     * Returns 0.0 if the value is empty or cannot be parsed,
     * while recording the parsing failure.
     */
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

    /**
     * Converts a formatted numeric string into a double.
     * Formatting characters such as commas are removed
     * before parsing.
     */
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
