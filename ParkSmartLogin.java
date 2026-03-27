import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class ParkSmartLogin extends JFrame {
    private final JToggleButton userBtn;
    private final JToggleButton watchBtn;
    private final JLabel heading;
    private final JLabel infoLabel;
    private final JTextField mobileField;
    private final JPanel actionPanel;

    public ParkSmartLogin() {
        super("ParkSmart - Hotel Parking");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(460, 660);
        setLocationRelativeTo(null);
        setResizable(false);

        getContentPane().setBackground(new Color(239, 244, 250));
        getContentPane().setLayout(new GridBagLayout());

        JPanel card = new RoundedPanel(26, Color.WHITE);
        card.setPreferredSize(new Dimension(420, 580));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("ParkSmart");
        title.setFont(new Font("Segoe UI Black", Font.BOLD, 40));
        title.setForeground(new Color(11, 98, 242));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Hotel Parking Management System");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setForeground(new Color(100, 121, 150));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(6, 0, 20, 0));

        JPanel toggleWrapper = new RoundedPanel(18, new Color(236, 243, 255));
        toggleWrapper.setLayout(new GridLayout(1, 2, 8, 0));
        toggleWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggleWrapper.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        toggleWrapper.setMaximumSize(new Dimension(380, 70));

        userBtn = makeTabButton("User Login");
        watchBtn = makeTabButton("Watchman Login");

        ButtonGroup group = new ButtonGroup();
        group.add(userBtn);
        group.add(watchBtn);
        userBtn.setSelected(true);

        userBtn.addActionListener(e -> setMode("user"));
        watchBtn.addActionListener(e -> setMode("watchman"));

        toggleWrapper.add(userBtn);
        toggleWrapper.add(watchBtn);

        JSeparator progress = new JSeparator();
        progress.setForeground(new Color(37, 114, 255));
        progress.setPreferredSize(new Dimension(380, 3));
        progress.setMaximumSize(new Dimension(380, 3));
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        progress.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 0));

        heading = new JLabel("Welcome Back");
        heading.setFont(new Font("Segoe UI Black", Font.PLAIN, 30));
        heading.setForeground(new Color(22, 28, 52));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoLabel = new JLabel("Enter your mobile number to receive an OTP");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        infoLabel.setForeground(new Color(113, 125, 143));
        infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(4,0,16,0));

        actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setOpaque(false);
        actionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(0,0,14,0));

        JLabel mobileLabel = new JLabel("Mobile Number");
        mobileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        mobileLabel.setForeground(new Color(118, 128, 146));
        mobileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel inputCard = new RoundedPanel(14, new Color(245, 248, 253));
        inputCard.setLayout(new BorderLayout(8, 0));
        inputCard.setMaximumSize(new Dimension(380, 50));
        inputCard.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        inputCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel country = new JLabel("+91 ");
        country.setFont(new Font("Segoe UI Semibold", Font.BOLD, 16));
        country.setForeground(new Color(84, 102, 134));

        mobileField = new JTextField();
        mobileField.setBorder(null);
        mobileField.setOpaque(false);
        mobileField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        mobileField.setForeground(new Color(16, 29, 47));

        inputCard.add(country, BorderLayout.WEST);
        inputCard.add(mobileField, BorderLayout.CENTER);

        JButton getOtp = new JButton("Get OTP →");
        getOtp.setFont(new Font("Segoe UI Semibold", Font.BOLD, 20));
        getOtp.setForeground(Color.WHITE);
        getOtp.setBackground(new Color(27, 122, 252));
        getOtp.setFocusPainted(false);
        getOtp.setBorder(new RoundedLineBorder(new Color(24, 90, 201), 0, 24, true));
        getOtp.setMaximumSize(new Dimension(380, 56));
        getOtp.setAlignmentX(Component.LEFT_ALIGNMENT);
        getOtp.addActionListener(e -> onGetOtp());

        card.add(title);
        card.add(subtitle);
        card.add(toggleWrapper);
        card.add(progress);
        card.add(heading);
        card.add(infoLabel);
        card.add(actionPanel);
        card.add(mobileLabel);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(inputCard);
        card.add(Box.createRigidArea(new Dimension(0, 20)));
        card.add(getOtp);

        getContentPane().add(card);
        setMode("user");
    }

    private JToggleButton makeTabButton(String text) {
        JToggleButton btn = new JToggleButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btn.setFocusable(false);
        btn.setBorder(new RoundedLineBorder(new Color(188, 210, 249), 1, 16, true));
        btn.setBackground(new Color(245, 250, 255));
        btn.setForeground(new Color(42, 84, 148));
        return btn;
    }

    private void setMode(String mode) {
        if ("watchman".equals(mode)) {
            userBtn.setForeground(new Color(85, 106, 140));
            watchBtn.setForeground(new Color(20, 89, 235));
            watchBtn.setBorder(new RoundedLineBorder(new Color(57, 120, 245), 2, 16, true));
            userBtn.setBorder(new RoundedLineBorder(new Color(188, 210, 249), 1, 16, true));
            heading.setText("Watchman Access");
            infoLabel.setText("Enter your registered watchman mobile number");
            actionPanel.removeAll();
            actionPanel.add(createInfoBadge("Watchman portal — your number must be registered in the admin list."));
        } else {
            userBtn.setForeground(new Color(20, 89, 235));
            watchBtn.setForeground(new Color(85, 106, 140));
            userBtn.setBorder(new RoundedLineBorder(new Color(57, 120, 245), 2, 16, true));
            watchBtn.setBorder(new RoundedLineBorder(new Color(188, 210, 249), 1, 16, true));
            heading.setText("Welcome Back");
            infoLabel.setText("Enter your mobile number to receive an OTP");
            actionPanel.removeAll();
        }
        actionPanel.revalidate();
        actionPanel.repaint();
    }

    private JPanel createInfoBadge(String text) {
        RoundedPanel badge = new RoundedPanel(12, new Color(255, 247, 230));
        badge.setBorder(BorderFactory.createLineBorder(new Color(255, 188, 96), 1));
        badge.setLayout(new BoxLayout(badge, BoxLayout.Y_AXIS));
        badge.setMaximumSize(new Dimension(380, 64));
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);
        badge.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel label = new JLabel("<html><body style='width:330px'>" + text + "</body></html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(new Color(141, 98, 41));
        label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        badge.add(label);

        return badge;
    }

    private void onGetOtp() {
        String mobile = mobileField.getText().trim();
        if (mobile.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter your mobile number", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, "OTP sent to +91 " + mobile, "OTP", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new ParkSmartLogin().setVisible(true));
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color background;
        RoundedPanel(int radius, Color background) {
            this.radius = radius;
            this.background = background;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(background);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedLineBorder extends LineBorder {
        private final int radius;
        RoundedLineBorder(Color color, int thickness, int radius, boolean roundedCorners) {
            super(color, thickness, roundedCorners);
            this.radius = radius;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }
    }
}