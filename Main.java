import javax.swing.*;
import javax.sound.sampled.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.Timer;

public class Main extends JFrame {
    public static final int WIDTH = 900;
    public static final int HEIGHT = 650;

    public static final Color BG_NAVY_TOP = new Color(8, 14, 34);
    public static final Color BG_NAVY_BOTTOM = new Color(16, 28, 58);
    public static final Color NEON_MAGENTA = new Color(255, 46, 151);
    public static final Color NEON_CYAN = new Color(0, 200, 255);
    public static final Color NEON_YELLOW = new Color(255, 214, 10);
    public static final Color NEON_GREEN = new Color(57, 255, 136);
    public static final Color NEON_ORANGE = new Color(255, 140, 40);
    public static final Color NEON_PURPLE = new Color(140, 120, 255);
    public static final Color TEXT_LIGHT = new Color(235, 240, 255);
    public static final Color TEXT_DIM = new Color(140, 150, 190);

    public enum Difficulty { EASY, NORMAL, HARD }
    public enum FruitMode { CLASSIC, ARCADE }
    public static volatile FruitMode fruitMode = FruitMode.ARCADE;
    public static volatile Difficulty difficulty = Difficulty.NORMAL;
    public static volatile boolean soundOn = true;
    public static volatile String username = "Guest";

    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final JLayeredPane rootLayers;
    private final BackgroundPanel backgroundPanel;
    private final ScanlineOverlay scanlineOverlay;
    private MenuPanel menuPanel;

    public Main() {
        setTitle("FUN ARENA");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(BG_NAVY_TOP);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        cardPanel.setBounds(0, 0, WIDTH, HEIGHT);

        LoginPanel loginPanel = new LoginPanel(this);
        menuPanel = new MenuPanel(this);
        SettingsPanel settingsPanel = new SettingsPanel(this);
        FruitNinjaPanel fruitNinjaPanel = new FruitNinjaPanel(this);
        BrickBreakerPanel brickPanel = new BrickBreakerPanel(this);

        cardPanel.add(loginPanel, "login");
        cardPanel.add(menuPanel, "menu");
        cardPanel.add(settingsPanel, "settings");
        cardPanel.add(fruitNinjaPanel, "fruitninja");
        cardPanel.add(brickPanel, "brick");

        backgroundPanel = new BackgroundPanel();
        backgroundPanel.setBounds(0, 0, WIDTH, HEIGHT);

        rootLayers = new JLayeredPane();
        rootLayers.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        rootLayers.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);
        rootLayers.add(cardPanel, JLayeredPane.PALETTE_LAYER);

        scanlineOverlay = new ScanlineOverlay();
        scanlineOverlay.setBounds(0, 0, WIDTH, HEIGHT);
        rootLayers.add(scanlineOverlay, JLayeredPane.MODAL_LAYER);

        add(rootLayers);
        pack();
        setLocationRelativeTo(null);
        showScreen("login");
    }

    public void onLoggedIn(String name) {
        username = (name == null || name.isBlank()) ? "Guest" : name.trim();
        menuPanel.refreshWelcome();
        showScreen("menu");
    }

    public void showScreen(String name) {
        cardLayout.show(cardPanel, name);
        for (Component c : cardPanel.getComponents()) {
            if (c.isVisible()) c.requestFocusInWindow();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}

class BackgroundPanel extends JPanel {
    private static final int STAR_COUNT = 60;
    private final double[] sx = new double[STAR_COUNT];
    private final double[] sy = new double[STAR_COUNT];
    private final double[] sv = new double[STAR_COUNT];
    private final float[] sr = new float[STAR_COUNT];
    private final float[] twinklePhase = new float[STAR_COUNT];
    private final Random rand = new Random();
    private final Timer animTimer;
    private float t = 0f;

    public BackgroundPanel() {
        setOpaque(true);
        for (int i = 0; i < STAR_COUNT; i++) {
            sx[i] = rand.nextDouble() * Main.WIDTH;
            sy[i] = rand.nextDouble() * Main.HEIGHT;
            sv[i] = 0.08 + rand.nextDouble() * 0.3;
            sr[i] = 0.6f + rand.nextFloat() * 1.6f;
            twinklePhase[i] = rand.nextFloat() * 6.28f;
        }
        animTimer = new Timer(40, e -> {
            t += 0.05f;
            for (int i = 0; i < STAR_COUNT; i++) {
                sy[i] += sv[i];
                if (sy[i] > Main.HEIGHT) { sy[i] = 0; sx[i] = rand.nextDouble() * Main.WIDTH; }
            }
            repaint();
        });
        animTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint gp = new GradientPaint(0, 0, Main.BG_NAVY_TOP, 0, getHeight(), Main.BG_NAVY_BOTTOM);
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());

        for (int i = 0; i < STAR_COUNT; i++) {
            float twinkle = 0.5f + 0.5f * (float) Math.sin(t + twinklePhase[i]);
            g2.setColor(new Color(255, 255, 255, (int) (60 + 90 * twinkle)));
            g2.fill(new Ellipse2D.Double(sx[i], sy[i], sr[i], sr[i]));
        }
    }
}

class ScanlineOverlay extends JComponent {
    public ScanlineOverlay() { setOpaque(false); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();

        g2.setColor(new Color(0, 0, 0, 16));
        for (int y = 0; y < h; y += 3) g2.drawLine(0, y, w, y);

        RadialGradientPaint vignette = new RadialGradientPaint(
                new Point(w / 2, h / 2), Math.max(w, h) * 0.75f,
                new float[]{0f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 80)});
        g2.setPaint(vignette);
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }
}

class GlassPanel extends JPanel {
    private final Color accent;
    public GlassPanel(Color accent) {
        this.accent = accent;
        setOpaque(false);
        setLayout(null);
    }
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 22, 22);
        g2.setColor(new Color(255, 255, 255, 12));
        g2.fill(shape);
        g2.setStroke(new BasicStroke(1.3f));
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));
        g2.draw(shape);
        g2.dispose();
    }
}

class GlowButton extends JButton {
    private final Color accent;
    private boolean hover = false;
    private boolean pressedDown = false;
    private boolean selected = false;

    public GlowButton(String text, Color accent) {
        super(text);
        this.accent = accent;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setForeground(Main.TEXT_LIGHT);
        setFont(new Font("Segoe UI", Font.BOLD, 20));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; pressedDown = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressedDown = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { pressedDown = false; repaint(); }
        });
    }

    public void setSelectedState(boolean sel) {
        this.selected = sel;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        RoundRectangle2D shape = new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 18, 18);

        Color top = accent.brighter();
        Color bottom = accent.darker();
        if (pressedDown) { top = accent.darker(); bottom = accent.darker().darker(); }
        else if (hover) { top = accent.brighter().brighter(); bottom = accent; }

        GradientPaint gp = new GradientPaint(0, 0, top, 0, h, bottom);
        g2.setPaint(gp);
        g2.fill(shape);

        g2.setStroke(new BasicStroke(selected ? 3f : (hover ? 2f : 1.4f)));
        g2.setColor(selected ? Color.WHITE : (hover ? new Color(255, 255, 255, 200) : new Color(255, 255, 255, 70)));
        g2.draw(shape);

        g2.setColor(Main.TEXT_LIGHT);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(getText())) / 2;
        int y = (h + fm.getAscent()) / 2 - 2;
        g2.drawString(getText(), x, y);
        g2.dispose();
    }
}

class GlowLabel extends JLabel {
    private final Color glow;
    public GlowLabel(String text, Color glow) {
        super(text, SwingConstants.CENTER);
        this.glow = glow;
        setForeground(Main.TEXT_LIGHT);
    }
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics fm = g2.getFontMetrics(getFont());
        String text = getText();
        int x = (getWidth() - fm.stringWidth(text)) / 2;
        int y = (getHeight() + fm.getAscent()) / 2 - 4;
        g2.setFont(getFont());
        for (int r = 8; r >= 1; r--) {
            int alpha = (int) (60 / (float) r);
            g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), Math.min(255, alpha)));
            g2.drawString(text, x + r, y + r);
            g2.drawString(text, x - r, y - r);
        }
        g2.setColor(getForeground());
        g2.drawString(text, x, y);
        g2.dispose();
    }
}

