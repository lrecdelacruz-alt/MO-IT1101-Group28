package motorph;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

// ============================================================
// EMPLOYEE MENU CLASS
// Provides the GUI window for all employee management (CRUD).
//
// Responsibilities:
//   - Display employees in a JTable with MS2 required columns
//   - Real-time search/filter by ID or name
//   - Add, update, delete, view employees (all synced to CSV)
//   - View real attendance records from the MotorPH Attendance CSV
//   - Validate all inputs and show user-friendly messages
//   - Stay in sync with data changes from other windows (Observer pattern)
//
// Table columns (MS2):
//   Employee #, First Name, Last Name, Position,
//   Hourly Rate, Hours Worked, Gross Pay, Net Pay
// ============================================================

public class EmployeeMenu implements DataChangeListener {

    // GUI components
    private JFrame            frame;
    private JTable            employeeTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField        searchField;
    private JButton           addButton, updateButton, deleteButton,
                              viewButton, attendanceButton, addAttendanceButton;
    private JTextArea         outputArea;
    private JLabel            statusBanner;

    private PayrollService payrollService;

    private Runnable onCloseCallback;

    /** Registers a callback to run when this window is closed (used by Main for the singleton guard). */
    public void onCloseClearReference(Runnable callback) {
        this.onCloseCallback = callback;
    }

    /** Brings this window to the front and focuses it if it's already open. */
    public void bringToFront() {
        if (frame != null) {
            frame.setState(Frame.NORMAL);
            frame.toFront();
            frame.requestFocus();
        }
    }

