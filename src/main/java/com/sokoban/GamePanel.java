package com.sokoban;

import com.sokoban.objects.Grid;
import com.sokoban.objects.Tile;
import com.sokoban.util.GameState;
import com.sokoban.util.Theme;
import com.sokoban.util.SoundEngine;
import com.sokoban.util.Particle;

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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Random;
import java.util.prefs.Preferences;

public class GamePanel extends JPanel {
    private static final int TILE_SIZE = 50;
    
    // Phase 5 & 6 Screen Modes
    private static final int MAIN_MENU = 0;
    private static final int PLAYING = 1;
    private static final int LEVEL_SELECTOR = 2;
    private static final int PAUSED = 3;
    private static final int LEVEL_EDITOR = 4;
    private int screenMode = MAIN_MENU;
    
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
    
    // Phase 3 Particle System
    private final List<Particle> particles = new CopyOnWriteArrayList<>();
    private final Random rand = new Random();
    
    // Phase 4 Animated visual coordinates
    private double visualPlayerX;
    private double visualPlayerY;
    private double[] visualBoxXs;
    private double[] visualBoxYs;
    
    // Phase 5 Preferences Save System
    private final Preferences prefs = Preferences.userNodeForPackage(GamePanel.class);
    
    // Phase 6 Level Editor Fields
    private int editorWidth = 9;
    private int editorHeight = 6;
    private int[][] editorGrid;
    private int selectedBrush = 1; // Default to Wall Brush

    public GamePanel() {
        initGame();
        initEditor();
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                
                // Handle different screen mode inputs
                if (screenMode == MAIN_MENU) {
                    if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_SPACE) {
                        screenMode = PLAYING;
                        initGame();
                        SoundEngine.playMove();
                        repaint();
                    } else if (keyCode == KeyEvent.VK_L) {
                        screenMode = LEVEL_SELECTOR;
                        SoundEngine.playMove();
                        repaint();
                    } else if (keyCode == KeyEvent.VK_E) {
                        screenMode = LEVEL_EDITOR;
                        initEditor();
                        SoundEngine.playMove();
                        repaint();
                    }
                    return;
                } else if (screenMode == LEVEL_SELECTOR) {
                    if (keyCode == KeyEvent.VK_ESCAPE) {
                        screenMode = MAIN_MENU;
                        SoundEngine.playMove();
                        repaint();
                    }
                    return;
                } else if (screenMode == LEVEL_EDITOR) {
                    if (keyCode == KeyEvent.VK_ESCAPE) {
                        screenMode = MAIN_MENU;
                        SoundEngine.playMove();
                        repaint();
                    }
                    return;
                } else if (screenMode == PAUSED) {
                    if (keyCode == KeyEvent.VK_ESCAPE) {
                        screenMode = PLAYING;
                        startTime = System.currentTimeMillis() - elapsedMs;
                        SoundEngine.playMove();
                        repaint();
                    }
                    return;
                }
                
                // Inside PLAYING Mode
                if (keyCode == KeyEvent.VK_ESCAPE) {
                    screenMode = PAUSED;
                    SoundEngine.playWallBump();
                    repaint();
                    return;
                }

                boolean isMovementKey = keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W ||
                                        keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S ||
                                        keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A ||
                                        keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D;
                
                boolean isActionKey = isMovementKey || keyCode == KeyEvent.VK_R || 
                                      keyCode == KeyEvent.VK_U || keyCode == KeyEvent.VK_Y ||
                                      keyCode == KeyEvent.VK_T || keyCode == KeyEvent.VK_M;
                
                if (!isActionKey) return;

                // Calculate current grid offsets to find particle pixel locations
                int offsetX = (getWidth() - (width * TILE_SIZE)) / 2;
                int offsetY = ((getHeight() - 80) - (height * TILE_SIZE)) / 2 + 80;

                // Capture pre-move state in case we move successfully
                GameState preMoveState = captureState();
                
                // Store old box coordinates before move to detect push/snap
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
                    