class LoginPanel extends JPanel {
    public LoginPanel(Main frame) {
        setLayout(null);
        setOpaque(false);
        setPreferredSize(new Dimension(Main.WIDTH, Main.HEIGHT));

        GlowLabel title = new GlowLabel("FUN ARENA", Main.NEON_MAGENTA);
        title.setFont(new Font("Segoe UI", Font.BOLD, 44));
        title.setBounds(30, 60, Main.WIDTH - 60, 60);
        add(title);

        JLabel sub = new JLabel("Sign in to continue", SwingConstants.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(Main.TEXT_DIM);
        sub.setBounds(30, 122, Main.WIDTH - 60, 20);
        add(sub);

        GlassPanel card = new GlassPanel(Main.NEON_MAGENTA);
        card.setBounds(Main.WIDTH / 2 - 210, 170, 420, 320);
        add(card);

        JLabel emailLabel = new JLabel("EMAIL");
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        emailLabel.setForeground(Main.TEXT_DIM);
        emailLabel.setBounds(30, 30, 360, 16);
        card.add(emailLabel);

        JTextField emailField = new JTextField("you@example.com");
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        emailField.setForeground(Main.TEXT_LIGHT);
        emailField.setBackground(new Color(255, 255, 255, 25));
        emailField.setCaretColor(Main.TEXT_LIGHT);
        emailField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        emailField.setBounds(30, 50, 360, 38);
        emailField.setOpaque(true);
        card.add(emailField);

        emailField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (emailField.getText().equals("you@example.com")) emailField.setText("");
            }
        });

        GlowButton signInBtn = new GlowButton("SIGN IN", Main.NEON_MAGENTA);
        signInBtn.setBounds(30, 105, 360, 54);
        signInBtn.addActionListener(e -> {
            SoundManager.playClick();
            String email = emailField.getText().trim();
            if (email.isEmpty() || email.equals("you@example.com")) {
                JOptionPane.showMessageDialog(frame, "Please enter an email, or use Guest Mode instead.",
                        "Email required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            frame.onLoggedIn(email);
        });
        card.add(signInBtn);

        JLabel or = new JLabel("— OR —", SwingConstants.CENTER);
        or.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        or.setForeground(Main.TEXT_DIM);
        or.setBounds(30, 175, 360, 18);
        card.add(or);

        GlowButton guestBtn = new GlowButton("CONTINUE AS GUEST", Main.NEON_CYAN);
        guestBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        guestBtn.setBounds(30, 205, 360, 54);
        guestBtn.addActionListener(e -> { SoundManager.playClick(); frame.onLoggedIn("Guest"); });
        card.add(guestBtn);

        GlowButton exitBtn = new GlowButton("EXIT", new Color(220, 70, 80));
        exitBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        exitBtn.setBounds(Main.WIDTH / 2 - 90, 520, 180, 46);
        exitBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(frame, "Exit FUN ARENA?", "Exit",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                System.exit(0);
        });
        add(exitBtn);

        JLabel footer = new JLabel("This is a demo login — no account is actually created or verified.",
                SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setForeground(Main.TEXT_DIM);
        footer.setBounds(30, Main.HEIGHT - 34, Main.WIDTH - 60, 20);
        add(footer);
    }
}

class MenuPanel extends JPanel {
    private final JLabel welcomeLabel;

    public MenuPanel(Main frame) {
        setLayout(null);
        setOpaque(false);
        setPreferredSize(new Dimension(Main.WIDTH, Main.HEIGHT));

        welcomeLabel = new JLabel("Welcome, Guest", SwingConstants.RIGHT);
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        welcomeLabel.setForeground(Main.TEXT_DIM);
        welcomeLabel.setBounds(Main.WIDTH - 260, 16, 240, 18);
        add(welcomeLabel);

        GlowLabel title = new GlowLabel("FUN ARENA", Main.NEON_MAGENTA);
        title.setFont(new Font("Segoe UI", Font.BOLD, 44));
        title.setBounds(30, 40, Main.WIDTH - 60, 66);
        add(title);

        JLabel sub = new JLabel("PICK YOUR GAME", SwingConstants.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(Main.TEXT_DIM);
        sub.setBounds(30, 106, Main.WIDTH - 60, 20);
        add(sub);

        GlassPanel card = new GlassPanel(Main.NEON_MAGENTA);
        card.setBounds(Main.WIDTH / 2 - 220, 150, 440, 260);
        add(card);

        JLabel cardTitle = new JLabel("GAME LIBRARY", SwingConstants.CENTER);
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cardTitle.setForeground(Main.TEXT_DIM);
        cardTitle.setBounds(0, 18, 440, 18);
        card.add(cardTitle);

        JLabel fruitLabel = new JLabel("FRUIT NINJA", SwingConstants.CENTER);
        fruitLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        fruitLabel.setForeground(Main.NEON_MAGENTA);
        fruitLabel.setBounds(40, 42, 360, 16);
        card.add(fruitLabel);

        GlowButton fruitClassicBtn = new GlowButton("CLASSIC (3 LIVES)", Main.NEON_MAGENTA);
        fruitClassicBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        fruitClassicBtn.setBounds(40, 60, 175, 56);
        fruitClassicBtn.addActionListener(e -> {
            SoundManager.playClick();
            Main.fruitMode = Main.FruitMode.CLASSIC;
            frame.showScreen("fruitninja");
        });
        card.add(fruitClassicBtn);

        GlowButton fruitArcadeBtn = new GlowButton("ARCADE (TIME)", Main.NEON_PURPLE);
        fruitArcadeBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        fruitArcadeBtn.setBounds(225, 60, 175, 56);
        fruitArcadeBtn.addActionListener(e -> {
            SoundManager.playClick();
            Main.fruitMode = Main.FruitMode.ARCADE;
            frame.showScreen("fruitninja");
        });
        card.add(fruitArcadeBtn);

        GlowButton brickBtn = new GlowButton("BRICK BREAKER", Main.NEON_CYAN);
        brickBtn.setBounds(40, 145, 360, 70);
        brickBtn.addActionListener(e -> { SoundManager.playClick(); frame.showScreen("brick"); });
        card.add(brickBtn);

        int rowY = 430;
        int btnW = 170, btnH = 46, gap = 20;
        int totalW = 3 * btnW + 2 * gap;
        int startX = (Main.WIDTH - totalW) / 2;

        GlowButton settingsBtn = new GlowButton("SETTINGS", Main.NEON_PURPLE);
        settingsBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        settingsBtn.setBounds(startX, rowY, btnW, btnH);
        settingsBtn.addActionListener(e -> { SoundManager.playClick(); frame.showScreen("settings"); });
        add(settingsBtn);

        GlowButton aboutBtn = new GlowButton("HOW TO PLAY", Main.NEON_YELLOW);
        aboutBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        aboutBtn.setBounds(startX + btnW + gap, rowY, btnW, btnH);
        aboutBtn.addActionListener(e -> { SoundManager.playClick(); JOptionPane.showMessageDialog(frame,
                "FRUIT NINJA: drag the mouse to slice fruit, avoid bombs.\n" +
                "BRICK BREAKER: move the paddle with the mouse or arrow keys.\n\n" +
                "In any game: press ESC or the Pause button to pause, then Back to Menu or Resume.\n" +
                "ENTER restarts after game over.",
                "How To Play", JOptionPane.INFORMATION_MESSAGE); });
        add(aboutBtn);

        GlowButton exitBtn = new GlowButton("EXIT", new Color(220, 70, 80));
        exitBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        exitBtn.setBounds(startX + 2 * (btnW + gap), rowY, btnW, btnH);
        exitBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(frame, "Exit FUN ARENA?", "Exit",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                System.exit(0);
        });
        add(exitBtn);

        JLabel footer = new JLabel("Fruit Ninja: drag mouse to slice  ·  Brick Breaker: arrows/mouse to move  ·  ESC/Pause  ·  ENTER restart",
                SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footer.setForeground(Main.TEXT_DIM);
        footer.setBounds(30, Main.HEIGHT - 40, Main.WIDTH - 60, 24);
        add(footer);
    }

    public void refreshWelcome() {
        welcomeLabel.setText("Welcome, " + Main.username);
    }
}

class SettingsPanel extends JPanel {
    private final GlowButton easyBtn, medBtn, hardBtn, soundBtn;

