@echo off
REM Multimedia File Backup Manager - Launch Script
REM This script runs the application with proper JVM arguments to suppress warnings

echo Starting Multimedia File Backup Manager...
echo.

java --enable-native-access=ALL-UNNAMED -jar build\libs\MFBCM-1.0-SNAPSHOT.jar

pause

