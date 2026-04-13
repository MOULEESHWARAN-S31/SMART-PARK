import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.stream.Collectors;
import java.io.*;
import java.sql.*;

public class ParkSmartAdmin extends JFrame {
    java.sql.Connection dbConnection;

    // ═══ COLORS ═══
    static final Color BG        = new Color(240, 244, 250);
    static final Color CARD      = Color.WHITE;
    static final Color BORDER    = new Color(220, 229, 240);
    static final Color FREE      = new Color(26, 115, 232);
    static final Color FREE_BG   = new Color(26, 115, 232, 20);
    static final Color OCCUPIED  = new Color(229, 57, 53);
    static final Color OCC_BG    = new Color(229, 57, 53, 18);
    static final Color WARN      = new Color(245, 158, 11);
    static final Color WARN_BG   = new Color(245, 158, 11, 25);
    static final Color GREEN     = new Color(22, 163, 74);
    static final Color GREEN_BG  = new Color(22, 163, 74, 22);
    static final Color PURPLE    = new Color(124, 58, 237);
    static final Color PURPLE_BG = new Color(124, 58, 237, 20);
    static final Color TEXT      = new Color(26, 35, 50);
    static final Color MUTED     = new Color(107, 124, 153);
    static final Color MUTED2    = new Color(160, 174, 192);
    static final Color SIDEBAR_BG = Color.WHITE;

    // ═══ FONTS ═══
    static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    static final Font FONT_HEAD  = new Font("SansSerif", Font.BOLD, 14);
    static final Font FONT_BODY  = new Font("SansSerif", Font.PLAIN, 12);
    static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    static final Font FONT_MONO  = new Font("Monospaced", Font.PLAIN, 12);
    static final Font FONT_STAT  = new Font("SansSerif", Font.BOLD, 28);

    // ═══ DATA ═══
    static class Booking {
        String id, slot, mobile, name, vehicle, entry, exit, status;
        int paid, refund; long bookDate;
        boolean penaltyPaid;
        Booking(String id,String slot,String mobile,String name,String vehicle,
                String entry,String exit,String status,int paid,int refund,long bookDate, boolean penaltyPaid){
            this.id=id;this.slot=slot;this.mobile=mobile;this.name=name;
            this.vehicle=vehicle;this.entry=entry;this.exit=exit;this.status=status;
            this.paid=paid;this.refund=refund;this.bookDate=bookDate;
            this.penaltyPaid=penaltyPaid;
        }
    }
    static class SlotData {
        String id, state; Booking booking;
        SlotData(String id,String state,Booking booking){this.id=id;this.state=state;this.booking=booking;}
    }
    static class AdminUser {
        String name, phone, empid, registeredOn;
        AdminUser(String name,String phone,String empid,String registeredOn){
            this.name=name;this.phone=phone;this.empid=empid;this.registeredOn=registeredOn;
        }
    }

    List<Booking>   bookings  = new ArrayList<>();
    List<SlotData>  slots     = new ArrayList<>();
    List<AdminUser> adminList = new ArrayList<>();

    // ═══ UI REFS ═══
    JPanel     contentArea;
    CardLayout cardLayout;
    JLabel     clockLabel, topbarTitle;
    JLabel     statFree, statOcc, statOverdue, statRevenue, statBookingsCount, statPenalty, statRefund;
    JPanel     dashSlotGrid;
    JTable     todayTable;
    DefaultTableModel todayModel;
    JPanel     adminListBody;
    JLabel     adminBadge, empIdPreview;
    JTextField rNameField, rPhoneField;
    String     activeTab = "dashboard";
    Map<String,JButton> navButtons = new LinkedHashMap<>();

    // ─ Calendar & Dash refs ─
    java.time.LocalDate selectedDate = java.time.LocalDate.now();
    java.time.LocalDate calMonth     = java.time.LocalDate.now();
    JLabel calMonthLabel;
    JPanel calDaysPanel;
    JLabel selectedDateLabel;

    // ─── Reports refs ───
    JSpinner fromSpin, toSpin;
    JTable   reportTable;
    DefaultTableModel reportModel;

    // ═══ CONSTRUCTOR ═══
    public ParkSmartAdmin() { this(null); }
    public ParkSmartAdmin(java.sql.Connection conn) {
        this.dbConnection = conn;
        loadDataFromDB();
        setTitle("ParkSmart – Admin Panel");
        setSize(1150, 720);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildMainArea(), BorderLayout.CENTER);
        setContentPane(root);

