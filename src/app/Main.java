package app;

import ui.LoginFrame;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        // Try setting system Look & Feel (universal, safe)
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Launch Login UI on Event Dispatch Thread
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}
