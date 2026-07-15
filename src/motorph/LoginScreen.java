package motorph;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.*;

// ============================================================
// LOGIN SCREEN
// Provides a branded, dedicated login window for the MotorPH
// Payroll System. Replaces the plain JOptionPane login dialogs
// with a proper GUI that matches the rest of the application.
//
// Responsibilities:
//   - Display MotorPH branding and app title
//   - Accept username and password input
//   - Show/hide password toggle
//   - Validate credentials via AuthService
//   - Route to correct menu based on role
//   - Show inline error messages without popups
// ============================================================
public class LoginScreen {

    private JFrame frame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel errorLabel;
    private JCheckBox showPasswordCheckbox;

    private final PayrollService payrollService;
    private final AuthService authService;

    public LoginScreen(PayrollService payrollService, AuthService authService) {
        this.payrollService = payrollService;
        this.authService    = authService;
    }

    // --------------------------------------------------------
    // MAIN DISPLAY METHOD
    // --------------------------------------------------------

    public void display() {

        frame = new JFrame("MotorPH Payroll System — Login");
        frame.setIconImage(AppIcon.create());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setResizable(false);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showConfirmDialog(frame,
                        "Are you sure you want to exit?",
                        "Exit Confirmation",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) System.exit(0);
            }
        });

        // --------------------------------------------------------
        // HEADER — branding panel
        // --------------------------------------------------------
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIHelper.BLUE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));

        // App icon drawn as a label
        JLabel iconLabel;
        java.net.URL logoUrl = getClass().getResource("/motorph/resources/logo.png");
        if (logoUrl != null) {
            ImageIcon rawIcon = new ImageIcon(logoUrl);
            ImageIcon scaledIcon = new ImageIcon(
                    rawIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH));
            final Image finalScaledLogo = scaledIcon.getImage();

            iconLabel = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    // White circle backing so the logo is visible against the navy header
                    g2.setColor(Color.WHITE);
                    g2.fillOval(0, 0, 48, 48);
                    // Draw the transparent logo centered on top of the white circle
                    g2.drawImage(finalScaledLogo, 4, 4, null);
                }
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(48, 48);
                }
            };
        } else {
            iconLabel = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.WHITE);
                    g2.fillOval(0, 0, 48, 48);
                    g2.setColor(UIHelper.BLUE);
                    g2.setFont(new Font("Arial", Font.BOLD, 26));
                    g2.drawString("M", 13, 34);
                }
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(48, 48);
                }
            };
        }

        JLabel appTitle = new JLabel("MotorPH Payroll System");
        appTitle.setFont(new Font("Arial", Font.BOLD, 22));
        appTitle.setForeground(Color.WHITE);

        JLabel appSubtitle = new JLabel("Employee Management & Payroll Processing");
        appSubtitle.setFont(new Font("Arial", Font.PLAIN, 12));
        appSubtitle.setForeground(new Color(180, 210, 255));

        JPanel titleTextPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titleTextPanel.setOpaque(false);
        titleTextPanel.add(appTitle);
        titleTextPanel.add(appSubtitle);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        titleRow.setOpaque(false);
        titleRow.add(iconLabel);
        titleRow.add(titleTextPanel);

        headerPanel.add(titleRow, BorderLayout.CENTER);

        // --------------------------------------------------------
        // FORM PANEL — login fields
        // --------------------------------------------------------
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(28, 36, 10, 36));
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.weightx = 1.0;

        // Username label
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(new Font("Arial", Font.BOLD, 13));
        usernameLabel.setForeground(new Color(60, 60, 60));
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(usernameLabel, gbc);

        // Username field
        usernameField = new JTextField(22);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 13));
        usernameField.setPreferredSize(new Dimension(280, 36));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        gbc.gridy = 1;
        formPanel.add(usernameField, gbc);

        // Password label
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("Arial", Font.BOLD, 13));
        passwordLabel.setForeground(new Color(60, 60, 60));
        gbc.gridy = 2;
        formPanel.add(passwordLabel, gbc);

        // Password field
        passwordField = new JPasswordField(22);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 13));
        passwordField.setPreferredSize(new Dimension(280, 36));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        gbc.gridy = 3;
        formPanel.add(passwordField, gbc);

        // Show password checkbox
        showPasswordCheckbox = new JCheckBox("Show Password");
        showPasswordCheckbox.setFont(new Font("Arial", Font.PLAIN, 12));
        showPasswordCheckbox.setForeground(new Color(100, 100, 100));
        showPasswordCheckbox.setOpaque(false);
        showPasswordCheckbox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showPasswordCheckbox.addActionListener(e -> {
            if (showPasswordCheckbox.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('•');
            }
        });
        gbc.gridy = 4;
        gbc.insets = new Insets(2, 0, 6, 0);
        formPanel.add(showPasswordCheckbox, gbc);

        // Error label — shown inline instead of popup
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        errorLabel.setForeground(UIHelper.RED);
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 4, 0);
        formPanel.add(errorLabel, gbc);

        // --------------------------------------------------------
        // BUTTON PANEL
        // --------------------------------------------------------
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JButton loginButton = new JButton("Login");
        JButton exitButton  = new JButton("Exit");

        Color loginColor = UIHelper.BLUE;
        loginButton.setBackground(loginColor);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Arial", Font.BOLD, 13));
        loginButton.setBorderPainted(true);
        loginButton.setFocusPainted(false);
        loginButton.setOpaque(true);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, loginColor.brighter(), loginColor.darker()));
        loginButton.setPreferredSize(new Dimension(120, 36));

        Color exitColor = UIHelper.RED;
        exitButton.setBackground(exitColor);
        exitButton.setForeground(Color.WHITE);
        exitButton.setFont(new Font("Arial", Font.PLAIN, 13));
        exitButton.setBorderPainted(true);
        exitButton.setFocusPainted(false);
        exitButton.setOpaque(true);
        exitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exitButton.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, exitColor.brighter(), exitColor.darker()));
        exitButton.setPreferredSize(new Dimension(120, 36));

        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton);

        gbc.gridy = 6;
        gbc.insets = new Insets(8, 0, 0, 0);
        formPanel.add(buttonPanel, gbc);

        // --------------------------------------------------------
        // FOOTER — course info + credential hints for graders
        // --------------------------------------------------------
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(new Color(245, 247, 250));
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0,
                        new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));

        JLabel courseLabel = new JLabel(
                "MO-IT103 | Group 28 | H1101",
                SwingConstants.LEFT);
        courseLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        courseLabel.setForeground(new Color(150, 150, 150));

        footerPanel.add(courseLabel, BorderLayout.WEST);

        // --------------------------------------------------------
        // ASSEMBLE
        // --------------------------------------------------------
        JPanel bodyPanel = new JPanel(new BorderLayout());
        bodyPanel.setBackground(Color.WHITE);
        bodyPanel.add(formPanel, BorderLayout.CENTER);

        frame.add(headerPanel, BorderLayout.NORTH);
        frame.add(bodyPanel,   BorderLayout.CENTER);
        frame.add(footerPanel, BorderLayout.SOUTH);

        // --------------------------------------------------------
        // EVENT LISTENERS
        // --------------------------------------------------------

        // Allow pressing Enter in either field to trigger login
        ActionListener loginAction = e -> attemptLogin();
        usernameField.addActionListener(loginAction);
        passwordField.addActionListener(loginAction);

        loginButton.addActionListener(e -> attemptLogin());

        exitButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Are you sure you want to exit?",
                    "Exit Confirmation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) System.exit(0);
        });

        // --------------------------------------------------------
        // SHOW
        // --------------------------------------------------------
        frame.pack();
        frame.setMinimumSize(new Dimension(400, 420));
        frame.setLocationRelativeTo(null);

        // Auto-focus username field
        SwingUtilities.invokeLater(() -> usernameField.requestFocusInWindow());

        frame.setVisible(true);
    }

    // --------------------------------------------------------
    // LOGIN LOGIC
    // --------------------------------------------------------

    /**
     * Validates credentials and routes to the correct menu.
     * Shows inline error messages instead of popup dialogs
     * so the user stays in context and doesn't lose their input.
     */
    private void attemptLogin() {

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        // Clear previous error
        errorLabel.setText(" ");

        if (username.isEmpty()) {
            showError("Username cannot be empty.");
            usernameField.requestFocusInWindow();
            return;
        }

        if (password.isEmpty()) {
            showError("Password cannot be empty.");
            passwordField.requestFocusInWindow();
            return;
        }

        if (!authService.login(username, password)) {
            showError("Invalid username or password. Please try again.");
            passwordField.setText("");
            usernameField.requestFocusInWindow();
            return;
        }

        // Success — close login window and route
        frame.dispose();

        JOptionPane.showMessageDialog(null,
                "Welcome! Login successful.",
                "MotorPH Login", JOptionPane.INFORMATION_MESSAGE);

        String role = authService.getLoggedInRole();
        if (role.equals("admin")) {
            showAdminMenu();
        } else {
            showEmployeeFlow();
        }
    }

    private void showError(String message) {
        errorLabel.setText("⚠ " + message);
    }

    private void showAdminMenu() {
        // Delegate back to Main static methods via a thin bridge
        Main.launchMainMenu(payrollService, authService);
    }

    private void showEmployeeFlow() {
        Main.launchEmployeeFlow(payrollService, authService);
    }
}