                    // Emit soft dust trailing behind player's old position
                    int oldPX = offsetX + preMoveState.getPlayerX() * TILE_SIZE;
                    int oldPY = offsetY + preMoveState.getPlayerY() * TILE_SIZE;
                    emitPlayerDust(oldPX, oldPY);
                    
                    // Compare box positions before and after movement to detect pushes/snaps
                    boolean boxPushed = false;
                    boolean boxSnapped = false;
                    int snapX = 0, snapY = 0;
                    for (int i = 0; i < boxCount; i++) {
                        if (grid.getBoxes()[i].getX() != oldBoxPositions[i][0] ||
                            grid.getBoxes()[i].getY() != oldBoxPositions[i][1]) {
                            boxPushed = true;
                            if (grid.getBoxes()[i].onDestination()) {
                                boxSnapped = true;
                                snapX = grid.getBoxes()[i].getX();
                                snapY = grid.getBoxes()[i].getY();
                            }
                            break;
                        }
                    }
                    if (boxSnapped) {
                        SoundEngine.playDestinationSnap();
                        emitGoalSparkles(offsetX + snapX * TILE_SIZE, offsetY + snapY * TILE_SIZE);
                    } else if (boxPushed) {
                        SoundEngine.playPush();
                    } else {
                        SoundEngine.playMove();
                    }
                    