    public EmployeeMenu(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    // --------------------------------------------------------
    // MAIN DISPLAY METHOD
    // --------------------------------------------------------

    /**
     * Builds and displays the Employee Management window.
     * Add button is green; Delete button is red — following
     * standard UI conventions for constructive vs destructive actions.
     */
    public void displayMenu() {

        frame = new JFrame("MotorPH — Employee Management");
        frame.setIconImage(AppIcon.create());
        frame.setSize(1150, 580);
        frame.setMinimumSize(new Dimension(950, 480));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        // Register with PayrollService so this window refreshes itself
        // whenever employee data changes anywhere else in the app
        // (e.g. salaries computed from the Payroll Menu window).
        payrollService.addDataChangeListener(this);

        // Stop listening once this window is closed — avoids a disposed
        // window being notified, and avoids a memory leak.
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                payrollService.removeDataChangeListener(EmployeeMenu.this);
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                }
            }
        });

        // Title
        JLabel titleLabel = new JLabel("Employee Management", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));

        // Search bar — filters table in real time by ID, first name, or last name
        JLabel searchLabel = new JLabel("Search:");
        searchField = new JTextField(20);
        searchField.setPreferredSize(new Dimension(220, 30));
        searchField.setToolTipText("Search by Employee # or Name");

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);

        // Status banner — shows a hint when salaries haven't been computed
        // yet for one or more employees. Kept blank otherwise. Updated
        // automatically inside refreshTable().
        statusBanner = new JLabel(" ", SwingConstants.CENTER);
        statusBanner.setFont(new Font("Arial", Font.PLAIN, 12));
        statusBanner.setForeground(new Color(150, 100, 0));
        statusBanner.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        JPanel titleAndBannerPanel = new JPanel(new BorderLayout());
        titleAndBannerPanel.add(titleLabel,   BorderLayout.NORTH);
        titleAndBannerPanel.add(statusBanner, BorderLayout.SOUTH);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(titleAndBannerPanel, BorderLayout.NORTH);
        northPanel.add(searchPanel,         BorderLayout.SOUTH);

        // Table — MS2 columns include computed payroll fields
        String[] columns = {
            "Employee #", "First Name", "Last Name", "Position",
            "Hourly Rate", "Hours Worked", "Gross Pay", "Total Deductions", "Net Pay"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // all edits go through buttons only
            }
        };
        employeeTable = new JTable(tableModel);
        employeeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        employeeTable.getTableHeader().setReorderingAllowed(false);

        sorter = new TableRowSorter<>(tableModel);
        employeeTable.setRowSorter(sorter);

        // Column widths tuned per content: ID/numbers stay narrow,
        // Position gets more room since job titles run longer.
        employeeTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // Employee #
        employeeTable.getColumnModel().getColumn(1).setPreferredWidth(100); // First Name
        employeeTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Last Name
        employeeTable.getColumnModel().getColumn(3).setPreferredWidth(160); // Position
        employeeTable.getColumnModel().getColumn(4).setPreferredWidth(90);  // Hourly Rate
        employeeTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Hours Worked
        employeeTable.getColumnModel().getColumn(6).setPreferredWidth(100); // Gross Pay
        employeeTable.getColumnModel().getColumn(7).setPreferredWidth(110); // Total Deductions
        employeeTable.getColumnModel().getColumn(8).setPreferredWidth(100); // Net Pay

        refreshTable();

        JScrollPane tableScroll = new JScrollPane(employeeTable);

        // Search listener — filters on every keystroke
        searchField.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            }
        );

        // Buttons
        addButton          = new JButton("Add Employee");
        updateButton       = new JButton("Update Employee");
        deleteButton       = new JButton("Delete Employee");
        viewButton         = new JButton("View Details");
        attendanceButton   = new JButton("View Attendance");
        addAttendanceButton = new JButton("Add Attendance");

        Dimension buttonSize = new Dimension(170, 32);
        JButton[] toolbarButtons = { addButton, updateButton, deleteButton, viewButton, attendanceButton, addAttendanceButton };
        for (JButton b : toolbarButtons) {
            b.setPreferredSize(buttonSize);
            b.setFont(new Font("Arial", Font.PLAIN, 13));
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setContentAreaFilled(true);
            b.setOpaque(true);
        }

        // Color coding by what each action DOES:
        //   green = creates a new record (Add Employee, Add Attendance)
        //   amber = modifies an existing record (Update)
        //   red   = removes a record (Delete)
        //   gray  = read-only / informational (View, View Attendance)
        // Every button gets a raised bevel border shaded from its own base
        // color (.brighter() / .darker()) for a subtle 3D, pressable look.
        Color addColor    = new Color(46, 125, 50);   // green
        Color updateColor = new Color(47, 87, 138); // blue
        Color deleteColor = new Color(183, 28, 28);   // red
        Color neutralGray = new Color(190, 195, 200); // gray

        addButton.setBackground(addColor);
        addButton.setForeground(Color.WHITE);
        addButton.setBorderPainted(true);
        addButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, addColor.brighter(), addColor.darker()));

        addAttendanceButton.setBackground(addColor);
        addAttendanceButton.setForeground(Color.WHITE);
        addAttendanceButton.setBorderPainted(true);
        addAttendanceButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, addColor.brighter(), addColor.darker()));

        updateButton.setBackground(updateColor);
        updateButton.setForeground(Color.WHITE);
        updateButton.setBorderPainted(true);
        updateButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, updateColor.brighter(), updateColor.darker()));

        deleteButton.setBackground(deleteColor);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setBorderPainted(true);
        deleteButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, deleteColor.brighter(), deleteColor.darker()));

        viewButton.setBackground(neutralGray);
        viewButton.setForeground(Color.BLACK);
        viewButton.setBorderPainted(true);
        viewButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, neutralGray.brighter(), neutralGray.darker()));

        attendanceButton.setBackground(neutralGray);
        attendanceButton.setForeground(Color.BLACK);
        attendanceButton.setBorderPainted(true);
        attendanceButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, neutralGray.brighter(), neutralGray.darker()));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(attendanceButton);
        buttonPanel.add(addAttendanceButton);

        // Output area — shows details and status messages
        outputArea = new JTextArea(6, 40);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setBorder(BorderFactory.createTitledBorder("Details / Status"));
        JScrollPane outputScroll = new JScrollPane(outputArea);

        // Layout
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        centerPanel.add(tableScroll,  BorderLayout.CENTER);
        centerPanel.add(buttonPanel,  BorderLayout.SOUTH);

        frame.add(northPanel,   BorderLayout.NORTH);
        frame.add(centerPanel,  BorderLayout.CENTER);
        frame.add(outputScroll, BorderLayout.SOUTH);

        // Event listeners
        addButton.addActionListener(e        -> addEmployee());
        updateButton.addActionListener(e     -> updateEmployee());
        deleteButton.addActionListener(e     -> deleteEmployee());
        viewButton.addActionListener(e       -> viewEmployee());
        attendanceButton.addActionListener(e -> viewAttendance());
        addAttendanceButton.addActionListener(e -> addAttendance());

        frame.setVisible(true);
    }

    // --------------------------------------------------------
    // OBSERVER PATTERN CALLBACK
    // --------------------------------------------------------

    /**
     * Called automatically by PayrollService whenever employee data
     * changes — including changes made from a different window
     * (e.g. PayrollMenu's "Compute Salaries"). Simply re-renders the
     * table from the current data.
     */
    @Override
    public void onDataChanged() {
        refreshTable();
    }

    // --------------------------------------------------------
    // HELPERS
    // --------------------------------------------------------

    /**
     * Filters table rows by the search field text.
     * Searches columns 0 (Employee #), 1 (First Name), 2 (Last Name).
     *
     * BUG FIX: the search text is now treated as a literal string via
     * Pattern.quote() instead of being passed straight in as regex.
     * Previously, typing an unbalanced character like "(" threw a
     * PatternSyntaxException and broke the filter.
     */
    private void applyFilter() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        try {
            sorter.setRowFilter(
                RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0, 1, 2));
        } catch (PatternSyntaxException ex) {
            sorter.setRowFilter(null);
        }
    }

    /**
     * Clears and repopulates the table from the current employee list.
     * Called after any add, update, delete, or salary computation —
     * either directly, or automatically via onDataChanged().
     */
    private void refreshTable() {
        tableModel.setRowCount(0);
        boolean anyUncomputed = false;

        for (Employee emp : payrollService.getEmployees()) {
            tableModel.addRow(new Object[]{
                emp.getEmployeeID(),
                emp.getFirstName(),
                emp.getLastName(),
                emp.getPosition(),
                String.format("%.2f", emp.getHourlyRate()),
                String.format("%.2f", emp.getHoursWorked()),
                String.format("%.2f", emp.getGrossPay()),
                String.format("%.2f", emp.getTotalDeductions()),
                String.format("%.2f", emp.getNetPay())
            });
            if (emp.getGrossPay() == 0) {
                anyUncomputed = true;
            }
        }

        // Hint disappears on its own once every employee has a computed
        // salary — this method already runs after every add/update/delete
        // and whenever PayrollMenu's "Compute Salaries" notifies listeners.
        if (anyUncomputed) {
            statusBanner.setText(
                "Some employees have not had salaries computed yet — "
                + "open Payroll Menu and click \"Compute Salaries.\"");
        } else {
            statusBanner.setText(" ");
        }
    }

    /**
     * Validates a government ID field (SSS/PhilHealth/TIN/Pag-IBIG) — must
     * contain only digits and dashes. Shows a warning dialog and returns
     * false if invalid, so callers can chain multiple checks with ||.
     * Shared by addEmployee() and updateEmployee() to avoid duplicating
     * the same four checks in both methods.
     */
    private boolean isValidGovId(String value, String fieldLabel) {
        if (!value.matches("[0-9-]+")) {
            JOptionPane.showMessageDialog(frame,
                    fieldLabel + " must contain numbers and dashes only.",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    // --------------------------------------------------------
    // CRUD OPERATIONS
    // --------------------------------------------------------

    /**
     * Opens a form dialog to collect new employee details.
     * Validates all fields, checks for duplicate IDs,
     * then adds the employee and appends to CSV.
     *
     * Table refresh is handled automatically via onDataChanged() —
     * no manual refreshTable() call needed here.
     */
    public void addEmployee() {

        JTextField idField          = new JTextField(20);
        JTextField firstNameField   = new JTextField(20);
        JTextField lastNameField    = new JTextField(20);
        JTextField sssField         = new JTextField(20);
        JTextField philHealthField  = new JTextField(20);
        JTextField tinField         = new JTextField(20);
        JTextField pagIbigField     = new JTextField(20);
        JTextField positionField    = new JTextField(20);
        JTextField salaryField      = new JTextField(20);
        JTextField hourlyRateField  = new JTextField(20);

        Object[] fields = {
            "Employee #:",    idField,
            "First Name:",    firstNameField,
            "Last Name:",     lastNameField,
            "SSS #:",         sssField,
            "PhilHealth #:",  philHealthField,
            "TIN #:",         tinField,
            "Pag-IBIG #:",    pagIbigField,
            "Position:",      positionField,
            "Basic Salary:",  salaryField,
            "Hourly Rate:",   hourlyRateField
        };

        // Auto-focus the first field so users can start typing immediately
        // instead of having to click into the form first.
        SwingUtilities.invokeLater(idField::requestFocusInWindow);

        // Loop instead of a single check: on validation failure, the dialog
        // reopens with the SAME field objects, so anything already typed is
        // preserved. The loop ends only on success or an explicit Cancel.
        while (true) {

            int option = JOptionPane.showConfirmDialog(
                    frame, fields, "Add New Employee", JOptionPane.OK_CANCEL_OPTION);

            if (option != JOptionPane.OK_OPTION) return;

            // Collect and trim all values
            String id         = idField.getText().trim();
            String firstName  = firstNameField.getText().trim();
            String lastName   = lastNameField.getText().trim();
            String sssNum     = sssField.getText().trim();
            String philHealth = philHealthField.getText().trim();
            String tin        = tinField.getText().trim();
            String pagIbig    = pagIbigField.getText().trim();
            String position   = positionField.getText().trim();
            String salaryText = salaryField.getText().trim();
            String rateText   = hourlyRateField.getText().trim();

            // Empty field check
            if (id.isEmpty() || firstName.isEmpty() || lastName.isEmpty()
                    || sssNum.isEmpty() || philHealth.isEmpty()
                    || tin.isEmpty() || pagIbig.isEmpty()
                    || position.isEmpty() || salaryText.isEmpty() || rateText.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "All fields are required. Please complete the form.",
                        "Incomplete Fields", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            // Employee # must be numeric per MPHCR01 ("numeric input field").
            if (!id.matches("\\d+")) {
                JOptionPane.showMessageDialog(frame,
                        "Employee # must contain numbers only (e.g., 10001).",
                        "Invalid Input", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            // Government ID fields must be numbers (dashes allowed, e.g.
            // SSS "44-4506057-3" or TIN "442-605-657-000") — prevents
            // typos/garbage while still accepting real PH ID formats.
            if (!isValidGovId(sssNum, "SSS #") || !isValidGovId(philHealth, "PhilHealth #")
                    || !isValidGovId(tin, "TIN #") || !isValidGovId(pagIbig, "Pag-IBIG #")) {
                continue;
            }

            // Duplicate ID check
            if (payrollService.findEmployee(id) != null) {
                JOptionPane.showMessageDialog(frame,
                        "Employee # \"" + id + "\" already exists.\nPlease use a unique Employee ID.",
                        "Duplicate ID", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            // Numeric validation
            double salary, hourlyRate;
            try {
                salary     = Double.parseDouble(salaryText);
                hourlyRate = Double.parseDouble(rateText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Basic Salary and Hourly Rate must be valid numbers.\nExample: 40000.00 or 200.50",
                        "Invalid Input", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            // Positive value check
            if (salary <= 0 || hourlyRate <= 0) {
                JOptionPane.showMessageDialog(frame,
                        "Basic Salary and Hourly Rate must be greater than zero.",
                        "Invalid Value", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            // Add to system and CSV
            Employee newEmp = new Employee(
                    id, firstName, lastName,
                    sssNum, philHealth, tin, pagIbig,
                    position, hourlyRate, salary);

            // Only report success if the CSV write actually succeeded.
            boolean added = payrollService.addEmployee(newEmp);

            if (added) {
                outputArea.setText("Employee added successfully.\n" + newEmp.toString());
            } else {
                outputArea.setText("Failed to add employee.\n"
                        + "The record could not be saved to the CSV file.\n"
                        + "Please check file permissions and try again.");
            }
            return; // success or save-failure both exit; only validation loops back
        }
    }

    /**
     * Opens a pre-filled form dialog for editing the selected employee.
     * All non-computed, non-ID fields are editable.
     * Saves changes to the Employee object and rewrites the CSV.
     *
     * Table refresh is handled automatically via onDataChanged() —
     * no manual refreshTable() call needed here.
     */
    public void updateEmployee() {

        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            outputArea.setText("No employee selected.\nPlease click a row in the table first.");
            return;
        }

        int modelRow      = employeeTable.convertRowIndexToModel(selectedRow);
        String employeeID = (String) tableModel.getValueAt(modelRow, 0);
        Employee current  = payrollService.findEmployee(employeeID);

        if (current == null) {
            outputArea.setText("Employee not found. Please refresh and try again.");
            return;
        }

        // Pre-fill all editable fields with current values
        JTextField firstNameField  = new JTextField(current.getFirstName());
        JTextField lastNameField   = new JTextField(current.getLastName());
        JTextField sssField        = new JTextField(current.getSssNumber());
        JTextField philHealthField = new JTextField(current.getPhilHealthNumber());
        JTextField tinField        = new JTextField(current.getTin());
        JTextField pagIbigField    = new JTextField(current.getPagIbigNumber());
        JTextField positionField   = new JTextField(current.getPosition());
        JTextField salaryField     = new JTextField(String.valueOf(current.getBasicSalary()));
        JTextField hourlyRateField = new JTextField(String.valueOf(current.getHourlyRate()));

        firstNameField.setColumns(20);
        lastNameField.setColumns(20);
        sssField.setColumns(20);
        philHealthField.setColumns(20);
        tinField.setColumns(20);
        pagIbigField.setColumns(20);
        positionField.setColumns(20);
        salaryField.setColumns(20);
        hourlyRateField.setColumns(20);

        Object[] fields = {
            "First Name:",    firstNameField,
            "Last Name:",     lastNameField,
            "SSS #:",         sssField,
            "PhilHealth #:",  philHealthField,
            "TIN #:",         tinField,
            "Pag-IBIG #:",    pagIbigField,
            "Position:",      positionField,
            "Basic Salary:",  salaryField,
            "Hourly Rate:",   hourlyRateField
        };

        SwingUtilities.invokeLater(firstNameField::requestFocusInWindow);

        // Same retry pattern as addEmployee(): on validation failure, the
        // dialog reopens with the SAME fields instead of discarding the form.
        while (true) {

            int option = JOptionPane.showConfirmDialog(
                    frame, fields,
                    "Update Employee — " + employeeID,
                    JOptionPane.OK_CANCEL_OPTION);

            if (option != JOptionPane.OK_OPTION) return;

            String firstName  = firstNameField.getText().trim();
            String lastName   = lastNameField.getText().trim();
            String sssNum     = sssField.getText().trim();
            String philHealth = philHealthField.getText().trim();
            String tin        = tinField.getText().trim();
            String pagIbig    = pagIbigField.getText().trim();
            String position   = positionField.getText().trim();
            String salaryText = salaryField.getText().trim();
            String rateText   = hourlyRateField.getText().trim();

            // Empty field check
            if (firstName.isEmpty() || lastName.isEmpty() || sssNum.isEmpty()
                    || philHealth.isEmpty() || tin.isEmpty() || pagIbig.isEmpty()
                    || position.isEmpty() || salaryText.isEmpty() || rateText.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "All fields are required. Update cancelled.",
                        "Incomplete Fields", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            // Government ID fields must be numbers (dashes allowed).
            if (!isValidGovId(sssNum, "SSS #") || !isValidGovId(philHealth, "PhilHealth #")
                    || !isValidGovId(tin, "TIN #") || !isValidGovId(pagIbig, "Pag-IBIG #")) {
                continue;
            }

            // Numeric validation
            double salary, hourlyRate;
            try {
                salary     = Double.parseDouble(salaryText);
                hourlyRate = Double.parseDouble(rateText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Basic Salary and Hourly Rate must be valid numbers.",
                        "Invalid Input", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            if (salary <= 0 || hourlyRate <= 0) {
                JOptionPane.showMessageDialog(frame,
                        "Basic Salary and Hourly Rate must be greater than zero.",
                        "Invalid Value", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            // Apply update to Employee object and rewrite CSV.
            // Only report success if the CSV write actually succeeded.
            boolean updated = payrollService.updateEmployee(employeeID,
                    firstName, lastName,
                    sssNum, philHealth, tin, pagIbig,
                    position, hourlyRate, salary);

            if (updated) {
                outputArea.setText("Employee updated successfully.\n"
                        + "ID: " + employeeID + " | Name: " + firstName + " " + lastName);
            } else {
                outputArea.setText("Failed to update employee.\n"
                        + "The change could not be saved to the CSV file.\n"
                        + "Please check file permissions and try again.");
            }
            return;
        }
    }

    /**
     * Deletes the selected employee after a confirmation dialog.
     * Removes from the employee list and rewrites the CSV.
     *
     * Table refresh is handled automatically via onDataChanged() —
     * no manual row removal needed here.
     */
    public void deleteEmployee() {

        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            outputArea.setText("No employee selected.\nPlease click a row in the table first.");
            return;
        }

        int modelRow      = employeeTable.convertRowIndexToModel(selectedRow);
        String employeeID = (String) tableModel.getValueAt(modelRow, 0);
        String fullName   = tableModel.getValueAt(modelRow, 1)
                          + " " + tableModel.getValueAt(modelRow, 2);

        // Confirmation required before any deletion
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Are you sure you want to delete:\n"
                + employeeID + " — " + fullName + "?\n\nThis action cannot be undone.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        // BUG FIX: only report success if the CSV write actually succeeded.
        boolean deleted = payrollService.deleteEmployee(employeeID);

        if (deleted) {
            outputArea.setText("Employee \"" + fullName + "\" ("
                    + employeeID + ") deleted successfully.");
        } else {
            outputArea.setText("Failed to delete employee.\n"
                    + "The change could not be saved to the CSV file.\n"
                    + "Please check file permissions and try again.");
        }
    }

    /**
     * Displays the selected employee's full details in the output area,
     * including government ID numbers and computed payroll values.
     */
    public void viewEmployee() {

        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            outputArea.setText("No employee selected.\nPlease click a row in the table first.");
            return;
        }

        int modelRow      = employeeTable.convertRowIndexToModel(selectedRow);
        String employeeID = (String) tableModel.getValueAt(modelRow, 0);
        Employee emp      = payrollService.findEmployee(employeeID);

        String info = (emp != null)
            ? "Employee #     : " + emp.getEmployeeID()
            + "\nFirst Name     : " + emp.getFirstName()
            + "\nLast Name      : " + emp.getLastName()
            + "\nPosition       : " + emp.getPosition()
            + "\nSSS #          : " + emp.getSssNumber()
            + "\nPhilHealth #   : " + emp.getPhilHealthNumber()
            + "\nTIN #          : " + emp.getTin()
            + "\nPag-IBIG #     : " + emp.getPagIbigNumber()
            + "\nHourly Rate    : PHP " + String.format("%.2f", emp.getHourlyRate())
            + "\nHours Worked   : "     + String.format("%.2f", emp.getHoursWorked())
            + "\nGross Pay      : PHP " + String.format("%.2f", emp.getGrossPay())
            + "\nTotal Deduct.  : PHP " + String.format("%.2f", emp.getTotalDeductions())
            + "\nNet Pay        : PHP " + String.format("%.2f", emp.getNetPay())
            : "Employee not found. Please refresh and try again.";

        outputArea.setText(info);

        if (emp != null) {
            JTextArea detailArea = new JTextArea(info);
            detailArea.setEditable(false);
            detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JOptionPane.showMessageDialog(frame,
                    new JScrollPane(detailArea),
                    "Employee Details — " + emp.getEmployeeID(),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Reads and displays real attendance records for the selected employee
     * directly from the MotorPH Attendance CSV via CSVHandler.
     * Shows date, log in, log out, and hours for each record.
     */
    public void viewAttendance() {

        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            outputArea.setText("No employee selected.\nPlease click a row in the table first.");
            return;
        }

        int modelRow      = employeeTable.convertRowIndexToModel(selectedRow);
        String employeeID = (String) tableModel.getValueAt(modelRow, 0);
        String fullName   = tableModel.getValueAt(modelRow, 1)
                          + " " + tableModel.getValueAt(modelRow, 2);

        List<String[]> records =
            CSVHandler.readAttendanceForEmployee(
                PayrollService.ATTENDANCE_FILE, employeeID);

        StringBuilder sb = new StringBuilder();
        sb.append("Attendance Records — ").append(fullName).append("\n");
        sb.append("---------------------------------------------------\n");
        sb.append(String.format("%-12s %-10s %-10s %s%n",
                  "Date", "Log In", "Log Out", "Hours"));
        sb.append("---------------------------------------------------\n");

        if (records.isEmpty()) {
            sb.append("No attendance records found for this employee.");
        } else {
            for (String[] record : records) {
                sb.append(String.format("%-12s %-10s %-10s %s%n",
                          record[0], record[1], record[2], record[3]));
            }
            sb.append("---------------------------------------------------\n");
            sb.append("Total Records : ").append(records.size());
        }

        outputArea.setText(sb.toString());
    }

    /**
     * Opens a form dialog to record a new attendance entry (date, log in,
     * log out) for the currently selected employee, and appends it to the
     * Attendance CSV. Without this, a newly added employee can never
     * accumulate hours and would always compute to zero pay.
     */
    public void addAttendance() {

        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            outputArea.setText("No employee selected.\nPlease click a row in the table first.");
            return;
        }

        int modelRow      = employeeTable.convertRowIndexToModel(selectedRow);
        String employeeID = (String) tableModel.getValueAt(modelRow, 0);
        String firstName  = (String) tableModel.getValueAt(modelRow, 1);
        String lastName   = (String) tableModel.getValueAt(modelRow, 2);

        JTextField dateField   = new JTextField("MM/DD/YYYY", 12);
        JTextField logInField  = new JTextField("8:00", 8);
        JTextField logOutField = new JTextField("17:00", 8);

        Object[] fields = {
            "Date (MM/DD/YYYY):", dateField,
            "Log In (H:mm):",     logInField,
            "Log Out (H:mm):",    logOutField
        };

        SwingUtilities.invokeLater(dateField::requestFocusInWindow);

        while (true) {

            int option = JOptionPane.showConfirmDialog(
                    frame, fields,
                    "Add Attendance — " + employeeID,
                    JOptionPane.OK_CANCEL_OPTION);

            if (option != JOptionPane.OK_OPTION) return;

            String date   = dateField.getText().trim();
            String logIn  = logInField.getText().trim();
            String logOut = logOutField.getText().trim();

            if (date.isEmpty() || logIn.isEmpty() || logOut.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "All fields are required.",
                        "Incomplete Fields", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            if (!date.matches("\\d{2}/\\d{2}/\\d{4}")) {
                JOptionPane.showMessageDialog(frame,
                        "Date must be in MM/DD/YYYY format (e.g., 06/22/2026).",
                        "Invalid Input", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            java.time.LocalTime timeIn, timeOut;
            try {
                timeIn  = java.time.LocalTime.parse(logIn,
                        java.time.format.DateTimeFormatter.ofPattern("H:mm"));
                timeOut = java.time.LocalTime.parse(logOut,
                        java.time.format.DateTimeFormatter.ofPattern("H:mm"));
            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Log In and Log Out must be in H:mm 24-hour format (e.g., 8:59, 18:31).",
                        "Invalid Input", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            if (!timeOut.isAfter(timeIn)) {
                JOptionPane.showMessageDialog(frame,
                        "Log Out must be later than Log In.",
                        "Invalid Value", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            boolean added = CSVHandler.appendAttendance(
                    PayrollService.ATTENDANCE_FILE, employeeID,
                    lastName, firstName, date, logIn, logOut);

            if (added) {
                outputArea.setText("Attendance record added for " + firstName + " " + lastName
                        + ".\nRun 'Compute Salaries' in Payroll Menu to update pay using the new hours.");
            } else {
                outputArea.setText("Failed to add attendance record.\n"
                        + "Please check file permissions and try again.");
            }
            return;
        }
    }
}