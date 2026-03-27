@echo off
echo Starting ParkSmart Hotel Parking System...
cd /d "%~dp0"
java -cp "bin;lib/sqlite-jdbc-3.42.0.0.jar" com.parksmart.ParkSmartApp
pause