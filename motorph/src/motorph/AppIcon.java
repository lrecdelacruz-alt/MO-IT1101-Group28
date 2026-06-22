package motorph;

import java.awt.*;
import java.awt.image.BufferedImage;

// ============================================================
// APP ICON UTILITY
// Builds the shared 32x32 programmatic icon used by every window
// (Main Menu, Employee Menu, Payroll Menu) so the whole app has one
// consistent visual identity, without needing an external image file.
// Centralized here so the drawing code isn't duplicated in three
// different classes.
// ============================================================
public class AppIcon {
    public static Image create() {
        BufferedImage icon =
            new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setColor(new Color(21, 101, 192));
        g.fillOval(2, 2, 28, 28);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("M", 10, 22);
        g.dispose();
        return icon;
    }
}