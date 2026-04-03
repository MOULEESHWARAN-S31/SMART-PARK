  package com.parksmart;

import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

public class ParkSmartUserPage extends JFrame {

    // ── Colours ──────────────────────────────────────────────────────────
    static final Color BG       = new Color(240, 244, 250);
    static final Color CARD     = Color.WHITE;
    static final Color BORDER   = new Color(220, 229, 240);
    static final Color FREE_C   = new Color(26, 115, 232);
    static final Color FREE_BG  = new Color(26, 115, 232, 22);
    static final Color OCC_C    = new Color(229, 57, 53);
    static final Color OCC_BG   = new Color(229, 57, 53, 18);
    static final Color MUTED    = new Color(107, 124, 153);
    static final Color TEXT     = new Color(26, 35, 50);

    // ── Slot IDs ──────────────────────────────────────────────────────────
    static final String[] SLOT_IDS = {
        "A1","A2","A3","A4","A5","A6","A7","A8",
        "A9","A10","A11","A12","A13","A14","A15","A16"
    };

    // ── Demo bookings: dateKey → list of SlotBooking ──────────────────────
    // SlotBooking: slotId, entryMins, exitMins (minutes since midnight)
    static class SlotBooking {
        String slotId, dateDisp;
        int entryMins, exitMins;
        SlotBooking(String id, int e, int x, String d) {
            slotId=id; entryMins=e; exitMins=x; dateDisp=d;
        }
    }
    final Map<String, List<SlotBooking>> bookingsByDate = new HashMap<>();

    // ── State ─────────────────────────────────────────────────────────────
    LocalDate selectedDate = LocalDate.now();
    LocalDate calMonth     = LocalDate.now().withDayOfMonth(1);

    // ── UI refs ───────────────────────────────────────────────────────────

    JLabel calMonthLabel;
    JPanel calDaysPanel;
    JLabel freeCountLabel, occCountLabel;
    JLabel selectedDateLabel;
    JPanel slotGridPanel;

    // ── Helpers ───────────────────────────────────────────────────────────
    static String dateKey(LocalDate d) { return d.toString(); }

    static int nowMins() {
        LocalTime t = LocalTime.now();
        return t.getHour()*60 + t.getMinute();
    }

    static String fmt12(int totalMins) {
        if (totalMins < 0) totalMins = 0;
        int h = (totalMins/60)%24, m = totalMins%60;
        String ap = h>=12?"PM":"AM";
        int h12 = h%12; if(h12==0) h12=12;
        return String.format("%02d:%02d %s", h12, m, ap);
    }

    static int parse12(String s) {
        if(s==null||s.isBlank()) return -1;
        try {
            String[] parts = s.trim().split(" ");
            String[] hm = parts[0].split(":");
            int h = Integer.parseInt(hm[0]), m = Integer.parseInt(hm[1]);
            String ap = parts[1].toUpperCase();
            if(ap.equals("PM") && h!=12) h+=12;
            if(ap.equals("AM") && h==12) h=0;
            return h*60+m;
        } catch(Exception e){ return -1; }
    }

    static String fmtDate(LocalDate d) {
        return d.format(DateTimeFormatter.ofPattern("d MMM yyyy"));
    }

    // ── Constructor ───────────────────────────────────────────────────────
    String loggedInMobile;
    Connection dbConnection;

    public ParkSmartUserPage(String mobile, Connection dbConnection) {
        super("ParkSmart – Hotel Parking");
        this.loggedInMobile = mobile;
        this.dbConnection = dbConnection;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 780);
        setLocationRelativeTo(null);
        setResizable(true);
        getContentPane().setBackground(BG);

