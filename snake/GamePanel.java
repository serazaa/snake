/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.snake;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JPanel;
import javax.swing.*;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener {

    static final int screen_width = 600;
    static final int screen_height = 600;
    static final int unit_size = 25;
    static final int game_units = (screen_width * screen_height) / unit_size;
    static final int delay = 75;

    final int[] x = new int[game_units];
    final int[] y = new int[game_units];

    int bodyparts = 6;
    int applesEaten = 0;
    int appleX;
    int appleY;
    char direction = 'R';
    boolean running = false;

    Timer timer;
    Random random;

    JButton startButton;
    JButton replayButton;

    GamePanel() {
        random = new Random();
        setPreferredSize(new Dimension(screen_width, screen_height));
        setBackground(Color.green);
        setFocusable(true);
        addKeyListener(new MyKeyAdapter());

        setLayout(null);  // Allows absolute positioning for buttons

        // Start Button
        startButton = new JButton("Start");
        startButton.setBounds(screen_width / 2 - 60, screen_height / 2 - 30, 120, 50);
        startButton.setFont(new Font("Arial", Font.BOLD, 18));
        startButton.addActionListener(e -> {
            remove(startButton);
            startGame();
            requestFocusInWindow(); // Ensure key events work
        });
        add(startButton);
    }

    public void startGame() {
    // Reset state
    bodyparts = 6;
    applesEaten = 0;
    direction = 'R';
    running = true;

    // Clear snake position
    for (int i = 0; i < game_units; i++) {
        x[i] = 0;
        y[i] = 0;
    }

    // Place new apple
    newApple();

    // Remove replay button if it's visible
    if (replayButton != null && this.isAncestorOf(replayButton)) {
        remove(replayButton);
        replayButton = null; // Optional: allows a fresh button to be created
    }

    // Start or restart timer
    if (timer != null) {
        timer.stop();
    }
    timer = new Timer(delay, this);
    timer.start();

    repaint();
    requestFocusInWindow();
}


    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        if (running) {
            g.setColor(Color.red);
            g.fillOval(appleX, appleY, unit_size, unit_size);

            for (int i = 0; i < bodyparts; i++) {
                if (i == 0) {
                    g.setColor(new Color(255, 0, 255));
                } else {
                    g.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
                }
                g.fillRect(x[i], y[i], unit_size, unit_size);
            }

            g.setColor(Color.red);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            FontMetrics metrics = getFontMetrics(g.getFont());
            g.drawString("SCORE: " + applesEaten, (screen_width - metrics.stringWidth("SCORE: " + applesEaten)) / 2, g.getFont().getSize());
        } else if (timer != null) {
            gameOver(g);
        }
    }

    public void newApple() {
        appleX = random.nextInt(screen_width / unit_size) * unit_size;
        appleY = random.nextInt(screen_height / unit_size) * unit_size;
    }

    public void move() {
        for (int i = bodyparts; i > 0; i--) {
            x[i] = x[i - 1];
            y[i] = y[i - 1];
        }

        switch (direction) {
            case 'U' -> y[0] = y[0] - unit_size;
            case 'D' -> y[0] = y[0] + unit_size;
            case 'L' -> x[0] = x[0] - unit_size;
            case 'R' -> x[0] = x[0] + unit_size;
        }
    }

    public void checkApple() {
        if ((x[0] == appleX) && (y[0] == appleY)) {
            bodyparts++;
            applesEaten++;
            newApple();
        }
    }

    public void checkCollisions() {
        for (int i = bodyparts; i > 0; i--) {
            if ((x[0] == x[i]) && (y[0] == y[i])) {
                running = false;
            }
        }

        if (x[0] < 0 || x[0] >= screen_width || y[0] < 0 || y[0] >= screen_height) {
            running = false;
        }

        if (!running && timer != null) {
            timer.stop();
        }
    }

    public void gameOver(Graphics g) {
        g.setColor(Color.red);
        g.setFont(new Font("Arial", Font.BOLD, 75));
        FontMetrics metrics = getFontMetrics(g.getFont());
        g.drawString("GAME OVER", (screen_width - metrics.stringWidth("GAME OVER")) / 2, screen_height / 2 - 50);

        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("SCORE: " + applesEaten, (screen_width - metrics.stringWidth("SCORE: " + applesEaten)) / 2, screen_height / 2 + 10);

        // Show Replay Button
        if (replayButton == null) {
            replayButton = new JButton("Replay");
            replayButton.setBounds(screen_width / 2 - 60, screen_height / 2 + 60, 120, 50);
            replayButton.setFont(new Font("Arial", Font.BOLD, 18));
            replayButton.addActionListener(e -> {
                startGame();
                repaint();
                requestFocusInWindow(); // Regain focus for key input
            });
            add(replayButton);
            repaint(); // To show button immediately
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            move();
            checkApple();
            checkCollisions();
        }
        repaint();
    }

    public class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT -> {
                    if (direction != 'R') direction = 'L';
                }
                case KeyEvent.VK_RIGHT -> {
                    if (direction != 'L') direction = 'R';
                }
                case KeyEvent.VK_UP -> {
                    if (direction != 'D') direction = 'U';
                }
                case KeyEvent.VK_DOWN -> {
                    if (direction != 'U') direction = 'D';
                }
            }
        }
    }
}