    public SettingsPanel(Main frame) {
        setLayout(null);
        setOpaque(false);
        setPreferredSize(new Dimension(Main.WIDTH, Main.HEIGHT));

        GlowLabel title = new GlowLabel("SETTINGS", Main.NEON_PURPLE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setBounds(30, 50, Main.WIDTH - 60, 50);
        add(title);

        GlassPanel card = new GlassPanel(Main.NEON_PURPLE);
        card.setBounds(Main.WIDTH / 2 - 250, 140, 500, 300);
        add(card);

        JLabel diffLabel = new JLabel("DIFFICULTY", SwingConstants.CENTER);
        diffLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        diffLabel.setForeground(Main.TEXT_DIM);
        diffLabel.setBounds(0, 25, 500, 20);
        card.add(diffLabel);

        easyBtn = new GlowButton("EASY", Main.NEON_GREEN);
        easyBtn.setBounds(20, 55, 145, 56);
        card.add(easyBtn);

        medBtn = new GlowButton("MEDIUM", Main.NEON_CYAN);
        medBtn.setBounds(178, 55, 145, 56);
        card.add(medBtn);

        hardBtn = new GlowButton("HARD", new Color(255, 92, 92));
        hardBtn.setBounds(336, 55, 145, 56);
        card.add(hardBtn);

        easyBtn.addActionListener(e -> { Main.difficulty = Main.Difficulty.EASY; SoundManager.playClick(); refresh(); });
        medBtn.addActionListener(e -> { Main.difficulty = Main.Difficulty.NORMAL; SoundManager.playClick(); refresh(); });
        hardBtn.addActionListener(e -> { Main.difficulty = Main.Difficulty.HARD; SoundManager.playClick(); refresh(); });

        JLabel soundLabel = new JLabel("SOUND EFFECTS", SwingConstants.CENTER);
        soundLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        soundLabel.setForeground(Main.TEXT_DIM);
        soundLabel.setBounds(0, 140, 500, 20);
        card.add(soundLabel);

        soundBtn = new GlowButton(Main.soundOn ? "SOUND: ON" : "SOUND: OFF", Main.NEON_YELLOW);
        soundBtn.setBounds(150, 170, 200, 56);
        soundBtn.addActionListener(e -> {
            Main.soundOn = !Main.soundOn;
            soundBtn.setText(Main.soundOn ? "SOUND: ON" : "SOUND: OFF");
            if (Main.soundOn) SoundManager.playClick();
        });
        card.add(soundBtn);

        GlowButton backBtn = new GlowButton("◂ BACK TO MENU", Main.TEXT_DIM);
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backBtn.setBounds(Main.WIDTH / 2 - 130, 470, 260, 46);
        backBtn.addActionListener(e -> { SoundManager.playClick(); frame.showScreen("menu"); });
        add(backBtn);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { refresh(); }
        });
        refresh();
    }

    private void refresh() {
        easyBtn.setSelectedState(Main.difficulty == Main.Difficulty.EASY);
        medBtn.setSelectedState(Main.difficulty == Main.Difficulty.NORMAL);
        hardBtn.setSelectedState(Main.difficulty == Main.Difficulty.HARD);
    }
}

class PauseOverlay extends JPanel {
    private final GlowButton easyBtn, medBtn, hardBtn;

    public PauseOverlay(Runnable onResume, Runnable onBackToMenu) {
        setOpaque(false);
        setLayout(null);
        setBounds(Main.WIDTH / 2 - 210, Main.HEIGHT / 2 - 195, 420, 390);

        GlassPanel card = new GlassPanel(Main.NEON_PURPLE);
        card.setBounds(0, 0, 420, 390);
        add(card);

        GlowLabel title = new GlowLabel("PAUSED", Main.NEON_PURPLE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setBounds(0, 20, 420, 44);
        card.add(title);

        JLabel diffLabel = new JLabel("DIFFICULTY", SwingConstants.CENTER);
        diffLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        diffLabel.setForeground(Main.TEXT_DIM);
        diffLabel.setBounds(0, 80, 420, 18);
        card.add(diffLabel);

        easyBtn = new GlowButton("EASY", Main.NEON_GREEN);
        easyBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        easyBtn.setBounds(20, 105, 120, 50);
        card.add(easyBtn);

        medBtn = new GlowButton("MEDIUM", Main.NEON_CYAN);
        medBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        medBtn.setBounds(150, 105, 120, 50);
        card.add(medBtn);

        hardBtn = new GlowButton("HARD", new Color(255, 92, 92));
        hardBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        hardBtn.setBounds(280, 105, 120, 50);
        card.add(hardBtn);

        easyBtn.addActionListener(e -> { Main.difficulty = Main.Difficulty.EASY; SoundManager.playClick(); refreshDifficulty(); });
        medBtn.addActionListener(e -> { Main.difficulty = Main.Difficulty.NORMAL; SoundManager.playClick(); refreshDifficulty(); });
        hardBtn.addActionListener(e -> { Main.difficulty = Main.Difficulty.HARD; SoundManager.playClick(); refreshDifficulty(); });

        GlowButton resumeBtn = new GlowButton("▶ RESUME", Main.NEON_GREEN);
        resumeBtn.setBounds(30, 190, 360, 62);
        resumeBtn.addActionListener(e -> { SoundManager.playClick(); onResume.run(); });
        card.add(resumeBtn);

        GlowButton backBtn = new GlowButton("◂ BACK TO MENU", Main.TEXT_DIM);
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        backBtn.setBounds(30, 265, 360, 54);
        backBtn.addActionListener(e -> { SoundManager.playClick(); onBackToMenu.run(); });
        card.add(backBtn);

        refreshDifficulty();
    }

    public void refreshDifficulty() {
        easyBtn.setSelectedState(Main.difficulty == Main.Difficulty.EASY);
        medBtn.setSelectedState(Main.difficulty == Main.Difficulty.NORMAL);
        hardBtn.setSelectedState(Main.difficulty == Main.Difficulty.HARD);
    }
}

class PauseButton extends GlowButton {
    public PauseButton() {
        super("❚❚ PAUSE", Main.TEXT_DIM);
        setFont(new Font("Segoe UI", Font.BOLD, 13));
    }
}

class ResourceException extends Exception {
    public ResourceException(String message) { super(message); }
    public ResourceException(String message, Throwable cause) { super(message, cause); }
}

class ImageAssets {
    static final BufferedImage background = tryLoad("resources/backgound.jpg");
    static final BufferedImage fruitNinjaBg = tryLoad("resources/fruitninja_bg.png");
    static final BufferedImage bomb = tryLoad("resources/bomb.png");
    static final BufferedImage explosion = tryLoad("resources/explosion.png");
    static final BufferedImage gameOver = tryLoad("resources/game_over.png");

    private static BufferedImage tryLoad(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) throw new ResourceException("Image not found: " + path);
            BufferedImage img = ImageIO.read(file);
            if (img == null) throw new ResourceException("Unsupported or corrupt image: " + path);
            return img;
        } catch (Exception e) {
            System.err.println("[ImageAssets] " + e.getMessage() + " -- using built-in fallback drawing.");
            return null;
        }
    }
}

class HighScoreManager {
    private static final String FILE_NAME = "arcade_scores.dat";
    private static final Object lock = new Object();

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> loadMapInternal() throws ResourceException {
        File file = new File(FILE_NAME);
        if (!file.exists()) return new LinkedHashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Map<String, Integer>) ois.readObject();
        } catch (Exception e) {
            throw new ResourceException("Error reading high score database", e);
        }
    }

    public static int loadHighScore(String gameKey) {
        synchronized (lock) {
            try {
                return loadMapInternal().getOrDefault(gameKey, 0);
            } catch (ResourceException e) {
                System.err.println("[HighScore] " + e.getMessage() + " -- defaulting to 0.");
                return 0;
            }
        }
    }

    public static void saveHighScoreAsync(String gameKey, int score) {
        Thread saver = new Thread(() -> {
            synchronized (lock) {
                try {
                    Map<String, Integer> map = loadMapInternal();
                    if (score > map.getOrDefault(gameKey, 0)) {
                        map.put(gameKey, score);
                        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
                            oos.writeObject(map);
                        }
                    }
                } catch (ResourceException | IOException e) {
                    System.err.println("[HighScore] Save error: " + e.getMessage());
                }
            }
        }, "ScoreSave-Thread");
        saver.setDaemon(true);
        saver.start();
    }
}