        syncBookingsFromDB();

        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout(16, 16));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // title row
        JPanel titleRow = new JPanel();
        titleRow.setOpaque(false);
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.Y_AXIS));
        JLabel pageTitle = new JLabel("Hotel Parking");
        pageTitle.setFont(new Font("Segoe UI Black", Font.BOLD, 30));
        pageTitle.setForeground(TEXT);
        JLabel pageSub = new JLabel("Pick a date on the calendar — then click a slot to book.");
        pageSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pageSub.setForeground(MUTED);
        pageSub.setBorder(BorderFactory.createEmptyBorder(4,0,12,0));
        titleRow.add(pageTitle);
        titleRow.add(pageSub);

        // legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        legend.setOpaque(false);
        legend.add(legendItem(FREE_C, "Available"));
        legend.add(legendItem(OCC_C,  "Occupied"));
        titleRow.add(legend);

        main.add(titleRow, BorderLayout.NORTH);

        // two-column centre
        JPanel centre = new JPanel(new BorderLayout(16, 0));
        centre.setOpaque(false);
        centre.add(buildCalendarCard(), BorderLayout.WEST);
        centre.add(buildRightPanel(),   BorderLayout.CENTER);
        main.add(centre, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);

        // refresh only
        startSlotRefresh();

        refreshCalendar();
        refreshSlotGrid();
    }

    // ── Header ────────────────────────────────────────────────────────────
    JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(CARD);
        h.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0,0,1,0,BORDER),
            BorderFactory.createEmptyBorder(0,24,0,24)
        ));
        h.setPreferredSize(new Dimension(0, 64));

        // Logo
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        logoPanel.setOpaque(false);
        JLabel l1 = new JLabel("Park");
        l1.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        l1.setForeground(TEXT);
        JLabel l2 = new JLabel("Smart");
        l2.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        l2.setForeground(FREE_C);
        logoPanel.add(l1); logoPanel.add(l2);

        // Logout button
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setBackground(new Color(229, 57, 53));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setOpaque(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        logoutBtn.addActionListener(e -> {
            ParkSmartApp loginPage = new ParkSmartApp();
            loginPage.setVisible(true);
            dispose();
        });

        // Custom paint for rounded logout button
        JButton styledLogout = new JButton("Logout") {
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
        styledLogout.setFont(new Font("Segoe UI", Font.BOLD, 13));
        styledLogout.setForeground(Color.WHITE);
        styledLogout.setFocusPainted(false);
        styledLogout.setContentAreaFilled(false);
        styledLogout.setOpaque(false);
        styledLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        styledLogout.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        styledLogout.setPreferredSize(new Dimension(100, 36));
        styledLogout.addActionListener(e -> {
            ParkSmartApp loginPage = new ParkSmartApp();
            loginPage.setVisible(true);
            dispose();
        });

        // Custom paint for rounded refresh button
        JButton styledRefresh = new JButton("Refresh") {
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
        styledRefresh.setFont(new Font("Segoe UI", Font.BOLD, 13));
        styledRefresh.setForeground(Color.WHITE);
        styledRefresh.setFocusPainted(false);
        styledRefresh.setContentAreaFilled(false);
        styledRefresh.setOpaque(false);
        styledRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        styledRefresh.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        styledRefresh.setPreferredSize(new Dimension(100, 36));
        styledRefresh.addActionListener(e -> {
            syncBookingsFromDB();
            refreshSlotGrid();
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 16));
        right.setOpaque(false);
        right.add(styledRefresh);
        right.add(styledLogout);

        h.add(logoPanel, BorderLayout.WEST);
        h.add(right,     BorderLayout.EAST);
        return h;
    }

    // ── Calendar card ─────────────────────────────────────────────────────
    JPanel buildCalendarCard() {
        RPanel card = new RPanel(18, CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(14,14,14,14)
        ));
        card.setPreferredSize(new Dimension(280, 0));

        // month nav
        JPanel nav = new JPanel(new BorderLayout());
        nav.setOpaque(false);
        JButton prev = navBtn("‹");
        JButton next = navBtn("›");
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

        // days grid (7 cols: headers + day cells)
        calDaysPanel = new JPanel(new GridLayout(0, 7, 3, 3));
        calDaysPanel.setOpaque(false);

        // stat row
        freeCountLabel = new JLabel("—");
        freeCountLabel.setFont(new Font("Segoe UI Black", Font.BOLD, 18));
        freeCountLabel.setForeground(FREE_C);
        occCountLabel  = new JLabel("—");
        occCountLabel.setFont(new Font("Segoe UI Black", Font.BOLD, 18));
        occCountLabel.setForeground(OCC_C);

        JPanel stats = new JPanel(new GridLayout(1, 2, 8, 0));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        stats.add(statBox("🟢", freeCountLabel, "Free Now",   FREE_BG));
        stats.add(statBox("🔴", occCountLabel,  "Occupied",   OCC_BG));

        card.add(nav);
        card.add(Box.createRigidArea(new Dimension(0,8)));
        card.add(calDaysPanel);
        card.add(Box.createRigidArea(new Dimension(0,10)));
        card.add(stats);
        return card;
    }

    // ── Right panel  ──────────────────────────────────────────────────────
    JPanel buildRightPanel() {
        JPanel p = new JPanel(new BorderLayout(0,12));
        p.setOpaque(false);

        selectedDateLabel = new JLabel();
        selectedDateLabel.setFont(new Font("Segoe UI Black", Font.BOLD, 16));
        selectedDateLabel.setForeground(TEXT);
        selectedDateLabel.setBorder(BorderFactory.createEmptyBorder(0,0,4,0));

        JLabel sectionLbl = new JLabel("ZONE A – GROUND FLOOR");
        sectionLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        sectionLbl.setForeground(MUTED);
        sectionLbl.setBorder(BorderFactory.createEmptyBorder(0,0,4,0));

        slotGridPanel = new JPanel(new GridLayout(4, 4, 10, 10));
        slotGridPanel.setOpaque(false);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(selectedDateLabel);
        top.add(sectionLbl);

        p.add(top,          BorderLayout.NORTH);
        p.add(slotGridPanel,BorderLayout.CENTER);
        return p;
    }

    // ── Refresh calendar ──────────────────────────────────────────────────
    void refreshCalendar() {
        calMonthLabel.setText(calMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        calDaysPanel.removeAll();

        // day-name headers
        for (String d : new String[]{"Sun","Mon","Tue","Wed","Thu","Fri","Sat"}) {
            JLabel lbl = new JLabel(d, JLabel.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
            lbl.setForeground(MUTED);
            calDaysPanel.add(lbl);
        }

        LocalDate firstOfMonth = calMonth;
        int startDow = firstOfMonth.getDayOfWeek().getValue() % 7; // Sun=0
        int daysInMonth = calMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        // empty cells
        for (int i = 0; i < startDow; i++) {
            calDaysPanel.add(new JLabel());
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = calMonth.withDayOfMonth(day);
            boolean isPast     = date.isBefore(today);
            boolean isToday    = date.equals(today);
            boolean isSel      = date.equals(selectedDate);
            boolean hasBooking = bookingsByDate.containsKey(dateKey(date)) &&
                                 !bookingsByDate.get(dateKey(date)).isEmpty();

            // Wrap cell in a small panel to allow dot indicator beneath number
            JPanel cellWrap = new JPanel();
            cellWrap.setLayout(new BoxLayout(cellWrap, BoxLayout.Y_AXIS));
            cellWrap.setOpaque(false);

            JLabel cell = new JLabel(String.valueOf(day), JLabel.CENTER);
            cell.setFont(new Font("Segoe UI", Font.BOLD, 12));
            cell.setOpaque(true);
            cell.setHorizontalAlignment(JLabel.CENTER);
            cell.setBorder(BorderFactory.createEmptyBorder(3,2,1,2));
            cell.setAlignmentX(Component.CENTER_ALIGNMENT);

            if (isPast) {
                cell.setForeground(new Color(197, 205, 216));
                cell.setBackground(new Color(248,250,253));
            } else if (isToday) {
                cell.setBackground(FREE_C);
                cell.setForeground(Color.WHITE);
            } else if (isSel) {
                cell.setBackground(FREE_BG);
                cell.setForeground(FREE_C);
                cell.setBorder(BorderFactory.createLineBorder(FREE_C, 1));
            } else {
                cell.setBackground(new Color(235,245,255));
                cell.setForeground(TEXT);
            }

            // Booking dot indicator
            JLabel dot = new JLabel("●", JLabel.CENTER);
            dot.setFont(new Font("Segoe UI", Font.PLAIN, 6));
            dot.setAlignmentX(Component.CENTER_ALIGNMENT);
            if (hasBooking && !isPast) {
                // Check if any occupied now
                List<SlotBooking> blist = bookingsByDate.get(dateKey(date));
                boolean hasOcc = false;
                int now2 = nowMins();
                for (SlotBooking b : blist) {
                    if (isToday && now2 >= b.entryMins && now2 < b.exitMins) { hasOcc = true; break; }
                    else if (!isToday) { hasOcc = true; break; }
                }
                dot.setForeground(hasOcc ? OCC_C : FREE_C);
                dot.setVisible(true);
            } else {
                dot.setVisible(false);
            }

            cellWrap.add(cell);
            cellWrap.add(dot);

            if (!isPast) {
                final LocalDate d = date;
                cellWrap.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                cellWrap.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        selectedDate = d;
                        refreshCalendar();
                        refreshSlotGrid();
                    }
                    public void mouseEntered(MouseEvent e) {
                        if (!d.equals(selectedDate) && !d.equals(today))
                            cell.setBackground(FREE_BG);
                    }
                    public void mouseExited(MouseEvent e) {
                        if (!d.equals(selectedDate) && !d.equals(today))
                            cell.setBackground(new Color(235,245,255));
                    }
                });
            }
            calDaysPanel.add(cellWrap);
        }
        calDaysPanel.revalidate();
        calDaysPanel.repaint();
    }

    // ── Refresh slot grid ─────────────────────────────────────────────────
    void refreshSlotGrid() {
        selectedDateLabel.setText(fmtDate(selectedDate) +
            (selectedDate.equals(LocalDate.now()) ? "  TODAY" : ""));

        String key = dateKey(selectedDate);
        boolean isToday = selectedDate.equals(LocalDate.now());
        int now = nowMins();
        List<SlotBooking> blist = bookingsByDate.getOrDefault(key, Collections.emptyList());

        slotGridPanel.removeAll();
        int free = 0, occ = 0;

        for (String id : SLOT_IDS) {
            SlotBooking booking = null;
            for (SlotBooking b : blist) {
                if (b.slotId.equals(id)) { booking = b; break; }
            }

            boolean occupied = false;
            boolean upcoming = false;
            String tag = "FREE";
            final SlotBooking fb = booking;

            if (booking != null) {
                if (isToday) {
                    if (now >= booking.entryMins && now < booking.exitMins) {
                        occupied = true;
                        int rem = booking.exitMins - now;
                        tag = rem >= 60
                            ? String.format("%dh %dm left", rem/60, rem%60)
                            : rem + "m left";
                    } else if (now < booking.entryMins) {
                        upcoming = true;
                        tag = "from " + fmt12(booking.entryMins);
                    }
                    // past booking → slot is free again
                } else {
                    occupied = true;
                    tag = fmt12(booking.entryMins) + "–" + fmt12(booking.exitMins);
                }
            }

            if (occupied) occ++; else free++;

            final Color slotBg     = occupied ? OCC_BG : FREE_BG;
            final Color slotBorder  = occupied ? new Color(229,57,53,120) : new Color(26,115,232,120);
            RPanel slot = new RPanel(16, slotBg) {
                @Override public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(slotBorder);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 16, 16);
                    g2.dispose();
                }
            };
            slot.setLayout(new BoxLayout(slot, BoxLayout.Y_AXIS));
            slot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel idLbl = new JLabel(id, JLabel.CENTER);
            idLbl.setFont(new Font("Segoe UI Black", Font.BOLD, 14));
            idLbl.setForeground(occupied ? OCC_C : FREE_C);
            idLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel tagLbl = new JLabel(tag, JLabel.CENTER);
            tagLbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            tagLbl.setForeground(occupied ? OCC_C : (upcoming ? new Color(180,115,20) : FREE_C));
            tagLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            slot.add(Box.createVerticalGlue());
            slot.add(idLbl);
            slot.add(Box.createRigidArea(new Dimension(0,2)));
            slot.add(tagLbl);
            slot.add(Box.createVerticalGlue());

            final boolean isOcc = occupied;
            slot.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    openBookingDialog(id, isOcc, fb);
                }
            });

            slotGridPanel.add(slot);
        }

        freeCountLabel.setText(String.valueOf(free));
        occCountLabel.setText(String.valueOf(occ));
        slotGridPanel.revalidate();
        slotGridPanel.repaint();
    }

    // ── Booking dialog ────────────────────────────────────────────────────
    void openBookingDialog(String slotId, boolean occupied, SlotBooking existingBooking) {
        JDialog dlg = new JDialog(this, "Book Slot " + slotId, true);
        dlg.setSize(480, 640);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        // OTP store for this dialog
        final String[] dialogOtp = {null};

        CardLayout cl = new CardLayout();
        JPanel root = new JPanel(cl);
        root.setBorder(BorderFactory.createEmptyBorder(20,22,20,22));
        root.setBackground(CARD);
        dlg.add(root);

        // ── Step 0: Occupied info ─────────────────────────────────────────
        JPanel step0 = new JPanel();
        step0.setBackground(CARD);
        step0.setLayout(new BoxLayout(step0, BoxLayout.Y_AXIS));

        // ── Step 0: Occupied info (own slot vs other's slot) ─────────────
        boolean isOwnBooking = isUserOwnBooking(slotId);

        if (isOwnBooking && existingBooking != null) {
            // ── OWN SLOT: Extend / Cancel / Exit Verify ──────────────────
            JLabel s0title = sectionHead("Your Booked Slot");
            JLabel s0sub   = mutedLbl("Manage your active booking for slot " + slotId);
            JLabel s0badge = badge(slotId + " – Your Booking", FREE_C, FREE_BG);

            RPanel ownPanel = new RPanel(12, new Color(240, 248, 255));
            ownPanel.setLayout(new BoxLayout(ownPanel, BoxLayout.Y_AXIS));
            ownPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(26,115,232,64),2,true),
                BorderFactory.createEmptyBorder(10,12,10,12)
            ));
            ownPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
            ownPanel.add(occInfoRow("Slot",       slotId));
            ownPanel.add(occInfoRow("Entry Time", fmt12(existingBooking.entryMins)));
            ownPanel.add(occInfoRow("Exit Time",  fmt12(existingBooking.exitMins)));
            ownPanel.add(occInfoRow("Status",     "● Active"));

            JButton extendBtn = bigBtn("⏰  Extend Time",  new Color(245, 158, 11));
            JButton cancelBtn = bigBtn("✕  Cancel Slot",   new Color(229, 57, 53));
            JButton exitBtn   = bigBtn("✓  Exit Verify",   new Color(22, 163, 74));

            extendBtn.addActionListener(ev -> showExtendDialog(dlg, existingBooking));
            cancelBtn.addActionListener(ev -> {
                int confirm = JOptionPane.showConfirmDialog(dlg,
                    "Cancel booking for slot " + slotId + "?\nYour ₹20 deposit will be refunded.",
                    "Confirm Cancel", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    cancelBookingInDB(slotId);
                    String k2 = dateKey(selectedDate);
                    List<SlotBooking> bl = bookingsByDate.get(k2);
                    if (bl != null) bl.removeIf(b -> b.slotId.equals(slotId));
                    refreshSlotGrid(); refreshCalendar(); dlg.dispose();
                    JOptionPane.showMessageDialog(null,
                        "✅ Slot " + slotId + " cancelled.\n₹20 deposit refunded.",
                        "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            exitBtn.addActionListener(ev -> {
                String exitOtp = String.format("%04d", new java.util.Random().nextInt(10000));
                ExitOtpStore.put(dbConnection, slotId, exitOtp);
                showExitOtpPopup(dlg, slotId, exitOtp);
            });

            step0.add(s0title); step0.add(s0sub); step0.add(Box.createRigidArea(new Dimension(0,8)));
            step0.add(s0badge); step0.add(Box.createRigidArea(new Dimension(0,10)));
            step0.add(ownPanel); step0.add(Box.createRigidArea(new Dimension(0,14)));
            step0.add(extendBtn); step0.add(Box.createRigidArea(new Dimension(0,6)));
            step0.add(cancelBtn); step0.add(Box.createRigidArea(new Dimension(0,6)));
            step0.add(exitBtn);
        } else {
            // ── OTHER'S SLOT: show occupied info ─────────────────────────
            JLabel s0title = sectionHead("Slot Currently Occupied");
            JLabel s0sub   = mutedLbl("Current booking details for this slot");
            JLabel s0badge = badge(slotId + " – Occupied", OCC_C, OCC_BG);

            RPanel occPanel = new RPanel(12, new Color(255,245,245));
            occPanel.setLayout(new BoxLayout(occPanel, BoxLayout.Y_AXIS));
            occPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(229,57,53,64),2,true),
                BorderFactory.createEmptyBorder(10,12,10,12)
            ));
            occPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
            String occExit = existingBooking!=null ? fmt12(existingBooking.exitMins) : "—";
            occPanel.add(occInfoRow("Date",       existingBooking!=null ? existingBooking.dateDisp : fmtDate(selectedDate)));
            occPanel.add(occInfoRow("Entry Time", existingBooking!=null ? fmt12(existingBooking.entryMins) : "—"));
            occPanel.add(occInfoRow("Exit Time",  occExit));
            occPanel.add(occInfoRow("Status",     "● Occupied"));

            JPanel freeBanner = new RPanel(10, FREE_BG);
            freeBanner.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
            freeBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            JLabel freeFromLbl = new JLabel("🕐  Slot free from " + occExit);
            freeFromLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            freeFromLbl.setForeground(FREE_C);
            freeBanner.add(freeFromLbl);

            JButton bookAfterBtn = bigBtn("Book After " + occExit + " →", FREE_C);
            JButton cancelBtn0   = outlineBtn("Cancel – Choose Another Slot");
            bookAfterBtn.addActionListener(e -> cl.show(root, "step1"));
            cancelBtn0.addActionListener(e -> dlg.dispose());

            step0.add(s0title); step0.add(s0sub); step0.add(Box.createRigidArea(new Dimension(0,8)));
            step0.add(s0badge); step0.add(Box.createRigidArea(new Dimension(0,10)));
            step0.add(occPanel); step0.add(Box.createRigidArea(new Dimension(0,8)));
            step0.add(freeBanner); step0.add(Box.createRigidArea(new Dimension(0,14)));
            step0.add(bookAfterBtn); step0.add(Box.createRigidArea(new Dimension(0,6)));
            step0.add(cancelBtn0);
        }

        // ── Step 1: Booking form ──────────────────────────────────────────
        JPanel step1 = new JPanel();
        step1.setBackground(CARD);
        step1.setLayout(new BoxLayout(step1, BoxLayout.Y_AXIS));

        JLabel s1title = sectionHead("Book Slot " + slotId);
        JLabel s1sub   = mutedLbl("Enter your booking details");
        JLabel s1badge = badge(slotId + " – Available", FREE_C, FREE_BG);

        JTextField nameF    = formField();
        JTextField mobileF  = formField();
        mobileF.setText(loggedInMobile != null ? loggedInMobile : "");
        mobileF.setEditable(false);
        mobileF.setBackground(new Color(235, 240, 248));
        mobileF.setForeground(new Color(80, 100, 130));
        JTextField vehicleF = formField();
        JTextField entryF   = formField();
        JTextField exitF    = formField();
        entryF.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));
        exitF.setText(LocalTime.now().plusHours(1).format(DateTimeFormatter.ofPattern("hh:mm a")));

        JPanel pricingBar = new RPanel(10, new Color(248,250,253));
        pricingBar.setLayout(new BorderLayout());
        pricingBar.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER,1,true),
            BorderFactory.createEmptyBorder(8,12,8,12)
        ));
        pricingBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        JLabel priceLbl   = new JLabel("<html><b style='font-size:14px'>₹50</b><br><span style='color:#6b7c99;font-size:10px'>₹30 rent + ₹20 deposit</span></html>");
        JLabel depositLbl = new JLabel("<html><span style='color:#6b7c99;font-size:11px'>Total Amount</span><br><span style='font-size:10px;color:#1a73e8'>₹20 refundable deposit</span></html>");
        pricingBar.add(depositLbl, BorderLayout.WEST);
        pricingBar.add(priceLbl,   BorderLayout.EAST);

        JButton sendOtpBtn = bigBtn("Send OTP & Continue →", FREE_C);
        sendOtpBtn.addActionListener(e -> {
            if (nameF.getText().isBlank()||mobileF.getText().isBlank()||
                vehicleF.getText().isBlank()||entryF.getText().isBlank()||exitF.getText().isBlank()) {
                JOptionPane.showMessageDialog(dlg,"Please fill all fields.","Validation",JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (mobileF.getText().trim().length() < 10) {
                JOptionPane.showMessageDialog(dlg,"Enter a valid 10-digit mobile.","Validation",JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Generate OTP and show in POPUP (not terminal)
            String otp = String.format("%04d", new java.util.Random().nextInt(10000));
            dialogOtp[0] = otp;
            showBookingOtpPopup(dlg, mobileF.getText().trim(), otp);
            cl.show(root,"step2");
        });

        step1.add(s1title); step1.add(s1sub); step1.add(Box.createRigidArea(new Dimension(0,6)));
        step1.add(s1badge); step1.add(Box.createRigidArea(new Dimension(0,8)));
        step1.add(formRow("Full Name",      nameF));
        step1.add(formRow("Mobile Number",  mobileF));
        step1.add(formRow("Vehicle Number", vehicleF));
        step1.add(formRow("Entry Time (hh:mm AM/PM)", entryF));
        step1.add(formRow("Exit Time  (hh:mm AM/PM)", exitF));
        step1.add(Box.createRigidArea(new Dimension(0,6)));
        step1.add(pricingBar);
        step1.add(Box.createRigidArea(new Dimension(0,12)));
        step1.add(sendOtpBtn);

        // ── Step 2: OTP ───────────────────────────────────────────────────
        JPanel step2 = new JPanel();
        step2.setBackground(CARD);
        step2.setLayout(new BoxLayout(step2, BoxLayout.Y_AXIS));

        JLabel s2title = sectionHead("Verify OTP");
        JLabel s2sub   = mutedLbl("OTP has been shown in a popup on your screen");

        JTextField[] otpBoxes = new JTextField[4];
        JPanel otpRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        otpRow.setOpaque(false);
        otpRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < 4; i++) {
            JTextField tf = new JTextField(1) {
                @Override public void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                    g2.dispose();
                    super.paintComponent(g);
                }
                @Override public void paintBorder(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(BORDER);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 18, 18);
                    g2.dispose();
                }
            };
            tf.setHorizontalAlignment(JTextField.CENTER);
            tf.setFont(new Font("Segoe UI Black", Font.BOLD, 20));
            tf.setPreferredSize(new Dimension(52, 52));
            tf.setOpaque(false);
            tf.setBackground(new Color(248,250,253));
            tf.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
            final int idx = i;
            tf.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
                void upd(){ if(tf.getText().length()==1 && idx<3) otpBoxes[idx+1].requestFocus(); }
                public void insertUpdate(javax.swing.event.DocumentEvent e){upd();}
                public void removeUpdate(javax.swing.event.DocumentEvent e){}
                public void changedUpdate(javax.swing.event.DocumentEvent e){}
            });
            otpBoxes[i] = tf;
            otpRow.add(tf);
        }

        // Declare receipt early so verifyBtn lambda can access it
        RPanel receipt = new RPanel(12, new Color(248,250,253));
        receipt.setLayout(new BoxLayout(receipt, BoxLayout.Y_AXIS));
        receipt.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER,1,true),
            BorderFactory.createEmptyBorder(10,14,10,14)
        ));
        receipt.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        JButton verifyBtn = bigBtn("Verify & Confirm Booking →", FREE_C);
        JButton backBtn   = outlineBtn("← Back");
        verifyBtn.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            for (JTextField tf : otpBoxes) sb.append(tf.getText().trim());
            if (dialogOtp[0] == null || !sb.toString().equals(dialogOtp[0])) {
                JOptionPane.showMessageDialog(dlg,"Invalid OTP. Check terminal for the correct OTP.","OTP Error",JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Save booking locally
            int entry = parse12(entryF.getText().trim());
            int exit  = parse12(exitF.getText().trim());
            String key = dateKey(selectedDate);
            bookingsByDate.computeIfAbsent(key, k -> new ArrayList<>())
                .add(new SlotBooking(slotId, entry < 0 ? 0 : entry, exit < 0 ? 60 : exit, fmtDate(selectedDate)));
            
            // Save booking to Database
            if (dbConnection != null) {
                String sql = "INSERT IGNORE INTO bookings (mobile, name, slot_id, booking_date, entry_time, exit_time, vehicle, status, amount) VALUES (?, ?, ?, ?, ?, ?, ?, 'confirmed', 50.0)";
                try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                    stmt.setString(1, loggedInMobile != null ? loggedInMobile : "9876543210");
                    stmt.setString(2, nameF.getText().trim());
                    stmt.setString(3, slotId);
                    stmt.setString(4, selectedDate.toString());
                    stmt.setString(5, entryF.getText().trim());
                    stmt.setString(6, exitF.getText().trim());
                    stmt.setString(7, vehicleF.getText().trim());
                    stmt.executeUpdate();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

                // Try to update user name if provided
                if (!nameF.getText().trim().isEmpty()) {
                    try (PreparedStatement stmt = dbConnection.prepareStatement("UPDATE users SET name = ? WHERE mobile = ?")) {
                        stmt.setString(1, nameF.getText().trim());
                        stmt.setString(2, loggedInMobile != null ? loggedInMobile : "9876543210");
                        stmt.executeUpdate();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            refreshSlotGrid();
            refreshCalendar();
            // Build receipt dynamically with actual form values
            receipt.removeAll();
            receipt.add(receiptRow("Slot",       slotId));
            receipt.add(receiptRow("Name",       nameF.getText()));
            receipt.add(receiptRow("Vehicle",    vehicleF.getText()));
            receipt.add(receiptRow("Date",       fmtDate(selectedDate)));
            receipt.add(receiptRow("Entry",      entryF.getText()));
            receipt.add(receiptRow("Exit",       exitF.getText()));
            receipt.add(receiptRow("Amount Paid","₹50"));
            receipt.revalidate();
            receipt.repaint();
            cl.show(root,"step3");
        });
        backBtn.addActionListener(e -> cl.show(root,"step1"));

        step2.add(s2title); step2.add(s2sub);
        step2.add(Box.createRigidArea(new Dimension(0,20)));
        step2.add(new JLabel("Enter 4-Digit OTP"));
        step2.add(Box.createRigidArea(new Dimension(0,6)));
        step2.add(otpRow);
        step2.add(Box.createRigidArea(new Dimension(0,20)));
        step2.add(verifyBtn);
        step2.add(Box.createRigidArea(new Dimension(0,6)));
        step2.add(backBtn);

        // ── Step 3: Success ───────────────────────────────────────────────
        JPanel step3 = new JPanel();
        step3.setBackground(CARD);
        step3.setLayout(new BoxLayout(step3, BoxLayout.Y_AXIS));

        // Green tick icon — large checkmark on a soft green circle background
        JPanel tickCircle = new RPanel(40, new Color(220, 252, 231)) {
            @Override public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(220,252,231));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),40,40);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tickCircle.setPreferredSize(new Dimension(60,60));
        tickCircle.setMaximumSize(new Dimension(60,60));
        tickCircle.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        tickCircle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel checkIcon = new JLabel("✓", JLabel.CENTER);
        checkIcon.setFont(new Font("Segoe UI", Font.BOLD, 30));
        checkIcon.setForeground(new Color(22, 163, 74));
        tickCircle.setLayout(new BorderLayout());
        tickCircle.add(checkIcon, BorderLayout.CENTER);

        JLabel s3title = new JLabel("Booking Confirmed!", JLabel.CENTER);
        s3title.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        s3title.setForeground(TEXT);
        s3title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel s3sub = new JLabel("Show this receipt to the watchman at entry.", JLabel.CENTER);
        s3sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        s3sub.setForeground(MUTED);
        s3sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        s3sub.setBorder(BorderFactory.createEmptyBorder(4,0,14,0));

        // receipt is already declared above (before verifyBtn listener)

        RPanel refundBadge = new RPanel(16, FREE_BG) {
            @Override public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(26,115,232,80));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,16,16);
                g2.dispose();
            }
        };
        refundBadge.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 8));
        JLabel refundLbl = new JLabel("💙  Leave before exit time to get ₹20 refund!");
        refundLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        refundLbl.setForeground(FREE_C);
        refundBadge.add(refundLbl);
        refundBadge.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        refundBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton doneBtn = bigBtn("Done", FREE_C);
        doneBtn.addActionListener(e -> dlg.dispose());

        step3.add(tickCircle); step3.add(Box.createRigidArea(new Dimension(0,8))); step3.add(s3title); step3.add(s3sub);
        step3.add(receipt); step3.add(Box.createRigidArea(new Dimension(0,10)));
        step3.add(refundBadge); step3.add(Box.createRigidArea(new Dimension(0,14)));
        step3.add(doneBtn);

        root.add(step0,"step0");
        root.add(step1,"step1");
        root.add(step2,"step2");
        root.add(step3,"step3");

        cl.show(root, occupied ? "step0" : "step1");

        dlg.setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HELPER: check if the logged-in user owns this slot's booking
    // ═══════════════════════════════════════════════════════════════════
    boolean isUserOwnBooking(String slotId) {
        if (loggedInMobile == null || dbConnection == null) return false;
        try {
            String sql = "SELECT id FROM bookings WHERE slot_id=? AND mobile=? AND status='confirmed' AND booking_date=?";
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                ps.setString(1, slotId);
                ps.setString(2, loggedInMobile);
                ps.setString(3, selectedDate.toString());
                ResultSet rs = ps.executeQuery();
                return rs.next();
            }
        } catch (Exception e) { return false; }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  POPUP: Booking OTP (dark themed, 60s countdown)
    // ═══════════════════════════════════════════════════════════════════
    void showBookingOtpPopup(JDialog parent, String mobile, String otp) {
        JDialog pop = new JDialog(parent, "Your Booking OTP", false);
        pop.setSize(320, 270); pop.setLocationRelativeTo(parent);
        pop.setResizable(false); pop.setUndecorated(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(22,28,52));
        root.setBorder(BorderFactory.createLineBorder(new Color(57,120,245), 2));

        JPanel hdr = new JPanel(new FlowLayout(FlowLayout.LEFT,14,10));
        hdr.setBackground(new Color(27,34,62));
        JLabel hIcon = new JLabel("🔐"); hIcon.setFont(new Font("SansSerif",Font.PLAIN,16));
        JLabel hTitle = new JLabel("Booking OTP"); hTitle.setFont(new Font("Segoe UI",Font.BOLD,13)); hTitle.setForeground(Color.WHITE);
        hdr.add(hIcon); hdr.add(hTitle);

        JPanel body = new JPanel(); body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));
        body.setBackground(new Color(22,28,52)); body.setBorder(BorderFactory.createEmptyBorder(14,20,14,20));

        JLabel otpLbl = new JLabel(otp, JLabel.CENTER);
        otpLbl.setFont(new Font("Monospaced",Font.BOLD,46)); otpLbl.setForeground(new Color(99,179,237));
        otpLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel timerLbl = new JLabel("⏱  Expires in: 60s");
        timerLbl.setFont(new Font("Segoe UI",Font.BOLD,12)); timerLbl.setForeground(new Color(252,129,74));
        timerLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel warn = new JLabel("Enter this OTP to confirm booking", JLabel.CENTER);
        warn.setFont(new Font("Segoe UI",Font.PLAIN,11)); warn.setForeground(new Color(100,115,145));
        warn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("Segoe UI",Font.BOLD,11)); closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(new Color(57,80,140)); closeBtn.setFocusPainted(false);
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> pop.dispose());

        body.add(otpLbl); body.add(Box.createVerticalStrut(6));
        body.add(timerLbl); body.add(Box.createVerticalStrut(4));
        body.add(warn); body.add(Box.createVerticalStrut(10)); body.add(closeBtn);

        root.add(hdr, BorderLayout.NORTH); root.add(body, BorderLayout.CENTER);
        pop.setContentPane(root); pop.setVisible(true);

        int[] rem = {60};
        java.util.Timer t = new java.util.Timer(true);
        t.scheduleAtFixedRate(new java.util.TimerTask(){
            public void run(){
                rem[0]--;
                SwingUtilities.invokeLater(() -> {
                    if (rem[0] <= 0) {
                        timerLbl.setText("⏱  Expired"); timerLbl.setForeground(new Color(229,57,53)); t.cancel();
                        new java.util.Timer(true).schedule(new java.util.TimerTask(){public void run(){SwingUtilities.invokeLater(pop::dispose);}},1500);
                    } else {
                        timerLbl.setForeground(rem[0]<=15?new Color(229,57,53):new Color(252,129,74));
                        timerLbl.setText("⏱  Expires in: "+rem[0]+"s");
                    }
                });
            }
        },1000,1000);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  POPUP: Exit OTP (shown to user, told to watchman)
    // ═══════════════════════════════════════════════════════════════════
    void showExitOtpPopup(JDialog parent, String slotId, String otp) {
        JDialog pop = new JDialog(parent, "Exit OTP for Slot " + slotId, true);
        pop.setSize(340, 310); pop.setLocationRelativeTo(parent);
        pop.setResizable(false); pop.setUndecorated(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(15,30,20));
        root.setBorder(BorderFactory.createLineBorder(new Color(22,163,74), 2));

        JPanel hdr = new JPanel(new FlowLayout(FlowLayout.LEFT,14,10));
        hdr.setBackground(new Color(20,40,26));
        JLabel hIcon = new JLabel("🚗"); hIcon.setFont(new Font("SansSerif",Font.PLAIN,16));
        JLabel hTitle = new JLabel("Exit OTP – Slot "+slotId); hTitle.setFont(new Font("Segoe UI",Font.BOLD,13)); hTitle.setForeground(Color.WHITE);
        hdr.add(hIcon); hdr.add(hTitle);

        JPanel body = new JPanel(); body.setLayout(new BoxLayout(body,BoxLayout.Y_AXIS));
        body.setBackground(new Color(15,30,20)); body.setBorder(BorderFactory.createEmptyBorder(14,20,14,20));

        JLabel otpLbl = new JLabel(otp, JLabel.CENTER);
        otpLbl.setFont(new Font("Monospaced",Font.BOLD,46)); otpLbl.setForeground(new Color(74,222,128));
        otpLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel inst = new JLabel("Tell this OTP to the Watchman", JLabel.CENTER);
        inst.setFont(new Font("Segoe UI",Font.BOLD,12)); inst.setForeground(new Color(134,239,172));
        inst.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel warn = new JLabel("Watchman enters it in Admin Panel to release slot", JLabel.CENTER);
        warn.setFont(new Font("Segoe UI",Font.PLAIN,10)); warn.setForeground(new Color(75,120,90));
        warn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton closeBtn = new JButton("Done");
        closeBtn.setFont(new Font("Segoe UI",Font.BOLD,11)); closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(new Color(22,100,50)); closeBtn.setFocusPainted(false);
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> pop.dispose());

        body.add(otpLbl); body.add(Box.createVerticalStrut(6));
        body.add(inst); body.add(Box.createVerticalStrut(4));
        body.add(warn); body.add(Box.createVerticalStrut(10)); body.add(closeBtn);

        root.add(hdr, BorderLayout.NORTH); root.add(body, BorderLayout.CENTER);
        pop.setContentPane(root); pop.setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Extend Time dialog
    // ═══════════════════════════════════════════════════════════════════
    void showExtendDialog(JDialog parent, SlotBooking booking) {
        String newExit = JOptionPane.showInputDialog(parent,
            "Current exit: " + fmt12(booking.exitMins) + "\nEnter new exit time (hh:mm AM/PM):",
            "Extend Time", JOptionPane.PLAIN_MESSAGE);
        if (newExit == null || newExit.isBlank()) return;
        int newMins = parse12(newExit.trim());
        if (newMins < 0) { JOptionPane.showMessageDialog(parent,"Invalid time format.","Error",JOptionPane.ERROR_MESSAGE); return; }
        booking.exitMins = newMins;
        // Update in DB
        try {
            String sql = "UPDATE bookings SET exit_time=? WHERE slot_id=? AND mobile=? AND status='confirmed'";
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                ps.setString(1, fmt12(newMins));
                ps.setString(2, booking.slotId);
                ps.setString(3, loggedInMobile);
                ps.executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
        refreshSlotGrid();
        JOptionPane.showMessageDialog(parent,"✅ Exit time extended to " + fmt12(newMins),"Extended", JOptionPane.INFORMATION_MESSAGE);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Cancel booking in DB
    // ═══════════════════════════════════════════════════════════════════
    void cancelBookingInDB(String slotId) {
        if (dbConnection == null) return;
        try {
            String sql = "UPDATE bookings SET status='cancelled' WHERE slot_id=? AND mobile=? AND status='confirmed'";
            try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
                ps.setString(1, slotId); ps.setString(2, loggedInMobile);
                ps.executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Demo bookings ─────────────────────────────────────────────────────
    void buildDemoBookings() {
        int now = nowMins();
        String todayKey = dateKey(LocalDate.now());
        String todayDisp = fmtDate(LocalDate.now());

        List<SlotBooking> todayList = new ArrayList<>();
        todayList.add(new SlotBooking("A1",  now-40, now+50,  todayDisp));
        todayList.add(new SlotBooking("A3",  now-20, now+70,  todayDisp));
        todayList.add(new SlotBooking("A5",  now-60, now+30,  todayDisp));
        todayList.add(new SlotBooking("A9",  now-10, now+80,  todayDisp));
        todayList.add(new SlotBooking("A12", now-30, now+20,  todayDisp));
        todayList.add(new SlotBooking("A15", now-15, now+90,  todayDisp));
        todayList.add(new SlotBooking("A7",  now+90, now+150, todayDisp));
        todayList.add(new SlotBooking("A11", now+60, now+120, todayDisp));
        bookingsByDate.put(todayKey, todayList);

        String[][] futureSets = {
            {"A2","A4","A8"},
            {"A1","A6","A10","A14"},
            {"A3","A7","A11"},
            {"A2","A5","A9","A13"},
            {"A4","A8","A12","A16"},
            {"A1","A3","A6","A15"}
        };
        for (int offset = 1; offset <= 6; offset++) {
            LocalDate d = LocalDate.now().plusDays(offset);
            String key = dateKey(d), disp = fmtDate(d);
            List<SlotBooking> list = new ArrayList<>();
            for (String id : futureSets[offset-1])
                list.add(new SlotBooking(id, 600, 960, disp)); // 10:00–16:00
            bookingsByDate.put(key, list);
        }
    }

    // ── Timers ────────────────────────────────────────────────────────────
    void startClockTimer() {
        // Clock removed from header - no longer needed
    }

    void startSlotRefresh() {
        javax.swing.Timer t = new javax.swing.Timer(30_000, e -> {
            if (selectedDate.equals(LocalDate.now())) refreshSlotGrid();
        });
        t.start();
    }

    // ── UI component helpers ──────────────────────────────────────────────
    JPanel legendItem(Color c, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));
        p.setOpaque(false);
        JPanel dot = new JPanel(); dot.setBackground(c);
        dot.setPreferredSize(new Dimension(12,12));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI",Font.PLAIN,12));
        lbl.setForeground(MUTED);
        p.add(dot); p.add(lbl);
        return p;
    }

    JButton navBtn(String t) {
        JButton b = new JButton(t) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI",Font.PLAIN,16));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBackground(CARD);
        b.setForeground(MUTED);
        b.setBorder(BorderFactory.createEmptyBorder(2,6,2,6));
        b.setPreferredSize(new Dimension(30,30));
        return b;
    }

    JPanel statBox(String icon, JLabel val, String lbl, Color bg) {
        RPanel p = new RPanel(10, bg);
        p.setLayout(new FlowLayout(FlowLayout.LEFT,8,8));
        JLabel ic = new JLabel(icon);
        ic.setFont(new Font("Segoe UI",Font.PLAIN,18));
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info,BoxLayout.Y_AXIS));
        info.add(val);
        JLabel l2 = new JLabel(lbl);
        l2.setFont(new Font("Segoe UI",Font.PLAIN,9));
        l2.setForeground(MUTED);
        info.add(l2);
        p.add(ic); p.add(info);
        return p;
    }

    JLabel sectionHead(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI Black",Font.BOLD,18));
        l.setForeground(TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    JLabel mutedLbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI",Font.PLAIN,12));
        l.setForeground(MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(2,0,6,0));
        return l;
    }

    JLabel badge(String t, Color fg, Color bg) {
        JLabel l = new JLabel("🅿  " + t);
        l.setFont(new Font("Segoe UI",Font.BOLD,12));
        l.setForeground(fg);
        l.setBackground(bg);
        l.setOpaque(true);
        l.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(fg,1,true),
            BorderFactory.createEmptyBorder(4,10,4,10)
        ));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    JPanel occInfoRow(String k, String v) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE,28));
        JLabel kl = new JLabel(k); kl.setFont(new Font("Segoe UI",Font.PLAIN,12)); kl.setForeground(MUTED);
        JLabel vl = new JLabel(v); vl.setFont(new Font("Segoe UI",Font.BOLD,12));  vl.setForeground(TEXT);
        r.add(kl,BorderLayout.WEST); r.add(vl,BorderLayout.EAST);
        r.setBorder(new MatteBorder(0,0,1,0,new Color(229,57,53,30)));
        return r;
    }

    JPanel receiptRow(String k, String v) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE,26));
        JLabel kl = new JLabel(k); kl.setFont(new Font("Segoe UI",Font.PLAIN,12)); kl.setForeground(MUTED);
        JLabel vl = new JLabel(v); vl.setFont(new Font("Segoe UI",Font.BOLD,12));  vl.setForeground(TEXT);
        r.add(kl,BorderLayout.WEST); r.add(vl,BorderLayout.EAST);
        r.setBorder(new MatteBorder(0,0,1,0,BORDER));
        return r;
    }

    JTextField formField() {
        JTextField tf = new JTextField() {
            @Override public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEditable() ? BORDER : new Color(190, 205, 225));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 18, 18);
                g2.dispose();
            }
        };
        tf.setOpaque(false);
        tf.setFont(new Font("Segoe UI",Font.PLAIN,13));
        tf.setBackground(new Color(248,250,253));
        tf.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE,38));
        return tf;
    }

    JPanel formRow(String lbl, JTextField tf) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = new JLabel(lbl);
        l.setFont(new Font("Segoe UI",Font.PLAIN,12));
        l.setForeground(MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createRigidArea(new Dimension(0,3)));
        p.add(tf);
        p.add(Box.createRigidArea(new Dimension(0,7)));
        return p;
    }

    JButton bigBtn(String t, Color bg) {
        Color[] bgRef = {bg};
        JButton b = new JButton(t) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgRef[0]);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI",Font.BOLD,14));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder(11,0,11,0));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    JButton outlineBtn(String t) {
        JButton b = new JButton(t) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 22, 22);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI",Font.PLAIN,13));
        b.setForeground(TEXT);
        b.setBackground(CARD);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder(8,0,8,0));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Rounded panel ─────────────────────────────────────────────────────
    static class RPanel extends JPanel {
        int r; Color bg;
        RPanel(int r, Color bg){ this.r=r; this.bg=bg; setOpaque(false); }
        public void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg); g2.fillRoundRect(0,0,getWidth(),getHeight(),r,r); g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── DB Sync ───────────────────────────────────────────────────────────
    void syncBookingsFromDB() {
        if (dbConnection == null) {
            buildDemoBookings();
            return;
        }
        bookingsByDate.clear();
        String sql = "SELECT * FROM bookings WHERE status = 'confirmed'";
        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String dDate = rs.getString("booking_date");
                if (dDate == null || dDate.isBlank()) continue;
                try {
                    LocalDate d = LocalDate.parse(dDate); // yyyy-MM-dd
                    String slot = rs.getString("slot_id");
                    int entry = parse12(rs.getString("entry_time"));
                    int exit = parse12(rs.getString("exit_time"));
                    if (entry < 0) entry = 0;
                    if (exit < 0) exit = 60;
                    bookingsByDate.computeIfAbsent(dateKey(d), k -> new ArrayList<>())
                        .add(new SlotBooking(slot, entry, exit, fmtDate(d)));
                } catch (Exception e) {}
            }
        } catch (SQLException e) {
            e.printStackTrace();
            buildDemoBookings(); // fallback
        }
    }

    // ── Main ──────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ParkSmartUserPage("9876543210", null).setVisible(true));
    }
}
