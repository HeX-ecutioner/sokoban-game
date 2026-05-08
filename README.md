# Sokoban Java

A classic offline Sokoban game built in Java with a Swing GUI. 

## How to Play

The goal of the game is to push all the boxes (brown) onto the destinations (red X's). The player (cyan) can only push boxes, not pull them. If a box is pushed into a corner, it's stuck!

**Controls:**
- **W, A, S, D** or **Arrow Keys**: Move up, left, down, and right.
- **R**: Reset the current level (if you get stuck).
- **Shift + R**: Generate a new random map for the current level.

Levels get progressively larger as you win.

## Compiling and Running

This game requires **Java 8 or higher**.

**Windows:**
Double-click `run.bat` or execute it from the command line:
```cmd
.\run.bat
```

**Manual Compilation (Any OS):**
```bash
# Compile the sources
javac -d bin src/main/java/com/sokoban/*.java src/main/java/com/sokoban/entity/*.java src/main/java/com/sokoban/objects/*.java src/main/java/com/sokoban/util/*.java

# Run the game
java -cp bin com.sokoban.Main
```
