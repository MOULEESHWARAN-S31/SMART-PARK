package com.parksmart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Shared Database store for Exit OTPs.
 */
public class ExitOtpStore {

    private static void ensureTable(Connection conn) {
        if (conn == null) return;
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS exit_otps (slot_id VARCHAR(10) PRIMARY KEY, otp VARCHAR(10))");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void put(Connection conn, String slotId, String otp) {
        ensureTable(conn);
        if (conn == null) return;
        try {
            String sql = "INSERT INTO exit_otps (slot_id, otp) VALUES (?, ?) ON DUPLICATE KEY UPDATE otp = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, slotId.toUpperCase());
                ps.setString(2, otp);
                ps.setString(3, otp);
                ps.executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static String get(Connection conn, String slotId) {
        ensureTable(conn);
        if (conn == null) return null;
        try {
            String sql = "SELECT otp FROM exit_otps WHERE slot_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, slotId.toUpperCase());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getString("otp");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public static void remove(Connection conn, String slotId) {
        ensureTable(conn);
        if (conn == null) return;
        try {
            String sql = "DELETE FROM exit_otps WHERE slot_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, slotId.toUpperCase());
                ps.executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static boolean has(Connection conn, String slotId) {
        return get(conn, slotId) != null;
    }
}
