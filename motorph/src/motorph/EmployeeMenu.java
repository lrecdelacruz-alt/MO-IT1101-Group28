package motorph;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

// EMPLOYEE MENU CLASS
// Provides the GUI window for all employee management (CRUD).
//
// Responsibilities:
//   - Display a JTable of all employees
//   - Allow searching/filtering employees by ID or name
//   - Allow adding, updating, deleting, and viewing employees
//   - Allow viewing attendance records per employee
//   - Validate all inputs and show user-friendly error messages
//   - Sync every change back to PayrollService

public class EmployeeMenu {

    // GUI COMPONENTS

    private JFrame            frame;
    private JTable            employeeTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField        searchField;
    private JButton           addButton, updateButton, deleteButton,
                              viewButton, attendanceButton;
    private JTextArea         outputArea;

    // SERVICE REFERENCE

    private PayrollService payrollService;

    // CONSTRUCTOR

    public EmployeeMenu(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    // MAIN DISPLAY METHOD

    /**
     * Builds and displays the Employee Management window.
     * Sets up the search bar, JTable, buttons, output area,
     * and all event listeners.
     */
    public void displayMenu() {

        frame = new JFrame("MotorPH — Employee Management");
        frame.setSize(900, 580);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        // --- Title ---
        JLabel titleLabel = new JLabel("Employee Management", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));

        // --- Search Bar ---
        // Filters the table in real time as the user types an ID or name.
        // We chose to search across ID, first name, and last name columns
        // so users aren't forced to remember exact IDs.
        JLabel searchLabel = new JLabel("Search:");
        searchField = new JTextField(20);
        searchField.setToolTipText("Search by Employee ID or Name");

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);

        // --- North Panel: title + search bar stacked ---
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(titleLabel,   BorderLayout.NORTH);
        northPanel.add(searchPanel,  BorderLayout.SOUTH);

