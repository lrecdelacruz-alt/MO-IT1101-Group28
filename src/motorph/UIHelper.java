package motorph;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;

// ============================================================
// UI HELPER UTILITY
// Centralizes button styling so the same color conventions
// are applied consistently across all windows without
// duplicating the same 6-line styling block everywhere.
// ============================================================
public class UIHelper {

    /**
     * Applies standard MotorPH button styling.
     * @param button     The button to style
     * @param bg         Background color
     * @param fg         Foreground (text) color
     * @param size       Preferred size
     */
    public static void styleButton(JButton button, Color bg, Color fg, Dimension size) {
        button.setPreferredSize(size);
        button.setFont(new Font("Arial", Font.PLAIN, 13));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createBevelBorder(
                BevelBorder.RAISED, bg.brighter(), bg.darker()));
    }

    // Shared color constants so every window uses identical shades
    public static final Color GREEN   = new Color(46, 125, 50);
    public static final Color BLUE    = new Color(11, 24, 73);
    public static final Color RED     = new Color(128, 0, 0);
    public static final Color AMBER   = new Color(241, 109, 52);
    public static final Color GRAY    = new Color(190, 195, 200);
    public static final Color COMPUTE = new Color(11, 24, 73);
}
