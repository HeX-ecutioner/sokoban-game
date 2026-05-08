package com.sokoban.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;

public class Particle {
    private double x, y;
    private double vx, vy;
    private final Color color;
    private final int size;
    private double alpha = 1.0;
    private final double decay;

    public Particle(double x, double y, double vx, double vy, Color color, int size, double decay) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.size = size;
        this.decay = decay;
    }

    public boolean update() {
        x += vx;
        y += vy;
        // Apply minor gravity/air resistance
        vy += 0.08;
        alpha -= decay;
        return alpha > 0;
    }

    public void draw(Graphics2D g2) {
        if (alpha <= 0) return;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
        g2.setColor(color);
        g2.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }
}