        buildDashSlotGrid(dashSlotGrid);
        buildTodayTable();
        startOverdueCheckTimer();
        setVisible(true);
    }

    private Set<String> overdueAlertsShown = new HashSet<>();
    void startOverdueCheckTimer() {
        javax.swing.Timer t = new javax.swing.Timer(10000, e -> {
            updateOverdueBookingsInDB(); // ← Fix Bug 2: keep DB status current
            loadDataFromDB();
            buildDashSlotGrid(dashSlotGrid);
            buildTodayTable();
            
            for (SlotData s : slots) {
                if (s.state.equals("overdue") && s.booking != null && !overdueAlertsShown.contains(s.booking.id)) {
                    overdueAlertsShown.add(s.booking.id);
                    showAdminOverdueAlert(s);
                }
            }
        });
        t.start();
    }

    void showAdminOverdueAlert(SlotData s) {
        String msg = "<html><body style='width: 300px; padding: 10px;'>" +
                     "<h2 style='color: #e53935;'>🚨 OVERTIME ALERT (ADMIN)</h2>" +
                     "<p><b>Slot ID:</b> " + s.id + "</p>" +
                     "<p><b>User Name:</b> " + s.booking.name + "</p>" +
                     "<p><b>Arrival Time:</b> " + s.booking.entry + "</p>" +
                     "<p><b>Exit Time:</b> " + s.booking.exit + "</p>" +
                     "<p style='color: #6b7c99; font-size: 10px;'>This user has exceeded their stay. ₹20 refund should be withheld.</p>" +
                     "</body></html>";
        JOptionPane.showMessageDialog(this, msg, "Overdue Alert - " + s.id, JOptionPane.ERROR_MESSAGE);
    }

    // ─── HELPER: current time in minutes since midnight ───────────────────
    private int nowMinsAdmin() {
        java.time.LocalTime t = java.time.LocalTime.now();
        return t.getHour() * 60 + t.getMinute();
    }

    // ─── HELPER: parse "hh:mm AM/PM" → minutes since midnight ────────────
    private int parseAdminTime12(String s) {
        if (s == null || s.isBlank()) return -1;
        try {
            String[] parts = s.trim().split(" ");
            String[] hm    = parts[0].split(":");
            int h = Integer.parseInt(hm[0].trim());
            int m = Integer.parseInt(hm[1].trim());
            String ap = parts[1].trim().toUpperCase();
            if (ap.equals("PM") && h != 12) h += 12;
            if (ap.equals("AM") && h == 12) h = 0;
            return h * 60 + m;
        } catch (Exception e) { return -1; }
    }

    // ─── Update 'confirmed' → 'overdue' in DB when exit time has passed ──
    private void updateOverdueBookingsInDB() {
        if (dbConnection == null) return;
        String today = java.time.LocalDate.now().toString();
        int nowMin = nowMinsAdmin();
        try {
            String sql = "SELECT id, exit_time FROM bookings WHERE status='confirmed' AND booking_date=?";
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                ps.setString(1, today);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id      = rs.getInt("id");
                        int exitMin = parseAdminTime12(rs.getString("exit_time"));
                        if (exitMin >= 0 && nowMin > exitMin + 20) {  // Only overdue AFTER 20-min grace
                            try (PreparedStatement upd = dbConnection.prepareStatement(
                                    "UPDATE bookings SET status='overdue' WHERE id=?")) {
                                upd.setInt(1, id);
                                upd.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ═══ DEMO DATA ═══
    void loadDataFromDB() {
        bookings.clear(); slots.clear(); adminList.clear();
        if (dbConnection == null) { initData(); return; }
        try {
            String sql = "SELECT * FROM bookings ORDER BY booking_date DESC, entry_time ASC";
            try (Statement smt = dbConnection.createStatement();
                 ResultSet rs = smt.executeQuery(sql)) {
                while (rs.next()) {
                    String bId = String.valueOf(rs.getInt("id"));
                    String mod = rs.getString("mobile");
                    String slo = rs.getString("slot_id");
                    java.sql.Date d = rs.getDate("booking_date");
                    String ent = rs.getString("entry_time");
                    String ext = rs.getString("exit_time");
                    String veh = rs.getString("vehicle");
                    String sts = rs.getString("status");
                    if (sts.equals("confirmed")) sts = "active";
                    double amt = rs.getDouble("amount");
                    String nm = rs.getString("name");
                    boolean pPaid = rs.getBoolean("penalty_paid");
                    bookings.add(new Booking("B" + bId, slo, mod, nm, veh, ent, ext, sts, (int)amt, 0, d.getTime(), pPaid));
                }
            }
            try {
                try(Statement st = dbConnection.createStatement()) { 
                    st.execute("CREATE TABLE IF NOT EXISTS admins (phone VARCHAR(15) PRIMARY KEY, name VARCHAR(50), empid VARCHAR(20), reg_date VARCHAR(20))"); 
                }
                String s = "SELECT * FROM admins";
                try (Statement stmt = dbConnection.createStatement(); ResultSet rs = stmt.executeQuery(s)) {
                    while(rs.next()) adminList.add(new AdminUser(rs.getString("name"), rs.getString("phone"), rs.getString("empid"), rs.getString("reg_date")));
                }
            } catch (Exception ex) {}
            if (adminList.isEmpty()) adminList.add(new AdminUser("Super Admin","9876543210","EMP-000","System"));
            String[] ids = {"A1","A2","A3","A4","A5","A6","A7","A8","A9","A10","A11","A12","A13","A14","A15","A16"};
            Map<String,Booking> sm = new HashMap<>();
            long startToday = java.sql.Date.valueOf(selectedDate).getTime();
            for (Booking b : bookings) {
                if (!b.status.equals("done") && !b.status.equals("cancelled") && b.bookDate == startToday) {
                    sm.put(b.slot, b); // Overwrites earlier if multiple, so get the latest active
                }
            }
            int nowMin = nowMinsAdmin();
            for (String id : ids) {
                Booking b = sm.get(id);
                String st;
                if (b == null) {
                    st = "free";
                } else if (b.status.equals("overdue")) {
                    st = "overdue";
                } else if (selectedDate.equals(java.time.LocalDate.now())) {
                    // Fix Bug 3: for today, only show OCCUPIED if entry time has passed
                    int entryMin = parseAdminTime12(b.entry);
                    st = (entryMin >= 0 && nowMin < entryMin) ? "upcoming" : "occupied";
                } else {
                    st = "occupied";
                }
                slots.add(new SlotData(id, st, b));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (dbConnection == null) initData();
        }
    }

    void initData() {
        long now = System.currentTimeMillis(), day = 86400000L;
        bookings.add(new Booking("B001","A1","9876543210","Rajesh Kumar","TN 33 AB 1234","09:30 AM","11:30 AM","active",120,0,now, false));
        bookings.add(new Booking("B002","A3","9988776655","Priya Singh","TN 22 CD 5678","08:00 AM","10:00 AM","overdue",120,0,now, false));
        bookings.add(new Booking("B003","A5","9445566778","Arjun Patel","KA 05 MN 9988","10:00 AM","12:00 PM","active",120,0,now, false));
        bookings.add(new Booking("B004","A6","9123456780","Aisha Khan","TN 01 ZZ 4321","07:00 AM","09:00 AM","done",120,20,now-day, false));
        bookings.add(new Booking("B005","A9","9000112233","Vikram Sharma","MH 12 XY 7654","09:00 AM","11:00 AM","overdue",120,0,now, false));
        bookings.add(new Booking("B006","A12","9776655443","Neha Verma","TN 72 HH 1122","10:30 AM","12:30 PM","active",120,0,now-day, false));
        bookings.add(new Booking("B007","A15","9654321098","Amit Desai","TN 11 KK 8877","11:00 AM","01:00 PM","active",120,0,now-2*day, false));
        bookings.add(new Booking("B008","A2","9333444555","Deepak Nair","TN 55 PP 3344","07:30 AM","09:30 AM","done",120,20,now-3*day, false));

        String[] ids = {"A1","A2","A3","A4","A5","A6","A7","A8","A9","A10","A11","A12","A13","A14","A15","A16"};
        Map<String,Booking> sm = new HashMap<>();
        for (Booking b : bookings) if (!b.status.equals("done")) sm.put(b.slot, b);
        for (String id : ids) {
            Booking b = sm.get(id);
            String st = b==null?"free":(b.status.equals("overdue")?"overdue":"occupied");
            slots.add(new SlotData(id, st, b));
        }
        adminList.add(new AdminUser("Raju K","9876500001","EMP-001","20 Mar 2026"));
    }

    // ════════════════════════════════════════
    //  SIDEBAR — 3 items only
    // ════════════════════════════════════════
    JPanel buildSidebar() {
        JPanel sb = new JPanel(new BorderLayout());
        sb.setBackground(SIDEBAR_BG);
        sb.setPreferredSize(new Dimension(210, 0));
        sb.setBorder(new MatteBorder(0,0,0,1,BORDER));

        // Logo
        JPanel logo = new JPanel();
        logo.setLayout(new BoxLayout(logo, BoxLayout.Y_AXIS));
        logo.setBackground(SIDEBAR_BG);
        logo.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,BORDER), new EmptyBorder(14,16,14,16)));
        JLabel logoLbl = new JLabel("<html><span style='color:#1a73e8;font-size:16px;font-weight:bold'>Park</span>"
            +"<span style='font-size:16px;font-weight:bold'>Smart</span></html>");
        JLabel roleLbl = new JLabel("ADMIN PANEL");
        roleLbl.setFont(new Font("SansSerif", Font.BOLD, 9));
        roleLbl.setForeground(MUTED);
        logo.add(logoLbl); logo.add(Box.createVerticalStrut(2)); logo.add(roleLbl);

        // Nav
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBackground(SIDEBAR_BG);
        nav.setBorder(new EmptyBorder(10,0,8,0));
        nav.add(sideLabel("MAIN"));
        nav.add(navItem("🏠  Dashboard", "dashboard"));
        nav.add(sideLabel("DATA"));
        nav.add(navItem("📊  Reports", "reports"));
        nav.add(sideLabel("ADMINISTRATION"));
        nav.add(navItem("👤  Manage Admin", "admins"));
        nav.add(Box.createVerticalGlue());

        JScrollPane navScroll = new JScrollPane(nav);
        navScroll.setBorder(null); navScroll.setBackground(SIDEBAR_BG);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        footer.setBackground(SIDEBAR_BG);
        footer.setBorder(new MatteBorder(1,0,0,0,BORDER));
        JLabel av = new JLabel("W");
        av.setFont(new Font("SansSerif", Font.BOLD, 12)); av.setForeground(Color.WHITE);
        av.setOpaque(true); av.setBackground(FREE);
        av.setHorizontalAlignment(SwingConstants.CENTER);
        av.setPreferredSize(new Dimension(30,30));
        JPanel fi = new JPanel();
        fi.setLayout(new BoxLayout(fi, BoxLayout.Y_AXIS)); fi.setBackground(SIDEBAR_BG);
        JLabel fn = new JLabel("Watchman"); fn.setFont(FONT_HEAD); fn.setForeground(TEXT);
        JLabel fr = new JLabel("Gate Officer"); fr.setFont(FONT_SMALL); fr.setForeground(MUTED);
        fi.add(fn); fi.add(fr);
        footer.add(av); footer.add(fi);

        sb.add(logo, BorderLayout.NORTH);
        sb.add(navScroll, BorderLayout.CENTER);
        sb.add(footer, BorderLayout.SOUTH);
        return sb;
    }

    JButton navItem(String text, String tab) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (tab.equals(activeTab)) {
                    g2.setColor(tab.equals("admins") ? PURPLE_BG : FREE_BG);
                    g2.fillRect(0,0,getWidth(),getHeight());
                    g2.setColor(tab.equals("admins") ? PURPLE : FREE);
                    g2.fillRect(0,0,3,getHeight());
                } else if (getModel().isRollover()) {
                    g2.setColor(FREE_BG); g2.fillRect(0,0,getWidth(),getHeight());
                }
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", tab.equals(activeTab)?Font.BOLD:Font.PLAIN, 12));
        btn.setForeground(tab.equals(activeTab)?(tab.equals("admins")?PURPLE:FREE):MUTED);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(9,16,9,16));
        btn.setContentAreaFilled(false); btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> switchTab(tab));
        navButtons.put(tab, btn);
        return btn;
    }

    JLabel sideLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 9));
        l.setForeground(MUTED2);
        l.setBorder(new EmptyBorder(10,16,2,16));
        return l;
    }

    // ════════════════════════════════════════
    //  MAIN AREA
    // ════════════════════════════════════════
    JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG);

        // Topbar
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(new Color(255,255,255,245));
        topbar.setPreferredSize(new Dimension(0,58));
        topbar.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,BORDER), new EmptyBorder(0,22,0,22)));
        topbarTitle = new JLabel("Dashboard"); topbarTitle.setFont(FONT_HEAD); topbarTitle.setForeground(TEXT);
        JPanel tr = new JPanel(new FlowLayout(FlowLayout.RIGHT,12,0)); tr.setOpaque(false);
        JButton logoutBtn = new JButton("Logout") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(229, 57, 53));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setOpaque(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        logoutBtn.setPreferredSize(new Dimension(100, 36));
        logoutBtn.addActionListener(e -> {
            try {
                Class<?> loginClass = Class.forName("com.parksmart.ParkSmartApp");
                Object loginInstance = loginClass.getDeclaredConstructor().newInstance();
                ((JFrame)loginInstance).setVisible(true);
            } catch (Exception ex) { ex.printStackTrace(); }
            this.dispose();
        });

        JButton refreshBtn = new JButton("Refresh") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(26, 115, 232));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setContentAreaFilled(false);
        refreshBtn.setOpaque(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        refreshBtn.setPreferredSize(new Dimension(100, 36));
        refreshBtn.addActionListener(e -> {
            loadDataFromDB();
            buildDashSlotGrid(dashSlotGrid);
            buildTodayTable();
            if(activeTab.equals("reports")) refreshReportTable();
        });

        tr.add(refreshBtn);
        tr.add(logoutBtn);
        topbar.add(topbarTitle, BorderLayout.WEST); topbar.add(tr, BorderLayout.EAST);

        cardLayout  = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(BG);
        contentArea.add(buildDashboard(), "dashboard");
        contentArea.add(buildReports(),   "reports");
        contentArea.add(buildAdmins(),    "admins");

        main.add(topbar, BorderLayout.NORTH);
        main.add(contentArea, BorderLayout.CENTER);
        return main;
    }

    // ════════════════════════════════════════
    //  DASHBOARD
    // ════════════════════════════════════════
    JPanel buildDashboard() {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG); p.setBorder(new EmptyBorder(16,16,16,16));

        // Stat row — 2 rows × 3 columns
        JPanel stats = new JPanel(new GridLayout(2,3,10,8));
        stats.setOpaque(false); stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 222));
        statFree    = new JLabel("10"); statOcc = new JLabel("5"); statOverdue = new JLabel("2");
        stats.add(statCard("FREE SLOTS",        statFree,    FREE,     "Available right now",      FREE));
        stats.add(statCard("OCCUPIED",          statOcc,     OCCUPIED, "Currently parked",         OCCUPIED));
        stats.add(statCard("OVERDUE",           statOverdue, WARN,     "Exceeded exit time",       WARN));
        statRevenue = new JLabel("\u20b9" + todayRevenue());
        stats.add(statCard("TODAY REVENUE",     statRevenue, FREE,     "Real-time earnings",       FREE));
        statPenalty = new JLabel("\u20b90");
        stats.add(statCard("PENALTY COLLECTED", statPenalty, OCCUPIED, "\u20b920 per overdue exit",OCCUPIED));
        statRefund  = new JLabel("\u20b90");
        stats.add(statCard("REFUNDS GIVEN",     statRefund,  GREEN,    "\u20b920 deposit returned", GREEN));
        p.add(stats); p.add(Box.createVerticalStrut(12));

        // Slot Map + Alerts row
        JPanel twoCol = new JPanel(new GridLayout(1,2,12,0));
        twoCol.setOpaque(false); twoCol.setMaximumSize(new Dimension(Integer.MAX_VALUE,290));

        JPanel slotCard = sectionCard("🅿  Live Slot Map – Zone A");
        dashSlotGrid = new JPanel(new GridLayout(4,4,8,8));
        dashSlotGrid.setOpaque(false); dashSlotGrid.setBorder(new EmptyBorder(12,12,12,12));
        slotCard.add(dashSlotGrid, BorderLayout.CENTER);

        twoCol.add(slotCard); twoCol.add(buildCalendarCard());
        p.add(twoCol); p.add(Box.createVerticalStrut(12));

        // Today's Bookings table
        JPanel todayCard = new JPanel(new BorderLayout());
        todayCard.setBackground(CARD);
        todayCard.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(0,0,0,0)));
        JPanel hd = new JPanel(new FlowLayout(FlowLayout.LEFT,12,10));
        hd.setBackground(CARD); hd.setBorder(new MatteBorder(0,0,1,0,BORDER));
        selectedDateLabel = new JLabel("📋  Bookings for: " + selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")));
        selectedDateLabel.setFont(FONT_HEAD);
        hd.add(selectedDateLabel); todayCard.add(hd, BorderLayout.NORTH);
        String[] cols = {"Date","Slot","Name","Mobile","Vehicle","Entry","Exit","Status","Amount","Action"};
        todayModel = new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        todayTable = styledTable(todayModel);
        JScrollPane sp = new JScrollPane(todayTable); sp.setBorder(null); sp.getViewport().setBackground(CARD);
        todayCard.add(sp, BorderLayout.CENTER);
        p.add(todayCard);

        JScrollPane scroll = new JScrollPane(p); scroll.setBorder(null);
        scroll.getViewport().setBackground(BG); scroll.getVerticalScrollBar().setUnitIncrement(14);
        JPanel w = new JPanel(new BorderLayout()); w.setBackground(BG); w.add(scroll);
        return w;
    }

    // ═══ CALENDAR ═══
    JPanel buildCalendarCard() {
        JPanel card = sectionCard("📅  Calendar");
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(CARD);
        wrapper.setBorder(new EmptyBorder(10,10,10,10));

        JPanel nav = new JPanel(new BorderLayout());
        nav.setOpaque(false);
        JButton prev = new JButton("<"); prev.setBorder(BorderFactory.createEmptyBorder(2,8,2,8)); prev.setContentAreaFilled(false);
        JButton next = new JButton(">"); next.setBorder(BorderFactory.createEmptyBorder(2,8,2,8)); next.setContentAreaFilled(false);
        calMonthLabel = new JLabel("", JLabel.CENTER);
        calMonthLabel.setFont(new Font("Segoe UI Black", Font.BOLD, 14));
        prev.addActionListener(e -> {
            calMonth = calMonth.minusMonths(1);
            refreshCalendar();
        });
        next.addActionListener(e -> {
            calMonth = calMonth.plusMonths(1);
            refreshCalendar();
        });
        nav.add(prev, BorderLayout.WEST);
        nav.add(calMonthLabel, BorderLayout.CENTER);
        nav.add(next, BorderLayout.EAST);
        nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        calDaysPanel = new JPanel(new GridLayout(0, 7, 3, 3));
        calDaysPanel.setOpaque(false);

        wrapper.add(nav);
        wrapper.add(Box.createRigidArea(new Dimension(0,8)));
        wrapper.add(calDaysPanel);

        card.add(wrapper, BorderLayout.CENTER);
        refreshCalendar();
        return card;
    }

    void refreshCalendar() {
        if(calMonthLabel == null || calDaysPanel == null) return;
        calMonthLabel.setText(calMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")));
        calDaysPanel.removeAll();

        for (String d : new String[]{"Sun","Mon","Tue","Wed","Thu","Fri","Sat"}) {
            JLabel lbl = new JLabel(d, JLabel.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lbl.setForeground(new Color(150, 160, 180));
            calDaysPanel.add(lbl);
        }

        java.time.LocalDate firstDay = calMonth.withDayOfMonth(1);
        int startDow = firstDay.getDayOfWeek().getValue() % 7;
        int daysInMonth = calMonth.lengthOfMonth();
        java.time.LocalDate today = java.time.LocalDate.now();

        for (int i = 0; i < startDow; i++) calDaysPanel.add(new JLabel(""));

        for (int d = 1; d <= daysInMonth; d++) {
            java.time.LocalDate date = calMonth.withDayOfMonth(d);
            long dms = java.sql.Date.valueOf(date).getTime();
            boolean hasBooking = bookings.stream().anyMatch(b -> b.bookDate == dms);

            JPanel cellPanel = new JPanel();
            cellPanel.setLayout(new BoxLayout(cellPanel, BoxLayout.Y_AXIS));
            cellPanel.setOpaque(false);

            JButton btn = new JButton(String.valueOf(d));
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            if (date.equals(selectedDate)) {
                btn.setBackground(new Color(26, 115, 232));
                btn.setForeground(Color.WHITE);
                btn.setOpaque(true);
            } else if (date.equals(today)) {
                btn.setBackground(new Color(220, 230, 250));
                btn.setForeground(new Color(26, 115, 232));
                btn.setOpaque(true);
            } else {
                btn.setContentAreaFilled(false);
                btn.setForeground(new Color(60, 70, 90));
                btn.setOpaque(false);
            }
            if (date.isBefore(today)) {
                btn.setForeground(new Color(200, 200, 200));
            }
            
            btn.addActionListener(e -> {
                selectedDate = date;
                refreshCalendar();
                loadDataFromDB();
                buildDashSlotGrid(dashSlotGrid);
                buildTodayTable();
            });
            
            cellPanel.add(btn);
            if (hasBooking && !date.isBefore(today)) {
                JLabel dot = new JLabel("●");
                dot.setFont(new Font("SansSerif", Font.PLAIN, 8));
                dot.setForeground(OCCUPIED);
                dot.setAlignmentX(Component.CENTER_ALIGNMENT);
                cellPanel.add(dot);
            } else {
                cellPanel.add(Box.createVerticalStrut(10)); // Maintain spacing
            }

            calDaysPanel.add(cellPanel);
        }
        calDaysPanel.revalidate();
        calDaysPanel.repaint();
    }

    // ═══ SLOT GRID ═══
    void buildDashSlotGrid(JPanel grid) {
        if (grid==null) return;
        grid.removeAll();
        int free=0,occ=0,ov=0;
        for (SlotData s : slots) {
            // Fix Bug 3: upcoming bookings are physically free slots
            if (s.state.equals("free") || s.state.equals("upcoming")) free++;
            else if (s.state.equals("occupied")) occ++;
            else ov++;
            grid.add(slotBtn(s));
        }
        if (statFree!=null)    statFree.setText(String.valueOf(free));
        if (statOcc!=null)     statOcc.setText(String.valueOf(occ));
        if (statOverdue!=null) statOverdue.setText(String.valueOf(ov));
        // Update penalty & refund stats dynamically
        if (statPenalty!=null) statPenalty.setText("\u20b9" + todayPenalty());
        if (statRefund!=null)  statRefund.setText("\u20b9"  + todayRefund());
        grid.revalidate(); grid.repaint();
    }

    JButton slotBtn(SlotData s) {
        Color bg,fg,brd; String tag;
        switch(s.state){
            case "occupied": bg=new Color(255,235,235); fg=OCCUPIED; brd=new Color(229,57,53,120);
                tag="OCCUPIED"; break;
            case "overdue":  bg=new Color(255,248,220); fg=WARN;     brd=new Color(245,158,11,150);
                tag="\u26a0 OVERDUE"; break;
            case "upcoming": bg=new Color(235,248,240); fg=GREEN;    brd=new Color(22,163,74,100);
                // Show entry time so admin knows when it starts
                tag = "from " + (s.booking != null ? s.booking.entry : "?"); break;
            default:         bg=new Color(235,245,255); fg=FREE;     brd=new Color(26,115,232,120);
                tag="FREE"; break;
        }
        
        final Color finalBg = bg;
        final Color finalBrd = brd;

        JButton btn = new JButton("<html><center><b>"+s.id+"</b><br><span style='font-size:8px'>"+tag+"</span></center></html>") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(finalBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(finalBrd);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(fg);
        btn.setFont(new Font("SansSerif",Font.BOLD,11));
        btn.setBorder(new EmptyBorder(8,8,8,8));
        btn.setFocusPainted(false); btn.setContentAreaFilled(false); btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(85,68));
        btn.addActionListener(e -> showSlotVerifyDialog(s));
        return btn;
    }

    // ════════════════════════════════════════
    //  SLOT CLICK → OTP VERIFY DIALOG
    // ════════════════════════════════════════
    void showSlotVerifyDialog(SlotData s) {
        JDialog dlg = new JDialog(this, "Slot " + s.id + " – Verify OTP", true);
        dlg.setSize(400, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(CARD);

        // Header
        JPanel hdr = new JPanel(new FlowLayout(FlowLayout.LEFT,14,14));
        Color hdrColor = s.state.equals("free") ? GREEN : s.state.equals("overdue") ? WARN : OCCUPIED;
        hdr.setBackground(s.state.equals("free") ? GREEN_BG : s.state.equals("overdue") ? WARN_BG : OCC_BG);
        hdr.setBorder(new MatteBorder(0,0,1,0,BORDER));
        JLabel hTitle = new JLabel("Slot " + s.id + "  —  " + s.state.toUpperCase());
        hTitle.setFont(new Font("SansSerif",Font.BOLD,15)); hTitle.setForeground(hdrColor);
        hdr.add(hTitle);

        // Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(CARD);
        body.setBorder(new EmptyBorder(18,20,18,20));

        if (s.booking != null) {
            JPanel info = new JPanel(new GridLayout(2,3,8,8));
            info.setOpaque(false); info.setMaximumSize(new Dimension(Integer.MAX_VALUE,90));
            info.add(infoCell("Booking ID", s.booking.id));
            info.add(infoCell("Vehicle",  s.booking.vehicle));
            info.add(infoCell("Entry",    s.booking.entry));
            info.add(infoCell("Exit",     s.booking.exit));
            info.add(infoCell("Mobile",   s.booking.mobile));
            info.add(infoCell("Status",   s.state.toUpperCase()));
            if (s.state.equalsIgnoreCase("overdue")) {
                info.add(infoCell("Penalty", s.booking.penaltyPaid ? "₹20 PAID" : "₹20 UNPAID"));
            }
            body.add(info);
            body.add(Box.createVerticalStrut(16));
        } else {
            JLabel avail = new JLabel("✓  Slot is FREE — no active booking");
            avail.setForeground(GREEN); avail.setFont(FONT_HEAD);
            body.add(avail);
            body.add(Box.createVerticalStrut(16));
        }

        // OTP entry section
        JLabel otpLbl = new JLabel("ENTER EXIT OTP");
        otpLbl.setFont(new Font("SansSerif",Font.BOLD,10)); otpLbl.setForeground(MUTED);
        body.add(otpLbl); body.add(Box.createVerticalStrut(6));

        JTextField[] boxes = new JTextField[4];
        JPanel otpRow = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        otpRow.setOpaque(false);
        for (int i=0;i<4;i++){
            boxes[i] = new JTextField(1);
            boxes[i].setFont(new Font("Monospaced",Font.BOLD,22));
            boxes[i].setHorizontalAlignment(JTextField.CENTER);
            boxes[i].setPreferredSize(new Dimension(52,52));
            boxes[i].setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(4,4,4,4)));
            final int idx=i;
            boxes[i].addKeyListener(new KeyAdapter(){
                public void keyTyped(KeyEvent e){
                    if(idx<3 && Character.isDigit(e.getKeyChar()))
                        SwingUtilities.invokeLater(()->boxes[idx+1].requestFocus());
                }
            });
            otpRow.add(boxes[i]);
        }
        body.add(otpRow);
        body.add(Box.createVerticalStrut(4));

        body.add(Box.createVerticalStrut(14));

        JLabel resultLbl = new JLabel(" ");
        resultLbl.setFont(new Font("SansSerif",Font.BOLD,12));
        body.add(resultLbl);
        body.add(Box.createVerticalStrut(10));

        // Buttons
        JPanel btnRow = new JPanel(new GridLayout(1,2,10,0));
        btnRow.setOpaque(false); btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
        JButton verifyBtn = actionBtn("✓ Verify & Release", FREE, Color.WHITE);
        JButton closeBtn  = outlineBtn("Cancel");
        closeBtn.addActionListener(e -> dlg.dispose());
        verifyBtn.addActionListener(e -> {
            StringBuilder otp = new StringBuilder();
            for (JTextField b : boxes) otp.append(b.getText().trim());
            if (otp.length() < 4) {
                resultLbl.setForeground(OCCUPIED); resultLbl.setText("❌ Enter all 4 OTP digits"); return;
            }
            if (!com.parksmart.ExitOtpStore.has(dbConnection, s.id) || !com.parksmart.ExitOtpStore.get(dbConnection, s.id).equals(otp.toString())) {
                resultLbl.setForeground(OCCUPIED); resultLbl.setText("❌ Invalid OTP. Try again."); return;
            }
            
            // Release the slot
            com.parksmart.ExitOtpStore.remove(dbConnection, s.id);
            boolean isOverdue = s.state.equals("overdue");
            String penaltyStatus = isOverdue ? (s.booking.penaltyPaid ? " (Penalty Paid)" : " (Penalty UNPAID!)") : "";
            String refundMsg = isOverdue ? "₹20 Refund: NO (Overtime Stay" + penaltyStatus + ")" : "₹20 Refund: YES (On Time)";
            
            if (s.booking != null) {
                try {
                    // Auto-record penalty when watchman releases an overdue slot
                    String updateSql = isOverdue
                        ? "UPDATE bookings SET status='done', penalty_paid=TRUE WHERE id=?"
                        : "UPDATE bookings SET status='done' WHERE id=?";
                    try (PreparedStatement ps = dbConnection.prepareStatement(updateSql)) {
                        ps.setInt(1, Integer.parseInt(s.booking.id.replace("B","")));
                        ps.executeUpdate();
                    }
                } catch (Exception e_sql) { e_sql.printStackTrace(); }
                s.booking.status = "done";
                slots.stream().filter(x -> x.id.equals(s.id)).findFirst().ifPresent(x -> { x.state="free"; x.booking=null; });
                buildDashSlotGrid(dashSlotGrid);
                buildTodayTable();
            }
            resultLbl.setForeground(isOverdue ? WARN : GREEN);
            resultLbl.setText("<html>✓ Verified! Slot " + s.id + " Released.<br>" + refundMsg + "</html>");
            verifyBtn.setEnabled(false);
            Timer t = new Timer();
            t.schedule(new TimerTask(){public void run(){SwingUtilities.invokeLater(dlg::dispose);}},2500);
        });
        btnRow.add(verifyBtn); btnRow.add(closeBtn);
        body.add(btnRow);

        root.add(hdr, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    // ═══ TODAY TABLE ═══
    void buildTodayTable() {
        if (todayModel==null) return;
        todayModel.setRowCount(0);
        long startToday = java.sql.Date.valueOf(selectedDate).getTime();
        for (Booking b : bookings) {
            if (b.bookDate != startToday && b.bookDate < startToday) continue; // Show only today's
            String st = b.status.equals("active")?"● Active":b.status.equals("overdue")?"⚠ Overdue":"✓ Done";
            todayModel.addRow(new Object[]{new java.sql.Date(b.bookDate).toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")),b.slot,b.name,b.mobile,b.vehicle,b.entry,b.exit,st,"₹"+b.paid,
                b.status.equals("done")?"—":"Verify"});
        }
        if (selectedDateLabel != null) {
            selectedDateLabel.setText("📋  Bookings for: " + selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")));
        }
    }

    // ════════════════════════════════════════
    //  REPORTS PAGE
    // ════════════════════════════════════════
    JPanel buildReports() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG); p.setBorder(new EmptyBorder(16,16,16,16));

        // ── Top filter bar ──
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT,12,8));
        filterBar.setBackground(CARD);
        filterBar.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true), new EmptyBorder(8,12,8,12)));

        JLabel fromLbl = new JLabel("From:");
        fromLbl.setFont(new Font("SansSerif",Font.BOLD,11)); fromLbl.setForeground(MUTED);
        fromSpin = new JSpinner(new SpinnerDateModel());
        fromSpin.setEditor(new JSpinner.DateEditor(fromSpin,"dd-MM-yyyy"));
        fromSpin.setPreferredSize(new Dimension(120,30));

        JLabel toLbl = new JLabel("To:");
        toLbl.setFont(new Font("SansSerif",Font.BOLD,11)); toLbl.setForeground(MUTED);
        toSpin = new JSpinner(new SpinnerDateModel());
        toSpin.setEditor(new JSpinner.DateEditor(toSpin,"dd-MM-yyyy"));
        toSpin.setPreferredSize(new Dimension(120,30));

        JButton filterBtn = actionBtn("🔍  Filter", FREE, Color.WHITE);
        filterBtn.setPreferredSize(new Dimension(100,30));
        filterBtn.setMaximumSize(new Dimension(100,30));
        filterBtn.addActionListener(e -> refreshReportTable());

        JButton dlBtn = new JButton("⬇  Download CSV");
        dlBtn.setFont(new Font("SansSerif",Font.BOLD,11));
        dlBtn.setForeground(Color.WHITE); dlBtn.setBackground(GREEN);
        dlBtn.setBorder(new CompoundBorder(new LineBorder(GREEN.darker(),1,true),new EmptyBorder(6,12,6,12)));
        dlBtn.setFocusPainted(false); dlBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dlBtn.addActionListener(e -> exportCSV());

        filterBar.add(fromLbl); filterBar.add(fromSpin);
        filterBar.add(Box.createHorizontalStrut(6));
        filterBar.add(toLbl); filterBar.add(toSpin);
        filterBar.add(Box.createHorizontalStrut(10));
        filterBar.add(filterBtn);
        filterBar.add(Box.createHorizontalStrut(6));
        filterBar.add(dlBtn);

        // ── Today's list card ──
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD);
        card.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true), new EmptyBorder(0,0,0,0)));

        JPanel head = new JPanel(new BorderLayout());
        head.setBackground(CARD);
        head.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,BORDER),new EmptyBorder(10,14,10,14)));
        JLabel title = new JLabel("📋  Today's Parking List");
        title.setFont(FONT_HEAD);

        // Stats chips
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        chips.setOpaque(false);
        chips.add(chip("Today: " + countToday() + " bookings", FREE, FREE_BG));
        chips.add(chip("Revenue: ₹" + todayRevenue(), GREEN, GREEN_BG));
        head.add(title, BorderLayout.WEST);
        head.add(chips, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        String[] cols = {"Date","Slot","Name","Mobile","Vehicle","Entry","Exit","Status","Amount (₹)"};
        reportModel = new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        reportTable = styledTable(reportModel);
        refreshReportTable();

        JScrollPane sp = new JScrollPane(reportTable);
        sp.setBorder(null); sp.getViewport().setBackground(CARD);
        card.add(sp, BorderLayout.CENTER);

        // Footer total
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.LEFT,10,8));
        foot.setBackground(new Color(250,251,254));
        foot.setBorder(new MatteBorder(1,0,0,0,BORDER));
        JLabel totLbl = new JLabel(bookings.size() + " total records");
        totLbl.setFont(FONT_SMALL); totLbl.setForeground(MUTED);
        foot.add(totLbl);
        card.add(foot, BorderLayout.SOUTH);

        p.add(filterBar, BorderLayout.NORTH);
        p.add(card, BorderLayout.CENTER);
        return p;
    }

    int countToday() {
        long cut = System.currentTimeMillis()-86400000L;
        return (int) bookings.stream().filter(b->b.bookDate>cut).count();
    }
    int todayRevenue() {
        long cut = System.currentTimeMillis()-86400000L;
        return bookings.stream().filter(b->b.bookDate>cut).mapToInt(b->b.paid).sum();
    }

    // Penalty collected today: ₹20 × count of penalty_paid=TRUE bookings
    int todayPenalty() {
        if (dbConnection == null) return (int) bookings.stream().filter(b -> b.penaltyPaid).count() * 20;
        String today = java.time.LocalDate.now().toString();
        try {
            String sql = "SELECT COUNT(*) FROM bookings WHERE booking_date=? AND penalty_paid=TRUE";
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                ps.setString(1, today);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt(1) * 20;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // Refunds given today: ₹20 × count of done bookings with no penalty
    int todayRefund() {
        if (dbConnection == null) return 0;
        String today = java.time.LocalDate.now().toString();
        try {
            String sql = "SELECT COUNT(*) FROM bookings WHERE booking_date=? AND status='done' AND penalty_paid=FALSE";
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                ps.setString(1, today);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt(1) * 20;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    void refreshReportTable() {
        if (reportModel==null) return;
        reportModel.setRowCount(0);
        java.util.Date from = (java.util.Date)fromSpin.getValue();
        java.util.Date to   = (java.util.Date)toSpin.getValue();
        long fms  = from.getTime() - 86400000L; // start of from-day
        long tms  = to.getTime()   + 86400000L; // end of to-day
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
        for (Booking b : bookings) {
            if (b.bookDate < fms || b.bookDate > tms) continue;
            String st = b.status.equals("active")?"● Active":b.status.equals("overdue")?"⚠ Overdue":"✓ Done";
            reportModel.addRow(new Object[]{sdf.format(new java.util.Date(b.bookDate)),b.slot,b.name,b.mobile,b.vehicle,b.entry,b.exit,st,b.paid});
        }
    }

    void exportCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save CSV Report");
        fc.setSelectedFile(new File("ParkSmart_Report.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().endsWith(".csv")) f = new File(f.getAbsolutePath()+".csv");
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println("Date,Slot,Name,Mobile,Vehicle,Entry,Exit,Status,Amount");
            for (int r=0;r<reportModel.getRowCount();r++) {
                StringBuilder sb = new StringBuilder();
                for (int c=0;c<reportModel.getColumnCount();c++) {
                    if (c>0) sb.append(",");
                    Object val = reportModel.getValueAt(r,c);
                    sb.append(val==null?"":val.toString().replace(",",""));
                }
                pw.println(sb);
            }
            JOptionPane.showMessageDialog(this,
                "✅ Report saved!\n" + f.getAbsolutePath(),
                "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage(),"Export Failed",JOptionPane.ERROR_MESSAGE);
        }
    }

    JLabel chip(String text, Color fg, Color bg) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif",Font.BOLD,10)); l.setForeground(fg);
        l.setOpaque(true); l.setBackground(bg);
        l.setBorder(new CompoundBorder(new LineBorder(new Color(fg.getRed(),fg.getGreen(),fg.getBlue(),76),1,true),
            new EmptyBorder(4,10,4,10)));
        return l;
    }

    // ════════════════════════════════════════
    //  MANAGE ADMIN
    // ════════════════════════════════════════
    JPanel buildAdmins() {
        JPanel p = new JPanel(new GridLayout(1,2,14,0));
        p.setBackground(BG); p.setBorder(new EmptyBorder(16,16,16,16));

        // Register card
        JPanel regCard = new JPanel(new BorderLayout());
        regCard.setBackground(CARD);
        regCard.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(0,0,0,0)));
        JPanel regHd = new JPanel(new FlowLayout(FlowLayout.LEFT,12,14));
        regHd.setBackground(new Color(124,58,237,15));
        regHd.setBorder(new MatteBorder(0,0,1,0,BORDER));
        JLabel rIcon = new JLabel("👤"); rIcon.setFont(new Font("SansSerif",Font.PLAIN,20));
        JPanel rHdText = new JPanel(); rHdText.setLayout(new BoxLayout(rHdText,BoxLayout.Y_AXIS)); rHdText.setOpaque(false);
        JLabel rTitle = new JLabel("Register New Admin"); rTitle.setFont(new Font("SansSerif",Font.BOLD,13));
        JLabel rSub = new JLabel("Add a new staff member"); rSub.setFont(FONT_SMALL); rSub.setForeground(MUTED);
        rHdText.add(rTitle); rHdText.add(rSub);
        regHd.add(rIcon); regHd.add(rHdText);

        JPanel regBody = new JPanel(); regBody.setLayout(new BoxLayout(regBody,BoxLayout.Y_AXIS));
        regBody.setBackground(CARD); regBody.setBorder(new EmptyBorder(14,14,14,14));
        regBody.add(fLabel("Full Name *"));
        rNameField = new JTextField(); styleInput(rNameField); rNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE,34));
        regBody.add(rNameField); regBody.add(Box.createVerticalStrut(10));
        regBody.add(fLabel("Mobile Number *"));
        rPhoneField = new JTextField(); styleInput(rPhoneField); rPhoneField.setMaximumSize(new Dimension(Integer.MAX_VALUE,34));
        regBody.add(rPhoneField); regBody.add(Box.createVerticalStrut(10));
        regBody.add(fLabel("Employee ID (Auto-generated)"));
        empIdPreview = new JLabel("EMP-00"+String.valueOf(adminList.size()+1));
        empIdPreview.setFont(new Font("Monospaced",Font.BOLD,13)); empIdPreview.setForeground(PURPLE);
        empIdPreview.setOpaque(true); empIdPreview.setBackground(new Color(240,244,250));
        empIdPreview.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(8,12,8,12)));
        empIdPreview.setMaximumSize(new Dimension(Integer.MAX_VALUE,34));
        regBody.add(empIdPreview); regBody.add(Box.createVerticalStrut(14));
        JButton regBtn = actionBtn("➕  Register Admin", PURPLE, Color.WHITE);
        regBtn.addActionListener(e -> registerAdmin());
        regBody.add(regBtn);

        regCard.add(regHd, BorderLayout.NORTH);
        regCard.add(regBody, BorderLayout.CENTER);

        // List card
        JPanel listCard = new JPanel(new BorderLayout());
        listCard.setBackground(CARD);
        listCard.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(0,0,0,0)));
        JPanel listHd = new JPanel(new BorderLayout());
        listHd.setBackground(CARD);
        listHd.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,BORDER),new EmptyBorder(10,14,10,14)));
        JLabel listTitle = new JLabel("👥  Registered Admins"); listTitle.setFont(FONT_HEAD);
        adminBadge = new JLabel(String.valueOf(adminList.size()));
        adminBadge.setFont(new Font("SansSerif",Font.BOLD,10)); adminBadge.setForeground(PURPLE);
        adminBadge.setOpaque(true); adminBadge.setBackground(PURPLE_BG);
        adminBadge.setBorder(new CompoundBorder(new LineBorder(new Color(124,58,237,76),1,true),new EmptyBorder(2,7,2,7)));
        listHd.add(listTitle,BorderLayout.WEST); listHd.add(adminBadge,BorderLayout.EAST);
        adminListBody = new JPanel(); adminListBody.setLayout(new BoxLayout(adminListBody,BoxLayout.Y_AXIS));
        adminListBody.setBackground(CARD); adminListBody.setBorder(new EmptyBorder(10,10,10,10));
        JScrollPane aScroll = new JScrollPane(adminListBody); aScroll.setBorder(null); aScroll.getViewport().setBackground(CARD);
        listCard.add(listHd, BorderLayout.NORTH); listCard.add(aScroll, BorderLayout.CENTER);

        p.add(regCard); p.add(listCard);

        JScrollPane scroll = new JScrollPane(p); scroll.setBorder(null); scroll.getViewport().setBackground(BG);
        JPanel w = new JPanel(new BorderLayout()); w.setBackground(BG); w.add(scroll, BorderLayout.CENTER);
        return w;
    }

    void registerAdmin() {
        String name = rNameField.getText().trim();
        String phone = rPhoneField.getText().trim();
        if (name.isEmpty()) { JOptionPane.showMessageDialog(this,"Enter admin's full name.","Validation",JOptionPane.WARNING_MESSAGE); return; }
        if (!phone.matches("\\d{10}")) { JOptionPane.showMessageDialog(this,"Enter a valid 10-digit mobile.","Validation",JOptionPane.WARNING_MESSAGE); return; }
        try {
            try (Statement st = dbConnection.createStatement()) { 
                st.execute("CREATE TABLE IF NOT EXISTS admins (phone VARCHAR(15) PRIMARY KEY, name VARCHAR(50), empid VARCHAR(20), reg_date VARCHAR(20))"); 
            }
            String sql = "INSERT INTO admins (phone, name, empid, reg_date) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE name=?";
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                String empId = String.format("EMP-%03d", adminList.size()+1);
                String dt = new SimpleDateFormat("dd MMM yyyy").format(new java.util.Date());
                ps.setString(1, phone); ps.setString(2, name); ps.setString(3, empId); ps.setString(4, dt); ps.setString(5, name);
                ps.executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }

        loadDataFromDB(); // refresh instead of append manually
        rNameField.setText(""); rPhoneField.setText("");
        empIdPreview.setText(String.format("EMP-%03d", adminList.size()+1));
        renderAdminList();
        JOptionPane.showMessageDialog(this,"✅ Admin registered!","Success",JOptionPane.INFORMATION_MESSAGE);
    }

    Color[] avColors = {FREE,PURPLE,GREEN,OCCUPIED,WARN,new Color(8,145,178),new Color(190,24,93)};

    void renderAdminList() {
        adminListBody.removeAll();
        adminBadge.setText(String.valueOf(adminList.size()));
        for (int i=0;i<adminList.size();i++) {
            AdminUser a = adminList.get(i);
            JPanel row = new JPanel(new BorderLayout(10,0));
            row.setBackground(new Color(248,250,253));
            row.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(8,10,8,10)));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE,52));
            JLabel av = new JLabel(String.valueOf(a.name.charAt(0)).toUpperCase());
            av.setFont(new Font("SansSerif",Font.BOLD,13)); av.setForeground(Color.WHITE);
            av.setOpaque(true); av.setBackground(avColors[i%avColors.length]);
            av.setPreferredSize(new Dimension(34,34)); av.setHorizontalAlignment(SwingConstants.CENTER);
            JPanel info = new JPanel(); info.setLayout(new BoxLayout(info,BoxLayout.Y_AXIS)); info.setOpaque(false);
            JLabel nl = new JLabel(a.name + "  [" + a.empid + "]"); nl.setFont(new Font("SansSerif",Font.BOLD,12));
            JLabel ml = new JLabel("📱 +91 " + a.phone + "  · " + a.registeredOn);
            ml.setFont(new Font("SansSerif",Font.PLAIN,10)); ml.setForeground(MUTED);
            info.add(nl); info.add(ml);
            final int idx=i;
            JButton del = new JButton("✕");
            del.setFont(new Font("SansSerif",Font.BOLD,10)); del.setForeground(MUTED);
            del.setBackground(Color.WHITE);
            del.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(3,6,3,6)));
            del.setFocusPainted(false); del.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            del.addActionListener(e -> {
                if (JOptionPane.showConfirmDialog(this,"Remove "+adminList.get(idx).name+"?","Confirm",JOptionPane.YES_NO_OPTION)==0) {
                    adminList.remove(idx); renderAdminList();
                    empIdPreview.setText(String.format("EMP-%03d",adminList.size()+1));
                }
            });
            row.add(av,BorderLayout.WEST); row.add(info,BorderLayout.CENTER); row.add(del,BorderLayout.EAST);
            adminListBody.add(row); adminListBody.add(Box.createVerticalStrut(6));
        }
        adminListBody.revalidate(); adminListBody.repaint();
    }

    // ════════════════════════════════════════
    //  TAB SWITCH
    // ════════════════════════════════════════
    void switchTab(String tab) {
        activeTab = tab;
        cardLayout.show(contentArea, tab);
        Map<String,String> titles = new HashMap<>();
        titles.put("dashboard","Dashboard"); titles.put("reports","Reports"); titles.put("admins","Manage Admin");
        topbarTitle.setText(titles.getOrDefault(tab, tab));
        navButtons.forEach((t,btn) -> {
            boolean active = t.equals(tab);
            btn.setFont(new Font("SansSerif",active?Font.BOLD:Font.PLAIN,12));
            btn.setForeground(active?(t.equals("admins")?PURPLE:FREE):MUTED);
        });
        if (tab.equals("admins"))  renderAdminList();
        if (tab.equals("reports")) refreshReportTable();
        contentArea.revalidate(); contentArea.repaint();
    }

    // ════════════════════════════════════════
    //  CLOCK
    // ════════════════════════════════════════


    // ════════════════════════════════════════
    //  COMPONENT HELPERS
    // ════════════════════════════════════════
    JPanel sectionCard(String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD);
        card.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(0,0,0,0)));
        JPanel hd = new JPanel(new FlowLayout(FlowLayout.LEFT,12,10));
        hd.setBackground(CARD); hd.setBorder(new MatteBorder(0,0,1,0,BORDER));
        JLabel t = new JLabel(title); t.setFont(FONT_HEAD);
        hd.add(t); card.add(hd, BorderLayout.NORTH);
        return card;
    }

    JPanel statCard(String lbl, JLabel val, Color accent, String sub, Color vc) {
        JPanel card = new JPanel(){
            protected void paintComponent(Graphics g){super.paintComponent(g);g.setColor(accent);g.fillRect(0,0,getWidth(),3);}
        };
        card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(14,14,14,14)));
        JLabel l = new JLabel(lbl); l.setFont(new Font("SansSerif",Font.BOLD,9)); l.setForeground(MUTED);
        val.setFont(FONT_STAT); val.setForeground(vc);
        JLabel s = new JLabel(sub); s.setFont(FONT_SMALL); s.setForeground(MUTED);
        card.add(l); card.add(Box.createVerticalStrut(6)); card.add(val);
        card.add(Box.createVerticalStrut(4)); card.add(s);
        return card;
    }

    JPanel alertRow(String icon,String title,String sub,String time,Color ac,Color bg){
        JPanel item = new JPanel(new BorderLayout(8,0));
        item.setBackground(bg);
        item.setBorder(new CompoundBorder(new LineBorder(new Color(ac.getRed(),ac.getGreen(),ac.getBlue(),76),1,true),new EmptyBorder(8,10,8,10)));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE,58));
        JLabel ico = new JLabel(icon); ico.setFont(new Font("SansSerif",Font.PLAIN,14));
        JPanel tp = new JPanel(); tp.setLayout(new BoxLayout(tp,BoxLayout.Y_AXIS)); tp.setOpaque(false);
        JLabel tl = new JLabel(title); tl.setFont(new Font("SansSerif",Font.BOLD,11));
        JLabel sl = new JLabel(sub); sl.setFont(new Font("SansSerif",Font.PLAIN,10)); sl.setForeground(MUTED);
        tp.add(tl); tp.add(sl);
        JLabel tm = new JLabel(time); tm.setFont(new Font("Monospaced",Font.PLAIN,10)); tm.setForeground(MUTED2);
        item.add(ico,BorderLayout.WEST); item.add(tp,BorderLayout.CENTER); item.add(tm,BorderLayout.EAST);
        return item;
    }

    JPanel infoCell(String label, String value) {
        JPanel cell = new JPanel(); cell.setLayout(new BoxLayout(cell,BoxLayout.Y_AXIS));
        cell.setBackground(new Color(248,250,253));
        cell.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(6,9,6,9)));
        JLabel l = new JLabel(label.toUpperCase()); l.setFont(new Font("SansSerif",Font.BOLD,9)); l.setForeground(MUTED);
        JLabel v = new JLabel(value); v.setFont(new Font("SansSerif",Font.BOLD,12));
        cell.add(l); cell.add(Box.createVerticalStrut(2)); cell.add(v);
        return cell;
    }

    JTable styledTable(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setFont(FONT_BODY); t.setRowHeight(32); t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0,0));
        t.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,10));
        t.getTableHeader().setBackground(new Color(250,251,254));
        t.getTableHeader().setForeground(MUTED);
        t.getTableHeader().setBorder(new MatteBorder(0,0,1,0,BORDER));
        t.setSelectionBackground(FREE_BG); t.setSelectionForeground(TEXT);
        t.setBackground(CARD); t.setForeground(TEXT);
        t.setShowHorizontalLines(true); t.setGridColor(BORDER);
        return t;
    }

    JButton actionBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif",Font.BOLD,12)); b.setForeground(fg); b.setBackground(bg);
        b.setBorder(new CompoundBorder(new LineBorder(bg.darker(),1,true),new EmptyBorder(9,14,9,14)));
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }

    JButton outlineBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif",Font.PLAIN,11)); b.setForeground(MUTED); b.setBackground(CARD);
        b.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(8,14,8,14)));
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }

    void styleInput(JTextField tf) {
        tf.setFont(FONT_BODY); tf.setForeground(TEXT); tf.setBackground(new Color(248,250,253));
        tf.setBorder(new CompoundBorder(new LineBorder(BORDER,1,true),new EmptyBorder(8,10,8,10)));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    JLabel fLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif",Font.BOLD,10)); l.setForeground(MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT); return l;
    }

    // ════════════════════════════════════════
    //  MAIN
    // ════════════════════════════════════════
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ParkSmartAdmin());
    }
}