class SoundManager {
    private static final float SAMPLE_RATE = 44100f;
    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Sound-Thread");
        t.setDaemon(true);
        return t;
    });
    private static volatile boolean available = true;

    private static void playSamples(byte[] samples) {
        if (!available || !Main.soundOn) return;
        POOL.submit(() -> {
            try {
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
                try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                    line.open(format);
                    line.start();
                    line.write(samples, 0, samples.length);
                    line.drain();
                }
            } catch (LineUnavailableException e) {
                System.err.println("[SoundManager] " + new ResourceException("No audio output line available", e).getMessage()
                        + " -- disabling sound for this session.");
                available = false;
            } catch (Exception e) {
                System.err.println("[SoundManager] Playback error: " + e.getMessage());
            }
        });
    }

    private static byte[] tone(double freqStart, double freqEnd, int durationMs, double volume) {
        int totalSamples = (int) (SAMPLE_RATE * durationMs / 1000.0);
        byte[] buffer = new byte[totalSamples * 2];
        for (int i = 0; i < totalSamples; i++) {
            double t = i / SAMPLE_RATE;
            double progress = totalSamples <= 1 ? 0 : (double) i / totalSamples;
            double freq = freqStart + (freqEnd - freqStart) * progress;
            double envelope = Math.min(1.0, Math.min(i / 150.0, (totalSamples - i) / 150.0));
            short sample = (short) (Math.sin(2 * Math.PI * freq * t) * volume * envelope * Short.MAX_VALUE);
            buffer[i * 2] = (byte) (sample & 0xff);
            buffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }
        return buffer;
    }

    private static byte[] noiseBurst(int durationMs, double volume) {
        int totalSamples = (int) (SAMPLE_RATE * durationMs / 1000.0);
        byte[] buffer = new byte[totalSamples * 2];
        Random r = new Random();
        for (int i = 0; i < totalSamples; i++) {
            double envelope = Math.max(0, 1.0 - (double) i / totalSamples);
            short sample = (short) ((r.nextDouble() * 2 - 1) * volume * envelope * Short.MAX_VALUE);
            buffer[i * 2] = (byte) (sample & 0xff);
            buffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }
        return buffer;
    }

    private static byte[] whoosh(int durationMs, double volume) {
        int totalSamples = (int) (SAMPLE_RATE * durationMs / 1000.0);
        byte[] buffer = new byte[totalSamples * 2];
        Random r = new Random();
        double filterState = 0;
        for (int i = 0; i < totalSamples; i++) {
            double progress = (double) i / totalSamples;
            double envelope = Math.exp(-progress * 7.0) * Math.min(1.0, i / 60.0);
            double noise = r.nextDouble() * 2 - 1;
            double cutoff = 0.55 - 0.4 * progress;
            filterState += cutoff * (noise - filterState);
            double sample = filterState * volume * envelope;
            sample = Math.max(-1, Math.min(1, sample));
            short s = (short) (sample * Short.MAX_VALUE);
            buffer[i * 2] = (byte) (s & 0xff);
            buffer[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
        }
        return buffer;
    }

    public static void playSlice() {
        playSamples(whoosh(140, 0.55));
    }
    public static void playBombExplosion() { playSamples(noiseBurst(350, 0.35)); }
    public static void playBrickHit() { playSamples(tone(700, 500, 60, 0.22)); }
    public static void playPaddleHit() { playSamples(tone(300, 420, 50, 0.2)); }
    public static void playGameOver() { playSamples(tone(500, 150, 400, 0.3)); }
    public static void playWin() { playSamples(tone(500, 1050, 400, 0.3)); }
    public static void playClick() { playSamples(tone(1100, 1100, 40, 0.15)); }
}

class HudUtil {
    public static void drawBadge(Graphics2D g2, String text, Color accent, int x, int y) {
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text) + 20;
        int h = 26;
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRoundRect(x + 1, y + 2, w, h, 8, 8);
        g2.setColor(new Color(16, 24, 46, 220));
        g2.fillRoundRect(x, y, w, h, 8, 8);
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 100));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 8, 8);
        g2.setColor(Main.TEXT_LIGHT);
        g2.drawString(text, x + 10, y + 17);
    }

    public static void drawCountdown(Graphics2D g2, String text, Color accent, int panelW, int panelH) {
        g2.setFont(new Font("Segoe UI", Font.BOLD, 70));
        FontMetrics fm = g2.getFontMetrics();
        int x = (panelW - fm.stringWidth(text)) / 2;
        int y = (panelH + fm.getAscent()) / 2 - 20;
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
        g2.drawString(text, x + 4, y + 4);
        g2.setColor(Main.TEXT_LIGHT);
        g2.drawString(text, x, y);
    }

    public static void drawOverlay(Graphics2D g2, String mainText, String subText, Color accent, int panelW, int panelH) {
        g2.setColor(new Color(6, 10, 24, 215));
        g2.fillRect(0, 0, panelW, panelH);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 40));
        FontMetrics fm = g2.getFontMetrics();
        int mx = (panelW - fm.stringWidth(mainText)) / 2;
        for (int r = 4; r >= 1; r--) {
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40 / r));
            g2.drawString(mainText, mx + r, panelH / 2 - 20 + r);
        }
        g2.setColor(accent);
        g2.drawString(mainText, mx, panelH / 2 - 20);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        fm = g2.getFontMetrics();
        int sx = (panelW - fm.stringWidth(subText)) / 2;
        g2.setColor(Main.TEXT_DIM);
        g2.drawString(subText, sx, panelH / 2 + 20);
    }

    public static void drawGameOverOverlay(Graphics2D g2, String subText, Color accent, int panelW, int panelH) {
        g2.setColor(new Color(6, 10, 24, 215));
        g2.fillRect(0, 0, panelW, panelH);

        if (ImageAssets.gameOver != null) {
            int imgW = 200;
            int imgH = (int) (imgW * (ImageAssets.gameOver.getHeight() / (double) ImageAssets.gameOver.getWidth()));
            int ix = (panelW - imgW) / 2;
            int iy = panelH / 2 - imgH - 20;
            g2.drawImage(ImageAssets.gameOver, ix, iy, imgW, imgH, null);
        } else {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 40));
            FontMetrics fm2 = g2.getFontMetrics();
            String mainText = "GAME OVER";
            int mx = (panelW - fm2.stringWidth(mainText)) / 2;
            for (int r = 4; r >= 1; r--) {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40 / r));
                g2.drawString(mainText, mx + r, panelH / 2 - 20 + r);
            }
            g2.setColor(accent);
            g2.drawString(mainText, mx, panelH / 2 - 20);
        }

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        FontMetrics fm3 = g2.getFontMetrics();
        int sx2 = (panelW - fm3.stringWidth(subText)) / 2;
        g2.setColor(Main.TEXT_DIM);
        g2.drawString(subText, sx2, panelH / 2 + 40);
    }
}

enum FruitType { APPLE, BANANA, ORANGE, MANGO, WATERMELON }

class Fruit {
    double x, y, vx, vy;
    final double gravity;
    int size;
    boolean sliced = false;
    FruitType type;
    double rotation = 0;
    final double rotationSpeed;
    int scoreValue = 10;

