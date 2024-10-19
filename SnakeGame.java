import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class SnakeGame extends JPanel implements ActionListener {

    private final int B_WIDTH = 300;
    private final int B_HEIGHT = 300;
    private final int DOT_SIZE = 10;  // Size of each snake segment
    private final int ALL_DOTS = 900;  // Max possible dots (snake length)
    private final int RAND_POS = 29;   // Random position for food
    private int DELAY = 140;           // Snake speed (can be adjusted)

    private final int[] x = new int[ALL_DOTS];
    private final int[] y = new int[ALL_DOTS];

    private int dots;  // Current length of the snake
    private int apple_x;
    private int apple_y;
    private int score; // Score counter

    private boolean leftDirection = false;
    private boolean rightDirection = true;
    private boolean upDirection = false;
    private boolean downDirection = false;
    private boolean inGame = false;  // Initially false, so speed selection can happen

    private boolean speedSelection = true;  // Speed selection menu active
    private Timer timer;
    private Instant startTime; // To track the start time

    public SnakeGame() {
        initBoard();
    }

    private void initBoard() {
        addKeyListener(new TAdapter());
        setBackground(Color.black);
        setFocusable(true);
        setPreferredSize(new Dimension(B_WIDTH, B_HEIGHT));
    }

    private void initGame() {
        dots = 3;
        score = 0;
        startTime = Instant.now();  // Record the start time

        for (int z = 0; z < dots; z++) {
            x[z] = 50 - z * 10;
            y[z] = 50;
        }

        locateApple();

        timer = new Timer(DELAY, this);
        timer.start();
        inGame = true;  // Start the game
        speedSelection = false;  // Disable speed selection
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (speedSelection) {
            showSpeedSelection(g);  // Show the speed selection menu
        } else if (inGame) {
            drawApple(g);
            drawSnake(g);
            drawScoreAndTime(g);
            Toolkit.getDefaultToolkit().sync();
        } else {
            gameOver(g);
        }
    }

    private void showSpeedSelection(Graphics g) {
        String msg = "Select Speed";
        String slowMsg = "1: Slow";
        String mediumMsg = "2: Medium";
        String fastMsg = "3: Fast";
        
        Font small = new Font("Helvetica", Font.BOLD, 14);
        FontMetrics metr = getFontMetrics(small);

        g.setColor(Color.white);
        g.setFont(small);
        g.drawString(msg, (B_WIDTH - metr.stringWidth(msg)) / 2, B_HEIGHT / 2 - 40);
        g.drawString(slowMsg, (B_WIDTH - metr.stringWidth(slowMsg)) / 2, B_HEIGHT / 2);
        g.drawString(mediumMsg, (B_WIDTH - metr.stringWidth(mediumMsg)) / 2, B_HEIGHT / 2 + 20);
        g.drawString(fastMsg, (B_WIDTH - metr.stringWidth(fastMsg)) / 2, B_HEIGHT / 2 + 40);
    }

    private void drawApple(Graphics g) {
        g.setColor(Color.RED);
        g.fillOval(apple_x, apple_y, DOT_SIZE, DOT_SIZE);
    }

    private void drawSnake(Graphics g) {
        for (int z = 0; z < dots; z++) {
            if (z == 0) {
                g.setColor(new Color(184, 134, 11));  // Darker yellow for snake head
                g.fillRect(x[z], y[z], DOT_SIZE, DOT_SIZE);
            } else {
                g.setColor(new Color(255, 255, 102));  // Lighter yellow for snake body
                g.fillRect(x[z], y[z], DOT_SIZE, DOT_SIZE);
            }
        }
    }

    private void drawScoreAndTime(Graphics g) {
        String scoreText = "Score: " + score;
        Font small = new Font("Helvetica", Font.BOLD, 14);
        g.setColor(Color.white);
        g.setFont(small);
        g.drawString(scoreText, 10, 20);

        Duration timeElapsed = Duration.between(startTime, Instant.now());
        long seconds = timeElapsed.getSeconds();
        String timeText = String.format("Time: %02d:%02d", seconds / 60, seconds % 60);
        g.drawString(timeText, B_WIDTH - 100, 20);
    }

    private void gameOver(Graphics g) {
        String msg = "Game Over!";
        String scoreMsg = "Score: " + score;
        String timeMsg = "Time: " + getElapsedTimeString();
        String restartMsg = "Press 'R' to Restart";

        Font small = new Font("Helvetica", Font.BOLD, 14);
        FontMetrics metr = getFontMetrics(small);

        g.setColor(Color.white);
        g.setFont(small);
        g.drawString(msg, (B_WIDTH - metr.stringWidth(msg)) / 2, B_HEIGHT / 2);
        g.drawString(scoreMsg, (B_WIDTH - metr.stringWidth(scoreMsg)) / 2, B_HEIGHT / 2 + 20);
        g.drawString(timeMsg, (B_WIDTH - metr.stringWidth(timeMsg)) / 2, B_HEIGHT / 2 + 40);
        g.drawString(restartMsg, (B_WIDTH - metr.stringWidth(restartMsg)) / 2, B_HEIGHT / 2 + 60);
    }

    private String getElapsedTimeString() {
        Duration timeElapsed = Duration.between(startTime, Instant.now());
        long minutes = timeElapsed.toMinutes();
        long seconds = timeElapsed.getSeconds() % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void checkApple() {
        if ((x[0] == apple_x) && (y[0] == apple_y)) {
            dots++;
            score++;
            locateApple();
            playSound("eat.wav");  // Play sound when snake eats an apple
        }
    }

    private void move() {
        for (int z = dots; z > 0; z--) {
            x[z] = x[(z - 1)];
            y[z] = y[(z - 1)];
        }

        if (leftDirection) {
            x[0] -= DOT_SIZE;
        }

        if (rightDirection) {
            x[0] += DOT_SIZE;
        }

        if (upDirection) {
            y[0] -= DOT_SIZE;
        }

        if (downDirection) {
            y[0] += DOT_SIZE;
        }

        wrapAroundBorders();
    }

    private void wrapAroundBorders() {
        if (x[0] >= B_WIDTH) {
            x[0] = 0;
        }
        if (x[0] < 0) {
            x[0] = B_WIDTH;
        }
        if (y[0] >= B_HEIGHT) {
            y[0] = 0;
        }
        if (y[0] < 0) {
            y[0] = B_HEIGHT;
        }
    }

    private void checkCollision() {
        for (int z = dots; z > 0; z--) {
            if ((z > 4) && (x[0] == x[z]) && (y[0] == y[z])) {
                inGame = false;
            }
        }

        if (!inGame) {
            timer.stop();
        }
    }

    private void locateApple() {
        int r = (int) (Math.random() * RAND_POS);
        apple_x = r * DOT_SIZE;

        r = (int) (Math.random() * RAND_POS);
        apple_y = r * DOT_SIZE;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (inGame) {
            checkApple();
            checkCollision();
            move();
        }
        repaint();
    }

    // Method to play the sound when an apple is eaten
    private void playSound(String soundFile) {
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File(soundFile).getAbsoluteFile());
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) {
            System.out.println("Error playing sound: " + e.getMessage());
        }
    }

    private class TAdapter extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if (speedSelection) {
                // Speed selection logic
                if (key == KeyEvent.VK_1) {
                    DELAY = 200;  // Slow speed
                    initGame();
                } else if (key == KeyEvent.VK_2) {
                    DELAY = 140;  // Medium speed
                    initGame();
                } else if (key == KeyEvent.VK_3) {
                    DELAY = 80;   // Fast speed
                    initGame();
                }
            } else if (inGame) {
                // Movement control logic
                if ((key == KeyEvent.VK_LEFT) && (!rightDirection)) {
                    leftDirection = true;
                    upDirection = false;
                    downDirection = false;
                }

                if ((key == KeyEvent.VK_RIGHT) && (!leftDirection)) {
                    rightDirection = true;
                    upDirection = false;
                    downDirection = false;
                }

                if ((key == KeyEvent.VK_UP) && (!downDirection)) {
                    upDirection = true;
                    rightDirection = false;
                    leftDirection = false;
                }

                if ((key == KeyEvent.VK_DOWN) && (!upDirection)) {
                    downDirection = true;
                    rightDirection = false;
                    leftDirection = false;
                }
            } else if (!inGame) {
                // Restart the game if 'R' is pressed
                if (key == KeyEvent.VK_R) {
                    inGame = false;
                    speedSelection = true;
                    repaint();
                }
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Snake Game");
        SnakeGame game = new SnakeGame();
        frame.add(game);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
