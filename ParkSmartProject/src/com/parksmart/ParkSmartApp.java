package com.parksmart;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class ParkSmartApp extends JFrame {
    private static final Color BG = new Color(240, 244, 250);
    private static final Color CARD = Color.WHITE;
    private static final Color BORDER = new Color(220, 229, 240);
    private static final Color FREE_COLOR = new Color(26, 115, 232);
    private static final Color OCC_COLOR = new Color(229, 57, 53);
    private static final Color MUTED = new Color(107, 124, 153);
    private static final Font HEAD_FONT = new Font("Syne", Font.BOLD, 24);
    private static final Font BODY_FONT = new Font("DM Sans", Font.PLAIN, 13);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final String[] SLOT_IDS = {"A1","A2","A3","A4","B1","B2","B3","B4","C1","C2","C3","C4","D1","D2","D3","D4"};

    private CardLayout rootCardLayout;
    private JPanel rootPanel;

    private JPanel loginPanel;
    private JTextField mobileField;
    private JTextField otpField;
    private JLabel otpStatus;
    private JLabel loginStatus;

    private JPanel homePanel;
    private JLabel userInfoLabel;
    private JLabel selectedDateLabel;
    private JPanel slotGrid;
    private JPanel calendarDaysGrid;
    private JTable historyTable;
    private HistoryModel historyModel;

    private LocalDate currentMonth;
    private LocalDate selectedDate;

    private Connection dbConnection;
    private Map<String, OtpEntry> otpStore = new HashMap<>();
    private String loggedInMobile;
    private ActiveBooking activeBooking;
    private String exitOtp;

    public ParkSmartApp() {
        super("ParkSmart – Hotel Parking");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 820);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(BG);

        try {
            initDatabase();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
            System.exit(1);
        }

        // Initialize dates before creating UI components
        currentMonth = LocalDate.now();
        selectedDate = LocalDate.now();

        rootCardLayout = new CardLayout();
        rootPanel = new JPanel(rootCardLayout);

        initLoginPanel();
        initHomePanel();

        rootPanel.add(loginPanel, "login");
        rootPanel.add(homePanel, "home");
        getContentPane().add(rootPanel);

        initDemoBookings();

        showLogin();
    }

    private void initDatabase() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found");
        }

        dbConnection = DriverManager.getConnection("jdbc:sqlite:parksmart.db");
        createTables();
    }

    private void createTables() throws SQLException {
        try (Statement stmt = dbConnection.createStatement()) {
            // Users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    mobile TEXT PRIMARY KEY,
                    name TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Bookings table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bookings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    mobile TEXT,
                    slot_id TEXT,
                    booking_date TEXT,
                    entry_time TEXT,
                    exit_time TEXT,
                    vehicle TEXT,
                    status TEXT DEFAULT 'confirmed',
                    amount REAL DEFAULT 120.0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (mobile) REFERENCES users(mobile)
                )
            """);

            // Insert demo user if not exists
            stmt.execute("""
                INSERT OR IGNORE INTO users (mobile, name) VALUES
                ('9876543210', 'Demo User')
            """);
        }
    }

    private void initLoginPanel() {
        loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(BG);

        JPanel card = new RoundedPanel(24, CARD);
        card.setPreferredSize(new Dimension(460, 450));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("ParkSmart");
        title.setFont(new Font("Syne", Font.BOLD, 46));
        title.setForeground(FREE_COLOR);

        JLabel subtitle = new JLabel("Hotel Parking Management System");
        subtitle.setFont(new Font("DM Sans", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);

        JLabel info = new JLabel("Step 1: Enter mobile, get OTP. OTP expires in 3 minutes.");
        info.setFont(BODY_FONT);
        info.setForeground(MUTED);

        mobileField = new JTextField();
        mobileField.setFont(BODY_FONT);
        mobileField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JButton sendOtp = new JButton("Get OTP");
        sendOtp.setBackground(FREE_COLOR);
        sendOtp.setForeground(Color.WHITE);
        sendOtp.setFocusPainted(false);
        sendOtp.addActionListener(e -> sendOtpAction());

        otpField = new JTextField();
        otpField.setFont(BODY_FONT);
        otpField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JButton verifyOtp = new JButton("Verify OTP");
        verifyOtp.setBackground(new Color(12, 131, 70));
        verifyOtp.setForeground(Color.WHITE);
        verifyOtp.setFocusPainted(false);
        verifyOtp.addActionListener(e -> verifyOtpAction());

        otpStatus = new JLabel(" ");
        otpStatus.setFont(BODY_FONT);

        loginStatus = new JLabel(" ");
        loginStatus.setFont(BODY_FONT);

        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(subtitle);
        card.add(Box.createRigidArea(new Dimension(0, 20)));
        card.add(info);
        card.add(Box.createRigidArea(new Dimension(0, 16)));
        card.add(new JLabel("Mobile Number"));
        card.add(mobileField);
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        card.add(sendOtp);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(otpStatus);
        card.add(Box.createRigidArea(new Dimension(0, 16)));
        card.add(new JLabel("Enter OTP"));
        card.add(otpField);
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        card.add(verifyOtp);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(loginStatus);

        loginPanel.add(card);
    }

    private void initHomePanel() {
        homePanel = new JPanel(new BorderLayout());
        homePanel.setBackground(BG);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel logo = new JLabel("Park");
        logo.setFont(new Font("Syne", Font.BOLD, 30));
        JLabel logo2 = new JLabel("Smart");
        logo2.setFont(new Font("Syne", Font.BOLD, 30));
        logo2.setForeground(FREE_COLOR);

        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoPanel.setOpaque(false);
        logoPanel.add(logo);
        logoPanel.add(logo2);

        userInfoLabel = new JLabel("Not logged in");
        userInfoLabel.setFont(BODY_FONT);
        userInfoLabel.setForeground(MUTED);

        header.add(logoPanel, BorderLayout.WEST);
        header.add(userInfoLabel, BorderLayout.EAST);

        // center content
        JPanel center = new JPanel(new BorderLayout(20, 20));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JPanel topText = new JPanel();
        topText.setOpaque(false);
        topText.setLayout(new BoxLayout(topText, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Hotel Parking");
        title.setFont(new Font("Syne", Font.BOLD, 34));
        title.setForeground(TEXT_COLOR());
        JLabel subtitle = new JLabel("Select date → pick slot → book.");
        subtitle.setFont(BODY_FONT);
        subtitle.setForeground(MUTED);
        topText.add(title);
        topText.add(subtitle);

        center.add(topText, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(1, 2, 18, 0));
        content.setOpaque(false);

        content.add(createCalendarCard());
        content.add(createSlotCard());

        center.add(content, BorderLayout.CENTER);

        historyModel = new HistoryModel();
        historyTable = new JTable(historyModel);
        historyTable.setFillsViewportHeight(true);
        historyTable.setRowHeight(26);
        JScrollPane historyScroll = new JScrollPane(historyTable);
        historyScroll.setBorder(new LineBorder(BORDER));

        JPanel historyCard = new RoundedPanel(18, CARD);
        historyCard.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER), BorderFactory.createEmptyBorder(12,12,12,12)));
        historyCard.setLayout(new BorderLayout(8,8));
        JLabel historyTitle = new JLabel("🗂 Booking History");
        historyTitle.setFont(new Font("Syne", Font.BOLD, 16));
        JButton clearHistory = new JButton("Clear History");
        clearHistory.setFont(BODY_FONT);
        clearHistory.addActionListener(e -> {
            try {
                clearHistoryFromDB();
                historyModel.clear();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error clearing history: " + ex.getMessage());
            }
        });
        JPanel histTop = new JPanel(new BorderLayout()); histTop.setOpaque(false);
        histTop.add(historyTitle, BorderLayout.WEST);
        histTop.add(clearHistory, BorderLayout.EAST);
        historyCard.add(histTop, BorderLayout.NORTH);
        historyCard.add(historyScroll, BorderLayout.CENTER);

        homePanel.add(header, BorderLayout.NORTH);
        homePanel.add(center, BorderLayout.CENTER);
        homePanel.add(historyCard, BorderLayout.SOUTH);

        loadHistoryFromDB();
    }

    private JPanel createCalendarCard() {
        RoundedPanel cal = new RoundedPanel(18, CARD);
        cal.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER), BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        cal.setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JButton prev = navButton("<");
        JButton next = navButton(">");
        JLabel monthLabel = new JLabel(); monthLabel.setFont(new Font("Syne", Font.BOLD, 16)); monthLabel.setHorizontalAlignment(JLabel.CENTER);
        prev.addActionListener(e -> { currentMonth = currentMonth.minusMonths(1); refreshCalendarCards(monthLabel); });
        next.addActionListener(e -> { currentMonth = currentMonth.plusMonths(1); refreshCalendarCards(monthLabel); });
        top.add(prev, BorderLayout.WEST); top.add(monthLabel, BorderLayout.CENTER); top.add(next, BorderLayout.EAST);

        calendarDaysGrid = new JPanel(new GridLayout(7,7,4,4));
        calendarDaysGrid.setOpaque(false);
        for (String d : new String[]{"SUN","MON","TUE","WED","THU","FRI","SAT"}) {
            JLabel lbl = new JLabel(d, JLabel.CENTER);
            lbl.setFont(new Font("DM Sans", Font.BOLD, 10));
            lbl.setForeground(MUTED);
            calendarDaysGrid.add(lbl);
        }
        for (int i = 0; i < 42; i++) {
            JLabel cell = new JLabel("", JLabel.CENTER);
            cell.setFont(new Font("DM Sans", Font.BOLD, 13));
            cell.setOpaque(true);
            cell.setBackground(new Color(248, 250, 253));
            cell.setBorder(new LineBorder(BORDER));
            calendarDaysGrid.add(cell);
        }

        cal.add(top, BorderLayout.NORTH);
        cal.add(calendarDaysGrid, BorderLayout.CENTER);

        selectedDateLabel = new JLabel();
        selectedDateLabel.setFont(new Font("Syne", Font.BOLD, 16));
        selectedDateLabel.setBorder(BorderFactory.createEmptyBorder(4,0,0,0));
        cal.add(selectedDateLabel, BorderLayout.SOUTH);

        refreshCalendarCards(monthLabel);

        return cal;
    }

    private void refreshCalendarCards(JLabel monthLabel) {
        monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        selectedDateLabel.setText("Selected: " + selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));

        if (calendarDaysGrid == null) return;

        Component[] gridComps = calendarDaysGrid.getComponents();
        int startDay = currentMonth.withDayOfMonth(1).getDayOfWeek().getValue() % 7;
        int len = currentMonth.lengthOfMonth();

        for (int i = 7; i < gridComps.length; i++) { // Skip day labels
            int cellIndex = i - 7;
            JLabel cell = (JLabel) gridComps[i];
            if (cellIndex < startDay || cellIndex >= startDay + len) {
                cell.setText("");
                cell.setBackground(new Color(248, 250, 253));
                cell.setCursor(Cursor.getDefaultCursor());
                for (MouseListener ml : cell.getMouseListeners()) {
                    cell.removeMouseListener(ml);
                }
            } else {
                int day = cellIndex - startDay + 1;
                cell.setText(String.valueOf(day));
                LocalDate date = currentMonth.withDayOfMonth(day);
                cell.setBackground(date.equals(LocalDate.now()) ? FREE_COLOR : new Color(235, 245, 255));
                cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                for (MouseListener ml : cell.getMouseListeners()) {
                    cell.removeMouseListener(ml);
                }
                cell.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        selectedDate = date;
                        updateSlotGrid();
                        selectedDateLabel.setText("Selected: " + date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                    }
                });
            }
        }

        if (slotGrid != null) {
            updateSlotGrid();
        }
    }

    private JPanel createSlotCard() {
        RoundedPanel slotCard = new RoundedPanel(18, CARD);
        slotCard.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER), BorderFactory.createEmptyBorder(14,14,14,14)));
        slotCard.setLayout(new BorderLayout(6,6));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setOpaque(false);
        top.add(new JLabel("Available"));
        top.add(colorDot(FREE_COLOR));
        top.add(new JLabel("Occupied"));
        top.add(colorDot(OCC_COLOR));
        slotCard.add(top, BorderLayout.NORTH);

        slotGrid = new JPanel(new GridLayout(4,4,10,10));
        slotGrid.setOpaque(false);

        for (String id : SLOT_IDS) {
            JButton b = new JButton(id);
            b.setFont(new Font("Syne", Font.BOLD, 15));
            b.setFocusPainted(false);
            b.addActionListener(e -> onSlotClick(id));
            slotGrid.add(b);
        }

        slotCard.add(slotGrid, BorderLayout.CENTER);

        return slotCard;
    }

    private JPanel colorDot(Color color){ JPanel dot = new JPanel(); dot.setBackground(color); dot.setPreferredSize(new Dimension(12,12)); dot.setBorder(new LineBorder(color)); dot.setOpaque(true); return dot; }
    private JButton navButton(String label){ JButton b=new JButton(label); b.setFocusPainted(false); b.setBackground(Color.WHITE); b.setBorder(new LineBorder(BORDER)); return b; }

    private void initDemoBookings() {
        try {
            LocalDate today = LocalDate.now();
            addBookingToDB(today, "A1", "09:00 AM", "11:00 AM", "Demo", "TN 12 XX 1234");
            addBookingToDB(today, "A3", "10:30 AM", "02:30 PM", "Demo", "TN 12 XX 1234");
            addBookingToDB(today.plusDays(1), "A4", "11:00 AM", "02:00 PM", "Demo", "TN 12 XX 1234");
            addBookingToDB(today.plusDays(2), "A7", "03:00 PM", "06:00 PM", "Demo", "TN 12 XX 1234");
        } catch (SQLException e) {
            System.err.println("Error initializing demo bookings: " + e.getMessage());
        }
    }

    private void addBookingToDB(LocalDate date, String slotId, String entry, String exit, String name, String vehicle) throws SQLException {
        String sql = "INSERT OR IGNORE INTO bookings (mobile, slot_id, booking_date, entry_time, exit_time, vehicle, status) VALUES (?, ?, ?, ?, ?, ?, 'confirmed')";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, loggedInMobile != null ? loggedInMobile : "9876543210");
            stmt.setString(2, slotId);
            stmt.setString(3, date.toString());
            stmt.setString(4, entry);
            stmt.setString(5, exit);
            stmt.setString(6, vehicle);
            stmt.executeUpdate();
        }
    }

    private void showLogin() { rootCardLayout.show(rootPanel, "login"); }
    private void showHome() { rootCardLayout.show(rootPanel, "home"); updateSlotGrid(); loadHistoryFromDB(); }

    private void sendOtpAction() {
        String mobile = mobileField.getText().trim();
        if (mobile.length() < 10) {
            loginStatus.setText("Enter valid 10-digit mobile"); loginStatus.setForeground(OCC_COLOR); return;
        }
        String otp = String.format("%04d", new Random().nextInt(10000));
        otpStore.put(mobile, new OtpEntry(otp, Instant.now().plusSeconds(180)));

        // Print OTP to terminal
        System.out.println("=== PARKSMART OTP ===");
        System.out.println("Mobile: " + mobile);
        System.out.println("OTP: " + otp);
        System.out.println("Expires in 3 minutes");
        System.out.println("====================");

        otpStatus.setText("OTP sent to " + mobile + " (check terminal)"); otpStatus.setForeground(FREE_COLOR);
        loginStatus.setText("Enter OTP and verify"); loginStatus.setForeground(TEXT_COLOR());
    }

    private void verifyOtpAction() {
        String mobile = mobileField.getText().trim();
        String entered = otpField.getText().trim();
        OtpEntry entry = otpStore.get(mobile);
        if (entry == null) { loginStatus.setText("OTP missing/expired"); loginStatus.setForeground(OCC_COLOR); return; }
        if (Instant.now().isAfter(entry.expiresAt)) { loginStatus.setText("OTP expired"); loginStatus.setForeground(OCC_COLOR); otpStore.remove(mobile); return; }
        if (!entry.code.equals(entered)) { loginStatus.setText("Wrong OTP"); loginStatus.setForeground(OCC_COLOR); return; }

        loggedInMobile = mobile;
        try {
            // Ensure user exists in DB
            String sql = "INSERT OR IGNORE INTO users (mobile) VALUES (?)";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, mobile);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
            return;
        }

        loginStatus.setText("Login success"); loginStatus.setForeground(new Color(0,128,0));
        userInfoLabel.setText("Logged in: " + mobile);
        showHome();
    }

    private void updateSlotGrid() {
        selectedDateLabel.setText("Selected date: " + selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        try {
            String sql = "SELECT slot_id FROM bookings WHERE booking_date = ? AND status = 'confirmed'";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, selectedDate.toString());
                ResultSet rs = stmt.executeQuery();
                Set<String> occupied = new HashSet<>();
                while (rs.next()) {
                    occupied.add(rs.getString("slot_id"));
                }

                for (Component c : slotGrid.getComponents()) {
                    if (!(c instanceof JButton)) continue;
                    JButton b = (JButton)c;
                    boolean occ = occupied.contains(b.getText());
                    if (occ) { b.setBackground(new Color(255, 230, 232)); b.setForeground(OCC_COLOR); }
                    else { b.setBackground(new Color(235, 246, 255)); b.setForeground(FREE_COLOR); }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading slots: " + e.getMessage());
        }
    }

    private void onSlotClick(String slotId) {
        try {
            String sql = "SELECT * FROM bookings WHERE booking_date = ? AND slot_id = ? AND status = 'confirmed'";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, selectedDate.toString());
                stmt.setString(2, slotId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String exitTime = rs.getString("exit_time");
                    JOptionPane.showMessageDialog(this, "Slot " + slotId + " is occupied\nFree after " + exitTime, "Slot Occupied", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
            return;
        }
        showBookingDialog(slotId);
    }

    private void showBookingDialog(String slotId) {
        JDialog dialog = new JDialog(this, "Book " + slotId, true);
        dialog.setSize(420, 520);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new RoundedPanel(16, CARD);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));

        panel.add(new JLabel("Book " + slotId + " on " + selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))));
        panel.add(Box.createRigidArea(new Dimension(0,10)));

        JTextField name = labeledField(panel, "Name");
        JTextField vehicle = labeledField(panel, "Vehicle number");
        JTextField entryTime = labeledField(panel, "Entry (hh:mm AM/PM)");
        JTextField exitTime = labeledField(panel, "Exit (hh:mm AM/PM)");

        JLabel note = new JLabel("Pay ₹120 (₹100 rent + ₹20 deposit) after filling details.");
        note.setFont(new Font("DM Sans", Font.PLAIN, 12));
        note.setForeground(MUTED);
        panel.add(note);

        JButton pay = new JButton("Pay & Confirm Booking");
        pay.setBackground(FREE_COLOR);
        pay.setForeground(Color.WHITE);
        pay.setFocusPainted(false);
        pay.addActionListener(ev -> {
            if (name.getText().isBlank() || vehicle.getText().isBlank() || entryTime.getText().isBlank() || exitTime.getText().isBlank()) {
                JOptionPane.showMessageDialog(dialog, "Fill all fields"); return;
            }
            try { LocalTime.parse(entryTime.getText().trim(), TIME_FMT); LocalTime.parse(exitTime.getText().trim(), TIME_FMT);} catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Time format must be hh:mm AM/PM"); return;
            }
            try {
                addBookingToDB(selectedDate, slotId, entryTime.getText(), exitTime.getText(), name.getText(), vehicle.getText());
                historyModel.add(new BookingRecord(selectedDate.toString(), slotId, name.getText(), vehicle.getText(), entryTime.getText(), exitTime.getText(), "Confirmed", 120.0));
                activeBooking = new ActiveBooking(slotId, selectedDate.toString(), entryTime.getText(), exitTime.getText());
                updateSlotGrid();
                JOptionPane.showMessageDialog(dialog, "Booking confirmed and paid ₹120.\nShow receipt to watchman.");
                dialog.dispose();
                handleAfterBooking();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialog, "Database error: " + e.getMessage());
            }
        });

        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(pay);
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private JTextField labeledField(JPanel panel, String label) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(BODY_FONT);
        JTextField tf = new JTextField();
        tf.setFont(BODY_FONT);
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        panel.add(lbl);
        panel.add(tf);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        return tf;
    }

    private void handleAfterBooking() {
        if (activeBooking == null) return;
        int res = JOptionPane.showConfirmDialog(this, "Your booking is active. Click OK when you arrive to exit.", "Arrival", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            processExit();
        }
    }

    private void processExit() {
        if (activeBooking == null) return;
        LocalTime exit = LocalTime.parse(activeBooking.exit, TIME_FMT);
        LocalTime now = LocalTime.now();

        boolean early = now.isBefore(exit);
        if (early) {
            JOptionPane.showMessageDialog(this, "Early exit: refund ₹20 and generate Exit OTP.");
            issueExitOtp(20);
        } else if (now.isAfter(exit)) {
            int choice = JOptionPane.showOptionDialog(this, "Late exit: penalty ₹50. Pay now?", "Late Exit", JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE, null, new Object[]{"Pay Now","Pay Cash","Cancel"}, "Pay Now");
            if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) return;
            JOptionPane.showMessageDialog(this, "Penalty paid. Generate Exit OTP.");
            issueExitOtp(-50);
        } else {
            issueExitOtp(0);
        }

        String input = JOptionPane.showInputDialog(this, "Enter Exit OTP from watchman:");
        if (input != null && input.equals(exitOtp)) {
            JOptionPane.showMessageDialog(this, "Exit verified. Slot freed.");
            try {
                String sql = "UPDATE bookings SET status = 'completed' WHERE booking_date = ? AND slot_id = ?";
                try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                    stmt.setString(1, activeBooking.date);
                    stmt.setString(2, activeBooking.slotId);
                    stmt.executeUpdate();
                }
                activeBooking = null;
                exitOtp = null;
                updateSlotGrid();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "OTP mismatch. Try again.");
        }
    }

    private void issueExitOtp(int balanceChange) {
        exitOtp = String.format("%04d", new Random().nextInt(10000));
        String txt = "Exit OTP: " + exitOtp + ". " + (balanceChange > 0 ? "Refund " + balanceChange : balanceChange < 0 ? "Penalty " + Math.abs(balanceChange) : "");
        JOptionPane.showMessageDialog(this, txt);
    }

    private void loadHistoryFromDB() {
        try {
            String sql = "SELECT * FROM bookings WHERE mobile = ? ORDER BY created_at DESC";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, loggedInMobile);
                ResultSet rs = stmt.executeQuery();
                historyModel.clear();
                while (rs.next()) {
                    historyModel.add(new BookingRecord(
                        rs.getString("booking_date"),
                        rs.getString("slot_id"),
                        "User", // We don't store name in bookings table
                        rs.getString("vehicle"),
                        rs.getString("entry_time"),
                        rs.getString("exit_time"),
                        rs.getString("status"),
                        rs.getDouble("amount")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading history: " + e.getMessage());
        }
    }

    private void clearHistoryFromDB() throws SQLException {
        String sql = "DELETE FROM bookings WHERE mobile = ?";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, loggedInMobile);
            stmt.executeUpdate();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ParkSmartApp().setVisible(true));
    }

    static class SlotBooking {
        String slotId, date, name, vehicle, entry, exit;
        SlotBooking(String slotId, String date, String name, String vehicle, String entry, String exit) {
            this.slotId = slotId; this.date = date; this.name = name; this.vehicle = vehicle; this.entry = entry; this.exit = exit;
        }
    }

    static class ActiveBooking {
        String slotId, date, entry, exit;
        ActiveBooking(String slotId, String date, String entry, String exit) {
            this.slotId = slotId; this.date = date; this.entry = entry; this.exit = exit;
        }
    }

    static class OtpEntry { String code; Instant expiresAt; OtpEntry(String code, Instant expiresAt) { this.code = code; this.expiresAt = expiresAt; } }

    static class HistoryModel extends AbstractTableModel {
        private final java.util.List<BookingRecord> rows = new ArrayList<>();
        private final String[] columns = {"Date","Slot","Name","Vehicle","Entry","Exit","Status","Amount"};

        void add(BookingRecord b) { rows.add(0, b); fireTableDataChanged(); }
        void clear() { rows.clear(); fireTableDataChanged(); }

        public int getRowCount() { return rows.size(); }
        public int getColumnCount() { return columns.length; }
        public String getColumnName(int c) { return columns[c]; }
        public Object getValueAt(int r,int c) {
            BookingRecord b = rows.get(r);
            return switch(c) {
                case 0 -> b.date;
                case 1 -> b.slot;
                case 2 -> b.name;
                case 3 -> b.vehicle;
                case 4 -> b.entry;
                case 5 -> b.exit;
                case 6 -> b.status;
                case 7 -> "₹" + b.amount;
                default -> "";
            };
        }
    }

    static class BookingRecord {
        final String date, slot, name, vehicle, entry, exit, status;
        final double amount;
        BookingRecord(String date, String slot, String name, String vehicle, String entry, String exit, String status, double amount) {
            this.date=date; this.slot=slot; this.name=name; this.vehicle=vehicle; this.entry=entry; this.exit=exit; this.status=status; this.amount=amount;
        }
    }

    static class RoundedPanel extends JPanel {
        int radius; Color bg;
        RoundedPanel(int radius, Color bg) { this.radius = radius; this.bg = bg; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg); g2.fillRoundRect(0,0,getWidth(),getHeight(),radius,radius); g2.dispose(); super.paintComponent(g);
        }
    }

    private Color TEXT_COLOR() { return new Color(22, 28, 45); }
}