    Fruit(double x, double y, double vx, double vy, double gravity, int size, FruitType type) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.gravity = gravity; this.size = size; this.type = type;
        this.rotationSpeed = (Math.random() - 0.5) * 0.15;
    }

    void update() {
        x += vx; y += vy; vy += gravity; rotation += rotationSpeed;
    }

    Color haloColor() {
        if (type == null) return Color.WHITE;
        switch (type) {
            case APPLE: return new Color(220, 20, 60);
            case BANANA: return new Color(255, 215, 0);
            case ORANGE: return new Color(255, 140, 0);
            case MANGO: return new Color(255, 170, 40);
            case WATERMELON: return new Color(50, 205, 90);
            default: return Color.WHITE;
        }
    }

    void draw(Graphics2D g2) {
        Graphics2D g = (Graphics2D) g2.create();
        g.translate(x, y);
        g.rotate(rotation);
        Color halo = haloColor();
        g.setColor(new Color(halo.getRed(), halo.getGreen(), halo.getBlue(), 70));
        g.fillOval(-size / 2 - 4, -size / 2 - 4, size + 8, size + 8);

        if (type != null) renderShape(g, type, size);
        g.dispose();
    }

    static void renderShape(Graphics2D g, FruitType type, int size) {
        switch (type) {
            case APPLE: drawApple(g, size); break;
            case BANANA: drawBanana(g, size); break;
            case ORANGE: drawOrange(g, size); break;
            case MANGO: drawMango(g, size); break;
            case WATERMELON: drawWatermelon(g, size); break;
        }
    }

    private static void drawApple(Graphics2D g, int s) {
        GradientPaint grad = new GradientPaint(-s / 2f, -s / 2f, new Color(255, 70, 70),
                s / 2f, s / 2f, new Color(190, 20, 30));
        g.setPaint(grad);
        g.fillOval(-s / 2, -s / 2, s, s);
        g.setColor(new Color(120, 20, 20));
        g.setStroke(new BasicStroke(2f));
        g.drawOval(-s / 2, -s / 2, s, s);
        g.setColor(new Color(101, 67, 33));
        g.fillRect(-2, -s / 2 - 8, 4, 10);
        g.setColor(new Color(60, 160, 60));
        g.fillOval(2, -s / 2 - 6, s / 5, s / 8);
        g.setColor(new Color(255, 255, 255, 110));
        g.fillOval(-s / 4, -s / 3, s / 5, s / 6);
    }

    private static void drawBanana(Graphics2D g, int s) {
        Path2D path = new Path2D.Double();
        path.moveTo(-s / 2.0, s / 6.0);
        path.quadTo(0, -s / 1.6, s / 2.0, s / 6.0);
        path.quadTo(0, s / 2.6, -s / 2.0, s / 6.0);
        path.closePath();
        g.setColor(new Color(240, 210, 40));
        g.fill(path);
        g.setColor(new Color(160, 120, 20));
        g.setStroke(new BasicStroke(2f));
        g.draw(path);
        g.setColor(new Color(90, 60, 10));
        g.fillOval((int) (-s / 2.0) - 3, (int) (s / 6.0) - 4, 8, 8);
        g.fillOval((int) (s / 2.0) - 5, (int) (s / 6.0) - 4, 8, 8);
    }

    private static void drawOrange(Graphics2D g, int s) {
        GradientPaint grad = new GradientPaint(-s / 2f, -s / 2f, new Color(255, 175, 60),
                s / 2f, s / 2f, new Color(230, 110, 10));
        g.setPaint(grad);
        g.fillOval(-s / 2, -s / 2, s, s);
        g.setColor(new Color(170, 90, 10));
        g.setStroke(new BasicStroke(2f));
        g.drawOval(-s / 2, -s / 2, s, s);
        g.setColor(new Color(90, 140, 60));
        g.fillRoundRect(-3, -s / 2 - 7, 6, 8, 3, 3);
        Random dotR = new Random(s);
        g.setColor(new Color(200, 110, 20, 130));
        for (int i = 0; i < 6; i++) {
            int dx = -s / 3 + dotR.nextInt(2 * s / 3);
            int dy = -s / 3 + dotR.nextInt(2 * s / 3);
            g.fillOval(dx, dy, 3, 3);
        }
    }

    private static void drawMango(Graphics2D g, int s) {
        int w = s, h = (int) (s * 0.85);
        GradientPaint grad = new GradientPaint(-w / 2f, -h / 2f, new Color(255, 210, 60),
                w / 2f, h / 2f, new Color(230, 90, 40));
        g.setPaint(grad);
        Ellipse2D shape = new Ellipse2D.Float(-w / 2f, -h / 2f, w, h);
        g.fill(shape);
        g.setColor(new Color(180, 60, 20));
        g.setStroke(new BasicStroke(2f));
        g.draw(shape);
        g.setColor(new Color(255, 90, 60, 120));
        g.fillOval(-w / 6, -h / 3, w / 3, h / 3);
        g.setColor(new Color(80, 120, 40));
        g.fillRoundRect(-2, -h / 2 - 6, 4, 7, 2, 2);
    }

    private static void drawWatermelon(Graphics2D g, int s) {
        GradientPaint grad = new GradientPaint(-s / 2f, -s / 2f, new Color(70, 200, 90),
                s / 2f, s / 2f, new Color(20, 130, 60));
        g.setPaint(grad);
        g.fillOval(-s / 2, -s / 2, s, s);
        g.setColor(new Color(10, 90, 40));
        g.setStroke(new BasicStroke(3f));
        for (int i = -2; i <= 2; i++) {
            double offset = i * (s / 6.0);
            g.draw(new Arc2D.Double(-s / 2.0 + Math.abs(offset) * 0.3, -s / 2.0, s - Math.abs(offset) * 0.6, s,
                    0, 360, Arc2D.OPEN));
        }
        g.setColor(new Color(10, 70, 30));
        g.drawOval(-s / 2, -s / 2, s, s);
    }

    Rectangle getBounds() {
        return new Rectangle((int) (x - size / 2.0), (int) (y - size / 2.0), size, size);
    }

    boolean isOffScreen(int panelHeight) { return y - size > panelHeight; }
}

class Bomb extends Fruit {
    Bomb(double x, double y, double vx, double vy, double gravity, int size) {
        super(x, y, vx, vy, gravity, size, null);
        this.scoreValue = 0;
    }

    @Override
    void draw(Graphics2D g2) {
        Graphics2D g = (Graphics2D) g2.create();
        g.translate(x, y);
        g.rotate(rotation);
        g.setColor(new Color(255, 60, 60, 60));
        g.fillOval(-size / 2 - 5, -size / 2 - 5, size + 10, size + 10);
        if (ImageAssets.bomb != null) {
            g.drawImage(ImageAssets.bomb, -size / 2, -size / 2, size, size, null);
        } else {
            g.setColor(Color.BLACK);
            g.fillOval(-size / 2, -size / 2, size, size);
            g.setColor(new Color(90, 90, 90));
            g.drawOval(-size / 2, -size / 2, size, size);
            g.setColor(Color.ORANGE);
            g.fillRect(-3, -size / 2 - 10, 6, 12);
            g.setColor(Color.RED);
            g.fillOval(-size / 2 + 8, -size / 2 + 8, 10, 10);
        }
        g.dispose();
    }
}

class SlicedHalf {
    private final FruitType type;
    private final int size;
    private double x, y, vx, vy;
    private final double gravity;
    private double rotation;
    private final double rotationSpeed;
    private final double splitAngle;
    private final boolean leftSide;
    private final long startTime = System.currentTimeMillis();
    private static final long LIFETIME_MS = 1400;

    SlicedHalf(FruitType type, int size, double x, double y, double vx, double vy, double gravity,
               double rotation, double splitAngle, boolean leftSide) {
        this.type = type; this.size = size;
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.gravity = gravity; this.rotation = rotation;
        this.splitAngle = splitAngle; this.leftSide = leftSide;
        this.rotationSpeed = (Math.random() - 0.5) * 0.25;
    }

    void update() {
        x += vx; y += vy; vy += gravity; rotation += rotationSpeed;
    }

    boolean isExpired(int panelHeight) {
        return y - size > panelHeight || System.currentTimeMillis() - startTime > LIFETIME_MS;
    }

    void draw(Graphics2D g2) {
        Graphics2D g = (Graphics2D) g2.create();
        g.translate(x, y);
        g.rotate(rotation);

        Rectangle2D half = leftSide
                ? new Rectangle2D.Double(-size, -size, size, size * 2)
                : new Rectangle2D.Double(0, -size, size, size * 2);
        Shape clipShape = AffineTransform.getRotateInstance(splitAngle).createTransformedShape(half);
        g.clip(clipShape);

        if (type != null) Fruit.renderShape(g, type, size);

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed < 250) {
            float flashAlpha = 1f - elapsed / 250f;
            g.setColor(new Color(255, 255, 255, (int) (180 * flashAlpha)));
            g.setStroke(new BasicStroke(2.5f));
            Shape edge = AffineTransform.getRotateInstance(splitAngle)
                    .createTransformedShape(new Line2D.Double(0, -size, 0, size));
            g.draw(edge);
        }
        g.dispose();
    }
}

