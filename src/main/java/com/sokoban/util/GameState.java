package com.sokoban.util;

public class GameState {
    private final int playerX;
    private final int playerY;
    private final int[][] boxPositions; // Array of [x, y] coordinates for each box
    private final int moves;
    private final int pushes;
    private final long elapsedMs;

    public GameState(int playerX, int playerY, int[][] boxPositions, int moves, int pushes, long elapsedMs) {
        this.playerX = playerX;
        this.playerY = playerY;
        this.boxPositions = new int[boxPositions.length][2];
        for (int i = 0; i < boxPositions.length; i++) {
            this.boxPositions[i][0] = boxPositions[i][0];
            this.boxPositions[i][1] = boxPositions[i][1];
        }
        this.moves = moves;
        this.pushes = pushes;
        this.elapsedMs = elapsedMs;
    }

    public int getPlayerX() {
        return playerX;
    }

    public int getPlayerY() {
        return playerY;
    }

    public int[][] getBoxPositions() {
        return boxPositions;
    }

    public int getMoves() {
        return moves;
    }

    public int getPushes() {
        return pushes;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }
}
