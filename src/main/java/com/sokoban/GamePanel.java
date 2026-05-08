package com.sokoban;

import com.sokoban.objects.Grid;
import com.sokoban.objects.Tile;
import com.sokoban.util.GameState;
import com.sokoban.util.Theme;
import com.sokoban.util.SoundEngine;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Stack;

public class GamePanel extends JPanel {
    private static final int TILE_SIZE = 50;
    
    private int level = 1;
    private int width = 9;
    private int height = 6;
    private Grid grid;
    
    // Stats & Undo/Redo Fields
    private int moves = 0;
    private int pushes = 0;
    private long startTime = 0;
    private long elapsedMs = 0;
    private boolean gameStarted = false;
    
    private final Stack<GameState> undoStack = new Stack<>();
    private final Stack<GameState> redoStack = new Stack<>();
    private javax.swing.Timer repaintTimer;
    
    // Theme Engine
    private Theme currentTheme = Theme.NEON_CYBERPUNK;

    public GamePanel() {
        initGame();
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Check if key is one of the valid gameplay/undo/redo/options actions
                int keyCode = e.getKeyCode();
                boolean isMovementKey = keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W ||
                                        keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S ||
                                        keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A ||
                                        keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D;
                
                boolean isActionKey = isMovementKey || keyCode == KeyEvent.VK_R || 
                                      keyCode == KeyEvent.VK_U || keyCode == KeyEvent.VK_Y ||
                                      keyCode == KeyEvent.VK_T || keyCode == KeyEvent.VK_M;
                
                if (!isActionKey) return;

                // Capture pre-move state in case we move successfully
                GameState preMoveState = captureState();
                
                // Store old box coordinates before move to detect push
                int boxCount = grid.getBoxCount();
                int[][] oldBoxPositions = new int[boxCount][2];
                for (int i = 0; i < boxCount; i++) {
                    oldBoxPositions[i][0] = grid.getBoxes()[i].getX();
                    oldBoxPositions[i][1] = grid.getBoxes()[i].getY();
                }

                boolean moved = false;
                switch (keyCode) {
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_W:
                        moved = grid.getPlayer().moveUp();
                        break;
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_S:
                        moved = grid.getPlayer().moveDown();
                        break;
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_A:
                        moved = grid.getPlayer().moveLeft();
                        break;
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_D:
                        moved = grid.getPlayer().moveRight();
                        break;
                    case KeyEvent.VK_R:
                        if (e.isShiftDown()) {
                            grid.resetMap();
                        } else {
                            grid.reset();
                        }
                        resetStats();
                        moved = true;
                        break;
                    case KeyEvent.VK_U:
                        // Undo Action
                        if (!undoStack.isEmpty()) {
                            GameState popped = undoStack.pop();
                            redoStack.push(captureState());
                            restoreState(popped);
                            SoundEngine.playMove();
                        }
                        break;
                    case KeyEvent.VK_Y:
                        // Redo Action
                        if (!redoStack.isEmpty()) {
                            GameState popped = redoStack.pop();
                            undoStack.push(captureState());
                            restoreState(popped);
                            SoundEngine.playMove();
                        }
                        break;
                    case KeyEvent.VK_T:
                        // Toggle Theme
                        Theme[] themes = Theme.values();
                        int nextIdx = (currentTheme.ordinal() + 1) % themes.length;
                        currentTheme = themes[nextIdx];
                        repaint();
                        break;
                    case KeyEvent.VK_M:
                        // Mute/Unmute Sounds
                        SoundEngine.toggleSound();
                        repaint();
                        break;
                }
                
                if (moved && isMovementKey) {
                    if (!gameStarted) {
                        gameStarted = true;
                        startTime = System.currentTimeMillis() - elapsedMs;
                    }
                    undoStack.push(preMoveState);
                    redoStack.clear();
                    moves++;
                    
                    // Compare box positions before and after movement to detect pushes/snaps
                    boolean boxPushed = false;
                    boolean boxSnapped = false;
                    for (int i = 0; i < boxCount; i++) {
                        if (grid.getBoxes()[i].getX() != oldBoxPositions[i][0] ||
                            grid.getBoxes()[i].getY() != oldBoxPositions[i][1]) {
                            boxPushed = true;
                            if (grid.getBoxes()[i].onDestination()) {
                                boxSnapped = true;
                            }
                            break;
                        }
                    }
                    if (boxSnapped) {
                        SoundEngine.playDestinationSnap();
                    } else if (boxPushed) {
                        SoundEngine.playPush();
                    } else {
                        SoundEngine.playMove();
                    }
                    
                    grid.updateGrid();
                    if (grid.hasWon()) {
                        SoundEngine.playLevelClear();
                        levelUp();
                    }
                    repaint();
                } else if (!moved && isMovementKey) {
                    // Hit a wall or stuck crate
                    SoundEngine.playWallBump();
                }
            }
        });
        setFocusable(true);
        
        // Start live timer loop running every 20ms
        repaintTimer = new javax.swing.Timer(20, e -> {
            if (gameStarted && !grid.hasWon()) {
                elapsedMs = System.currentTimeMillis() - startTime;
                repaint();
            }
        });
        repaintTimer.start();
    }
    
    private void initGame() {
        grid = new Grid(width, height, level);
        setPreferredSize(new Dimension(13 * TILE_SIZE, 8 * TILE_SIZE + 80));
    }
    
    private void levelUp() {
        level++;
        if (width < 13) width += 2;
        if (height < 8) height += 1;
        grid = new Grid(width, height, level);
        resetStats();
    }
    
    private void resetStats() {
        moves = 0;
        pushes = 0;
        elapsedMs = 0;
        gameStarted = false;
        undoStack.clear();
        redoStack.clear();
    }
    
    private GameState captureState() {
        int pX = grid.getPlayer().getX();
        int pY = grid.getPlayer().getY();
        int boxCount = grid.getBoxCount();
        int[][] boxPositions = new int[boxCount][2];
        for (int i = 0; i < boxCount; i++) {
            boxPositions[i][0] = grid.getBoxes()[i].getX();
            boxPositions[i][1] = grid.getBoxes()[i].getY();
        }
        return new GameState(pX, pY, boxPositions, moves, pushes, elapsedMs);
    }
    
    private void restoreState(GameState state) {
        grid.getPlayer().setPosition(state.getPlayerX(), state.getPlayerY());
        int boxCount = grid.getBoxCount();
        for (int i = 0; i < boxCount; i++) {
            grid.getBoxes()[i].setPosition(state.getBoxPositions()[i][0], state.getBoxPositions()[i][1]);
        }
        this.moves = state.getMoves();
        this.pushes = state.getPushes();
        this.elapsedMs = state.getElapsedMs();
        if (this.moves == 0) {
            this.gameStarted = false;
        } else {
            this.gameStarted = true;
            this.startTime = System.currentTimeMillis() - this.elapsedMs;
        }
        grid.updateGrid();
        repaint();
    }
    
    private String formatTime(long ms) {
        long minutes = (ms / 60000) % 60;
        long seconds = (ms / 1000) % 60;
        long centiseconds = (ms / 10) % 100;
        return String.format("%02d:%02d.%02d", minutes, seconds, centiseconds);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Draw background using theme
        g2.setColor(currentTheme.getBg());
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        if (grid == null) return;
        
        Tile[][] tiles = grid.getGrid();
        
        // Calculate offsets to center the grid with 80px top padding for HUD
        int offsetX = (getWidth() - (width * TILE_SIZE)) / 2;
        int offsetY = ((getHeight() - 80) - (height * TILE_SIZE)) / 2 + 80;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int drawX = offsetX + x * TILE_SIZE;
                int drawY = offsetY + y * TILE_SIZE;
                
                int status = tiles[x][y].getStatus();
                int colorIdx = tiles[x][y].getColor();
                
                drawTile(g2, status, colorIdx, drawX, drawY, x, y);
            }
        }
        
        // Draw Glassmorphic HUD Bar
        g2.setColor(new Color(25, 25, 25, 200));
        g2.fillRoundRect(15, 12, getWidth() - 30, 56, 12, 12);
        g2.setColor(new Color(255, 255, 255, 30));
        g2.setStroke(new java.awt.BasicStroke(1.5f));
        g2.drawRoundRect(15, 12, getWidth() - 30, 56, 12, 12);
        g2.setStroke(new java.awt.BasicStroke(1.0f));

        // Draw HUD Text - Level Indicator (Left)
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.setColor(new Color(0, 255, 200));
        g2.drawString("LEVEL " + level, 30, 34);
        
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(180, 180, 180));
        g2.drawString("Theme: " + currentTheme.getName(), 30, 52);
        
        // Stats (Center)
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.setColor(Color.WHITE);
        String statsText = "Moves: " + moves + "  •  Pushes: " + pushes + "  •  Time: " + formatTime(elapsedMs);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(statsText, (getWidth() - fm.stringWidth(statsText)) / 2, 45);
        
        // Controls hint (Right)
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        String controlsText = "U: Undo | Y: Redo | R: Reset";
        int rightX = getWidth() - g2.getFontMetrics().stringWidth(controlsText) - 30;
        g2.drawString(controlsText, rightX, 34);
        
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(180, 180, 180));
        String soundStatus = SoundEngine.isEnabled() ? "Sound: ON (M: Mute)" : "Sound: MUTED (M: Unmute)";
        String themeStatus = "T: Toggle Theme | " + soundStatus;
        int rightXSub = getWidth() - g2.getFontMetrics().stringWidth(themeStatus) - 30;
        g2.drawString(themeStatus, rightXSub, 52);
    }
    
    private void drawTile(Graphics g, int status, int colorIdx, int drawX, int drawY, int tileX, int tileY) {
        Graphics2D g2 = (Graphics2D) g;
        switch (status) {
            case 0: // GROUND
                g2.setColor(currentTheme.getGround());
                g2.fillRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                g2.setColor(currentTheme.getGridLine());
                g2.drawRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                break;
            case 1: // WALL or Box on Destination
                boolean isBoundaryWall = tileX == 0 || tileX == width - 1 || tileY == 0 || tileY == height - 1;
                if (!isBoundaryWall) {
                    // It is a BOX on DESTINATION! Draw it with a glowing green/destination theme color!
                    g2.setColor(currentTheme.getDestination());
                    g2.fillRoundRect(drawX + 5, drawY + 5, TILE_SIZE - 10, TILE_SIZE - 10, 6, 6);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new java.awt.BasicStroke(2.0f));
                    g2.drawRoundRect(drawX + 5, drawY + 5, TILE_SIZE - 10, TILE_SIZE - 10, 6, 6);
                    // Inner checkmark or 'X'
                    g2.drawLine(drawX + 15, drawY + 15, drawX + TILE_SIZE - 15, drawY + TILE_SIZE - 15);
                    g2.drawLine(drawX + TILE_SIZE - 15, drawY + 15, drawX + 15, drawY + TILE_SIZE - 15);
                    g2.setStroke(new java.awt.BasicStroke(1.0f));
                } else {
                    // Boundary wall
                    g2.setColor(currentTheme.getWall());
                    g2.fillRoundRect(drawX + 2, drawY + 2, TILE_SIZE - 4, TILE_SIZE - 4, 8, 8);
                    g2.setColor(currentTheme.getWall().brighter());
                    g2.setStroke(new java.awt.BasicStroke(1.5f));
                    g2.drawRoundRect(drawX + 2, drawY + 2, TILE_SIZE - 4, TILE_SIZE - 4, 8, 8);
                    g2.setStroke(new java.awt.BasicStroke(1.0f));
                }
                break;
            case 2: // BOX
                g2.setColor(currentTheme.getBox());
                g2.fillRoundRect(drawX + 5, drawY + 5, TILE_SIZE - 10, TILE_SIZE - 10, 6, 6);
                g2.setColor(currentTheme.getBoxDetail());
                g2.setStroke(new java.awt.BasicStroke(2.0f));
                g2.drawRoundRect(drawX + 5, drawY + 5, TILE_SIZE - 10, TILE_SIZE - 10, 6, 6);
                // Cross planks
                g2.drawLine(drawX + 8, drawY + 8, drawX + TILE_SIZE - 8, drawY + TILE_SIZE - 8);
                g2.drawLine(drawX + TILE_SIZE - 8, drawY + 8, drawX + 8, drawY + TILE_SIZE - 8);
                g2.setStroke(new java.awt.BasicStroke(1.0f));
                break;
            case 3: // DESTINATION
                g2.setColor(currentTheme.getGround());
                g2.fillRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                g2.setColor(currentTheme.getGridLine());
                g2.drawRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                
                // Neon glowing target pad
                g2.setColor(currentTheme.getDestination());
                g2.setStroke(new java.awt.BasicStroke(2.0f));
                g2.drawOval(drawX + 10, drawY + 10, TILE_SIZE - 20, TILE_SIZE - 20);
                g2.fillOval(drawX + 20, drawY + 20, TILE_SIZE - 40, TILE_SIZE - 40);
                g2.setStroke(new java.awt.BasicStroke(1.0f));
                break;
            case 4: // PLAYER
                g2.setColor(currentTheme.getGround());
                g2.fillRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                g2.setColor(currentTheme.getGridLine());
                g2.drawRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                
                g2.setColor(currentTheme.getPlayer());
                g2.fillOval(drawX + 6, drawY + 6, TILE_SIZE - 12, TILE_SIZE - 12);
                g2.setColor(Color.WHITE);
                g2.drawOval(drawX + 6, drawY + 6, TILE_SIZE - 12, TILE_SIZE - 12);
                
                // Detailed eyes
                g2.setColor(Color.DARK_GRAY);
                g2.fillOval(drawX + 15, drawY + 16, 5, 5);
                g2.fillOval(drawX + 28, drawY + 16, 5, 5);
                break;
        }
    }
}