class Explosion {
    final double x, y;
    final int size;
    final long startTime = System.currentTimeMillis();
    private static final long LIFETIME_MS = 380;

    Explosion(double x, double y, int size) {
        this.x = x; this.y = y; this.size = size;
    }

    boolean isExpired() {
        return System.currentTimeMillis() - startTime > LIFETIME_MS;
    }

    void draw(Graphics2D g2) {
        long elapsed = System.currentTimeMillis() - startTime;
        float progress = Math.min(1f, elapsed / (float) LIFETIME_MS);
        float alpha = 1f - progress;
        int drawSize = (int) (size * (1f + progress * 0.6));

        Graphics2D g = (Graphics2D) g2.create();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
        if (ImageAssets.explosion != null) {
            g.drawImage(ImageAssets.explosion, (int) (x - drawSize / 2.0), (int) (y - drawSize / 2.0), drawSize, drawSize, null);
        } else {
            g.setColor(new Color(255, 200, 60));
            g.fillOval((int) (x - drawSize / 2.0), (int) (y - drawSize / 2.0), drawSize, drawSize);
            g.setColor(new Color(255, 90, 30));
            g.fillOval((int) (x - drawSize / 3.0), (int) (y - drawSize / 3.0), (int) (drawSize * 0.66), (int) (drawSize * 0.66));
        }
        g.dispose();
    }
}

class Blade {
    private static final class TrailPoint {
        final int x, y; final long t;
        TrailPoint(int x, int y) { this.x = x; this.y = y; this.t = System.currentTimeMillis(); }
    }
    private final LinkedList<TrailPoint> points = new LinkedList<>();
    private static final long LIFETIME_MS = 200;
    private static final int MAX_POINTS = 30;

    synchronized void addPoint(int x, int y) {
        points.addLast(new TrailPoint(x, y));
        while (points.size() > MAX_POINTS) points.removeFirst();
    }

    synchronized double lastAngle() {
        if (points.size() < 2) return 0;
        TrailPoint a = points.get(Math.max(0, points.size() - 4));
        TrailPoint b = points.getLast();
        if (a == b) return 0;
        return Math.atan2(b.y - a.y, b.x - a.x);
    }

    synchronized void draw(Graphics2D g2) {
        long now = System.currentTimeMillis();
        points.removeIf(p -> now - p.t > LIFETIME_MS);
        if (points.size() < 2) return;
        int n = points.size();
        for (int i = 1; i < n; i++) {
            TrailPoint a = points.get(i - 1);
            TrailPoint b = points.get(i);
            float alpha = (float) i / n;
            g2.setStroke(new BasicStroke(2f + alpha * 5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 255, 255, (int) (alpha * 220)));
            g2.drawLine(a.x, a.y, b.x, b.y);
        }
    }
}

class FruitNinjaPanel extends JPanel implements KeyListener {
    private final Color ACCENT = Main.NEON_MAGENTA;
    private static final int BG_STAR_COUNT = 45;
    private final double[] bgStarX = new double[BG_STAR_COUNT];
    private final double[] bgStarBaseY = new double[BG_STAR_COUNT];
    private final double[] bgStarSpeed = new double[BG_STAR_COUNT];
    private final float[] bgStarRadius = new float[BG_STAR_COUNT];
    private final float[] bgStarPhase = new float[BG_STAR_COUNT];
    private final long bgAnimStart = System.currentTimeMillis();

    private final CopyOnWriteArrayList<Fruit> fruits = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Explosion> explosions = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<SlicedHalf> halves = new CopyOnWriteArrayList<>();
    private final Blade blade = new Blade();
    private final Random rand = new Random();
    private final Main frame;
    private final PauseOverlay pauseOverlay;

    private int score, lives, highScore;
    private boolean running = false;
    private boolean paused = false;
    private boolean gameOver = false;
    private double remainingMs;
    private Timer renderTimer;
    private ScheduledExecutorService spawner;

    public FruitNinjaPanel(Main frame) {
        this.frame = frame;
        setLayout(null);
        setOpaque(true);
        setBackground(new Color(10, 5, 15));
        setPreferredSize(new Dimension(Main.WIDTH, Main.HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        Random bgRand = new Random();
        for (int i = 0; i < BG_STAR_COUNT; i++) {
            bgStarX[i] = bgRand.nextDouble() * Main.WIDTH;
            bgStarBaseY[i] = bgRand.nextDouble() * Main.HEIGHT;
            bgStarSpeed[i] = 6 + bgRand.nextDouble() * 14;
            bgStarRadius[i] = 0.8f + bgRand.nextFloat() * 2.0f;
            bgStarPhase[i] = bgRand.nextFloat() * 6.28f;
        }

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { slice(e.getX(), e.getY()); }
            @Override public void mouseDragged(MouseEvent e) { slice(e.getX(), e.getY()); }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        PauseButton pauseBtn = new PauseButton();
        pauseBtn.setBounds(Main.WIDTH / 2 - 55, 10, 110, 30);
        pauseBtn.addActionListener(e -> togglePause());
        add(pauseBtn);

        pauseOverlay = new PauseOverlay(this::resumeGame, this::backToMenuFromPause);
        pauseOverlay.setVisible(false);
        add(pauseOverlay);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { startGame(); }
            @Override public void componentHidden(ComponentEvent e) { stopAll(); }
        });
    }

    private void togglePause() {
        if (gameOver) return;
        if (!paused && !running) return;
        paused = !paused;
        if (paused) {
            pauseOverlay.refreshDifficulty();
            pauseOverlay.setVisible(true);
        } else {
            pauseOverlay.setVisible(false);
        }
        requestFocusInWindow();
        repaint();
    }

    private void resumeGame() {
        paused = false;
        pauseOverlay.setVisible(false);
        requestFocusInWindow();
        repaint();
    }

    private void backToMenuFromPause() {
        stopAll();
        paused = false;
        pauseOverlay.setVisible(false);
        frame.showScreen("menu");
    }

    private void spawnHalves(Fruit f) {
        double bladeAngle = blade.lastAngle();
        double sep = 2.3;
        double pushX = Math.cos(bladeAngle + Math.PI / 2) * sep;
        double pushY = Math.sin(bladeAngle + Math.PI / 2) * sep;
        halves.add(new SlicedHalf(f.type, f.size, f.x, f.y, f.vx - pushX, f.vy - pushY, f.gravity,
                f.rotation, bladeAngle, true));
        halves.add(new SlicedHalf(f.type, f.size, f.x, f.y, f.vx + pushX, f.vy + pushY, f.gravity,
                f.rotation, bladeAngle, false));
    }

    private String fruitScoreKey() {
        return Main.fruitMode == Main.FruitMode.CLASSIC ? "fruitninja_classic" : "fruitninja_arcade";
    }

    private void slice(int x, int y) {
        if (paused) return;
        blade.addPoint(x, y);
        if (!running) return;
        for (Fruit f : fruits) {
            if (f.sliced) continue;
            if (f.getBounds().contains(x, y)) {
                f.sliced = true;
                fruits.remove(f);
                if (f instanceof Bomb) {
                    lives--;
                    explosions.add(new Explosion(f.x, f.y, f.size + 30));
                    SoundManager.playBombExplosion();
                    if (lives <= 0) endGame();
                } else {
                    score += f.scoreValue;
                    spawnHalves(f);
                    SoundManager.playSlice();
                }
            }
        }
    }

