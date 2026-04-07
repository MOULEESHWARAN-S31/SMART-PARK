# 🅿️ ParkSmart - Hotel Parking Excellence

A luxury Java Swing management system for high-end hotel parking zones. ParkSmart offers a **privacy-first logic**, **interactive dashboard UI**, and **secure real-time synchronization** between guests and security personnel.

---

## 🏗️ Core Concept

The system is designed for quick, secure entry and exit without relying on personal data. By focusing solely on the **10-digit Mobile Number**, the system maintains a professional, privacy-compliant workflow.

### 🛡️ Privacy Guarantee
*   **Zero Name Retention**: The "Name" field has been fully removed from the Database (`parksmart_db`), the User UI, the Admin UI, and all exported CSV reports.
*   **Verification-Based**: Access is granted only via secure 4-digit OTPs that appear in **on-screen pop-up windows** (no more terminal checking).

---

## 🚀 Key Features

*   **✨ Premium UI**: Pill-shaped rounded designs across all panels, slots, and buttons for a modern aesthetic.
*   **📅 Interactive Calendar**: 
    *   **Users**: Select dates to view future/past availability.
    *   **Admins**: A custom dashboard calendar showing **Red Point (●)** markers on dates that have active bookings.
*   **🔐 Admin Security**: Watchman login is strictly controlled.
    *   **Authorized Access Only**: Only mobile numbers pre-authorized in the `admins` table can login.
    *   **Master Number**: `9876543210` remains the hardcoded fallback for super-admin access.
*   **🔄 Instant Refresh**: Real-time "Refresh" buttons near the logout area on both panels. This forces an immediate sync with the MySQL database, ensuring the Watchman and the Guest see the same slot status (Free/Occupied) instantly.
*   **📲 Dynamic OTP Pop-ups**: 
    *   Logins and Exit Verifications trigger a **secure pop-up dialog** containing the unique 4-digit code.
*   **📊 CSV Reports**: Watchmen can generate date-range specific reports (Slot, Mobile, Vehicle, Entry, Exit, Status) for audit purposes.

---

## 📁 Source Structure

```text
SMART-PARK/
├── Admin/
│   └── ParkSmartAdmin.java       # Watchman Interface (Verification & Reports)
├── src/com/parksmart/
│   ├── ParkSmartApp.java         # Main Entry & User Login Pop-up logic
│   ├── ParkSmartUserPage.java    # Client Dashboard (Pill-shaped Slot Grid)
│   ├── ExitOtpStore.java         # Database-backed OTP synchronization
│   └── ParkSmartAdminLauncher.java # Admin panel invocation bridge
├── lib/
│   └── mysql-connector-j-9.6.0.jar # JDBC Connection Driver
├── run.bat                       # Build, Compile, and Launch script
└── README.md                     # This documentation
```

---

## ⚙️ How it Works (Logic Flow)

### 1. Booking a Slot (User)
*   Guest logs in with Mobile Number -> OTP Pop-up.
*   Guest selects a date and an available slot.
*   Guest enters vehicle number -> Booking confirms live in MySQL.

### 2. Requesting Exit (User → Admin Sync)
*   Guest clicks their occupied slot.
*   System generates an **Exit OTP** and shows it in a pop-up.
*   This OTP is simultaneously stored in the `exit_otps` table in the database.

### 3. Verification & Release (Admin)
*   The Guest provides the OTP to the Watchman.
*   The Watchman clicks the same slot on the **Admin Panel**.
*   The Watchman enters the Guest's OTP.
*   The system verifies it against the `exit_otps` table.
*   On success, the slot becomes **Free** instantly for all other users.

---

## 🛠️ Installation & Execution

1.  **Preparation**: Start Apache & MySQL in **XAMPP**.
2.  **Database**: Create a database named `parksmart_db` in `phpMyAdmin`.
    - *Note: The app will build all necessary tables automatically on the first run.*
3.  **Run**: Execute the **`run.bat`** file from the root directory.

---
*ParkSmart: Ensuring Every Guest Parks with Peace of Mind.*