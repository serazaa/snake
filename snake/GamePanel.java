package com.mycompany.snake;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.prefs.Preferences;

/**
 * GamePanel
 * Renders and runs the Snake simulation on a fixed grid.
 * Design notes:
 *  - All movement ticks happen on Swing's Timer (game loop on the EDT).
 *  - Grid is 24x24 at 25px to keep it visually crisp and light on older GPUs.
 *  - Golden apples speed the loop and act as small risk/reward spikes.
 *  - No background threads; avoids synchronization bugs for beginners.
 */
public class GamePanel extends JPanel implements ActionListener {
    // Logical grid
    public static final int CELL = 25;
    public static final int COLS = 24;             // 24 * 25 = 600
    public static final int ROWS = 24;             // 24 * 25 = 600
    public static final int WIDTH = COLS * CELL;
    public static final int HEIGHT = ROWS * CELL;

    // Game data
    private final List<Point> snake = new ArrayList<>();
    private Direction dir = Direction.RIGHT;
    private GameState state = GameState.MENU;
    private boolean showGrid = true;
    private boolean wrapWalls = false;
    private boolean showHelpOverlay = true;
    private boolean obstaclesEnabled = false;

    private final List<Point> obstacles = new ArrayList<>();
    private Point apple;
    private Point goldenApple;
    private int applesEaten = 0;
    private int highScore = 0;

    // Timing
    private int speedMs = 120; // lower is faster
    private final Timer timer;

    // Visual themes
    private static class Theme {
        final Color bg1, bg2, snakeHead, snakeBody, apple, golden, grid, text;
        Theme(Color bg1, Color bg2, Color snakeHead, Color snakeBody, Color apple, Color golden, Color grid, Color text) {
            this.bg1 = bg1; this.bg2 = bg2; this.snakeHead = snakeHead; this.snakeBody = snakeBody;
            this.apple = apple; this.golden = golden; this.grid = grid; this.text = text;
        }
    }
    private final Theme[] themes = new Theme[]{
            new Theme(new Color(18,18,18), new Color(30,30,30),
                    new Color(0x7FDBFF), new Color(0x39CCCC),
                    new Color(0xFF4136), new Color(0xFFDC00),
                    new Color(60,60,60), new Color(230,230,230)),
            new Theme(new Color(0x0B486B), new Color(0x3B8686),
                    new Color(0x88CC00), new Color(0x66A61E),
                    new Color(0xFF6B6B), new Color(0xFFD93D),
                    new Color(255,255,255,40), Color.WHITE),
            new Theme(new Color(0x232526), new Color(0x414345),
                    new Color(0xFEC5E5), new Color(0xFD79A8),
                    new Color(0x55EFC4), new Color(0xFFEAA7),
                    new Color(255,255,255,35), new Color(240,240,240))
    };
    private int themeIndex = 0;

    // Persistence
    private final Preferences prefs = Preferences.userRoot().node("com.mycompany.snake.enhanced");

    // Status callback
    public interface StatusListener { void onStatus(String text); }
    private StatusListener statusListener = null;
    public void setStatusListener(StatusListener l) { this.statusListener = l; }
    private void updateStatus() {
        if (statusListener == null) return;
        String text = switch (state) {
            case MENU -> "Ready. Space to start. H for help.";
            case RUNNING -> String.format("Score: %d  High: %d  Speed: %dms  %s %s",
                    applesEaten, highScore, speedMs,
                    showGrid? "Grid:ON":"Grid:OFF",
                    wrapWalls? "Wrap:ON":"Wrap:OFF");
            case PAUSED -> "Paused. Press P to resume.";
            case GAME_OVER -> String.format("Game Over! Score: %d  High: %d. Press R to restart.",
                    applesEaten, highScore);
        };
        statusListener.onStatus(text);
    }

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        loadPrefs();
        timer = new Timer(speedMs, this);