    private void startGame() {
        stopAll();
        fruits.clear();
        halves.clear();
        score = 0;
        lives = 3;
        running = true;
        paused = false;
        gameOver = false;
        pauseOverlay.setVisible(false);
        highScore = HighScoreManager.loadHighScore(fruitScoreKey());

        remainingMs = switch (Main.difficulty) {
            case EASY -> 75_000; case NORMAL -> 60_000; case HARD -> 45_000;
        };
        if (Main.fruitMode == Main.FruitMode.CLASSIC) remainingMs = Double.POSITIVE_INFINITY;

        spawner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FruitSpawner-Thread");
            t.setDaemon(true);
            return t;
        });
        long spawnIntervalMs = switch (Main.difficulty) {
            case EASY -> 1400; case NORMAL -> 1100; case HARD -> 800;
        };
        spawner.scheduleAtFixedRate(this::spawnWave, 400, spawnIntervalMs, TimeUnit.MILLISECONDS);

        renderTimer = new Timer(16, e -> { if (running && !paused) tick(); repaint(); });
        renderTimer.start();

        requestFocusInWindow();
    }

    private void spawnWave() {
        if (!running || paused) return;
        int count = 1 + rand.nextInt(2);
        for (int i = 0; i < count; i++) fruits.add(createRandomEntity());
    }

    private Fruit createRandomEntity() {
        double x = 60 + rand.nextInt(Math.max(1, Main.WIDTH - 120));
        double y = Main.HEIGHT + 40;

        double vyMin = switch (Main.difficulty) { case EASY -> 13; case NORMAL -> 15; case HARD -> 17; };
        double vyRange = switch (Main.difficulty) { case EASY -> 5; case NORMAL -> 6; case HARD -> 8; };
        double bombChance = switch (Main.difficulty) { case EASY -> 0.08; case NORMAL -> 0.15; case HARD -> 0.22; };

        double vx = (rand.nextDouble() - 0.5) * 4;
        double vy = -(vyMin + rand.nextDouble() * vyRange);
        int size = 55 + rand.nextInt(20);

        if (rand.nextDouble() < bombChance) return new Bomb(x, y, vx, vy, 0.35, size);
        FruitType[] types = FruitType.values();
        return new Fruit(x, y, vx, vy, 0.35, size, types[rand.nextInt(types.length)]);
    }

    private void tick() {
        if (Main.fruitMode == Main.FruitMode.ARCADE) {
            remainingMs -= 16;
            if (remainingMs <= 0) {
                remainingMs = 0;
                endGame();
                return;
            }
        }

        explosions.removeIf(Explosion::isExpired);
        for (SlicedHalf h : halves) {
            h.update();
            if (h.isExpired(Main.HEIGHT)) halves.remove(h);
        }
        for (Fruit f : fruits) {
            f.update();
            if (f.isOffScreen(Main.HEIGHT)) {
                if (!f.sliced && !(f instanceof Bomb)) lives--;
                fruits.remove(f);
                if (lives <= 0) { endGame(); break; }
            }
        }
    }

    private void endGame() {
        running = false;
        gameOver = true;
        paused = false;
        pauseOverlay.setVisible(false);
        SoundManager.playGameOver();
        if (spawner != null) spawner.shutdownNow();
        if (score > highScore) {
            highScore = score;
            HighScoreManager.saveHighScoreAsync(fruitScoreKey(), highScore);
        }
    }

    private void stopAll() {
        running = false;
        paused = false;
        pauseOverlay.setVisible(false);
        if (renderTimer != null) { renderTimer.stop(); renderTimer = null; }
        if (spawner != null) { spawner.shutdownNow(); spawner = null; }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (ImageAssets.fruitNinjaBg != null) {
            g2.drawImage(ImageAssets.fruitNinjaBg, 0, 0, getWidth(), getHeight(), null);
            g2.setColor(new Color(10, 5, 15, 120));
            g2.fillRect(0, 0, getWidth(), getHeight());
        } else if (ImageAssets.background != null) {
            g2.drawImage(ImageAssets.background, 0, 0, getWidth(), getHeight(), null);
            g2.setColor(new Color(10, 5, 15, 150));
            g2.fillRect(0, 0, getWidth(), getHeight());
        } else {
            GradientPaint gp = new GradientPaint(0, 0, Main.BG_NAVY_TOP, 0, getHeight(), Main.BG_NAVY_BOTTOM);
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        double elapsedSec = (System.currentTimeMillis() - bgAnimStart) / 1000.0;
        for (int i = 0; i < BG_STAR_COUNT; i++) {
            double y = (bgStarBaseY[i] + elapsedSec * bgStarSpeed[i]) % Main.HEIGHT;
            float twinkle = 0.5f + 0.5f * (float) Math.sin(elapsedSec * 2 + bgStarPhase[i]);
            g2.setColor(new Color(255, 255, 255, (int) (60 + 120 * twinkle)));
            g2.fill(new Ellipse2D.Double(bgStarX[i], y, bgStarRadius[i], bgStarRadius[i]));
        }

        for (Fruit f : fruits) if (!f.sliced) f.draw(g2);
        for (SlicedHalf h : halves) h.draw(g2);
        blade.draw(g2);
        for (Explosion ex : explosions) ex.draw(g2);

        HudUtil.drawBadge(g2, "SCORE " + score, ACCENT, 10, 10);
        HudUtil.drawBadge(g2, "HIGH " + highScore, Main.NEON_YELLOW, 140, 10);
        HudUtil.drawBadge(g2, "LIVES " + lives, Main.NEON_GREEN, Main.WIDTH - 130, 10);
        if (Main.fruitMode == Main.FruitMode.ARCADE) {
            int seconds = (int) Math.ceil(remainingMs / 1000.0);
            String timeText = String.format("TIME %01d:%02d", seconds / 60, seconds % 60);
            HudUtil.drawBadge(g2, timeText, Main.NEON_CYAN, Main.WIDTH / 2 - 55, 46);
        } else {
            HudUtil.drawBadge(g2, "CLASSIC", Main.NEON_MAGENTA, Main.WIDTH / 2 - 45, 46);
        }

        if (paused) {
            g2.setColor(new Color(6, 10, 24, 170));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        if (gameOver) HudUtil.drawGameOverOverlay(g2,
                "Final Score: " + score + "   Press ENTER to restart, ESC for menu",
                ACCENT, Main.WIDTH, Main.HEIGHT);
    }

    @Override public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_ESCAPE) {
            if (gameOver) { stopAll(); frame.showScreen("menu"); return; }
            togglePause();
            return;
        }
        if (gameOver && key == KeyEvent.VK_ENTER) startGame();
    }
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}

class BrickBreakerPanel extends JPanel implements ActionListener, KeyListener {
    private final int PADDLE_W = 90, PADDLE_H = 14, BALL_SIZE = 14;
    private final int BRICK_ROWS = 5, BRICK_COLS = 10;
    private final int BRICK_W = 68, BRICK_H = 22, BRICK_GAP = 6, BRICK_TOP = 70;
    private final Color ACCENT = Main.NEON_CYAN;

    private int paddleX;
    private double ballX, ballY, ballVX, ballVY, speedMul;
    private boolean[][] bricks;
    private boolean running, win, gameOver, paused;
    private int score, lives, highScore;
    private Timer gameTimer, countdownTimer;
    private boolean leftPressed, rightPressed;
    private final Main frame;
    private final PauseOverlay pauseOverlay;
    private int countdownValue;
    private boolean countdownActive;
    private String countdownText = "";

    private static final Color[] ROW_COLORS = {
            new Color(255, 92, 92), new Color(255, 184, 77), new Color(255, 225, 77),
            new Color(125, 255, 77), new Color(77, 225, 255)
    };

    public BrickBreakerPanel(Main frame) {
        this.frame = frame;
        setLayout(null);
        setOpaque(false);
        setPreferredSize(new Dimension(Main.WIDTH, Main.HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) { dragPaddle(e.getX()); }
            @Override public void mouseMoved(MouseEvent e) { dragPaddle(e.getX()); }
        });

        PauseButton pauseBtn = new PauseButton();
        pauseBtn.setBounds(Main.WIDTH / 2 - 55, 10, 110, 30);
        pauseBtn.addActionListener(e -> togglePause());
        add(pauseBtn);

