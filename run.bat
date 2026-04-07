@echo off
echo ============================================================
echo  ParkSmart Hotel Parking System
echo ============================================================
cd /d "%~dp0"

echo [1/3] Compiling Admin Panel...
javac -d bin src\com\parksmart\ExitOtpStore.java
javac -cp bin -d bin Admin\ParkSmartAdmin.java
if errorlevel 1 ( echo ERROR: Admin compile failed. & pause & exit /b 1 )

echo [2/3] Compiling Main Application...
javac -cp "bin;lib/mysql-connector-j-9.6.0.jar" -d bin src\com\parksmart\ParkSmartApp.java src\com\parksmart\ParkSmartUserPage.java src\com\parksmart\ParkSmartAdminLauncher.java
if errorlevel 1 ( echo ERROR: Main app compile failed. & pause & exit /b 1 )

echo [3/3] Launching ParkSmart...
java -cp "bin;lib/mysql-connector-j-9.6.0.jar" com.parksmart.ParkSmartApp

pause