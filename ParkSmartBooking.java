import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class ParkSmartBooking extends JFrame {
    private static final Color BG = new Color(240, 244, 250);
    private static final Color CARD = Color.WHITE;
    private static final Color BORDER = new Color(220, 229, 240);
    private static final Color FREE = new Color(26, 115, 232);
    private static final Color FREE_BG = new Color(239, 244, 255);
    private static final Color OCCUPIED = new Color(229, 57, 53);
    private static final Color OCC_BG = new Color(255, 245, 245);
    private static final Color MUTED = new Color(107, 124, 153);
    private static final Color TEXT = new Color(26, 35, 50);

    private LocalDate currentMonth;
    private LocalDate selectedDate;
    private JPanel calendarPanel;
    private JPanel slotGridPanel;
    private JPanel historyPanel;
    private JLabel freeCountLabel;
    private JLabel occCountLabel;
    private JLabel selectedDateLabel;
    private BookingModal bookingModal;

    private Map<String, List<SlotBooking>> bookingsByDate = new HashMap<>();
    private List<BookingRecord> bookingHistory = new ArrayList<>();
    private static final String[] SLOT_IDS = {"A1","A2","A3","A4","A5","A6","A7","A8","A9","A10","A11","A12","A13","A14","A15","A16"};

    public ParkSmartBooking() {
        super("ParkSmart – Hotel Parking");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 800);
        setLocationRelativeTo(null);
        setResizable(false);

        initBookings();
        currentMonth = LocalDate.now();
        selectedDate = LocalDate.now();

        getContentPane().setBackground(BG);
        getContentPane().setLayout(new BorderLayout());

        // Header
        getContentPane().add(createHeader(), BorderLayout.NORTH);

        // Main content
        getContentPane().add(createMainContent(), BorderLayout.CENTER);

        // History section
        getContentPane().add(createHistorySection(), BorderLayout.SOUTH);

        // Modal
        bookingModal = new BookingModal(this);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel();
        header.setBackground(new Color(255, 255, 255, 245));
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        header.setLayout(new BorderLayout());
        header.setPreferredSize(new Dimension(0, 68));

        JLabel logo = new JLabel("Park");
        JLabel smart = new JLabel("Smart");
        smart.setForeground(FREE);
        logo.setFont(new Font("Syne", Font.BOLD, 22));
        smart.setFont(new Font("Syne", Font.BOLD, 22));

        JPanel logoPanel = new JPanel();
        logoPanel.setOpaque(false);
        logoPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoPanel.setBorder(BorderFactory.createEmptyBorder(0, 32, 0, 0));
        logoPanel.add(logo);
        logoPanel.add(smart);

        JLabel badge = new JLabel("Live Availability");
        badge.setFont(new Font("DM Sans", Font.PLAIN, 12));
        badge.setForeground(FREE);
        badge.setOpaque(true);
        badge.setBackground(FREE_BG);
        badge.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(FREE, 1, true),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));

        JPanel badgePanel = new JPanel();
        badgePanel.setOpaque(false);
        badgePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 32));
        badgePanel.add(badge);

        header.add(logoPanel, BorderLayout.WEST);
        header.add(badgePanel, BorderLayout.EAST);
        return header;
    }

    private JPanel createMainContent() {
        JPanel main = new JPanel();
        main.setOpaque(false);
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(BorderFactory.createEmptyBorder(40, 48, 40, 48));

        JLabel title = new JLabel("Hotel Parking");
        title.setFont(new Font("Syne", Font.BOLD, 40));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Pick a date on the calendar — then click a slot to book.");
        subtitle.setFont(new Font("DM Sans", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(6, 0, 24, 0));

        // Legend
        JPanel legendPanel = new JPanel();
        legendPanel.setOpaque(false);
        legendPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 24, 0));
        legendPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        legendPanel.add(createLegendItem("Available", FREE));
        legendPanel.add(createLegendItem("Occupied", OCCUPIED));
        legendPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

        // Layout: Calendar (left) + Slots (right)
        JPanel layout = new JPanel();
        layout.setOpaque(false);
        layout.setLayout(new BorderLayout(24, 0));
        layout.setAlignmentX(Component.LEFT_ALIGNMENT);
        layout.setMaximumSize(new Dimension(1200, 450));

        calendarPanel = createCalendar();
        slotGridPanel = createSlotGridPanel();
        layout.add(calendarPanel, BorderLayout.WEST);
        layout.add(slotGridPanel, BorderLayout.CENTER);

        main.add(title);
        main.add(subtitle);
        main.add(legendPanel);
        main.add(layout);

        return main;
    }

    private JPanel createLegendItem(String label, Color color) {
        JPanel item = new JPanel();
        item.setOpaque(false);
        item.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));

        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(0, 0, 12, 12, 3, 3);
            }
        };
        dot.setPreferredSize(new Dimension(12, 12));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("DM Sans", Font.PLAIN, 13));
        lbl.setForeground(MUTED);

        item.add(dot);
        item.add(lbl);
        return item;
    }

    private JPanel createCalendar() {
        JPanel card = new RoundedPanel(18, CARD);
        card.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        card.setPreferredSize(new Dimension(300, 450));

        // Header
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BorderLayout());

        JButton prevBtn = createNavButton("◀");
        JLabel monthLabel = new JLabel();
        monthLabel.setFont(new Font("Syne", Font.BOLD, 16));
        monthLabel.setForeground(TEXT);
        JButton nextBtn = createNavButton("▶");

        prevBtn.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            if (currentMonth.isBefore(LocalDate.now().withDayOfMonth(1))) {
                currentMonth = LocalDate.now().withDayOfMonth(1);
            }
            updateCalendar(monthLabel);
        });

        nextBtn.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendar(monthLabel);
        });

        header.add(prevBtn, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(nextBtn, BorderLayout.EAST);

        // Grid
        JPanel gridPanel = new JPanel();
        gridPanel.setOpaque(false);
        gridPanel.setLayout(new GridLayout(7, 7, 3, 3));

        String[] days = {"SUN","MON","TUE","WED","THU","FRI","SAT"};
        for (String day : days) {
            JLabel dayLabel = new JLabel(day);
            dayLabel.setFont(new Font("DM Sans", Font.BOLD, 10));
            dayLabel.setForeground(MUTED);
            dayLabel.setHorizontalAlignment(JLabel.CENTER);
            gridPanel.add(dayLabel);
        }

        // Store references for date cells
        final JLabel[][] dateCells = new JLabel[6][7];
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) {
                JLabel cell = new JLabel();
                cell.setFont(new Font("DM Sans", Font.PLAIN, 13));
                cell.setHorizontalAlignment(JLabel.CENTER);
                cell.setVerticalAlignment(JLabel.CENTER);
                cell.setOpaque(true);
                cell.setBackground(BG);
                cell.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
                dateCells[r][c] = cell;
                gridPanel.add(cell);
            }
        }

        // Stats
        JPanel stats = new JPanel();
        stats.setOpaque(false);
        stats.setLayout(new GridLayout(1, 2, 12, 0));
        stats.setBorder(BorderFactory.createEmptyBorder(16, 0, 12, 0));

        freeCountLabel = createStatBox("Free Now", "🟢", FREE_BG);
        occCountLabel = createStatBox("Occupied", "🔴", OCC_BG);
        stats.add(freeCountLabel.getParent());
        stats.add(occCountLabel.getParent());

        JLabel updated = new JLabel("Updated: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        updated.setFont(new Font("DM Sans", Font.PLAIN, 11));
        updated.setForeground(MUTED);
        updated.setHorizontalAlignment(JLabel.CENTER);

        card.add(header);
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        card.add(gridPanel);
        card.add(stats);
        card.add(updated);

        updateCalendar(monthLabel);
        populateCalendarDates(gridPanel, dateCells, monthLabel);

        return card;
    }

    private void populateCalendarDates(JPanel gridPanel, JLabel[][] cells, JLabel monthLabel) {
        Timer timer = new Timer(1000, e -> {
            YearMonth ym = YearMonth.of(currentMonth.getYear(), currentMonth.getMonth());
            LocalDate first = ym.atDay(1);
            int startDow = first.getDayOfWeek().getValue() % 7;
            int daysInMonth = ym.lengthOfMonth();
            LocalDate today = LocalDate.now();

            int idx = 7;
            for (int d = 1; d <= daysInMonth; d++) {
                LocalDate date = ym.atDay(d);
                int row = idx / 7;
                int col = idx % 7;
                JLabel cell = cells[row][col];

                boolean isToday = date.equals(today);
                boolean isSelected = date.equals(selectedDate);
                boolean isPast = date.isBefore(today);

                cell.setText(String.valueOf(d));
                cell.setForeground(isPast ? new Color(197, 205, 216) : TEXT);
                cell.setBackground(isToday ? FREE : isSelected ? FREE_BG : BG);
                cell.setCursor(isPast ? Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) :
                              Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                final LocalDate fdate = date;
                MouseListener[] listeners = cell.getMouseListeners();
                for (MouseListener ml : listeners) cell.removeMouseListener(ml);

                if (!isPast) {
                    cell.addMouseListener(new MouseAdapter() {
                        @Override public void mouseClicked(MouseEvent e) {
                            selectedDate = fdate;
                            updateSlotGrid();
                            populateCalendarDates(gridPanel, cells, monthLabel);
                        }
                        @Override public void mouseEntered(MouseEvent e) {
                            if (!fdate.equals(today)) cell.setBackground(new Color(225, 240, 255));
                        }
                        @Override public void mouseExited(MouseEvent e) {
                            cell.setBackground(fdate.equals(today) ? FREE : fdate.equals(selectedDate) ? FREE_BG : BG);
                        }
                    });
                }
                idx++;
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void updateCalendar(JLabel monthLabel) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy");
        monthLabel.setText(currentMonth.format(fmt));
    }

    private JLabel createStatBox(String label, String icon, Color bgColor) {
        JPanel box = new RoundedPanel(10, bgColor);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel count = new JLabel("—");
        count.setFont(new Font("Syne", Font.BOLD, 22));
        count.setForeground(FREE);
        count.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("DM Sans", Font.PLAIN, 10));
        lbl.setForeground(MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        box.add(count);
        box.add(lbl);

        if ("Free Now".equals(label)) freeCountLabel = count;
        else occCountLabel = count;

        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BorderLayout());
        container.add(box, BorderLayout.CENTER);
        return container.add(count) == null ? count : count;
    }

    private JButton createNavButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setForeground(MUTED);
        btn.setBackground(Color.WHITE);
        btn.setBorder(new LineBorder(BORDER, 1, true));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(30, 30));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(FREE_BG);
                btn.setForeground(FREE);
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(Color.WHITE);
                btn.setForeground(MUTED);
            }
        });
        return btn;
    }

    private JPanel createSlotGridPanel() {
        slotGridPanel = new JPanel();
        slotGridPanel.setOpaque(false);
        slotGridPanel.setLayout(new BoxLayout(slotGridPanel, BoxLayout.Y_AXIS));

        selectedDateLabel = new JLabel();
        selectedDateLabel.setFont(new Font("Syne", Font.BOLD, 18));
        selectedDateLabel.setForeground(TEXT);
        selectedDateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectedDateLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JLabel zoneLabel = new JLabel("ZONE A – GROUND FLOOR");
        zoneLabel.setFont(new Font("DM Sans", Font.BOLD, 10));
        zoneLabel.setForeground(MUTED);
        zoneLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel gridContainer = new JPanel();
        gridContainer.setOpaque(false);
        gridContainer.setLayout(new GridLayout(4, 4, 10, 10));
        gridContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        gridContainer.setMaximumSize(new Dimension(700, 320));

        for (String slotId : SLOT_IDS) {
            SlotButton slotBtn = new SlotButton(slotId);
            slotBtn.addActionListener(e -> openBookingModal(slotId));
            gridContainer.add(slotBtn);
        }

        slotGridPanel.add(selectedDateLabel);
        slotGridPanel.add(zoneLabel);
        slotGridPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        slotGridPanel.add(gridContainer);

        return slotGridPanel;
    }

    private void updateSlotGrid() {
        selectedDateLabel.setText(selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) +
            (selectedDate.equals(LocalDate.now()) ? " (TODAY)" : ""));

        int free = 0, occupied = 0;
        for (String slotId : SLOT_IDS) {
            SlotStatus status = getSlotStatus(slotId, selectedDate);
            if ("free".equals(status.state)) free++; else occupied++;
        }
        freeCountLabel.setText(String.valueOf(free));
        occCountLabel.setText(String.valueOf(occupied));
    }

    private SlotStatus getSlotStatus(String slotId, LocalDate date) {
        String key = date.toString();
        List<SlotBooking> bookings = bookingsByDate.getOrDefault(key, new ArrayList<>());
        SlotBooking found = bookings.stream().filter(b -> b.slotId.equals(slotId)).findFirst().orElse(null);

        if (found != null) {
            if (date.equals(LocalDate.now())) {
                LocalTime now = LocalTime.now();
                LocalTime entry = LocalTime.parse(found.entry, DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
                LocalTime exit = LocalTime.parse(found.exit, DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
                if (now.isAfter(entry) && now.isBefore(exit)) {
                    return new SlotStatus("occupied", found);
                }
            } else {
                return new SlotStatus("occupied", found);
            }
        }
        return new SlotStatus("free", null);
    }

    private void openBookingModal(String slotId) {
        SlotStatus status = getSlotStatus(slotId, selectedDate);
        bookingModal.show(slotId, status, selectedDate);
    }

    private JPanel createHistorySection() {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createEmptyBorder(40, 48, 40, 48));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BorderLayout());
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(1200, 40));

        JLabel title = new JLabel("📋 My Booking History");
        title.setFont(new Font("Syne", Font.BOLD, 16));
        title.setForeground(TEXT);

        JButton clearBtn = new JButton("Clear History");
        clearBtn.setFont(new Font("DM Sans", Font.PLAIN, 12));
        clearBtn.setForeground(OCCUPIED);
        clearBtn.setBackground(new Color(255, 245, 245));
        clearBtn.setBorder(new LineBorder(OCCUPIED, 1, true));
        clearBtn.setFocusPainted(false);
        clearBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Clear all history?") == 0) {
                bookingHistory.clear();
                updateHistoryTable();
            }
        });

        header.add(title, BorderLayout.WEST);
        header.add(clearBtn, BorderLayout.EAST);

        historyPanel = new JPanel();
        historyPanel.setOpaque(false);
        historyPanel.setLayout(new BorderLayout());
        historyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        historyPanel.setMaximumSize(new Dimension(1200, 180));

        section.add(header);
        section.add(Box.createRigidArea(new Dimension(0, 12)));
        section.add(historyPanel);

        updateHistoryTable();
        return section;
    }

    private void updateHistoryTable() {
        historyPanel.removeAll();

        if (bookingHistory.isEmpty()) {
            JPanel empty = new RoundedPanel(14, CARD);
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, false),
                BorderFactory.createEmptyBorder(40, 20, 40, 20)
            ));

            JLabel icon = new JLabel("🅿");
            icon.setFont(new Font("Arial", Font.PLAIN, 24));
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel text = new JLabel("No bookings yet. Book a slot to see your history here.");
            text.setFont(new Font("DM Sans", Font.PLAIN, 14));
            text.setForeground(MUTED);
            text.setAlignmentX(Component.CENTER_ALIGNMENT);

            empty.add(Box.createVerticalGlue());
            empty.add(icon);
            empty.add(Box.createRigidArea(new Dimension(0, 8)));
            empty.add(text);
            empty.add(Box.createVerticalGlue());

            historyPanel.add(empty, BorderLayout.CENTER);
        } else {
            String[] columns = {"#", "Slot", "Name", "Vehicle", "Amount", "Status"};
            Object[][] data = new Object[bookingHistory.size()][6];

            for (int i = 0; i < bookingHistory.size(); i++) {
                BookingRecord r = bookingHistory.get(i);
                data[i] = new Object[] {bookingHistory.size() - i, r.slot, r.name, r.vehicle, "₹" + r.amount, r.status};
            }

            JTable table = new JTable(data, columns);
            table.setFont(new Font("DM Sans", Font.PLAIN, 12));
            table.setRowHeight(28);
            table.setShowGrid(false);
            table.setIntercellSpacing(new Dimension(0, 0));
            table.setBackground(CARD);
            table.setSelectionBackground(FREE_BG);
            table.setDefaultRenderer(Object.class, new HistoryTableRenderer());

            JTableHeader th = table.getTableHeader();
            th.setBackground(new Color(248, 250, 253));
            th.setFont(new Font("DM Sans", Font.BOLD, 10));
            th.setForeground(MUTED);

            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(new LineBorder(BORDER, 1, true));
            scroll.getViewport().setBackground(CARD);

            historyPanel.add(scroll, BorderLayout.CENTER);
        }

        historyPanel.revalidate();
        historyPanel.repaint();
    }

    private void initBookings() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Today's bookings
        List<SlotBooking> todayBookings = new ArrayList<>();
        todayBookings.add(new SlotBooking("A1", fmt12(now.minusMinutes(40)), fmt12(now.plusMinutes(50))));
        todayBookings.add(new SlotBooking("A3", fmt12(now.minusMinutes(20)), fmt12(now.plusMinutes(70))));
        todayBookings.add(new SlotBooking("A5", fmt12(now.minusMinutes(60)), fmt12(now.plusMinutes(30))));
        todayBookings.add(new SlotBooking("A9", fmt12(now.minusMinutes(10)), fmt12(now.plusMinutes(80))));
        bookingsByDate.put(today.toString(), todayBookings);

        // Future bookings
        for (int offset = 1; offset <= 6; offset++) {
            LocalDate d = today.plusDays(offset);
            List<SlotBooking> bookings = new ArrayList<>();
            String[][] slotsByDay = {
                {"A2","A4","A8"},
                {"A1","A6","A10","A14"},
                {"A3","A7","A11"},
                {"A2","A5","A9","A13"},
                {"A4","A8","A12","A16"},
                {"A1","A3","A6","A15"}
            };
            for (String slot : slotsByDay[offset-1]) {
                bookings.add(new SlotBooking(slot, "10:00 AM", "04:00 PM"));
            }
            bookingsByDate.put(d.toString(), bookings);
        }
    }

    private String fmt12(LocalTime time) {
        return time.format(DateTimeFormatter.ofPattern("hh:mm a"));
    }

    static class RoundedPanel extends JPanel {
        private int radius;
        private Color bgColor;

        RoundedPanel(int radius, Color bg) {
            this.radius = radius;
            this.bgColor = bg;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            super.paintComponent(g);
        }
    }

    class SlotButton extends JButton {
        String slotId;
        SlotButton(String id) {
            super(id);
            this.slotId = id;
            setFont(new Font("Syne", Font.BOLD, 16));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorder(new RoundedBorder(12, 1.5f, FREE, true));
            updateStyle();
        }

        void updateStyle() {
            SlotStatus status = getSlotStatus(slotId, selectedDate);
            if ("free".equals(status.state)) {
                setBackground(FREE_BG);
                setForeground(FREE);
            } else {
                setBackground(OCC_BG);
                setForeground(OCCUPIED);
            }
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            super.paintComponent(g);
        }
    }

    static class RoundedBorder extends LineBorder {
        private int radius;

        RoundedBorder(int radius, float thickness, Color color, boolean rounded) {
            super(color, 1, rounded);
            this.radius = radius;
        }

        @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }
    }

    class BookingModal extends JDialog {
        private JTextField nameField, mobileField, vehicleField;
        private JLabel slotLabel;
        private JPanel cardPanel;
        private String currentSlot;
        private LocalDate currentDate;

        BookingModal(JFrame parent) {
            super(parent, "Book Parking Slot", true);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setSize(480, 580);
            setLocationRelativeTo(parent);
            setResizable(false);

            JPanel main = new RoundedPanel(20, CARD);
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

            slotLabel = new JLabel();
            slotLabel.setFont(new Font("Syne", Font.BOLD, 16));
            slotLabel.setForeground(FREE);
            slotLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            slotLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

            JLabel title = new JLabel("Book Parking Slot");
            title.setFont(new Font("Syne", Font.BOLD, 28));
            title.setForeground(TEXT);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel sub = new JLabel("Enter your booking details");
            sub.setFont(new Font("DM Sans", Font.PLAIN, 14));
            sub.setForeground(MUTED);
            sub.setAlignmentX(Component.LEFT_ALIGNMENT);
            sub.setBorder(BorderFactory.createEmptyBorder(6, 0, 20, 0));

            nameField = createInputField("Full Name", "e.g. Arjun Kumar");
            mobileField = createInputField("Mobile Number", "10-digit mobile");
            vehicleField = createInputField("Vehicle Number", "e.g. TN 33 AB 1234");

            JLabel priceLabel = new JLabel("₹120 (₹100 rent + ₹20 deposit)");
            priceLabel.setFont(new Font("Syne", Font.BOLD, 20));
            priceLabel.setForeground(TEXT);
            priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            priceLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

            JButton bookBtn = new JButton("Send OTP & Continue →");
            bookBtn.setFont(new Font("Syne", Font.BOLD, 16));
            bookBtn.setBackground(FREE);
            bookBtn.setForeground(Color.WHITE);
            bookBtn.setFocusPainted(false);
            bookBtn.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
            bookBtn.addActionListener(e -> performBooking());

            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.setFont(new Font("DM Sans", Font.PLAIN, 14));
            cancelBtn.setBackground(Color.WHITE);
            cancelBtn.setForeground(TEXT);
            cancelBtn.setBorder(new LineBorder(BORDER, 1, true));
            cancelBtn.setFocusPainted(false);
            cancelBtn.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            cancelBtn.addActionListener(e -> dispose());

            main.add(slotLabel);
            main.add(title);
            main.add(sub);
            main.add(nameField);
            main.add(Box.createRigidArea(new Dimension(0, 12)));
            main.add(mobileField);
            main.add(Box.createRigidArea(new Dimension(0, 12)));
            main.add(vehicleField);
            main.add(priceLabel);
            main.add(bookBtn);
            main.add(Box.createRigidArea(new Dimension(0, 8)));
            main.add(cancelBtn);

            getContentPane().add(main);
        }

        private JTextField createInputField(String label, String placeholder) {
            JPanel container = new JPanel();
            container.setOpaque(false);
            container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
            container.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel lbl = new JLabel(label);
            lbl.setFont(new Font("DM Sans", Font.PLAIN, 12));
            lbl.setForeground(MUTED);

            JTextField field = new JTextField();
            field.setFont(new Font("DM Sans", Font.PLAIN, 14));
            field.setBackground(new Color(248, 250, 253));
            field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
            ));
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

            container.add(lbl);
            container.add(Box.createRigidArea(new Dimension(0, 6)));
            container.add(field);

            return field;
        }

        void show(String slotId, SlotStatus status, LocalDate date) {
            currentSlot = slotId;
            currentDate = date;
            slotLabel.setText("🅿 " + slotId + " – " + ("free".equals(status.state) ? "Available" : "Occupied"));
            slotLabel.setForeground("free".equals(status.state) ? FREE : OCCUPIED);

            nameField.setText("");
            mobileField.setText("");
            vehicleField.setText("");

            setVisible(true);
        }

        private void performBooking() {
            String name = nameField.getText().trim();
            String mobile = mobileField.getText().trim();
            String vehicle = vehicleField.getText().trim();

            if (name.isEmpty() || mobile.isEmpty() || vehicle.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields");
                return;
            }

            if (mobile.length() < 10) {
                JOptionPane.showMessageDialog(this, "Enter a valid 10-digit mobile number");
                return;
            }

            // Add to history
            bookingHistory.add(0, new BookingRecord(currentSlot, name, vehicle, 120, "confirmed"));
            updateHistoryTable();

            // Add to bookings
            if (!bookingsByDate.containsKey(currentDate.toString())) {
                bookingsByDate.put(currentDate.toString(), new ArrayList<>());
            }
            bookingsByDate.get(currentDate.toString()).add(new SlotBooking(currentSlot, "10:00 AM", "04:00 PM"));

            updateSlotGrid();
            dispose();
        }
    }

    static class SlotStatus {
        String state;
        SlotBooking booking;

        SlotStatus(String state, SlotBooking booking) {
            this.state = state;
            this.booking = booking;
        }
    }

    static class SlotBooking {
        String slotId, entry, exit;

        SlotBooking(String slotId, String entry, String exit) {
            this.slotId = slotId;
            this.entry = entry;
            this.exit = exit;
        }
    }

    static class BookingRecord {
        String slot, name, vehicle, status;
        int amount;

        BookingRecord(String slot, String name, String vehicle, int amount, String status) {
            this.slot = slot;
            this.name = name;
            this.vehicle = vehicle;
            this.amount = amount;
            this.status = status;
        }
    }

    static class HistoryTableRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                                  boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setBackground(isSelected ? FREE_BG : Color.WHITE);
            c.setForeground(TEXT);
            setFont(new Font("DM Sans", Font.PLAIN, 12));

            if (column == 1) setForeground(FREE);
            if (column == 5) setForeground("confirmed".equals(value) ? FREE : OCCUPIED);

             setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
            return c;
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new ParkSmartBooking().setVisible(true));
    }
}