        pauseOverlay = new PauseOverlay(this::resumeGame, this::backToMenuFromPause);
        pauseOverlay.setVisible(false);
        add(pauseOverlay);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { startGame(); }
            @Override public void componentHidden(ComponentEvent e) { stopTimers(); }
        });
    }

    private void togglePause() {
        if (gameOver) return;
        if (!paused && !running) return;
        paused = !paused;
        if (paused) {
            pauseOverlay.refreshDifficulty();
            pauseOverlay.setVisible(true);
        } else {
            pauseOverlay.setVisible(false);
        }
        requestFocusInWindow();
        repaint();
    }

    private void resumeGame() {
        paused = false;
        pauseOverlay.setVisible(false);
        requestFocusInWindow();
        repaint();
    }

    private void backToMenuFromPause() {
        stopTimers();
        paused = false;
        pauseOverlay.setVisible(false);
        frame.showScreen("menu");
    }

    private void stopTimers() {
        if (gameTimer != null) { gameTimer.stop(); gameTimer = null; }
        if (countdownTimer != null) { countdownTimer.stop(); countdownTimer = null; }
    }

    private void dragPaddle(int mouseX) {
        if (paused) return;
        paddleX = Math.max(0, Math.min(Main.WIDTH - PADDLE_W, mouseX - PADDLE_W / 2));
    }

    private void startGame() {
        stopTimers();
        paddleX = Main.WIDTH / 2 - PADDLE_W / 2;
        speedMul = switch (Main.difficulty) { case EASY -> 0.95; case NORMAL -> 1.3; case HARD -> 1.7; };
        ballX = Main.WIDTH / 2.0; ballY = 420;
        ballVX = 4.4 * speedMul; ballVY = -4.4 * speedMul;
        score = 0; lives = 3; running = false; win = false; gameOver = false; paused = false;
        pauseOverlay.setVisible(false);
        bricks = new boolean[BRICK_ROWS][BRICK_COLS];
        for (boolean[] row : bricks) Arrays.fill(row, true);
        highScore = HighScoreManager.loadHighScore("brick");
        gameTimer = new Timer(12, this);
        startCountdown();
        requestFocusInWindow();
    }

    private void startCountdown() {
        countdownValue = 3;
        countdownActive = true;
        countdownText = "3";
        countdownTimer = new Timer(600, e -> {
            countdownValue--;
            if (countdownValue > 0) countdownText = String.valueOf(countdownValue);
            else if (countdownValue == 0) countdownText = "GO!";
            else {
                countdownActive = false;
                countdownTimer.stop();
                gameTimer.start();
                running = true;
                countdownText = "";
            }
            repaint();
        });
        countdownTimer.setInitialDelay(0);
        countdownTimer.start();
    }

    private int bricksLeftMargin() {
        int totalW = BRICK_COLS * (BRICK_W + BRICK_GAP) - BRICK_GAP;
        return (Main.WIDTH - totalW) / 2;
    }

    private Rectangle brickRect(int r, int c) {
        int left = bricksLeftMargin();
        return new Rectangle(left + c * (BRICK_W + BRICK_GAP), BRICK_TOP + r * (BRICK_H + BRICK_GAP),
                BRICK_W, BRICK_H);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int r = 0; r < BRICK_ROWS; r++) {
            for (int c = 0; c < BRICK_COLS; c++) {
                if (bricks[r][c]) {
                    Rectangle rect = brickRect(r, c);
                    Color base = ROW_COLORS[r % ROW_COLORS.length];
                    GradientPaint grad = new GradientPaint(rect.x, rect.y, base.brighter(),
                            rect.x, rect.y + rect.height, base.darker());
                    g2.setPaint(grad);
                    g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 5, 5);
                    g2.setColor(new Color(255, 255, 255, 60));
                    g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height / 3, 5, 5);
                    g2.setColor(new Color(0, 0, 0, 80));
                    g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 5, 5);
                }
            }
        }

        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(paddleX, Main.HEIGHT - 38, PADDLE_W, PADDLE_H, 6, 6);
        GradientPaint paddleGrad = new GradientPaint(paddleX, Main.HEIGHT - 40, Color.WHITE,
                paddleX, Main.HEIGHT - 40 + PADDLE_H, new Color(200, 200, 220));
        g2.setPaint(paddleGrad);
        g2.fillRoundRect(paddleX, Main.HEIGHT - 40, PADDLE_W, PADDLE_H, 6, 6);

        g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 90));
        g2.fillOval((int) ballX - 3, (int) ballY - 3, BALL_SIZE + 6, BALL_SIZE + 6);
        g2.setColor(ACCENT);
        g2.fillOval((int) ballX, (int) ballY, BALL_SIZE, BALL_SIZE);

        HudUtil.drawBadge(g2, "SCORE " + score, ACCENT, 10, 10);
        HudUtil.drawBadge(g2, "HIGH " + highScore, Main.NEON_YELLOW, 140, 10);
        HudUtil.drawBadge(g2, "LIVES " + lives, Main.NEON_ORANGE, Main.WIDTH - 130, 10);

        if (paused) {
            g2.setColor(new Color(6, 10, 24, 170));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        if (countdownActive) HudUtil.drawCountdown(g2, countdownText, ACCENT, Main.WIDTH, Main.HEIGHT);
        else if (gameOver) {
            if (win) {
                HudUtil.drawOverlay(g2, "YOU WIN!", "Press ENTER to restart, ESC for menu",
                        Main.NEON_GREEN, Main.WIDTH, Main.HEIGHT);
            } else {
                HudUtil.drawGameOverOverlay(g2, "Press ENTER to restart, ESC for menu",
                        new Color(255, 92, 92), Main.WIDTH, Main.HEIGHT);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running && !paused) update();
        repaint();
    }

    private void update() {
        if (leftPressed) paddleX -= 6;
        if (rightPressed) paddleX += 6;
        paddleX = Math.max(0, Math.min(Main.WIDTH - PADDLE_W, paddleX));

        ballX += ballVX; ballY += ballVY;

        if (ballX <= 0) { ballX = 0; ballVX = -ballVX; }
        if (ballX >= Main.WIDTH - BALL_SIZE) { ballX = Main.WIDTH - BALL_SIZE; ballVX = -ballVX; }
        if (ballY <= 0) { ballY = 0; ballVY = -ballVY; }

        if (ballY >= Main.HEIGHT) {
            lives--;
            if (lives <= 0) {
                finish(false);
            } else {
                ballX = Main.WIDTH / 2.0; ballY = 420;
                ballVX = 4.4 * speedMul; ballVY = -4.4 * speedMul;
                running = false;
                startCountdown();
            }
            return;
        }

        Rectangle pRect = new Rectangle(paddleX, Main.HEIGHT - 40, PADDLE_W, PADDLE_H);
        Rectangle bRect = new Rectangle((int) ballX, (int) ballY, BALL_SIZE, BALL_SIZE);
        if (bRect.intersects(pRect)) {
            ballVY = -Math.abs(ballVY);
            double hit = (ballX + BALL_SIZE / 2.0) - (paddleX + PADDLE_W / 2.0);
            ballVX = (hit / (PADDLE_W / 2.0)) * 4.3 * speedMul;
            SoundManager.playPaddleHit();
        }

        boolean hitBrick = false;
        for (int r = 0; r < BRICK_ROWS && !hitBrick; r++) {
            for (int c = 0; c < BRICK_COLS && !hitBrick; c++) {
                if (bricks[r][c]) {
                    Rectangle bk = brickRect(r, c);
                    if (bRect.intersects(bk)) {
                        bricks[r][c] = false;
                        score += 20;
                        ballVY = -ballVY;
                        hitBrick = true;
                        SoundManager.playBrickHit();
                    }
                }
            }
        }

        boolean allCleared = true;
        outer:
        for (int r = 0; r < BRICK_ROWS; r++) {
            for (int c = 0; c < BRICK_COLS; c++) {
                if (bricks[r][c]) { allCleared = false; break outer; }
            }
        }
        if (allCleared) finish(true);
    }

    private void finish(boolean won) {
        running = false;
        gameOver = true;
        paused = false;
        pauseOverlay.setVisible(false);
        win = won;
        gameTimer.stop();
        if (won) SoundManager.playWin(); else SoundManager.playGameOver();
        if (score > highScore) {
            highScore = score;
            HighScoreManager.saveHighScoreAsync("brick", highScore);
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_ESCAPE) {
            if (gameOver) { stopTimers(); frame.showScreen("menu"); return; }
            togglePause();
            return;
        }
        if (gameOver && key == KeyEvent.VK_ENTER) { startGame(); return; }
        if (paused) return;
        if (key == KeyEvent.VK_LEFT) leftPressed = true;
        if (key == KeyEvent.VK_RIGHT) rightPressed = true;
    }
    @Override public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) leftPressed = false;
        if (key == KeyEvent.VK_RIGHT) rightPressed = false;
    }
    @Override public void keyTyped(KeyEvent e) {}
}