        // --- Table ---
        String[] columns = {
            "Employee ID", "First Name", "Last Name", "Position",
            "Basic Salary", "Hourly Rate"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            // Cells are non-editable; all edits go through buttons only
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        employeeTable = new JTable(tableModel);
        employeeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        employeeTable.getTableHeader().setReorderingAllowed(false);

        // Attach a sorter so we can filter rows using the search field
        sorter = new TableRowSorter<>(tableModel);
        employeeTable.setRowSorter(sorter);

        refreshTable();

        JScrollPane tableScroll = new JScrollPane(employeeTable);

        // --- Search Field Listener ---
        // Filters rows in real time as the user types.
        // Searches Employee ID (col 0), First Name (col 1), Last Name (col 2).
        searchField.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            }
        );

        // --- Buttons ---
        addButton        = new JButton("Add Employee");
        updateButton     = new JButton("Update Employee");
        deleteButton     = new JButton("Delete Employee");
        viewButton       = new JButton("View Details");
        attendanceButton = new JButton("View Attendance");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(attendanceButton);

        // --- Output Area ---
        outputArea = new JTextArea(6, 40);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setBorder(BorderFactory.createTitledBorder("Details / Status"));
        JScrollPane outputScroll = new JScrollPane(outputArea);

        // --- Layout Assembly ---
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        centerPanel.add(tableScroll,  BorderLayout.CENTER);
        centerPanel.add(buttonPanel,  BorderLayout.SOUTH);

        frame.add(northPanel,   BorderLayout.NORTH);
        frame.add(centerPanel,  BorderLayout.CENTER);
        frame.add(outputScroll, BorderLayout.SOUTH);

        // --- Button Event Listeners ---
        addButton.addActionListener(e        -> addEmployee());
        updateButton.addActionListener(e     -> updateEmployee());
        deleteButton.addActionListener(e     -> deleteEmployee());
        viewButton.addActionListener(e       -> viewEmployee());
        attendanceButton.addActionListener(e -> viewAttendance());

        frame.setVisible(true);
    }

    // HELPER — Search Filter

    /**
     * Filters the employee table rows based on the current text
     * in the search field. Matches against Employee ID, First Name,
     * and Last Name columns so users can search either way.
     * Clears the filter if the search field is empty.
     */
    private void applyFilter() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null); // show all rows
        } else {
            // RowFilter.regexFilter searches the given columns (0, 1, 2)
            // case-insensitively using the typed text as a pattern
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1, 2));
        }
    }

    // HELPER — Table Refresh

    /**
     * Clears and repopulates the JTable from the current employee list.
     * Called after any add or delete operation to keep the table in sync.
     */
    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Employee emp : payrollService.getEmployees()) {
            tableModel.addRow(new Object[]{
                emp.getEmployeeID(),
                emp.getFirstName(),
                emp.getLastName(),
                emp.getPosition(),
                String.format("%.2f", emp.getBasicSalary()),
                String.format("%.2f", emp.getHourlyRate())
            });
        }
    }

    // CRUD OPERATIONS

    public void addEmployee() {

        JTextField idField         = new JTextField();
        JTextField firstNameField  = new JTextField();
        JTextField lastNameField   = new JTextField();
        JTextField positionField   = new JTextField();
        JTextField salaryField     = new JTextField();
        JTextField hourlyRateField = new JTextField();

        Object[] fields = {
            "Employee ID:",   idField,
            "First Name:",    firstNameField,
            "Last Name:",     lastNameField,
            "Position:",      positionField,
            "Basic Salary:",  salaryField,
            "Hourly Rate:",   hourlyRateField
        };

        int option = JOptionPane.showConfirmDialog(
                frame, fields, "Add New Employee", JOptionPane.OK_CANCEL_OPTION);

        if (option != JOptionPane.OK_OPTION) return;

        // --- Empty Field Validation ---
        String id         = idField.getText().trim();
        String firstName  = firstNameField.getText().trim();
        String lastName   = lastNameField.getText().trim();
        String position   = positionField.getText().trim();
        String salaryText = salaryField.getText().trim();
        String rateText   = hourlyRateField.getText().trim();

        if (id.isEmpty() || firstName.isEmpty() || lastName.isEmpty()
                || position.isEmpty() || salaryText.isEmpty() || rateText.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "All fields are required. Please complete the form.",
                    "Incomplete Fields", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- Duplicate ID Check ---
        if (payrollService.findEmployee(id) != null) {
            JOptionPane.showMessageDialog(frame,
                    "Employee ID \"" + id + "\" already exists.\n"
                    + "Please use a unique Employee ID.",
                    "Duplicate ID", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- Numeric Validation ---
        double salary, hourlyRate;
        try {
            salary     = Double.parseDouble(salaryText);
            hourlyRate = Double.parseDouble(rateText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Basic Salary and Hourly Rate must be valid numbers.\n"
                    + "Example: 40000.00 or 200.50",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- Positive Value Validation ---
        if (salary <= 0 || hourlyRate <= 0) {
            JOptionPane.showMessageDialog(frame,
                    "Basic Salary and Hourly Rate must be greater than zero.",
                    "Invalid Value", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- Add to System ---
        Employee newEmp = new Employee(id, firstName, lastName, position, hourlyRate, salary);
        payrollService.addEmployee(newEmp);
        refreshTable();
        outputArea.setText("Employee added successfully.\n" + newEmp.toString());
    }

    /**
     * Updates the first name, last name, and position of the
     * currently selected employee. Employee ID and salary are
     * not editable to prevent data corruption.
     */
    public void updateEmployee() {

        int selectedRow = employeeTable.getSelectedRow();

        if (selectedRow == -1) {
            outputArea.setText("No employee selected.\nPlease click a row in the table first.");
            return;
        }

        // Convert view row index to model row index (needed when filter is active)
        int modelRow = employeeTable.convertRowIndexToModel(selectedRow);

        String currentFirst    = (String) tableModel.getValueAt(modelRow, 1);
        String currentLast     = (String) tableModel.getValueAt(modelRow, 2);
        String currentPosition = (String) tableModel.getValueAt(modelRow, 3);

        String firstName = JOptionPane.showInputDialog(frame, "First Name:", currentFirst);
        if (firstName == null) return;
        String lastName  = JOptionPane.showInputDialog(frame, "Last Name:",  currentLast);
        if (lastName == null)  return;
        String position  = JOptionPane.showInputDialog(frame, "Position:",   currentPosition);
        if (position == null)  return;

        // --- Empty Field Validation ---
        if (firstName.trim().isEmpty() || lastName.trim().isEmpty() || position.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Fields cannot be empty. Update cancelled.",
                    "Incomplete Fields", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // --- Apply Update ---
        String employeeID = (String) tableModel.getValueAt(modelRow, 0);
        payrollService.updateEmployee(employeeID, firstName.trim(), lastName.trim(), position.trim());

        tableModel.setValueAt(firstName.trim(), modelRow, 1);
        tableModel.setValueAt(lastName.trim(),  modelRow, 2);
        tableModel.setValueAt(position.trim(),  modelRow, 3);

        outputArea.setText("Employee updated successfully.\n"
                + "ID: " + employeeID + " | Name: "
                + firstName.trim() + " " + lastName.trim());
    }

    /**
     * Deletes the selected employee after prompting for confirmation.
     * No deletion occurs if the user cancels the confirmation dialog.
     */
    public void deleteEmployee() {

        int selectedRow = employeeTable.getSelectedRow();

        if (selectedRow == -1) {
            outputArea.setText("No employee selected.\nPlease click a row in the table first.");
            return;
        }

        // Convert view row index to model row index (needed when filter is active)
        int modelRow = employeeTable.convertRowIndexToModel(selectedRow);

        String employeeID = (String) tableModel.getValueAt(modelRow, 0);
        String fullName   = tableModel.getValueAt(modelRow, 1)
                          + " " + tableModel.getValueAt(modelRow, 2);

        // Confirmation required before deletion
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Are you sure you want to delete:\n"
                + employeeID + " — " + fullName + "?\n\nThis action cannot be undone.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        payrollService.deleteEmployee(employeeID);
        tableModel.removeRow(modelRow);
        outputArea.setText("Employee \"" + fullName + "\" (" + employeeID + ") deleted successfully.");
    }

    /**
     * Displays the selected employee's full details in the output area.
     */
    public void viewEmployee() {

        int selectedRow = employeeTable.getSelectedRow();

        if (selectedRow == -1) {
            outputArea.setText("No employee selected.\nPlease click a row in the table first.");
            return;
        }

        // Convert view row index to model row index (needed when filter is active)
        int modelRow = employeeTable.convertRowIndexToModel(selectedRow);

        String info =
              "Employee ID  : " + tableModel.getValueAt(modelRow, 0)
            + "\nFirst Name   : " + tableModel.getValueAt(modelRow, 1)
            + "\nLast Name    : " + tableModel.getValueAt(modelRow, 2)
            + "\nPosition     : " + tableModel.getValueAt(modelRow, 3)
            + "\nBasic Salary : PHP " + tableModel.getValueAt(modelRow, 4)
            + "\nHourly Rate  : PHP " + tableModel.getValueAt(modelRow, 5);

        outputArea.setText(info);
    }

    /**
     * Displays a list of all employees in the output area.
     * Useful for a quick overview without selecting individual rows.
     */
    public void viewEmployees() {
        StringBuilder sb = new StringBuilder("=== All Employees ===\n");
        for (Employee emp : payrollService.getEmployees()) {
            sb.append(emp.toString()).append("\n");
        }
        if (payrollService.getEmployees().isEmpty()) {
            sb.append("No employees found.");
        }
        outputArea.setText(sb.toString());
    }

    /**
     * Displays attendance records for the selected employee.
     * Shows date, time in, time out, and hours worked per record.
     * If no records exist for that employee, a message is shown instead.
     */
    public void viewAttendance() {

        int selectedRow = employeeTable.getSelectedRow();

        if (selectedRow == -1) {
            outputArea.setText("No employee selected.\nPlease click a row in the table first.");
            return;
        }

        // Convert view row index to model row index (needed when filter is active)
        int modelRow = employeeTable.convertRowIndexToModel(selectedRow);

        String employeeID = (String) tableModel.getValueAt(modelRow, 0);
        String fullName   = tableModel.getValueAt(modelRow, 1)
                          + " " + tableModel.getValueAt(modelRow, 2);

        StringBuilder sb    = new StringBuilder();
        boolean       found = false;

        sb.append("Attendance Records — ").append(fullName).append("\n");
        sb.append("--------------------------------\n");

        for (Attendance att : payrollService.getAttendanceList()) {
            if (att.getEmployeeID().equals(employeeID)) {
                sb.append("Date       : ").append(att.getDate()).append("\n");
                sb.append("Time In    : ").append(att.getTimeIn()).append("\n");
                sb.append("Time Out   : ").append(att.getTimeOut()).append("\n");
                sb.append("Hours      : ").append(
                    String.format("%.2f", att.calculateHoursWorked())).append("\n");
                sb.append("--------------------------------\n");
                found = true;
            }
        }

        if (!found) {
            sb.append("No attendance records found for this employee.");
        }

        outputArea.setText(sb.toString());
    }
}