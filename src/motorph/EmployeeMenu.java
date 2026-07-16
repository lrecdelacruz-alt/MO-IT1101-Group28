package motorph;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
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
    private JLabel            countLabel;

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

        JButton clearSearchButton = new JButton("✕");
        clearSearchButton.setPreferredSize(new Dimension(28, 28));
        clearSearchButton.setFont(new Font("Arial", Font.PLAIN, 11));
        clearSearchButton.setToolTipText("Clear search");
        clearSearchButton.setFocusPainted(false);
        clearSearchButton.setBackground(new Color(190, 195, 200));
        clearSearchButton.setForeground(Color.BLACK);
        clearSearchButton.setBorderPainted(true);
        clearSearchButton.setOpaque(true);
        clearSearchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            sorter.setRowFilter(null);
            updateCountLabel();
            searchField.requestFocusInWindow();
        });

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(clearSearchButton);

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

        countLabel = new JLabel(" ", SwingConstants.RIGHT);
        countLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        countLabel.setForeground(Color.GRAY);
        countLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        JPanel searchAndCountPanel = new JPanel(new BorderLayout());
        searchAndCountPanel.add(searchPanel, BorderLayout.WEST);
        searchAndCountPanel.add(countLabel,  BorderLayout.EAST);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(titleAndBannerPanel,  BorderLayout.NORTH);
        northPanel.add(searchAndCountPanel,  BorderLayout.SOUTH);

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

        // Selection highlight color
        employeeTable.setSelectionBackground(new Color(173, 216, 230));
        employeeTable.setSelectionForeground(Color.BLACK);

        // Zebra striping + Net Pay color indicator (red = uncomputed, green = computed)
        employeeTable.setDefaultRenderer(Object.class,
            new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                public java.awt.Component getTableCellRendererComponent(
                        JTable table, Object value, boolean isSelected,
                        boolean hasFocus, int row, int col) {
                    super.getTableCellRendererComponent(
                            table, value, isSelected, hasFocus, row, col);
                    if (isSelected) {
                        setBackground(new Color(173, 216, 230));
                        setForeground(Color.BLACK);
                    } else {
                        // Zebra stripe base
                        Color base = (row % 2 == 0)
                            ? Color.WHITE
                            : new Color(240, 245, 250);
                        setBackground(base);

                        // Net Pay column (index 8) — red if zero, green if computed
                        if (col == 8) {
                            String val = value != null ? value.toString() : "0.00";
                            double net = 0;
                            try { net = Double.parseDouble(val.replace(",", "")); }
                            catch (NumberFormatException ignored) {}
                            setForeground(net <= 0 ? new Color(183, 28, 28) : new Color(46, 125, 50));
                        } else {
                            setForeground(Color.BLACK);
                        }
                    }
                    return this;
                }
            }
        );

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

        // Tooltips — shown on hover, helps users understand each button's purpose
        addButton.setToolTipText("Add a new employee record to the system");
        updateButton.setToolTipText("Edit the selected employee's details");
        deleteButton.setToolTipText("Permanently remove the selected employee");
        viewButton.setToolTipText("View full details of the selected employee");
        attendanceButton.setToolTipText("View attendance records for the selected employee");
        addAttendanceButton.setToolTipText("Add a new attendance entry for the selected employee");

        // Color coding by what each action DOES:
        //   green = creates a new record (Add Employee, Add Attendance)
        //   blue  = modifies an existing record (Update)
        //   red   = removes a record (Delete)
        //   gray  = read-only / informational (View, View Attendance)
        // Routed through UIHelper.styleButton() so this matches the exact
        // same styling logic used everywhere else in the app (PayrollMenu,
        // dialog buttons), instead of duplicating the same bevel-border
        // pattern by hand in a fourth place.
        Dimension toolbarSize = new Dimension(170, 32);

        UIHelper.styleButton(addButton,          UIHelper.GREEN, Color.WHITE, toolbarSize);
        UIHelper.styleButton(addAttendanceButton, UIHelper.GREEN, Color.WHITE, toolbarSize);
        UIHelper.styleButton(updateButton,       UIHelper.BLUE,  Color.WHITE, toolbarSize);
        UIHelper.styleButton(deleteButton,       UIHelper.RED,   Color.WHITE, toolbarSize);
        UIHelper.styleButton(viewButton,         UIHelper.GRAY,  Color.BLACK, toolbarSize);
        UIHelper.styleButton(attendanceButton,   UIHelper.GRAY,  Color.BLACK, toolbarSize);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(4, 0, 0, 0)));
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(attendanceButton);
        buttonPanel.add(addAttendanceButton);

        // Output area — shows details and status messages
        outputArea = new JTextArea(6, 40);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        outputArea.setBorder(BorderFactory.createTitledBorder("Status"));
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
            updateCountLabel();
            return;
        }
        try {
            sorter.setRowFilter(
                RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0, 1, 2));
        } catch (PatternSyntaxException ex) {
            sorter.setRowFilter(null);
        }
        updateCountLabel();
    }

    /** Updates the employee count label based on current filter state. */
    private void updateCountLabel() {
        if (countLabel == null) return;
        int total    = payrollService.getEmployees().size();
        int showing  = sorter.getViewRowCount();
        if (showing == total) {
            countLabel.setText("Total: " + total + " employee(s)   ");
        } else {
            countLabel.setText("Showing " + showing + " of " + total + " employee(s)   ");
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

        updateCountLabel();

        if (anyUncomputed) {
            statusBanner.setText(
                "Some employees have not had salaries computed yet — "
                + "open Payroll Menu and click \"Compute Salaries.\"");
        } else {
            statusBanner.setText(" ");
        }
    }

    private String showDatePicker() {
        return showDatePicker(null);
    }

    /**
     * Same as showDatePicker(), but accepts an optional starting date
     * (MM/DD/YYYY format). When provided, the calendar opens already
     * positioned on that date instead of defaulting to today — used by
     * Edit Attendance so the picker starts where the existing record is,
     * rather than always jumping to the current real-world date.
     */
    private String showDatePicker(String startingDate) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        final int[] selectedYear;
        final int[] selectedMonth;
        final int[] selectedDay;

        if (startingDate != null) {
            String[] parts = startingDate.split("/"); // MM/DD/YYYY
            selectedMonth = new int[]{ Integer.parseInt(parts[0]) - 1 }; // 0-indexed
            selectedDay   = new int[]{ Integer.parseInt(parts[1]) };
            selectedYear  = new int[]{ Integer.parseInt(parts[2]) };
        } else {
            selectedYear  = new int[]{ cal.get(java.util.Calendar.YEAR) };
            selectedMonth = new int[]{ cal.get(java.util.Calendar.MONTH) };
            selectedDay   = new int[]{ cal.get(java.util.Calendar.DAY_OF_MONTH) };
        }

        JDialog calDialog = new JDialog(frame, "Select Date", true);
        calDialog.setLayout(new BorderLayout(5, 5));
        calDialog.setResizable(false);

        // Month/Year navigation — styled as an obvious dropdown button (with a
        // visible ▾ caret) rather than plain text, so it's clear at a glance
        // that clicking it opens a year picker, without needing to hover first.
        JButton monthYearButton = new JButton("");
        monthYearButton.setFont(new Font("Arial", Font.BOLD, 14));
        monthYearButton.setForeground(new Color(21, 101, 192));
        monthYearButton.setBackground(Color.WHITE);
        monthYearButton.setFocusPainted(false);
        monthYearButton.setBorderPainted(false);
        monthYearButton.setContentAreaFilled(false);
        monthYearButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        monthYearButton.setToolTipText("Click to jump to a specific year");
        monthYearButton.setHorizontalAlignment(SwingConstants.CENTER);

        JButton prevButton = new JButton("◀");
        JButton nextButton = new JButton("▶");
        prevButton.setFont(new Font("Arial", Font.PLAIN, 12));
        nextButton.setFont(new Font("Arial", Font.PLAIN, 12));
        prevButton.setFocusPainted(false);
        nextButton.setFocusPainted(false);

        JPanel navPanel = new JPanel(new BorderLayout(8, 0));
        navPanel.setBorder(BorderFactory.createEmptyBorder(8, 6, 4, 6));

        prevButton.setPreferredSize(new Dimension(34, 28));
        nextButton.setPreferredSize(new Dimension(34, 28));
        monthYearButton.setPreferredSize(new Dimension(190, 28));

        navPanel.add(prevButton,       BorderLayout.WEST);
        navPanel.add(monthYearButton,  BorderLayout.CENTER);
        navPanel.add(nextButton,       BorderLayout.EAST);

        // Calendar grid
        JPanel calGrid = new JPanel(new GridLayout(7, 7, 2, 2));
        calGrid.setBorder(BorderFactory.createEmptyBorder(0, 10, 4, 10));

        // Selected day tracking
        final JLabel[] selectedButton = {null};

        // Rebuild calendar grid
        String[] dayNames = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};

        Runnable rebuildCalendar = () -> {
            calGrid.removeAll();

            String[] months = {"January","February","March","April","May","June",
                            "July","August","September","October","November","December"};
            monthYearButton.setText(months[selectedMonth[0]] + " " + selectedYear[0] + "  ▾");

            // Day headers
            for (String d : dayNames) {
                JLabel lbl = new JLabel(d, SwingConstants.CENTER);
                lbl.setFont(new Font("Arial", Font.BOLD, 11));
                lbl.setForeground(new Color(21, 101, 192));
                calGrid.add(lbl);
            }

            // First day of month
            java.util.Calendar temp = java.util.Calendar.getInstance();
            temp.set(selectedYear[0], selectedMonth[0], 1);
            int firstDay = temp.get(java.util.Calendar.DAY_OF_WEEK) - 1;
            int daysInMonth = temp.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);

            // Empty cells before first day
            for (int i = 0; i < firstDay; i++) {
                calGrid.add(new JLabel(""));
            }

            // Day cells — built as JLabels instead of JButtons. JLabel has no
            // platform-native chrome to fight with, so the selection color and
            // text always render correctly and identically on every OS (Windows,
            // Mac, Linux), unlike JButton, whose look-and-feel can override or
            // clash with manually-set colors (this was the cause of the
            // "number disappears" bug).
            for (int day = 1; day <= daysInMonth; day++) {
                final int d = day;
                JLabel dayLbl = new JLabel(String.valueOf(day), SwingConstants.CENTER);
                dayLbl.setFont(new Font("Arial", Font.PLAIN, 12));
                dayLbl.setOpaque(true);
                dayLbl.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
                dayLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                if (day == selectedDay[0]) {
                    dayLbl.setBackground(new Color(21, 101, 192));
                    dayLbl.setForeground(Color.WHITE);
                    selectedButton[0] = dayLbl;
                } else {
                    dayLbl.setBackground(Color.WHITE);
                    dayLbl.setForeground(Color.BLACK);
                }

                dayLbl.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent ev) {
                        if (selectedButton[0] != null) {
                            selectedButton[0].setBackground(Color.WHITE);
                            selectedButton[0].setForeground(Color.BLACK);
                            selectedButton[0].repaint();
                        }
                        selectedDay[0] = d;
                        dayLbl.setBackground(new Color(21, 101, 192));
                        dayLbl.setForeground(Color.WHITE);
                        dayLbl.repaint();
                        selectedButton[0] = dayLbl;
                    }

                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent ev) {
                        if (dayLbl != selectedButton[0]) {
                            dayLbl.setBackground(new Color(230, 240, 250));
                        }
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent ev) {
                        if (dayLbl != selectedButton[0]) {
                            dayLbl.setBackground(Color.WHITE);
                        }
                    }
                });

                calGrid.add(dayLbl);
            }

            calGrid.revalidate();
            calGrid.repaint();
        };

        prevButton.addActionListener(e -> {
            selectedMonth[0]--;
            if (selectedMonth[0] < 0) {
                selectedMonth[0] = 11;
                selectedYear[0]--;
            }
            selectedDay[0] = 1;
            rebuildCalendar.run();
        });

        nextButton.addActionListener(e -> {
            selectedMonth[0]++;
            if (selectedMonth[0] > 11) {
                selectedMonth[0] = 0;
                selectedYear[0]++;
            }
            selectedDay[0] = 1;
            rebuildCalendar.run();
        });

        rebuildCalendar.run();

        monthYearButton.addActionListener(e -> {
            String[] yearOptions = new String[201]; // 1900–2100 inclusive
            for (int i = 0; i < yearOptions.length; i++) {
                yearOptions[i] = String.valueOf(1900 + i);
            }
            JComboBox<String> yearCombo = new JComboBox<>(yearOptions);
            yearCombo.setEditable(true);
            yearCombo.setSelectedItem(String.valueOf(selectedYear[0]));
            yearCombo.setPreferredSize(new Dimension(120, 28));

            int result = JOptionPane.showConfirmDialog(
                    calDialog, yearCombo, "Select Year",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) return; // cancelled

            String input = (String) yearCombo.getSelectedItem();
            if (input == null) return;

            try {
                int year = Integer.parseInt(input.trim());
                if (year < 1900 || year > 2100) {
                    JOptionPane.showMessageDialog(calDialog,
                            "Please enter a year between 1900 and 2100.",
                            "Invalid Year", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                selectedYear[0] = year;
                selectedDay[0] = 1;
                rebuildCalendar.run();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(calDialog,
                        "Please enter a valid 4-digit year.",
                        "Invalid Year", JOptionPane.WARNING_MESSAGE);
            }
        });

        // OK / Cancel buttons
        final boolean[] picked = {false};
        JButton okBtn     = new JButton("Select");
        JButton cancelBtn = new JButton("Cancel");
        UIHelper.styleButton(okBtn,     UIHelper.BLUE, Color.WHITE,
                new Dimension(90, 30));
        UIHelper.styleButton(cancelBtn, UIHelper.GRAY, Color.BLACK,
                new Dimension(90, 30));

        okBtn.addActionListener(e -> {
            picked[0] = true;
            calDialog.dispose();
        });
        cancelBtn.addActionListener(e -> calDialog.dispose());

        JPanel calBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        calBtnPanel.setBorder(BorderFactory.createMatteBorder(
                1, 0, 0, 0, new Color(220, 220, 220)));
        calBtnPanel.add(cancelBtn);
        calBtnPanel.add(okBtn);

        calDialog.add(navPanel,    BorderLayout.NORTH);
        calDialog.add(calGrid,     BorderLayout.CENTER);
        calDialog.add(calBtnPanel, BorderLayout.SOUTH);
        calDialog.pack();
        calDialog.setMinimumSize(new Dimension(340, 320));
        calDialog.setResizable(true); // safety net in case of font/DPI differences across machines
        calDialog.setLocationRelativeTo(frame);
        calDialog.setVisible(true);

        if (!picked[0]) return null;

        return String.format("%02d/%02d/%04d",
                selectedMonth[0] + 1, selectedDay[0], selectedYear[0]);
    }

    private String showTimePicker(String title) {
        return showTimePicker(title, null);
    }

    /**
     * Same as showTimePicker(), but accepts an optional starting time
     * (24-hour H:mm format, e.g. "17:30"). When provided, the spinners
     * and AM/PM toggle open already showing that time instead of always
     * defaulting to 8:00 AM — used by Edit Attendance so the picker
     * starts at the record's existing time.
     */
    private String showTimePicker(String title, String startingTime24h) {

        int defaultHour12 = 8;
        int defaultMinute = 0;
        boolean defaultIsPM = false;

        if (startingTime24h != null) {
            String[] parts = startingTime24h.split(":");
            int hour24 = Integer.parseInt(parts[0]);
            defaultMinute = Integer.parseInt(parts[1]);
            defaultIsPM = hour24 >= 12;
            defaultHour12 = hour24 % 12;
            if (defaultHour12 == 0) defaultHour12 = 12;
        }

        JDialog timeDialog = new JDialog(frame, title, true);
        timeDialog.setLayout(new BorderLayout(10, 10));
        timeDialog.setResizable(false);

        // Hour spinner: 1–12 (12-hour display), wraps around at the boundary
        // (clicking down from 1 goes to 12, clicking up from 12 goes to 1) —
        // matches how real time-wheel pickers behave, so reaching a "high"
        // value doesn't require spinning the long way around.
        SpinnerNumberModel hourModel = new SpinnerNumberModel(defaultHour12, 1, 12, 1) {
            @Override
            public Object getNextValue() {
                int current = (int) getValue();
                return (current >= 12) ? 1 : current + 1;
            }
            @Override
            public Object getPreviousValue() {
                int current = (int) getValue();
                return (current <= 1) ? 12 : current - 1;
            }
        };
        SpinnerNumberModel minuteModel = new SpinnerNumberModel(defaultMinute, 0, 59, 1) {
            @Override
            public Object getNextValue() {
                int current = (int) getValue();
                return (current >= 59) ? 0 : current + 1;
            }
            @Override
            public Object getPreviousValue() {
                int current = (int) getValue();
                return (current <= 0) ? 59 : current - 1;
            }
        };

        JSpinner hourSpinner   = new JSpinner(hourModel);
        JSpinner minuteSpinner = new JSpinner(minuteModel);

        hourSpinner.setFont(new Font("Arial", Font.PLAIN, 16));
        minuteSpinner.setFont(new Font("Arial", Font.PLAIN, 16));

        hourSpinner.setEditor(new JSpinner.NumberEditor(hourSpinner, "00"));
        minuteSpinner.setEditor(new JSpinner.NumberEditor(minuteSpinner, "00"));

        hourSpinner.addChangeListener(ev -> {
            int val = (int) hourSpinner.getValue();
            if (val < 1) hourSpinner.setValue(1);
            else if (val > 12) hourSpinner.setValue(12);
        });
        minuteSpinner.addChangeListener(ev -> {
            int val = (int) minuteSpinner.getValue();
            if (val < 0) minuteSpinner.setValue(0);
            else if (val > 59) minuteSpinner.setValue(59);
        });

        JTextField hourTextField = ((javax.swing.JSpinner.NumberEditor) hourSpinner.getEditor()).getTextField();
        JTextField minuteTextField = ((javax.swing.JSpinner.NumberEditor) minuteSpinner.getEditor()).getTextField();

        hourTextField.setHorizontalAlignment(JTextField.RIGHT);
        minuteTextField.setHorizontalAlignment(JTextField.RIGHT);

        // Caret-position fix: listening to the text field's own "value"
        // property change fires AFTER the formatter finishes reformatting,
        // avoiding the timing race that caused the caret to land before
        // the digit instead of after it.
        hourTextField.addPropertyChangeListener("value", ev ->
            SwingUtilities.invokeLater(() -> hourTextField.setCaretPosition(hourTextField.getText().length())));
        minuteTextField.addPropertyChangeListener("value", ev ->
            SwingUtilities.invokeLater(() -> minuteTextField.setCaretPosition(minuteTextField.getText().length())));

        // AM/PM toggle — built as JLabels instead of JToggleButtons. Like the
        // calendar day cells fixed earlier, JLabel has no platform-native
        // chrome to fight with, so the selected/unselected colors always
        // render clearly and consistently on every OS.
        final boolean[] isPM = {defaultIsPM};
        final JLabel[] amLbl = new JLabel[1];
        final JLabel[] pmLbl = new JLabel[1];

        amLbl[0] = new JLabel("AM", SwingConstants.CENTER);
        pmLbl[0] = new JLabel("PM", SwingConstants.CENTER);

        Dimension ampmSize = new Dimension(50, 30);
        for (JLabel lbl : new JLabel[]{amLbl[0], pmLbl[0]}) {
            lbl.setPreferredSize(ampmSize);
            lbl.setFont(new Font("Arial", Font.BOLD, 12));
            lbl.setOpaque(true);
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lbl.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        }

        Runnable refreshAmPm = () -> {
            if (isPM[0]) {
                pmLbl[0].setBackground(new Color(21, 101, 192));
                pmLbl[0].setForeground(Color.WHITE);
                amLbl[0].setBackground(Color.WHITE);
                amLbl[0].setForeground(Color.BLACK);
            } else {
                amLbl[0].setBackground(new Color(21, 101, 192));
                amLbl[0].setForeground(Color.WHITE);
                pmLbl[0].setBackground(Color.WHITE);
                pmLbl[0].setForeground(Color.BLACK);
            }
        };
        refreshAmPm.run();

        amLbl[0].addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                isPM[0] = false;
                refreshAmPm.run();
            }
        });
        pmLbl[0].addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                isPM[0] = true;
                refreshAmPm.run();
            }
        });

        // No gap between the two halves — reads clearly as one connected
        // switch rather than two separate, unrelated buttons.
        JPanel ampmPanel = new JPanel(new GridLayout(1, 2, 0, 0));
        ampmPanel.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150)));
        ampmPanel.add(amLbl[0]);
        ampmPanel.add(pmLbl[0]);

        JLabel colonLabel = new JLabel(":", SwingConstants.CENTER);
        colonLabel.setFont(new Font("Arial", Font.BOLD, 24));

        JPanel spinnerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 10));
        spinnerPanel.setBorder(BorderFactory.createEmptyBorder(14, 20, 4, 20));
        spinnerPanel.add(hourSpinner);
        spinnerPanel.add(colonLabel);
        spinnerPanel.add(minuteSpinner);
        spinnerPanel.add(Box.createHorizontalStrut(10));
        spinnerPanel.add(ampmPanel);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(spinnerPanel, BorderLayout.CENTER);

        final boolean[] picked = {false};
        JButton okBtn     = new JButton("Select");
        JButton cancelBtn = new JButton("Cancel");
        UIHelper.styleButton(okBtn,     UIHelper.BLUE, Color.WHITE,
                new Dimension(90, 30));
        UIHelper.styleButton(cancelBtn, UIHelper.GRAY, Color.BLACK,
                new Dimension(90, 30));

        okBtn.addActionListener(e -> {
            picked[0] = true;
            timeDialog.dispose();
        });
        cancelBtn.addActionListener(e -> timeDialog.dispose());

        JPanel timeBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        timeBtnPanel.setBorder(BorderFactory.createMatteBorder(
                1, 0, 0, 0, new Color(220, 220, 220)));
        timeBtnPanel.add(cancelBtn);
        timeBtnPanel.add(okBtn);

        timeDialog.add(centerPanel,  BorderLayout.CENTER);
        timeDialog.add(timeBtnPanel, BorderLayout.SOUTH);
        timeDialog.pack();
        timeDialog.setMinimumSize(new Dimension(300, 170));
        timeDialog.setLocationRelativeTo(frame);
        timeDialog.setVisible(true);

        if (!picked[0]) return null;

        int hour12 = (int) hourSpinner.getValue();
        int minute = (int) minuteSpinner.getValue();
        boolean finalIsPM = isPM[0];

        // Convert 12-hour + AM/PM back to 24-hour for storage, since
        // CSVHandler and LocalTime parsing expect H:mm in 24-hour format.
        int hour24;
        if (finalIsPM) {
            hour24 = (hour12 == 12) ? 12 : hour12 + 12;
        } else {
            hour24 = (hour12 == 12) ? 0 : hour12;
        }

        return hour24 + ":" + String.format("%02d", minute);
    }

    /** Creates a small red helper-text label for live field validation, hidden by default. */
    private JLabel makeErrorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(new Font("Arial", Font.PLAIN, 10));
        label.setForeground(new Color(183, 28, 28));
        return label;
    }

    /**
     * Attaches a live validator to a field. The validator function receives
     * the current text and returns an error message, or null if valid.
     * The error label updates on every keystroke.
     */
    private void attachLiveValidation(JTextField field, JLabel errorLabel,
            java.util.function.Function<String, String> validator) {
        field.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e)  { validate(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e)  { validate(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { validate(); }

                private void validate() {
                    String error = validator.apply(field.getText());
                    errorLabel.setText(error != null ? error : " ");
                }
            }
        );
    }

    /**
     * Creates a consistent bold label for form fields.
     * Shared by addEmployee() and updateEmployee() dialogs.
     */
    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setForeground(new Color(60, 60, 60));
        return label;
    }

    /**
     * Applies a character limit to a JTextField using a DocumentFilter.
     */
    private void setMaxLength(JTextField field, int max) {
        ((javax.swing.text.AbstractDocument) field.getDocument())
            .setDocumentFilter(new javax.swing.text.DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset,
                        String string, javax.swing.text.AttributeSet attr)
                        throws javax.swing.text.BadLocationException {
                    if (fb.getDocument().getLength() + string.length() <= max) {
                        super.insertString(fb, offset, string, attr);
                    }
                }
                @Override
                public void replace(FilterBypass fb, int offset, int length,
                        String text, javax.swing.text.AttributeSet attrs)
                        throws javax.swing.text.BadLocationException {
                    if (fb.getDocument().getLength() - length + text.length() <= max) {
                        super.replace(fb, offset, length, text, attrs);
                    }
                }
            });
    }

    /**
     * Adds auto-formatting to a gov ID field.
     * Automatically inserts dashes at the correct positions as the user types.
     * pattern example: "##-#######-#" for SSS
     */
    private void addAutoFormat(JTextField field, String pattern) {
        field.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                boolean updating = false;
                public void insertUpdate(javax.swing.event.DocumentEvent e) { format(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) {}
                public void changedUpdate(javax.swing.event.DocumentEvent e) {}

                private void format() {
                    if (updating) return;
                    SwingUtilities.invokeLater(() -> {
                        if (updating) return;
                        updating = true;
                        try {
                            String raw = field.getText().replaceAll("[^0-9]", "");
                            StringBuilder formatted = new StringBuilder();
                            int rawIndex = 0;
                            for (int i = 0; i < pattern.length() && rawIndex < raw.length(); i++) {
                                char p = pattern.charAt(i);
                                if (p == '#') {
                                    formatted.append(raw.charAt(rawIndex++));
                                } else {
                                    formatted.append(p);
                                }
                            }
                            field.setText(formatted.toString());
                            field.setCaretPosition(field.getText().length());
                        } finally {
                            updating = false;
                        }
                    });
                }
            }
        );
    }

    /**
     * Creates a text field with placeholder/hint text that is purely visual
     * (painted as an overlay), never stored as real text in the field.
     * This means the hint disappears the instant the user types a single
     * character — no select-all or manual deletion needed, matching how
     * placeholders work in real applications (e.g. HTML input placeholders).
     */
    private JTextField createHintField(String hint, int columns) {
        JTextField field = new JTextField(columns) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.GRAY);
                    g2.setFont(getFont());
                    Insets insets = getInsets();
                    int textBaseline = (getHeight() - getFontMetrics(getFont()).getHeight()) / 2
                            + getFontMetrics(getFont()).getAscent();
                    g2.drawString(hint, insets.left + 2, textBaseline);
                    g2.dispose();
                }
            }
        };

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) { field.repaint(); }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) { field.repaint(); }
        });

        return field;
    }

    /**
     * Returns true if the field is empty — meaning the user hasn't
     * entered a real value yet. (Hint text is now purely visual/overlay,
     * so it's never part of the actual field text — a simple emptiness
     * check is all that's needed.)
     */
    private boolean isHintOrEmpty(JTextField field, String hint) {
        return field.getText().trim().isEmpty();
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

        JTextField idField          = createHintField("e.g. 10001", 18);
        JTextField firstNameField   = createHintField("e.g. Juan", 18);
        JTextField lastNameField    = createHintField("e.g. dela Cruz", 18);
        JTextField sssField         = createHintField("e.g. 44-4506057-3", 18);
        JTextField philHealthField  = createHintField("e.g. 820126183514", 18);
        JTextField tinField         = createHintField("e.g. 442-605-657-000", 18);
        JTextField pagIbigField     = createHintField("e.g. 691295330870", 18);
        JTextField positionField    = createHintField("e.g. Rank and File", 18);
        JTextField salaryField      = createHintField("e.g. 40000.00", 18);
        JTextField hourlyRateField  = createHintField("e.g. 535.71", 18);

        JLabel idError          = makeErrorLabel();
        JLabel firstNameError   = makeErrorLabel();
        JLabel lastNameError    = makeErrorLabel();
        JLabel sssError         = makeErrorLabel();
        JLabel philHealthError  = makeErrorLabel();
        JLabel tinError         = makeErrorLabel();
        JLabel pagIbigError     = makeErrorLabel();
        JLabel positionError    = makeErrorLabel();
        JLabel salaryError      = makeErrorLabel();
        JLabel hourlyRateError  = makeErrorLabel();

        setMaxLength(idField, 10);
        setMaxLength(firstNameField, 30);
        setMaxLength(lastNameField, 30);
        setMaxLength(sssField, 14);
        setMaxLength(philHealthField, 14);
        setMaxLength(tinField, 15);
        setMaxLength(pagIbigField, 14);
        setMaxLength(positionField, 40);
        setMaxLength(salaryField, 12);
        setMaxLength(hourlyRateField, 10);

        addAutoFormat(sssField,        "##-#######-#");
        addAutoFormat(philHealthField, "############");
        addAutoFormat(tinField,        "###-###-###-###");
        addAutoFormat(pagIbigField,    "############");

        // Build a proper JDialog so the form is scrollable and never
        // gets cut off on smaller screens regardless of how many fields there are.
        JDialog addDialog = new JDialog(frame, "Add New Employee", true);
        addDialog.setLayout(new BorderLayout(0, 0));

        // --- Section 1: Employee Information ---
        JPanel infoSection = new JPanel(new GridBagLayout());
        infoSection.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Employee Information",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 11),
                new Color(21, 101, 192)));
        infoSection.setBackground(Color.WHITE);

        GridBagConstraints gi = new GridBagConstraints();
        gi.insets = new Insets(2, 8, 0, 8);
        gi.anchor = GridBagConstraints.WEST;

        gi.gridx = 0; gi.gridy = 0; gi.fill = GridBagConstraints.NONE;
        infoSection.add(makeLabel("Employee #:"), gi);
        gi.gridx = 1; gi.fill = GridBagConstraints.HORIZONTAL; gi.weightx = 1.0;
        infoSection.add(idField, gi);
        gi.gridx = 1; gi.gridy = 1; gi.insets = new Insets(0, 8, 4, 8);
        infoSection.add(idError, gi);

        gi.insets = new Insets(2, 8, 0, 8);
        gi.gridx = 0; gi.gridy = 2; gi.fill = GridBagConstraints.NONE; gi.weightx = 0;
        infoSection.add(makeLabel("First Name:"), gi);
        gi.gridx = 1; gi.fill = GridBagConstraints.HORIZONTAL; gi.weightx = 1.0;
        infoSection.add(firstNameField, gi);
        gi.gridx = 1; gi.gridy = 3; gi.insets = new Insets(0, 8, 4, 8);
        infoSection.add(firstNameError, gi);

        gi.insets = new Insets(2, 8, 0, 8);
        gi.gridx = 0; gi.gridy = 4; gi.fill = GridBagConstraints.NONE; gi.weightx = 0;
        infoSection.add(makeLabel("Last Name:"), gi);
        gi.gridx = 1; gi.fill = GridBagConstraints.HORIZONTAL; gi.weightx = 1.0;
        infoSection.add(lastNameField, gi);
        gi.gridx = 1; gi.gridy = 5; gi.insets = new Insets(0, 8, 4, 8);
        infoSection.add(lastNameError, gi);

        // --- Section 2: Government IDs ---
        JPanel govSection = new JPanel(new GridBagLayout());
        govSection.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Government IDs",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 11),
                new Color(21, 101, 192)));
        govSection.setBackground(Color.WHITE);

        GridBagConstraints gg = new GridBagConstraints();
        gg.insets = new Insets(2, 8, 0, 8);
        gg.anchor = GridBagConstraints.WEST;

        gg.gridx = 0; gg.gridy = 0; gg.fill = GridBagConstraints.NONE;
        govSection.add(makeLabel("SSS #:"), gg);
        gg.gridx = 1; gg.fill = GridBagConstraints.HORIZONTAL; gg.weightx = 1.0;
        govSection.add(sssField, gg);
        gg.gridx = 1; gg.gridy = 1; gg.insets = new Insets(0, 8, 4, 8);
        govSection.add(sssError, gg);

        gg.insets = new Insets(2, 8, 0, 8);
        gg.gridx = 0; gg.gridy = 2; gg.fill = GridBagConstraints.NONE; gg.weightx = 0;
        govSection.add(makeLabel("PhilHealth #:"), gg);
        gg.gridx = 1; gg.fill = GridBagConstraints.HORIZONTAL; gg.weightx = 1.0;
        govSection.add(philHealthField, gg);
        gg.gridx = 1; gg.gridy = 3; gg.insets = new Insets(0, 8, 4, 8);
        govSection.add(philHealthError, gg);

        gg.insets = new Insets(2, 8, 0, 8);
        gg.gridx = 0; gg.gridy = 4; gg.fill = GridBagConstraints.NONE; gg.weightx = 0;
        govSection.add(makeLabel("TIN #:"), gg);
        gg.gridx = 1; gg.fill = GridBagConstraints.HORIZONTAL; gg.weightx = 1.0;
        govSection.add(tinField, gg);
        gg.gridx = 1; gg.gridy = 5; gg.insets = new Insets(0, 8, 4, 8);
        govSection.add(tinError, gg);

        gg.insets = new Insets(2, 8, 0, 8);
        gg.gridx = 0; gg.gridy = 6; gg.fill = GridBagConstraints.NONE; gg.weightx = 0;
        govSection.add(makeLabel("Pag-IBIG #:"), gg);
        gg.gridx = 1; gg.fill = GridBagConstraints.HORIZONTAL; gg.weightx = 1.0;
        govSection.add(pagIbigField, gg);
        gg.gridx = 1; gg.gridy = 7; gg.insets = new Insets(0, 8, 4, 8);
        govSection.add(pagIbigError, gg);

        // --- Section 3: Position & Pay ---
        JPanel paySection = new JPanel(new GridBagLayout());
        paySection.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Position & Pay",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 11),
                new Color(21, 101, 192)));
        paySection.setBackground(Color.WHITE);

        GridBagConstraints gp = new GridBagConstraints();
        gp.insets = new Insets(2, 8, 0, 8);
        gp.anchor = GridBagConstraints.WEST;

        gp.gridx = 0; gp.gridy = 0; gp.fill = GridBagConstraints.NONE;
        paySection.add(makeLabel("Position:"), gp);
        gp.gridx = 1; gp.fill = GridBagConstraints.HORIZONTAL; gp.weightx = 1.0;
        paySection.add(positionField, gp);
        gp.gridx = 1; gp.gridy = 1; gp.insets = new Insets(0, 8, 4, 8);
        paySection.add(positionError, gp);

        gp.insets = new Insets(2, 8, 0, 8);
        gp.gridx = 0; gp.gridy = 2; gp.fill = GridBagConstraints.NONE; gp.weightx = 0;
        paySection.add(makeLabel("Basic Salary:"), gp);
        gp.gridx = 1; gp.fill = GridBagConstraints.HORIZONTAL; gp.weightx = 1.0;
        paySection.add(salaryField, gp);
        gp.gridx = 1; gp.gridy = 3; gp.insets = new Insets(0, 8, 4, 8);
        paySection.add(salaryError, gp);

        gp.insets = new Insets(2, 8, 0, 8);
        gp.gridx = 0; gp.gridy = 4; gp.fill = GridBagConstraints.NONE; gp.weightx = 0;
        paySection.add(makeLabel("Hourly Rate:"), gp);
        gp.gridx = 1; gp.fill = GridBagConstraints.HORIZONTAL; gp.weightx = 1.0;
        paySection.add(hourlyRateField, gp);
        gp.gridx = 1; gp.gridy = 5; gp.insets = new Insets(0, 8, 4, 8);
        paySection.add(hourlyRateError, gp);

        // --- Assemble all sections ---
        JPanel allSections = new JPanel();
        allSections.setLayout(new BoxLayout(allSections, BoxLayout.Y_AXIS));
        allSections.setBackground(Color.WHITE);
        allSections.setBorder(BorderFactory.createEmptyBorder(10, 14, 6, 14));
        allSections.add(infoSection);
        allSections.add(Box.createVerticalStrut(8));
        allSections.add(govSection);
        allSections.add(Box.createVerticalStrut(8));
        allSections.add(paySection);

        // --- Buttons ---
        JButton okButton     = new JButton("Add Employee");
        JButton cancelButton = new JButton("Cancel");

        UIHelper.styleButton(okButton,     UIHelper.GREEN, Color.WHITE,
                new Dimension(140, 32));
        UIHelper.styleButton(cancelButton, UIHelper.GRAY,  Color.BLACK,
                new Dimension(100, 32));

        JPanel dialogButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        dialogButtonPanel.setBorder(BorderFactory.createMatteBorder(
                1, 0, 0, 0, new Color(220, 220, 220)));
        dialogButtonPanel.add(cancelButton);
        dialogButtonPanel.add(okButton);

        addDialog.add(allSections,        BorderLayout.CENTER);
        addDialog.add(dialogButtonPanel,  BorderLayout.SOUTH);
        attachLiveValidation(idField, idError, text -> {
            if (text.isEmpty()) return null;
            if (!text.matches("\\d+")) return "Numbers only.";
            if (text.length() < 4 || text.length() > 10) return "Must be 4–10 digits.";
            return null;
        });
        attachLiveValidation(sssField, sssError, text -> {
            if (text.isEmpty()) return null;
            if (!text.matches("[0-9-]+")) return "Numbers and dashes only.";
            return null;
        });
        attachLiveValidation(philHealthField, philHealthError, text -> {
            if (text.isEmpty()) return null;
            if (!text.matches("[0-9-]+")) return "Numbers only.";
            return null;
        });
        attachLiveValidation(tinField, tinError, text -> {
            if (text.isEmpty()) return null;
            if (!text.matches("[0-9-]+")) return "Numbers and dashes only.";
            return null;
        });
        attachLiveValidation(pagIbigField, pagIbigError, text -> {
            if (text.isEmpty()) return null;
            if (!text.matches("[0-9-]+")) return "Numbers only.";
            return null;
        });
        attachLiveValidation(salaryField, salaryError, text -> {
            if (text.isEmpty()) return null;
            try {
                double v = Double.parseDouble(text);
                if (v <= 0) return "Must be greater than zero.";
            } catch (NumberFormatException ex) { return "Must be a valid number."; }
            return null;
        });
        attachLiveValidation(hourlyRateField, hourlyRateError, text -> {
            if (text.isEmpty()) return null;
            try {
                double v = Double.parseDouble(text);
                if (v <= 0) return "Must be greater than zero.";
            } catch (NumberFormatException ex) { return "Must be a valid number."; }
            return null;
        });

        addDialog.pack();
        addDialog.setMinimumSize(new Dimension(420, 560));
        addDialog.setLocationRelativeTo(frame);
        addDialog.setResizable(false);

        final boolean[] confirmed = {false};
        okButton.addActionListener(ev -> {
            confirmed[0] = true;
            addDialog.dispose();
        });
        cancelButton.addActionListener(ev -> {
            confirmed[0] = false;
            addDialog.dispose();
        });

        // Enter key in any field triggers the Add button
        ActionListener enterAction = e -> {
            confirmed[0] = true;
            addDialog.dispose();
        };
        idField.addActionListener(enterAction);
        firstNameField.addActionListener(enterAction);
        lastNameField.addActionListener(enterAction);
        sssField.addActionListener(enterAction);
        philHealthField.addActionListener(enterAction);
        tinField.addActionListener(enterAction);
        pagIbigField.addActionListener(enterAction);
        positionField.addActionListener(enterAction);
        salaryField.addActionListener(enterAction);
        hourlyRateField.addActionListener(enterAction);

        SwingUtilities.invokeLater(idField::requestFocusInWindow);

        while (true) {
            confirmed[0] = false;
            addDialog.setVisible(true); // blocks until disposed

            if (!confirmed[0]) return; // cancelled

            // Collect and trim all values — hint text is now purely visual overlay,
            // never real text, so the field's actual text is always the real value.
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
            if (isHintOrEmpty(idField, "e.g. 10001")
                    || isHintOrEmpty(firstNameField, "e.g. Juan")
                    || isHintOrEmpty(lastNameField, "e.g. dela Cruz")
                    || isHintOrEmpty(sssField, "e.g. 44-4506057-3")
                    || isHintOrEmpty(philHealthField, "e.g. 820126183514")
                    || isHintOrEmpty(tinField, "e.g. 442-605-657-000")
                    || isHintOrEmpty(pagIbigField, "e.g. 691295330870")
                    || isHintOrEmpty(positionField, "e.g. Rank and File")
                    || isHintOrEmpty(salaryField, "e.g. 40000.00")
                    || isHintOrEmpty(hourlyRateField, "e.g. 535.71")) {
                JOptionPane.showMessageDialog(frame,
                        "All fields are required. Please complete the form.",
                        "Incomplete Fields", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            // Employee # must be numeric per MPHCR01 ("numeric input field").
            // Length kept flexible (4-10 digits) rather than a fixed count, since
            // real ID schemes grow over time — but still catches obvious typos
            // like a 1-digit or 15-digit entry.
            if (!id.matches("\\d+")) {
                JOptionPane.showMessageDialog(frame,
                        "Employee # must contain numbers only (e.g., 10001).",
                        "Invalid Input", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if (id.length() < 4 || id.length() > 10) {
                JOptionPane.showMessageDialog(frame,
                        "Employee # must be between 4 and 10 digits.",
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
                outputArea.setText(
                    "Employee added successfully!\n"
                + "-----------------------------------\n"
                + "Employee # : " + newEmp.getEmployeeID()   + "\n"
                + "Name       : " + newEmp.getFullName()      + "\n"
                + "Position   : " + newEmp.getPosition()      + "\n"
                + "Hourly Rate: PHP " + String.format("%.2f", newEmp.getHourlyRate()) + "\n"
                + "-----------------------------------\n"
                + "Tip: Run 'Compute Salaries' in Payroll Menu to calculate their pay.");
            } else {
                outputArea.setText("Failed to add employee.\n"
                        + "The record could not be saved to the CSV file.\n"
                        + "Please check file permissions and try again.");
            }
            return;
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

        // Same auto-dash formatting used in Add Employee, so editing SSS/TIN
        // behaves consistently whether you're adding or updating.
        addAutoFormat(sssField,        "##-#######-#");
        addAutoFormat(philHealthField, "############");
        addAutoFormat(tinField,        "###-###-###-###");
        addAutoFormat(pagIbigField,    "############");

        JDialog updateDialog = new JDialog(frame, "Update Employee — " + employeeID, true);
        updateDialog.setLayout(new BorderLayout(0, 0));

        // Section 1 — Personal Info
        JPanel uInfoSection = new JPanel(new GridBagLayout());
        uInfoSection.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Employee Information",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 11),
                new Color(21, 101, 192)));
        uInfoSection.setBackground(Color.WHITE);

        GridBagConstraints ui = new GridBagConstraints();
        ui.insets = new Insets(4, 8, 4, 8);
        ui.anchor = GridBagConstraints.WEST;

        // ID shown as read-only label (not editable on update)
        ui.gridx = 0; ui.gridy = 0;
        uInfoSection.add(makeLabel("Employee #:"), ui);
        ui.gridx = 1; ui.fill = GridBagConstraints.HORIZONTAL; ui.weightx = 1.0;
        JLabel idReadOnly = new JLabel(employeeID + "  🔒");
        idReadOnly.setFont(new Font("Arial", Font.BOLD, 13));
        idReadOnly.setForeground(new Color(21, 101, 192));
        idReadOnly.setToolTipText("Employee # cannot be changed once created");
        uInfoSection.add(idReadOnly, ui);

        ui.gridx = 0; ui.gridy = 1; ui.fill = GridBagConstraints.NONE; ui.weightx = 0;
        uInfoSection.add(makeLabel("First Name:"), ui);
        ui.gridx = 1; ui.fill = GridBagConstraints.HORIZONTAL; ui.weightx = 1.0;
        uInfoSection.add(firstNameField, ui);

        ui.gridx = 0; ui.gridy = 2; ui.fill = GridBagConstraints.NONE; ui.weightx = 0;
        uInfoSection.add(makeLabel("Last Name:"), ui);
        ui.gridx = 1; ui.fill = GridBagConstraints.HORIZONTAL; ui.weightx = 1.0;
        uInfoSection.add(lastNameField, ui);

        // Section 2 — Government IDs
        JPanel uGovSection = new JPanel(new GridBagLayout());
        uGovSection.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Government IDs",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 11),
                new Color(21, 101, 192)));
        uGovSection.setBackground(Color.WHITE);

        GridBagConstraints ug = new GridBagConstraints();
        ug.insets = new Insets(4, 8, 4, 8);
        ug.anchor = GridBagConstraints.WEST;

        ug.gridx = 0; ug.gridy = 0;
        uGovSection.add(makeLabel("SSS #:"), ug);
        ug.gridx = 1; ug.fill = GridBagConstraints.HORIZONTAL; ug.weightx = 1.0;
        uGovSection.add(sssField, ug);

        ug.gridx = 0; ug.gridy = 1; ug.fill = GridBagConstraints.NONE; ug.weightx = 0;
        uGovSection.add(makeLabel("PhilHealth #:"), ug);
        ug.gridx = 1; ug.fill = GridBagConstraints.HORIZONTAL; ug.weightx = 1.0;
        uGovSection.add(philHealthField, ug);

        ug.gridx = 0; ug.gridy = 2; ug.fill = GridBagConstraints.NONE; ug.weightx = 0;
        uGovSection.add(makeLabel("TIN #:"), ug);
        ug.gridx = 1; ug.fill = GridBagConstraints.HORIZONTAL; ug.weightx = 1.0;
        uGovSection.add(tinField, ug);

        ug.gridx = 0; ug.gridy = 3; ug.fill = GridBagConstraints.NONE; ug.weightx = 0;
        uGovSection.add(makeLabel("Pag-IBIG #:"), ug);
        ug.gridx = 1; ug.fill = GridBagConstraints.HORIZONTAL; ug.weightx = 1.0;
        uGovSection.add(pagIbigField, ug);

        // Section 3 — Position & Pay
        JPanel uPaySection = new JPanel(new GridBagLayout());
        uPaySection.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Position & Pay",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 11),
                new Color(21, 101, 192)));
        uPaySection.setBackground(Color.WHITE);

        GridBagConstraints up = new GridBagConstraints();
        up.insets = new Insets(4, 8, 4, 8);
        up.anchor = GridBagConstraints.WEST;

        up.gridx = 0; up.gridy = 0;
        uPaySection.add(makeLabel("Position:"), up);
        up.gridx = 1; up.fill = GridBagConstraints.HORIZONTAL; up.weightx = 1.0;
        uPaySection.add(positionField, up);

        up.gridx = 0; up.gridy = 1; up.fill = GridBagConstraints.NONE; up.weightx = 0;
        uPaySection.add(makeLabel("Basic Salary:"), up);
        up.gridx = 1; up.fill = GridBagConstraints.HORIZONTAL; up.weightx = 1.0;
        uPaySection.add(salaryField, up);

        up.gridx = 0; up.gridy = 2; up.fill = GridBagConstraints.NONE; up.weightx = 0;
        uPaySection.add(makeLabel("Hourly Rate:"), up);
        up.gridx = 1; up.fill = GridBagConstraints.HORIZONTAL; up.weightx = 1.0;
        uPaySection.add(hourlyRateField, up);

        // Assemble
        JPanel uAllSections = new JPanel();
        uAllSections.setLayout(new BoxLayout(uAllSections, BoxLayout.Y_AXIS));
        uAllSections.setBackground(Color.WHITE);
        uAllSections.setBorder(BorderFactory.createEmptyBorder(10, 14, 6, 14));
        uAllSections.add(uInfoSection);
        uAllSections.add(Box.createVerticalStrut(8));
        uAllSections.add(uGovSection);
        uAllSections.add(Box.createVerticalStrut(8));
        uAllSections.add(uPaySection);

        JButton saveButton         = new JButton("Save Changes");
        JButton cancelUpdateButton = new JButton("Cancel");

        UIHelper.styleButton(saveButton,          UIHelper.BLUE, Color.WHITE,
                new Dimension(140, 32));
        UIHelper.styleButton(cancelUpdateButton,  UIHelper.GRAY, Color.BLACK,
                new Dimension(100, 32));

        JPanel updateButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        updateButtonPanel.setBorder(BorderFactory.createMatteBorder(
                1, 0, 0, 0, new Color(220, 220, 220)));
        updateButtonPanel.add(cancelUpdateButton);
        updateButtonPanel.add(saveButton);

        updateDialog.add(uAllSections,       BorderLayout.CENTER);
        updateDialog.add(updateButtonPanel,  BorderLayout.SOUTH);
        updateDialog.pack();
        updateDialog.setMinimumSize(new Dimension(400, 460));
        updateDialog.setLocationRelativeTo(frame);
        updateDialog.setResizable(false);

        final boolean[] updateConfirmed = {false};
        saveButton.addActionListener(ev -> {
            updateConfirmed[0] = true;
            updateDialog.dispose();
        });
        cancelUpdateButton.addActionListener(ev -> {
            updateConfirmed[0] = false;
            updateDialog.dispose();
        });

        SwingUtilities.invokeLater(firstNameField::requestFocusInWindow);

        while (true) {
            updateConfirmed[0] = false;
            updateDialog.setVisible(true);

            if (!updateConfirmed[0]) return; // cancelled

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

        if (emp != null) {
            // Show status BEFORE popup opens
            outputArea.setText("Viewing details for: "
                    + emp.getFullName()
                    + " (Employee #" + emp.getEmployeeID() + ")");

            JTextArea detailArea = new JTextArea(info);
            detailArea.setEditable(false);
            detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            detailArea.setMargin(new Insets(8, 10, 8, 10));
            detailArea.setBackground(new Color(250, 250, 252));

            JScrollPane detailScroll = new JScrollPane(detailArea);
            detailScroll.setPreferredSize(new Dimension(380, 280));

            JOptionPane.showMessageDialog(frame,
                    detailScroll,
                    "Employee Details — " + emp.getEmployeeID(),
                    JOptionPane.INFORMATION_MESSAGE);

            // Clear AFTER popup closes
            outputArea.setText("");
        } else {
            outputArea.setText("Employee not found. Please refresh and try again.");
        }
    }

    /**
     * Reads and displays attendance records for the selected employee in
     * an editable table — clicking a row lets the admin edit or delete
     * that specific record via the buttons below the table.
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

        outputArea.setText("Viewing attendance for: " + fullName);

        JDialog attDialog = new JDialog(frame, "Attendance Records — " + fullName, true);
        attDialog.setLayout(new BorderLayout(8, 8));

        String[] columns = {"Date", "Log In", "Log Out", "Hours"};
        DefaultTableModel attModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        Runnable[] reloadHolder = new Runnable[1];
        reloadHolder[0] = () -> {
            attModel.setRowCount(0);
            List<String[]> records = CSVHandler.readAttendanceForEmployee(
                    PayrollService.ATTENDANCE_FILE, employeeID);
            for (String[] r : records) {
                attModel.addRow(new Object[]{r[0], r[1], r[2], r[3]});
            }
        };
        reloadHolder[0].run();

        JTable attTable = new JTable(attModel);
        attTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        attTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane attScroll = new JScrollPane(attTable);
        attScroll.setPreferredSize(new Dimension(420, 260));

        JButton editAttBtn   = new JButton("Edit Selected");
        JButton deleteAttBtn = new JButton("Delete Selected");
        JButton closeBtn     = new JButton("Close");
        UIHelper.styleButton(editAttBtn,   UIHelper.BLUE, Color.WHITE, new Dimension(130, 30));
        UIHelper.styleButton(deleteAttBtn, UIHelper.RED,  Color.WHITE, new Dimension(130, 30));
        UIHelper.styleButton(closeBtn,     UIHelper.GRAY, Color.BLACK, new Dimension(90, 30));

        editAttBtn.addActionListener(e -> {
            int row = attTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(attDialog,
                        "Please select an attendance record first.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String originalDate   = (String) attModel.getValueAt(row, 0);
            String originalLogIn  = (String) attModel.getValueAt(row, 1);
            String originalLogOut = (String) attModel.getValueAt(row, 2);
            editAttendanceRecord(attDialog, employeeID, originalDate,
                    originalLogIn, originalLogOut, reloadHolder[0]);
        });

        deleteAttBtn.addActionListener(e -> {
            int row = attTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(attDialog,
                        "Please select an attendance record first.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String date = (String) attModel.getValueAt(row, 0);

            int confirm = JOptionPane.showConfirmDialog(attDialog,
                    "Delete the attendance record for " + date + "?\nThis cannot be undone.",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            boolean deleted = CSVHandler.deleteAttendance(
                    PayrollService.ATTENDANCE_FILE, employeeID, date);

            if (deleted) {
                AuditLogger.log("DELETE ATTENDANCE", "Employee #" + employeeID + " | Date: " + date);
                reloadHolder[0].run();
                outputArea.setText("Attendance record (" + date + ") deleted for " + fullName
                        + ".\nRun 'Compute Salaries' in Payroll Menu to update pay.");
            } else {
                JOptionPane.showMessageDialog(attDialog,
                        "Failed to delete the record. Please try again.",
                        "Delete Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        closeBtn.addActionListener(e -> attDialog.dispose());

        JPanel attBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        attBtnPanel.add(editAttBtn);
        attBtnPanel.add(deleteAttBtn);
        attBtnPanel.add(closeBtn);

        attDialog.add(attScroll,   BorderLayout.CENTER);
        attDialog.add(attBtnPanel, BorderLayout.SOUTH);
        attDialog.pack();
        attDialog.setMinimumSize(new Dimension(460, 360));
        attDialog.setLocationRelativeTo(frame);
        attDialog.setVisible(true);

        outputArea.setText("");
    }

    private void editAttendanceRecord(JDialog parent, String employeeID,
            String originalDate, String originalLogIn, String originalLogOut,
            Runnable onSaved) {

        String newDate = showDatePicker(originalDate);
        if (newDate == null) return;

        String newLogIn = showTimePicker("Select New Log In Time", originalLogIn);
        if (newLogIn == null) return;

        String newLogOut = showTimePicker("Select New Log Out Time", originalLogOut);
        if (newLogOut == null) return;

        java.time.LocalTime timeIn, timeOut;
        try {
            timeIn  = java.time.LocalTime.parse(newLogIn,
                    java.time.format.DateTimeFormatter.ofPattern("H:mm"));
            timeOut = java.time.LocalTime.parse(newLogOut,
                    java.time.format.DateTimeFormatter.ofPattern("H:mm"));
        } catch (java.time.format.DateTimeParseException ex) {
            JOptionPane.showMessageDialog(parent,
                    "Invalid time selected. Please try again.",
                    "Invalid Time", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!timeOut.isAfter(timeIn)) {
            JOptionPane.showMessageDialog(parent,
                    "Log Out time must be later than Log In time.",
                    "Invalid Time Range", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // If the date changed, make sure it doesn't collide with another
        // existing record for this employee (same duplicate-date rule as Add).
        if (!newDate.equals(originalDate)) {
            List<String[]> existing = CSVHandler.readAttendanceForEmployee(
                    PayrollService.ATTENDANCE_FILE, employeeID);
            boolean collides = existing.stream().anyMatch(r -> r[0].equals(newDate));
            if (collides) {
                JOptionPane.showMessageDialog(parent,
                        "An attendance record for " + newDate + " already exists.\n"
                        + "Please choose a different date.",
                        "Duplicate Entry", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        boolean updated = CSVHandler.updateAttendance(
                PayrollService.ATTENDANCE_FILE, employeeID, originalDate,
                newDate, newLogIn, newLogOut);

        if (updated) {
            AuditLogger.log("UPDATE ATTENDANCE", "Employee #" + employeeID
                    + " | " + originalDate + " -> " + newDate
                    + " | " + newLogIn + " - " + newLogOut);
            onSaved.run();
            outputArea.setText("Attendance record updated for Employee #" + employeeID
                    + ".\nRun 'Compute Salaries' in Payroll Menu to update pay.");
        } else {
            JOptionPane.showMessageDialog(parent,
                    "Failed to update the record. Please try again.",
                    "Update Error", JOptionPane.ERROR_MESSAGE);
        }
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

        // Use pickers instead of manual text entry
        String date   = showDatePicker();
        if (date == null) return; // user cancelled date picker

        String logIn  = showTimePicker("Select Log In Time");
        if (logIn == null) return; // user cancelled

        String logOut = showTimePicker("Select Log Out Time");
        if (logOut == null) return; // user cancelled

        // Parse and validate times
        java.time.LocalTime timeIn, timeOut;
        try {
            timeIn  = java.time.LocalTime.parse(logIn,
                    java.time.format.DateTimeFormatter.ofPattern("H:mm"));
            timeOut = java.time.LocalTime.parse(logOut,
                    java.time.format.DateTimeFormatter.ofPattern("H:mm"));
        } catch (java.time.format.DateTimeParseException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Invalid time selected. Please try again.",
                    "Invalid Time", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!timeOut.isAfter(timeIn)) {
            JOptionPane.showMessageDialog(frame,
                    "Log Out time must be later than Log In time.\n"
                    + "Log In: " + logIn + "  |  Log Out: " + logOut,
                    "Invalid Time Range", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Duplicate date check — prevent two entries for the same employee on the same day
        List<String[]> existing = CSVHandler.readAttendanceForEmployee(
                PayrollService.ATTENDANCE_FILE, employeeID);
        boolean duplicateDate = existing.stream()
                .anyMatch(r -> r[0].equals(date));

        if (duplicateDate) {
            JOptionPane.showMessageDialog(frame,
                    "An attendance record for " + date + " already exists\n"
                    + "for this employee. Duplicate entries are not allowed.",
                    "Duplicate Entry", JOptionPane.WARNING_MESSAGE);
            return; // can't change date without re-opening pickers, so just exit
        }

            boolean added = CSVHandler.appendAttendance(
                PayrollService.ATTENDANCE_FILE, employeeID,
                lastName, firstName, date, logIn, logOut);

        if (added) {
            AuditLogger.log("ADD ATTENDANCE", "Employee #" + employeeID
                    + " | Date: " + date + " | " + logIn + " – " + logOut);
            outputArea.setText("Attendance record added for " + firstName + " " + lastName
                    + ".\nRun 'Compute Salaries' in Payroll Menu to update pay using the new hours.");
        } else {
            outputArea.setText    ("Failed to add attendance record.\n"
                    + "Please check file permissions and try again.");
        }
    }
}
