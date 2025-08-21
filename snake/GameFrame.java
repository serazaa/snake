package com.mycompany.snake;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * GameFrame
 * Hosts the main window, menu bar, and a compact status bar.
 * Design notes:
 *  - Keep window concerns here (menus/status), push game logic into GamePanel.
 *  - Use root pane key bindings so global shortcuts work even if focus shifts.
 */
public class GameFrame extends JFrame {
    private final GamePanel gamePanel;
    private final JLabel status;

    public GameFrame() {
        super("Snake — Enhanced");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Content
        gamePanel = new GamePanel();
        status = new JLabel("Ready. Press SPACE to start. H for help.");
        status.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        setLayout(new BorderLayout());
        add(gamePanel, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        setJMenuBar(createMenuBar());

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // Sync status text with panel
        gamePanel.setStatusListener(text -> SwingUtilities.invokeLater(() -> status.setText(text)));

        // Global key shortcuts (menu equivalents)
        getRootPane().registerKeyboardAction(e -> gamePanel.togglePause(),
                KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> gamePanel.restart(),
                KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> gamePanel.toggleGrid(),
                KeyStroke.getKeyStroke(KeyEvent.VK_G, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> gamePanel.cycleTheme(),
                KeyStroke.getKeyStroke(KeyEvent.VK_T, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> gamePanel.toggleWrapWalls(),
                KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu game = new JMenu("Game");
        JMenuItem start = new JMenuItem("Start (Space)");
        start.addActionListener(e -> gamePanel.start());
        JMenuItem pause = new JMenuItem("Pause/Resume (P)");
        pause.addActionListener(e -> gamePanel.togglePause());
        JMenuItem restart = new JMenuItem("Restart (R)");
        restart.addActionListener(e -> gamePanel.restart());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> System.exit(0));
        game.add(start);
        game.add(pause);
        game.add(restart);
        game.addSeparator();
        game.add(exit);

        JMenu view = new JMenu("View");
        JCheckBoxMenuItem grid = new JCheckBoxMenuItem("Grid", true);
        grid.addActionListener(e -> gamePanel.toggleGrid());
        JMenuItem theme = new JMenuItem("Cycle Theme (T)");
        theme.addActionListener(e -> gamePanel.cycleTheme());
        JCheckBoxMenuItem wrap = new JCheckBoxMenuItem("Wrap Walls", false);
        wrap.addActionListener(e -> gamePanel.toggleWrapWalls());
        view.add(grid);
        view.add(theme);
        view.add(wrap);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Snake — Enhanced\n" +
                "• Arrow keys / WASD to move\n" +
                "• Space to start, P to pause, R to restart\n" +
                "• G grid, T theme, W wrap walls, O obstacles\n" +
                "• +/- speed, H toggle help overlay\n\n" +
                "Created by you ✨", "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);

        bar.add(game);
        bar.add(view);
        bar.add(help);
        return bar;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameFrame::new);
    }
}