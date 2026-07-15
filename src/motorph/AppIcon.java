package motorph;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Utility class that creates the application's shared icon
 * for consistent branding across all GUI windows.
 */

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