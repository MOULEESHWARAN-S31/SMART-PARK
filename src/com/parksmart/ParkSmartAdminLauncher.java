package com.parksmart;

import java.sql.Connection;
import javax.swing.*;

/**
 * Bridge launched from ParkSmartApp when a Watchman logs in.
 * Uses reflection so the compiler does not need ParkSmartAdmin
 * at compile time — it only needs to be on the runtime classpath.
 */
public class ParkSmartAdminLauncher {

    public ParkSmartAdminLauncher(Connection dbConnection) {
        SwingUtilities.invokeLater(() -> {
            try {
                Class<?> cls = Class.forName("ParkSmartAdmin");
                cls.getDeclaredConstructor(Connection.class).newInstance(dbConnection);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                    "Cannot open Admin Panel.\n" +
                    "Ensure ParkSmartAdmin.class is in the classpath.\n\nError: " + ex.getMessage(),
                    "Admin Launch Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
