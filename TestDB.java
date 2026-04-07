import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestDB {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3307/parksmart_db", "root", "");
        
        System.out.println("Current status in DB before update:");
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM bookings");
             ResultSet rs = ps.executeQuery()) {
            while(rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + " Slot: " + rs.getString("slot_id") + " Status: " + rs.getString("status"));
            }
        }
        
        System.out.println("\nTrying UPDATE bookings SET status='done' WHERE id=10 (or whatever ID is active)");
        try (PreparedStatement ps = conn.prepareStatement("UPDATE bookings SET status='done' WHERE status='confirmed' OR status='overdue'")) {
            int rows = ps.executeUpdate();
            System.out.println("Rows updated: " + rows);
        }
    }
}
