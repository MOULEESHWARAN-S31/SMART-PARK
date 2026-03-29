# ParkSmart - Hotel Parking Management System

A premium Java Swing desktop application for managing hotel parking with MySQL database integration (XAMPP), OTP-based login, calendar-based slot booking, and receipt generation.

## 🚀 Key Features

*   **Pill-Shaped UI Design**: Modern, consistent rounded aesthetic with smooth gradients.
*   **MySQL Integration**: Persistent storage using XAMPP/MariaDB.
*   **Real-Time Dashboard**: Interactive calendar and slot grid for parking management.
*   **Secure OTP Workflow**: Random 4-digit OTP generation (printed to terminal) for login and booking verification.
*   **Automatic Receipts**: Dynamically generated receipts with user-specific details.
*   **Read-Only Mobile Field**: Prevents editing the logged-in user's mobile number during booking.

## 📂 Project Structure

```text
ParkSmartProject/
├── src/
│   └── com/parksmart/
│       ├── ParkSmartApp.java    # Main Entry & DB Initialization
│       └── ParkSmartUserPage.java # User Dashboard & UI Logic
├── lib/
│   └── mysql-connector-j-9.6.0.jar # MySQL Connection Driver
├── bin/                         # Compiled Class Files
├── run.bat                      # Windows Runner Script
└── README.md                    # Documentation
```

## 🛠️ Prerequisites

1.  **Java Development Kit (JDK) 11+**
2.  **XAMPP Control Panel** (with Apache and MySQL services running)
3.  **MySQL Database**:
    *   Create a database named `parksmart_db` in `phpMyAdmin` (`http://localhost/phpmyadmin`).
    *   The application will automatically create the tables (`users`, `bookings`) on first run.

## ⚡ Setup & Execution

### 1. Database Setup
Ensure XAMPP is running and you have created the `parksmart_db` database.

### 2. Run the Application (Windows)
Simply double-click the `run.bat` file in the root directory.

*Alternatively, use the terminal:*
```bash
./run.bat
```

### 3. Manual Compilation (if needed)
```bash
javac -cp "lib/mysql-connector-j-9.6.0.jar" -d bin src/com/parksmart/*.java
```

## 📖 Usage Guide

1.  **Login**:
    *   Enter your 10-digit mobile number.
    *   Check your terminal/console for the generated **Login OTP**.
    *   Verify and enter the dashboard.
2.  **Booking**:
    *   Select a date on the left calendar.
    *   Click an available (Blue) slot.
    *   Enter your name and vehicle details.
    *   Check terminal for **Booking OTP** and verify.
3.  **Success**:
    *   View your modern, pill-shaped receipt.
    *   Data is saved live to your XAMPP MySQL database.

## ⚙️ Configuration
The default database settings are:
*   **Host**: `localhost:3306`
*   **DB Name**: `parksmart_db`
*   **User**: `root`
*   **Password**: *[None]*

## 📜 License
Educational project for ParkSmart - Hotel Parking Management.