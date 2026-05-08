@echo off
echo Compiling...
javac -d bin src\main\java\com\sokoban\*.java src\main\java\com\sokoban\entity\*.java src\main\java\com\sokoban\objects\*.java src\main\java\com\sokoban\util\*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)
echo Running...
java -cp bin com.sokoban.Main
