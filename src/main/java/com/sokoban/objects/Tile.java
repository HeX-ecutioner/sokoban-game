package com.sokoban.objects;

public class Tile {
    public static final int GROUND = 0;
    public static final int WALL = 1;
    public static final int BOX = 2;
    public static final int DESTINATION = 3;
    public static final int PLAYER = 4;
    
    private int color = 0;
    private int status = 0;
    
    public Tile(int status) {
        this.status = status;
    }
    
    public Tile(int status, int color) {
        this.status = status;
        this.color = color;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public void setStatus(int status, int color) {
        this.status = status;
        this.color = color;
    }
    
    public int getStatus() {
        return this.status;
    }
    
    public int getColor() {
        return this.color;
    }
}