                    grid.updateGrid();
                    if (grid.hasWon()) {
                        SoundEngine.playLevelClear();
                        emitConfetti();
                        levelUp();
                    }
                    repaint();
                } else if (!moved && isMovementKey) {
                    // Hit a wall or stuck crate
                    SoundEngine.playWallBump();
                }
            }
        });
        
        // Add Mouse clicks for Menus & Level Grid selection
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();
                
                if (screenMode == MAIN_MENU) {
                    int cx = getWidth() / 2;
                    if (mx >= cx - 100 && mx <= cx + 100) {
                        if (my >= 142 && my <= 178) {
                            screenMode = PLAYING;
                            initGame();
                            SoundEngine.playMove();
                        } else if (my >= 192 && my <= 228) {
                            screenMode = LEVEL_SELECTOR;
                            SoundEngine.playMove();
                        } else if (my >= 242 && my <= 278) {
                            screenMode = LEVEL_EDITOR;
                            initEditor();
                            SoundEngine.playMove();
                        } else if (my >= 292 && my <= 328) {
                            System.exit(0);
                        }
                    }
                } else if (screenMode == LEVEL_SELECTOR) {
                    int highLevel = prefs.getInt("highLevelReached", 1);
                    int startX = (getWidth() - (5 * 60 + 4 * 15)) / 2;
                    int startY = 120;
                    for (int i = 0; i < 15; i++) {
                        int row = i / 5;
                        int col = i % 5;
                        int bx = startX + col * 75;
                        int by = startY + row * 75;
                        if (mx >= bx && mx <= bx + 60 && my >= by && my <= by + 60) {
                            int selectedLevel = i + 1;
                            if (selectedLevel <= highLevel) {
                                level = selectedLevel;
                                screenMode = PLAYING;
                                initGame();
                                SoundEngine.playLevelClear();
                            } else {
                                SoundEngine.playWallBump();
                            }
                            break;
                        }
                    }
                    
                    int cx = getWidth() / 2;
                    if (mx >= cx - 80 && mx <= cx + 80 && my >= 360 && my <= 396) {
                        screenMode = MAIN_MENU;
                        SoundEngine.playMove();
                    }
                } else if (screenMode == PAUSED) {
                    int cx = getWidth() / 2;
                    if (mx >= cx - 90 && mx <= cx + 90) {
                        if (my >= 160 && my <= 200) {
                            screenMode = PLAYING;
                            startTime = System.currentTimeMillis() - elapsedMs;
                            SoundEngine.playMove();
                        } else if (my >= 220 && my <= 260) {
                            screenMode = PLAYING;
                            grid.reset();
                            resetStats();
                            SoundEngine.playMove();
                        } else if (my >= 280 && my <= 320) {
                            screenMode = MAIN_MENU;
                            resetStats();
                            SoundEngine.playMove();
                        }
                    }
                } else if (screenMode == LEVEL_EDITOR) {
                    int offsetX = (getWidth() - (editorWidth * TILE_SIZE)) / 2;
                    int offsetY = 90;
                    
                    if (mx >= offsetX && mx <= offsetX + editorWidth * TILE_SIZE &&
                        my >= offsetY && my <= offsetY + editorHeight * TILE_SIZE) {
                        int tx = (mx - offsetX) / TILE_SIZE;
                        int ty = (my - offsetY) / TILE_SIZE;
                        
                        if (tx >= 0 && tx < editorWidth && ty >= 0 && ty < editorHeight) {
                            editorGrid[tx][ty] = selectedBrush;
                            SoundEngine.playMove();
                        }
                    }
                    
                    // Brush buttons selections
                    int paletteY = offsetY + editorHeight * TILE_SIZE + 20;
                    for (int i = 0; i < 5; i++) {
                        int bx = 130 + i * 80;
                        if (mx >= bx && mx <= bx + 70 && my >= paletteY && my <= paletteY + 32) {
                            selectedBrush = i;
                            SoundEngine.playMove();
                            break;
                        }
                    }
                    
                    // Action Buttons
                    int actionY = paletteY + 50;
                    int cx = getWidth() / 2;
                    if (mx >= cx - 160 && mx <= mx - 60 && my >= actionY - 17 && my <= actionY + 17) {
                        // Play level: Validate first
                        if (validateEditorMap(false)) {
                            loadEditorMapIntoGame();
                            screenMode = PLAYING;
                            SoundEngine.playLevelClear();
                        } else {
                            SoundEngine.playWallBump();
                        }
                    } else if (mx >= cx - 50 && mx <= cx + 50 && my >= actionY - 17 && my <= actionY + 17) {
                        // Validate level
                        validateEditorMap(true);
                    } else if (mx >= cx + 55 && mx <= cx + 165 && my >= actionY - 17 && my <= actionY + 17) {
                        // Back to Menu
                        screenMode = MAIN_MENU;
                        SoundEngine.playMove();
                    }
                }
                repaint();
            }
        });
        
        // Add Mouse dragged drawing for the Level Editor
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (screenMode == LEVEL_EDITOR) {
                    int mx = e.getX();
                    int my = e.getY();
                    int offsetX = (getWidth() - (editorWidth * TILE_SIZE)) / 2;
                    int offsetY = 90;
                    
                    if (mx >= offsetX && mx <= offsetX + editorWidth * TILE_SIZE &&
                        my >= offsetY && my <= offsetY + editorHeight * TILE_SIZE) {
                        int tx = (mx - offsetX) / TILE_SIZE;
                        int ty = (my - offsetY) / TILE_SIZE;
                        
                        if (tx >= 0 && tx < editorWidth && ty >= 0 && ty < editorHeight) {
                            if (editorGrid[tx][ty] != selectedBrush) {
                                editorGrid[tx][ty] = selectedBrush;
                                SoundEngine.playMove();
                                repaint();
                            }
                        }
                    }
                }
            }
        });
        setFocusable(true);
        
        // Start live timer and animation update loop running every 20ms
        repaintTimer = new javax.swing.Timer(20, e -> {
            boolean needsRepaint = false;
            
            if (screenMode == PLAYING && gameStarted && !grid.hasWon()) {
                elapsedMs = System.currentTimeMillis() - startTime;
                needsRepaint = true;
            }
            
            // Check and update visual position animations
            double lerpFactor = 0.25;
            if (grid != null) {
                // Slide Player
                int targetPX = grid.getPlayer().getX();
                int targetPY = grid.getPlayer().getY();
                if (Math.abs(visualPlayerX - targetPX) > 0.01) {
                    visualPlayerX += (targetPX - visualPlayerX) * lerpFactor;
                    needsRepaint = true;
                } else {
                    visualPlayerX = targetPX;
                }
                if (Math.abs(visualPlayerY - targetPY) > 0.01) {
                    visualPlayerY += (targetPY - visualPlayerY) * lerpFactor;
                    needsRepaint = true;
                } else {
                    visualPlayerY = targetPY;
                }
                
                // Slide Boxes
                int count = grid.getBoxCount();
                if (visualBoxXs != null && visualBoxYs != null && visualBoxXs.length == count) {
                    for (int i = 0; i < count; i++) {
                        int targetBX = grid.getBoxes()[i].getX();
                        int targetBY = grid.getBoxes()[i].getY();
                        if (Math.abs(visualBoxXs[i] - targetBX) > 0.01) {
                            visualBoxXs[i] += (targetBX - visualBoxXs[i]) * lerpFactor;
                            needsRepaint = true;
                        } else {
                            visualBoxXs[i] = targetBX;
                        }
                        if (Math.abs(visualBoxYs[i] - targetBY) > 0.01) {
                            visualBoxYs[i] += (targetBY - visualBoxYs[i]) * lerpFactor;
                            needsRepaint = true;
                        } else {
                            visualBoxYs[i] = targetBY;
                        }
                    }
                }
            }

            if (!particles.isEmpty()) {
                for (Particle p : particles) {
                    if (!p.update()) {
                        particles.remove(p);
                    }
                }
                needsRepaint = true;
            }
            
            if (needsRepaint) {
                repaint();
            }
        });
        repaintTimer.start();
    }
    
    private void initGame() {
        grid = new Grid(width, height, level);
        setPreferredSize(new Dimension(13 * TILE_SIZE, 8 * TILE_SIZE + 80));
        syncVisualPositions();
    }
    
    private void initEditor() {
        editorGrid = new int[editorWidth][editorHeight];
        for (int x = 0; x < editorWidth; x++) {
            for (int y = 0; y < editorHeight; y++) {
                if (x == 0 || x == editorWidth - 1 || y == 0 || y == editorHeight - 1) {
                    editorGrid[x][y] = 1; // Wall
                } else {
                    editorGrid[x][y] = 0; // Ground
                }
            }
        }
    }
    
    private void loadEditorMapIntoGame() {
        width = editorWidth;
        height = editorHeight;
        grid = new Grid(editorGrid, width, height);
        resetStats();
        syncVisualPositions();
    }
    
    private boolean validateEditorMap(boolean showFeedback) {
        int playerCount = 0;
        int boxCount = 0;
        int targetCount = 0;
        
        for (int x = 0; x < editorWidth; x++) {
            for (int y = 0; y < editorHeight; y++) {
                if (editorGrid[x][y] == 2) boxCount++;
                else if (editorGrid[x][y] == 3) targetCount++;
                else if (editorGrid[x][y] == 4) playerCount++;
            }
        }
        
        boolean valid = playerCount == 1 && boxCount > 0 && boxCount == targetCount;
        if (showFeedback) {
            if (valid) {
                SoundEngine.playLevelClear();
            } else {
                SoundEngine.playWallBump();
            }
        }
        return valid;
    }
    
    private void levelUp() {
        // Local Save System: Unlock next level
        int highLevel = prefs.getInt("highLevelReached", 1);
        if (level >= highLevel) {
            prefs.putInt("highLevelReached", level + 1);
        }
        
        // Save current level records
        int bestMoves = prefs.getInt("bestMoves_" + level, 9999);
        if (moves < bestMoves) prefs.putInt("bestMoves_" + level, moves);
        
        int bestPushes = prefs.getInt("bestPushes_" + level, 9999);
        if (pushes < bestPushes) prefs.putInt("bestPushes_" + level, pushes);
        
        long bestTime = prefs.getLong("bestTime_" + level, 9999999);
        if (elapsedMs < bestTime) prefs.putLong("bestTime_" + level, elapsedMs);

        level++;
        if (width < 13) width += 2;
        if (height < 8) height += 1;
        grid = new Grid(width, height, level);
        resetStats();
        syncVisualPositions();
    }
    
    private void resetStats() {
        moves = 0;
        pushes = 0;
        elapsedMs = 0;
        gameStarted = false;
        undoStack.clear();
        redoStack.clear();
        particles.clear();
    }
    
    private void syncVisualPositions() {
        if (grid == null) return;
        visualPlayerX = grid.getPlayer().getX();
        visualPlayerY = grid.getPlayer().getY();
        
        int count = grid.getBoxCount();
        visualBoxXs = new double[count];
        visualBoxYs = new double[count];
        for (int i = 0; i < count; i++) {
            visualBoxXs[i] = grid.getBoxes()[i].getX();
            visualBoxYs[i] = grid.getBoxes()[i].getY();
        }
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
        syncVisualPositions(); // instantly snap visual positions on undo/redo actions
        repaint();
    }
    
    // Phase 3 Particle Emitters
    private void emitGoalSparkles(int pixelX, int pixelY) {
        int centerX = pixelX + TILE_SIZE / 2;
        int centerY = pixelY + TILE_SIZE / 2;
        Color sparkColor = currentTheme.getDestination();
        for (int i = 0; i < 25; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double speed = 0.8 + rand.nextDouble() * 2.5;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed - 0.8; // slightly upward
            int size = 5 + rand.nextInt(5);
            double decay = 0.015 + rand.nextDouble() * 0.02;
            particles.add(new Particle(centerX, centerY, vx, vy, sparkColor, size, decay));
        }
    }

    private void emitPlayerDust(int pixelX, int pixelY) {
        int centerX = pixelX + TILE_SIZE / 2;
        int centerY = pixelY + TILE_SIZE / 2 + 15; // trail near feet
        Color dustColor = new Color(180, 180, 180, 90);
        for (int i = 0; i < 6; i++) {
            double vx = (rand.nextDouble() - 0.5) * 1.5;
            double vy = -0.3 - rand.nextDouble() * 0.8;
            int size = 6 + rand.nextInt(5);
            double decay = 0.025 + rand.nextDouble() * 0.025;
            particles.add(new Particle(centerX, centerY, vx, vy, dustColor, size, decay));
        }
    }

    private void emitConfetti() {
        Color[] colors = {Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.MAGENTA, Color.ORANGE};
        for (int i = 0; i < 80; i++) {
            int x = rand.nextInt(getWidth());
            int y = rand.nextInt(60) - 60; // start above panel
            double vx = (rand.nextDouble() - 0.5) * 2.0;
            double vy = 1.2 + rand.nextDouble() * 2.5;
            Color color = colors[rand.nextInt(colors.length)];
            int size = 5 + rand.nextInt(6);
            double decay = 0.005 + rand.nextDouble() * 0.008;
            particles.add(new Particle(x, y, vx, vy, color, size, decay));
        }
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
        
        if (screenMode == MAIN_MENU) {
            drawMainMenu(g2);
        } else if (screenMode == LEVEL_SELECTOR) {
            drawLevelSelector(g2);
        } else if (screenMode == LEVEL_EDITOR) {
            drawLevelEditor(g2);
        } else if (screenMode == PLAYING) {
            drawGamePlay(g2);
        } else if (screenMode == PAUSED) {
            drawGamePlay(g2);
            drawPauseOverlay(g2);
        }
    }
    
    private void drawMainMenu(Graphics2D g2) {
        g2.setColor(currentTheme.getBg());
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        // Animated sparkles in the menu background
        for (Particle p : particles) {
            p.draw(g2);
        }
        
        // Title
        g2.setFont(new Font("Segoe UI", Font.BOLD, 36));
        g2.setColor(currentTheme.getDestination());
        String title = "S O K O B A N";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, 90);
        
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(new Color(150, 150, 150));
        String sub = "The Premium 3D Puzzle Experience";
        FontMetrics fmSub = g2.getFontMetrics();
        g2.drawString(sub, (getWidth() - fmSub.stringWidth(sub)) / 2, 115);
        
        // Buttons
        int cx = getWidth() / 2;
        drawButton(g2, "START GAME", cx, 160, 200, 36, currentTheme.getDestination());
        drawButton(g2, "LEVEL SELECTOR", cx, 210, 200, 36, currentTheme.getPlayer());
        drawButton(g2, "LEVEL EDITOR", cx, 260, 200, 36, currentTheme.getDestination());
        drawButton(g2, "EXIT", cx, 310, 200, 36, currentTheme.getWall());
    }

    private void drawLevelSelector(Graphics2D g2) {
        g2.setColor(currentTheme.getBg());
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
        g2.setColor(Color.WHITE);
        String title = "SELECT LEVEL";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, 65);
        
        int highLevel = prefs.getInt("highLevelReached", 1);
        int startX = (getWidth() - (5 * 60 + 4 * 15)) / 2;
        int startY = 120;
        
        for (int i = 0; i < 15; i++) {
            int row = i / 5;
            int col = i % 5;
            int bx = startX + col * 75;
            int by = startY + row * 75;
            
            boolean unlocked = (i + 1) <= highLevel;
            if (unlocked) {
                g2.setColor(new Color(35, 35, 45));
                g2.fillRoundRect(bx, by, 60, 60, 10, 10);
                g2.setColor(currentTheme.getDestination());
                g2.setStroke(new java.awt.BasicStroke(1.5f));
                g2.drawRoundRect(bx, by, 60, 60, 10, 10);
                g2.setStroke(new java.awt.BasicStroke(1.0f));
                
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                g2.setColor(Color.WHITE);
                String numStr = String.valueOf(i + 1);
                FontMetrics fmNum = g2.getFontMetrics();
                g2.drawString(numStr, bx + (60 - fmNum.stringWidth(numStr)) / 2, by + 35);
            } else {
                g2.setColor(new Color(25, 25, 25, 120));
                g2.fillRoundRect(bx, by, 60, 60, 10, 10);
                g2.setColor(new Color(255, 255, 255, 10));
                g2.drawRoundRect(bx, by, 60, 60, 10, 10);
                
                // Draw vector lock symbol
                g2.setColor(new Color(80, 80, 80));
                g2.drawRoundRect(bx + 22, by + 25, 16, 16, 4, 4);
                g2.drawArc(bx + 25, by + 16, 10, 14, 0, 180);
            }
        }
        
        // Back Button
        int cx = getWidth() / 2;
        drawButton(g2, "BACK TO MENU", cx, 378, 160, 36, currentTheme.getWall());
    }

    private void drawLevelEditor(Graphics2D g2) {
        g2.setColor(currentTheme.getBg());
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g2.setColor(Color.WHITE);
        String title = "LEVEL EDITOR";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, 45);
        
        // Draw editable grid centered
        int offsetX = (getWidth() - (editorWidth * TILE_SIZE)) / 2;
        int offsetY = 80;
        
        for (int y = 0; y < editorHeight; y++) {
            for (int x = 0; x < editorWidth; x++) {
                int drawX = offsetX + x * TILE_SIZE;
                int drawY = offsetY + y * TILE_SIZE;
                
                int status = editorGrid[x][y];
                drawTile(g2, status, 0, drawX, drawY, x, y);
                
                // Draw gridlines
                g2.setColor(new Color(255, 255, 255, 25));
                g2.drawRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
            }
        }
        
        // Draw Brushes below grid
        int paletteY = offsetY + editorHeight * TILE_SIZE + 20;
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        g2.drawString("SELECT BRUSH:", 25, paletteY + 21);
        
        String[] brushNames = {"GROUND", "WALL", "BOX", "TARGET", "PLAYER"};
        Color[] brushColors = {currentTheme.getGround(), currentTheme.getWall(), currentTheme.getBox(), currentTheme.getDestination(), currentTheme.getPlayer()};
        
        for (int i = 0; i < 5; i++) {
            int bx = 125 + i * 80;
            g2.setColor(new Color(25, 25, 25, 200));
            g2.fillRoundRect(bx, paletteY, 70, 30, 6, 6);
            
            if (selectedBrush == i) {
                g2.setColor(brushColors[i]);
                g2.setStroke(new java.awt.BasicStroke(2.0f));
                g2.drawRoundRect(bx, paletteY, 70, 30, 6, 6);
                g2.setStroke(new java.awt.BasicStroke(1.0f));
            } else {
                g2.setColor(new Color(255, 255, 255, 15));
                g2.drawRoundRect(bx, paletteY, 70, 30, 6, 6);
            }
            
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(Color.WHITE);
            FontMetrics fmb = g2.getFontMetrics();
            g2.drawString(brushNames[i], bx + (70 - fmb.stringWidth(brushNames[i])) / 2, paletteY + 19);
        }
        
        // Action Buttons at bottom
        int actionY = paletteY + 52;
        int cx = getWidth() / 2;
        drawButton(g2, "PLAY MAP", cx - 110, actionY, 100, 34, currentTheme.getDestination());
        drawButton(g2, "VALIDATE", cx, actionY, 100, 34, currentTheme.getPlayer());
        drawButton(g2, "BACK TO MENU", cx + 110, actionY, 110, 34, currentTheme.getWall());
    }

    private void drawPauseOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
        g2.setColor(Color.WHITE);
        String text = "GAME PAUSED";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, 110);
        
        int cx = getWidth() / 2;
        drawButton(g2, "RESUME", cx, 180, 180, 40, currentTheme.getDestination());
        drawButton(g2, "RESTART", cx, 240, 180, 40, currentTheme.getPlayer());
        drawButton(g2, "MAIN MENU", cx, 300, 180, 40, currentTheme.getWall());
    }

    private void drawButton(Graphics2D g2, String text, int cx, int cy, int w, int h, Color accent) {
        g2.setColor(new Color(25, 25, 25, 220));
        g2.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 10, 10);
        g2.setColor(new Color(255, 255, 255, 30));
        g2.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 10, 10);
        
        // Left accent indicator dot
        g2.setColor(accent);
        g2.fillOval(cx - w / 2 + 15, cy - 4, 8, 8);
        
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, cx - fm.stringWidth(text) / 2 + 10, cy + 5);
    }

    private void drawGamePlay(Graphics2D g2) {
        if (grid == null) return;
        
        Tile[][] tiles = grid.getGrid();
        
        // Calculate offsets to center the grid with 80px top padding for HUD
        int offsetX = (getWidth() - (width * TILE_SIZE)) / 2;
        int offsetY = ((getHeight() - 80) - (height * TILE_SIZE)) / 2 + 80;
        
        // Layer 1: Draw GROUND and Boundary WALLS statically in a grid
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int drawX = offsetX + x * TILE_SIZE;
                int drawY = offsetY + y * TILE_SIZE;
                
                // Always draw ground first as the underlying floor
                drawTile(g2, Tile.GROUND, 0, drawX, drawY, x, y);
                
                // If it is a boundary wall, draw the brick wall on top
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    drawTile(g2, Tile.WALL, tiles[x][y].getColor(), drawX, drawY, x, y);
                }
            }
        }
        
        // Layer 2: Draw Destinations statically
        for (int i = 0; i < grid.getBoxCount(); i++) {
            int destX = grid.getDestinations()[i].getX();
            int destY = grid.getDestinations()[i].getY();
            int drawX = offsetX + destX * TILE_SIZE;
            int drawY = offsetY + destY * TILE_SIZE;
            drawTile(g2, Tile.DESTINATION, 0, drawX, drawY, destX, destY);
        }
        
        // Layer 3: Draw Boxes dynamically (smooth sliding animation)
        int boxCount = grid.getBoxCount();
        if (visualBoxXs != null && visualBoxYs != null && visualBoxXs.length == boxCount) {
            for (int i = 0; i < boxCount; i++) {
                double visX = visualBoxXs[i];
                double visY = visualBoxYs[i];
                int drawX = (int) (offsetX + visX * TILE_SIZE);
                int drawY = (int) (offsetY + visY * TILE_SIZE);
                
                // Show completed glowing checkmark box if it is resting on a destination pad
                boolean onDest = grid.getBoxes()[i].onDestination();
                int status = onDest ? Tile.WALL : Tile.BOX;
                drawTile(g2, status, 0, drawX, drawY, (int) visX, (int) visY);
            }
        }
        
        // Layer 4: Draw Player dynamically (smooth sliding animation)
        int playerDrawX = (int) (offsetX + visualPlayerX * TILE_SIZE);
        int playerDrawY = (int) (offsetY + visualPlayerY * TILE_SIZE);
        drawTile(g2, Tile.PLAYER, 0, playerDrawX, playerDrawY, (int) visualPlayerX, (int) visualPlayerY);
        
        // Draw active Particles on top of the grid layer
        for (Particle p : particles) {
            p.draw(g2);
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
                    // BOX on DESTINATION: Draw a beautiful glossy completing crate!
                    Color startColor = currentTheme.getDestination();
                    Color endColor = startColor.darker();
                    java.awt.GradientPaint gp = new java.awt.GradientPaint(drawX, drawY, startColor, drawX + TILE_SIZE, drawY + TILE_SIZE, endColor);
                    g2.setPaint(gp);
                    g2.fillRoundRect(drawX + 5, drawY + 5, TILE_SIZE - 10, TILE_SIZE - 10, 8, 8);
                    
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new java.awt.BasicStroke(2.0f));
                    g2.drawRoundRect(drawX + 5, drawY + 5, TILE_SIZE - 10, TILE_SIZE - 10, 8, 8);
                    g2.drawLine(drawX + 15, drawY + 15, drawX + TILE_SIZE - 15, drawY + TILE_SIZE - 15);
                    g2.drawLine(drawX + TILE_SIZE - 15, drawY + 15, drawX + 15, drawY + TILE_SIZE - 15);
                    g2.setStroke(new java.awt.BasicStroke(1.0f));
                } else {
                    // WALL: Draw modern 3D brick with gradient
                    Color startColor = currentTheme.getWall();
                    Color endColor = startColor.darker();
                    java.awt.GradientPaint gp = new java.awt.GradientPaint(drawX, drawY, startColor, drawX, drawY + TILE_SIZE, endColor);
                    g2.setPaint(gp);
                    g2.fillRoundRect(drawX + 2, drawY + 2, TILE_SIZE - 4, TILE_SIZE - 4, 8, 8);
                    
                    g2.setColor(startColor.brighter());
                    g2.setStroke(new java.awt.BasicStroke(1.5f));
                    g2.drawRoundRect(drawX + 2, drawY + 2, TILE_SIZE - 4, TILE_SIZE - 4, 8, 8);
                    g2.setStroke(new java.awt.BasicStroke(1.0f));
                }
                break;
            case 2: // BOX
                Color crateStart = currentTheme.getBox();
                Color crateEnd = crateStart.darker();
                java.awt.GradientPaint crateGp = new java.awt.GradientPaint(drawX, drawY, crateStart, drawX + TILE_SIZE, drawY + TILE_SIZE, crateEnd);
                g2.setPaint(crateGp);
                g2.fillRoundRect(drawX + 5, drawY + 5, TILE_SIZE - 10, TILE_SIZE - 10, 8, 8);
                
                g2.setColor(currentTheme.getBoxDetail());
                g2.setStroke(new java.awt.BasicStroke(2.0f));
                g2.drawRoundRect(drawX + 5, drawY + 5, TILE_SIZE - 10, TILE_SIZE - 10, 8, 8);
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
                
                // Glossy sphere player
                Color pStart = currentTheme.getPlayer();
                Color pEnd = pStart.darker().darker();
                java.awt.GradientPaint playerGp = new java.awt.GradientPaint(drawX + 10, drawY + 10, pStart, drawX + TILE_SIZE - 10, drawY + TILE_SIZE - 10, pEnd);
                g2.setPaint(playerGp);
                g2.fillOval(drawX + 6, drawY + 6, TILE_SIZE - 12, TILE_SIZE - 12);
                
                g2.setColor(Color.WHITE);
                g2.setStroke(new java.awt.BasicStroke(1.5f));
                g2.drawOval(drawX + 6, drawY + 6, TILE_SIZE - 12, TILE_SIZE - 12);
                g2.setStroke(new java.awt.BasicStroke(1.0f));
                
                // Anime eyes
                g2.setColor(Color.WHITE);
                g2.fillOval(drawX + 14, drawY + 15, 6, 8);
                g2.fillOval(drawX + 28, drawY + 15, 6, 8);
                g2.setColor(Color.BLACK);
                g2.fillOval(drawX + 16, drawY + 17, 3, 4);
                g2.fillOval(drawX + 30, drawY + 17, 3, 4);
                break;
        }
    }
}
