package com.sokoban;

import com.sokoban.objects.Grid;
import com.sokoban.objects.Tile;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GamePanel extends JPanel {
    private static final int TILE_SIZE = 50;
    
    private int level = 1;
    private int width = 9;
    private int height = 6;
    private Grid grid;
    
    public GamePanel() {
        initGame();
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                boolean moved = false;
                switch (e.getKeyCode()) {
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
                        moved = true;
                        break;
                }
                
                if (moved) {
                    grid.updateGrid();
                    if (grid.hasWon()) {
                        levelUp();
                    }
                    repaint();
                }
            }
        });
        setFocusable(true);
    }
    
    private void initGame() {
        grid = new Grid(width, height, level);
        setPreferredSize(new Dimension(13 * TILE_SIZE, 8 * TILE_SIZE + 40));
    }
    
    private void levelUp() {
        level++;
        if (width < 13) width += 2;
        if (height < 8) height += 1;
        grid = new Grid(width, height, level);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Draw background
        g.setColor(new Color(30, 30, 30));
        g.fillRect(0, 0, getWidth(), getHeight());
        
        if (grid == null) return;
        
        Tile[][] tiles = grid.getGrid();
        
        // Calculate offsets to center the grid
        int offsetX = (getWidth() - (width * TILE_SIZE)) / 2;
        int offsetY = ((getHeight() - 40) - (height * TILE_SIZE)) / 2 + 40;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int drawX = offsetX + x * TILE_SIZE;
                int drawY = offsetY + y * TILE_SIZE;
                
                int status = tiles[x][y].getStatus();
                int colorIdx = tiles[x][y].getColor();
                
                drawTile(g, status, colorIdx, drawX, drawY);
            }
        }
        
        // Draw Level Info
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        String text = "Level: " + level + " | R: Reset | Shift+R: New Map";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, 25);
    }
    
    private void drawTile(Graphics g, int status, int colorIdx, int x, int y) {
        switch (status) {
            case 0: // GROUND
                g.setColor(Color.DARK_GRAY);
                g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                g.setColor(new Color(50, 50, 50));
                g.drawRect(x, y, TILE_SIZE, TILE_SIZE);
                break;
            case 1: // WALL
                Color[] wallColors = {
                    Color.RED, Color.ORANGE, Color.YELLOW, 
                    Color.GREEN, Color.BLUE, Color.MAGENTA
                };
                g.setColor(wallColors[colorIdx % wallColors.length]);
                g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                g.setColor(Color.WHITE);
                g.drawRect(x, y, TILE_SIZE, TILE_SIZE);
                break;
            case 2: // BOX
                g.setColor(new Color(139, 69, 19)); // Brown
                g.fillRect(x + 5, y + 5, TILE_SIZE - 10, TILE_SIZE - 10);
                g.setColor(new Color(160, 82, 45));
                g.drawRect(x + 5, y + 5, TILE_SIZE - 10, TILE_SIZE - 10);
                break;
            case 3: // DESTINATION
                g.setColor(Color.DARK_GRAY);
                g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                g.setColor(Color.RED);
                g.drawLine(x + 10, y + 10, x + TILE_SIZE - 10, y + TILE_SIZE - 10);
                g.drawLine(x + TILE_SIZE - 10, y + 10, x + 10, y + TILE_SIZE - 10);
                break;
            case 4: // PLAYER
                g.setColor(Color.DARK_GRAY);
                g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                g.setColor(Color.CYAN);
                g.fillOval(x + 5, y + 5, TILE_SIZE - 10, TILE_SIZE - 10);
                break;
        }
    }
}
