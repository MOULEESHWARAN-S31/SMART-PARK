# ParkSmart - Hotel Parking Management System

A complete Java Swing application for managing hotel parking with OTP-based login, calendar-based slot booking, payment processing, and exit verification workflow.

## Features

- **Secure OTP Login**: Mobile number verification with time-limited OTP (3 minutes)
- **Calendar Navigation**: Select booking dates with visual calendar interface
- **Slot Management**: 16 parking slots with real-time availability status
- **Booking System**: Complete booking workflow with payment simulation
- **Exit Process**: Arrival verification, penalty/refund calculation, and OTP-based exit
- **Booking History**: View and manage past bookings
- **SQLite Database**: Persistent storage for users and bookings

## Project Structure

```
ParkSmartProject/
├── src/
│   └── com/parksmart/
│       └── ParkSmartApp.java    # Main application
├── lib/
│   └── sqlite-jdbc-3.42.0.0.jar # SQLite JDBC driver
├── bin/                         # Compiled classes (created after build)
└── README.md                    # This file
```

## Prerequisites

- Java 11 or higher
- Windows/Linux/Mac OS

## Setup Instructions

1. **Clone/Download the project**
   ```
   # The project is already set up in ParkSmartProject/ directory
   ```

2. **Compile the application**
   ```bash
   cd ParkSmartProject
   javac -cp "lib/sqlite-jdbc-3.42.0.0.jar" -d bin src/com/parksmart/*.java
   ```

3. **Run the application**
   ```bash
   cd ParkSmartProject
   java -cp "bin;lib/sqlite-jdbc-3.42.0.0.jar" com.parksmart.ParkSmartApp
   ```

   Or simply double-click `run.bat` on Windows.

## Usage Workflow

### Step 1: Login with OTP
- Enter your 10-digit mobile number
- Click "Get OTP" - OTP will be displayed in the terminal
- Enter the OTP and click "Verify OTP"
- OTP expires in 3 minutes

### Step 2: Select Date & Slot
- Use the calendar to select your parking date
- Click on an available slot (blue color)
- Occupied slots are shown in red

### Step 3: Book & Pay
- Fill in your name, vehicle number, entry time, and exit time
- Click "Pay & Confirm Booking" (₹120 = ₹100 rent + ₹20 deposit)
- Booking is confirmed and added to history

### Step 4: Arrival & Exit
- When you arrive, click OK on the arrival dialog
- System calculates if you're early/late:
  - **Early exit**: Refund ₹20 deposit
  - **On time**: No additional charges
  - **Late exit**: Penalty ₹50
- Enter the exit OTP provided by the watchman
- Slot is freed and booking marked as completed

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    mobile TEXT PRIMARY KEY,
    name TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### Bookings Table
```sql
CREATE TABLE bookings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    mobile TEXT,
    slot_id TEXT,
    booking_date TEXT,
    entry_time TEXT,
    exit_time TEXT,
    vehicle TEXT,
    status TEXT DEFAULT 'confirmed',
    amount REAL DEFAULT 120.0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (mobile) REFERENCES users(mobile)
);
```

## Technical Details

- **UI Framework**: Java Swing with custom rounded panels
- **Database**: SQLite with JDBC
- **Architecture**: Single main class with CardLayout for screen navigation
- **OTP System**: In-memory storage with expiration (3 minutes)
- **Time Handling**: Java Time API for date/time operations
- **Styling**: Custom colors and fonts for modern look

## Demo Data

The application includes demo bookings for testing:
- Today's bookings on slots A1, A3
- Future date bookings on A4, A7

## Troubleshooting

### Compilation Issues
- Ensure Java 11+ is installed: `java -version`
- Make sure the classpath includes the SQLite JAR

### Runtime Warnings
- **Native access warning**: The SQLite JDBC driver shows a warning about restricted methods. This is normal and doesn't affect functionality. The warning can be ignored.

### Database Issues
- The `parksmart.db` file is created automatically in the working directory
- If database errors occur, delete the `.db` file and restart

### OTP Not Showing
- OTP is printed to the terminal/console where you run the application
- Make sure you're running from command line, not IDE

## License

This project is for educational purposes.