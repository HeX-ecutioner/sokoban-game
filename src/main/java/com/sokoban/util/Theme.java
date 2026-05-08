package com.sokoban.util;

import java.awt.Color;

public enum Theme {
    NEON_CYBERPUNK("Neon Cyberpunk",
        new Color(18, 18, 24),     // Panel Background
        new Color(35, 35, 45),     // Ground
        new Color(55, 55, 70),     // Ground Grid
        new Color(255, 0, 127),    // Wall
        new Color(255, 128, 0),    // Box
        new Color(255, 200, 0),    // Box Border / Accent
        new Color(0, 255, 200),    // Destination
        new Color(0, 191, 255)     // Player
    ),
    CLASSIC_WOODCRAFT("Classic Woodcraft",
        new Color(40, 26, 13),      // Panel Background
        new Color(101, 67, 33),     // Ground
        new Color(78, 51, 25),      // Ground Grid
        new Color(139, 69, 19),     // Wall (Oak/Wood)
        new Color(210, 105, 30),    // Box (Chocolate)
        new Color(244, 164, 96),    // Box Border / Accent (Sandy Brown)
        new Color(218, 165, 32),    // Destination (Goldenrod)
        new Color(255, 222, 173)    // Player (Navajo White)
    ),
    MYSTIC_RUINS("Mystic Ruins",
        new Color(20, 28, 20),      // Panel Background
        new Color(45, 60, 45),      // Ground
        new Color(35, 48, 35),      // Ground Grid
        new Color(85, 107, 47),     // Wall (Dark Olive Green)
        new Color(47, 79, 79),      // Box (Dark Slate Grey)
        new Color(102, 205, 170),   // Box Border / Accent (Medium Aquamarine)
        new Color(50, 205, 50),     // Destination (Lime Green)
        new Color(152, 251, 152)    // Player (Pale Green)
    ),
    ICE_FORTRESS("Ice Fortress",
        new Color(15, 30, 45),      // Panel Background
        new Color(30, 60, 90),      // Ground
        new Color(45, 80, 115),     // Ground Grid
        new Color(173, 216, 230),   // Wall (Light Blue)
        new Color(70, 130, 180),    // Box (Steel Blue)
        new Color(240, 248, 255),   // Box Border / Accent (Alice Blue)
        new Color(0, 191, 255),     // Destination (Deep Sky Blue)
        new Color(224, 255, 255)    // Player (Light Cyan)
    );

    private final String name;
    private final Color bg;
    private final Color ground;
    private final Color gridLine;
    private final Color wall;
    private final Color box;
    private final Color boxDetail;
    private final Color destination;
    private final Color player;

    Theme(String name, Color bg, Color ground, Color gridLine, Color wall, Color box, Color boxDetail, Color destination, Color player) {
        this.name = name;
        this.bg = bg;
        this.ground = ground;
        this.gridLine = gridLine;
        this.wall = wall;
        this.box = box;
        this.boxDetail = boxDetail;
        this.destination = destination;
        this.player = player;
    }

    public String getName() { return name; }
    public Color getBg() { return bg; }
    public Color getGround() { return ground; }
    public Color getGridLine() { return gridLine; }
    public Color getWall() { return wall; }
    public Color getBox() { return box; }
    public Color getBoxDetail() { return boxDetail; }
    public Color getDestination() { return destination; }
    public Color getPlayer() { return player; }
}