        initGame();
        setupKeys();
        updateStatus();
    }

    private void loadPrefs() {
        highScore = prefs.getInt("highScore", 0);
        themeIndex = prefs.getInt("theme", 0) % themes.length;
        showGrid = prefs.getBoolean("grid", true);
        wrapWalls = prefs.getBoolean("wrapWalls", false);
        obstaclesEnabled = prefs.getBoolean("obstacles", false);
    }

    private void savePrefs() {
        prefs.putInt("highScore", highScore);
        prefs.putInt("theme", themeIndex);
        prefs.putBoolean("grid", showGrid);
        prefs.putBoolean("wrapWalls", wrapWalls);
        prefs.putBoolean("obstacles", obstaclesEnabled);
    }

    private void setupKeys() {
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT, KeyEvent.VK_A -> turn(Direction.LEFT);
                    case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> turn(Direction.RIGHT);
                    case KeyEvent.VK_UP, KeyEvent.VK_W -> turn(Direction.UP);
                    case KeyEvent.VK_DOWN, KeyEvent.VK_S -> turn(Direction.DOWN);
                    case KeyEvent.VK_SPACE -> start();
                    case KeyEvent.VK_P -> togglePause();
                    case KeyEvent.VK_R -> restart();
                    case KeyEvent.VK_G -> toggleGrid();
                    case KeyEvent.VK_T -> cycleTheme();
                    case KeyEvent.VK_W -> toggleWrapWalls();
                    case KeyEvent.VK_O -> toggleObstacles();
                    case KeyEvent.VK_H -> { showHelpOverlay = !showHelpOverlay; repaint(); }
                    case KeyEvent.VK_EQUALS, KeyEvent.VK_PLUS -> faster();
                    case KeyEvent.VK_MINUS -> slower();
                    default -> {}
                }
            }
        });
    }

    private void initGame() {
        snake.clear();
        int cx = COLS/2, cy = ROWS/2;
        snake.add(new Point(cx, cy));
        snake.add(new Point(cx-1, cy));
        snake.add(new Point(cx-2, cy));
        dir = Direction.RIGHT;
        applesEaten = 0;
        goldenApple = null;
        obstacles.clear();
        if (obstaclesEnabled) generateObstacles(12);
        spawnApple();
        state = GameState.MENU;
        timer.setDelay(speedMs);
        repaint();
    }

    private void generateObstacles(int count) {
        var r = ThreadLocalRandom.current();
        for (int i=0;i<count;i++) {
            Point p;
            int guard = 0;
            do {
                p = new Point(r.nextInt(COLS), r.nextInt(ROWS));
                guard++;
                if (guard > 1000) break;
            } while (collidesWithSnake(p) || (apple!=null && apple.equals(p)));
            obstacles.add(p);
        }
    }

    public void start() {
        if (state == GameState.RUNNING) return;
        if (state != GameState.MENU && state != GameState.GAME_OVER) return; // guard illegal transitions
        state = GameState.RUNNING;
        timer.start();
        updateStatus();
        repaint();
    }

    public void togglePause() {
        if (state == GameState.RUNNING) {
            state = GameState.PAUSED;
            timer.stop();
        } else if (state == GameState.PAUSED) {
            state = GameState.RUNNING;
            timer.start();
        }
        updateStatus();
        repaint();
    }

    public void restart() {
        initGame();
        start();
    }

    public void toggleGrid() { showGrid = !showGrid; savePrefs(); repaint(); updateStatus(); }
    public void toggleWrapWalls() { wrapWalls = !wrapWalls; savePrefs(); repaint(); updateStatus(); }
    public void toggleObstacles() { obstaclesEnabled = !obstaclesEnabled; initGame(); savePrefs(); repaint(); }
    public void cycleTheme() { themeIndex = (themeIndex + 1) % themes.length; savePrefs(); repaint(); }
    public void faster() { speedMs = Math.max(50, speedMs - 10); timer.setDelay(speedMs); updateStatus(); }
    public void slower() { speedMs = Math.min(300, speedMs + 10); timer.setDelay(speedMs); updateStatus(); }

    private void turn(Direction next) {
        // Prevent reversing into yourself
        if ((dir == Direction.LEFT && next == Direction.RIGHT) ||
            (dir == Direction.RIGHT && next == Direction.LEFT) ||
            (dir == Direction.UP && next == Direction.DOWN) ||
            (dir == Direction.DOWN && next == Direction.UP)) return;
        dir = next;
    }

    private void spawnApple() {
        var r = ThreadLocalRandom.current();
        Point p;
        do {
            p = new Point(r.nextInt(COLS), r.nextInt(ROWS));
        } while (collidesWithSnake(p) || obstacles.contains(p));
        apple = p;

        // Chance to spawn golden apple (roughly 1/8).
        if (goldenApple == null && r.nextInt(8) == 0) {
            Point g;
            do {
                g = new Point(r.nextInt(COLS), r.nextInt(ROWS));
            } while (g.equals(apple) || collidesWithSnake(g) || obstacles.contains(g));
            goldenApple = g;
        }
    }

    private boolean collidesWithSnake(Point p) {
        for (Point s : snake) if (s.equals(p)) return true;
        return false;
    }

    @Override public void actionPerformed(ActionEvent e) { tick(); }

    private void tick() {
        if (state != GameState.RUNNING) return;

        Point head = new Point(snake.get(0));
        switch (dir) {
            case LEFT -> head.x--;
            case RIGHT -> head.x++;
            case UP -> head.y--;
            case DOWN -> head.y++;
        }

        // Wrap vs non-wrap: handle wrapping BEFORE collision checks so head stays on-grid.
        if (wrapWalls) {
            if (head.x < 0) head.x = COLS-1;
            if (head.x >= COLS) head.x = 0;
            if (head.y < 0) head.y = ROWS-1;
            if (head.y >= ROWS) head.y = 0;
        }

        // Collisions (order matters: bounds -> self -> obstacles).
        if (!wrapWalls && (head.x < 0 || head.x >= COLS || head.y < 0 || head.y >= ROWS)) {
            gameOver(); return;
        }
        if (collidesWithSnake(head)) { gameOver(); return; }
        if (obstacles.contains(head)) { gameOver(); return; }

        // Move: add new head
        snake.add(0, head);

        // Eat?
        boolean grew = false;
        if (head.equals(apple)) {
            applesEaten++;
            grew = true;
            spawnApple();
            // Every 5 apples, slightly faster (down to a floor).
            if (applesEaten % 5 == 0) faster();
            // Using Toolkit.beep() for hits—good enough for demo; replace with a line-clip later.
            Toolkit.getDefaultToolkit().beep();
        } else if (goldenApple != null && head.equals(goldenApple)) {
            applesEaten += 5;
            grew = true;
            goldenApple = null;
            faster();
            Toolkit.getDefaultToolkit().beep();
        }

        // If not grown, remove tail
        if (!grew) snake.remove(snake.size()-1);

        repaint();
        updateStatus();
    }

    private void gameOver() {
        state = GameState.GAME_OVER;
        timer.stop();
        if (applesEaten > highScore) {
            highScore = applesEaten;
            savePrefs();
        }
        updateStatus();
        repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Theme th = themes[themeIndex];

        // Background gradient
        Paint old = g2.getPaint();
        g2.setPaint(new GradientPaint(0,0, th.bg1, getWidth(),getHeight(), th.bg2));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setPaint(old);

        // Grid
        if (showGrid) {
            g2.setColor(th.grid);
            for (int x=0; x<=WIDTH; x+=CELL) g2.drawLine(x,0,x,HEIGHT);
            for (int y=0; y<=HEIGHT; y+=CELL) g2.drawLine(0,y,WIDTH,y);
        }

        // Obstacles
        if (obstaclesEnabled) {
            g2.setColor(new Color(0,0,0,90));
            for (Point o : obstacles) {
                g2.fillRoundRect(o.x*CELL+2, o.y*CELL+2, CELL-4, CELL-4, 6,6);
            }
        }

        // Apple(s)
        if (apple != null) {
            g2.setColor(th.apple);
            int x = apple.x * CELL, y = apple.y * CELL;
            g2.fillOval(x+4, y+4, CELL-8, CELL-8);
        }
        if (goldenApple != null) {
            g2.setColor(th.golden);
            int x = goldenApple.x * CELL, y = goldenApple.y * CELL;
            g2.fillOval(x+6, y+6, CELL-12, CELL-12);
            g2.setColor(new Color(255,255,255,120));
            g2.drawOval(x+6, y+6, CELL-12, CELL-12);
        }

        // Snake
        for (int i=snake.size()-1; i>=0; i--) {
            Point p = snake.get(i);
            int x = p.x * CELL, y = p.y * CELL;
            if (i == 0) {
                g2.setColor(th.snakeHead);
                g2.fillRoundRect(x+2, y+2, CELL-4, CELL-4, 10,10);
                // Eyes
                g2.setColor(new Color(0,0,0,120));
                int eye = 4, off = 6;
                switch (dir) {
                    case LEFT -> { g2.fillOval(x+off, y+off, eye, eye); g2.fillOval(x+off, y+CELL-off-eye, eye, eye); }
                    case RIGHT -> { g2.fillOval(x+CELL-off-eye, y+off, eye, eye); g2.fillOval(x+CELL-off-eye, y+CELL-off-eye, eye, eye); }
                    case UP -> { g2.fillOval(x+off, y+off, eye, eye); g2.fillOval(x+CELL-off-eye, y+off, eye, eye); }
                    case DOWN -> { g2.fillOval(x+off, y+CELL-off-eye, eye, eye); g2.fillOval(x+CELL-off-eye, y+CELL-off-eye, eye, eye); }
                }
            } else {
                g2.setColor(th.snakeBody);
                g2.fillRoundRect(x+3, y+3, CELL-6, CELL-6, 8,8);
            }
        }

        // HUD
        drawHud(g2);

        g2.dispose();
    }

    private void drawHud(Graphics2D g2) {
        Theme th = themes[themeIndex];
        String s1 = "Score: " + applesEaten;
        String s2 = "High: " + highScore;
        g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
        g2.setColor(new Color(0,0,0,100));
        g2.fillRoundRect(8, 8, 160, 44, 10,10);
        g2.setColor(new Color(255,255,255,80));
        g2.drawRoundRect(8, 8, 160, 44, 10,10);
        g2.setColor(th.text);
        g2.drawString(s1, 16, 26);
        g2.drawString(s2, 16, 44);

        if (state == GameState.MENU) {
            drawCenterText(g2, "Press SPACE to start", 0);
        } else if (state == GameState.PAUSED) {
            drawCenterText(g2, "Paused — press P to resume", 0);
        } else if (state == GameState.GAME_OVER) {
            drawCenterText(g2, "Game Over — press R to restart", 0);
        }

        if (showHelpOverlay) {
            String[] lines = {
                    "Controls: ←↑→↓ / WASD",
                    "SPACE start  •  P pause/resume  •  R restart",
                    "G grid  •  T theme  •  W wrap walls  •  O obstacles",
                    "+/- speed  •  H hide/show help"
            };
            int w = 420, h = 74;
            int x = WIDTH - w - 10, y = 10;
            g2.setColor(new Color(0,0,0,100));
            g2.fillRoundRect(x, y, w, h, 10,10);
            g2.setColor(new Color(255,255,255,80));
            g2.drawRoundRect(x, y, w, h, 10,10);
            g2.setColor(th.text);
            int yy = y + 24;
            for (String ln : lines) {
                g2.drawString(ln, x+12, yy);
                yy += 18;
            }
        }
    }

    private void drawCenterText(Graphics2D g2, String text, int offsetY) {
        Theme th = themes[themeIndex];
        g2.setFont(getFont().deriveFont(Font.BOLD, 22f));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        int x = (WIDTH - tw)/2;
        int y = HEIGHT/2 + offsetY;
        g2.setColor(new Color(0,0,0,120));
        g2.fillRoundRect(x-12, y-22, tw+24, 32, 10,10);
        g2.setColor(new Color(255,255,255,80));
        g2.drawRoundRect(x-12, y-22, tw+24, 32, 10,10);
        g2.setColor(th.text);
        g2.drawString(text, x, y);
    }

    // TODO(controls): Consider configurable key bindings via Preferences.
    // TODO(perf): Benchmark Timer <= 60ms on HiDPI; clamp if repaint thrashes.
}
