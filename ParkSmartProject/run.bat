@echo off
echo Starting ParkSmart Hotel Parking System...
cd /d "%~dp0"
java -cp "bin;lib/mysql-connector-j-9.6.0.jar" com.parksmart.ParkSmartApp
pause