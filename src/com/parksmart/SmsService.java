package com.parksmart;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * SmsService – Sends OTP SMS via Twilio REST API (Free Trial).
 *
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║ TWILIO SETUP (FREE): ║
 * ║ 1. Go to https://www.twilio.com/try-twilio ║
 * ║ 2. Sign up (no credit card, get $15.50 free credits) ║
 * ║ 3. Verify your mobile number in Twilio Console ║
 * ║ 4. Go to Console → Account Info and copy: ║
 * ║ - ACCOUNT_SID (starts with AC...) ║
 * ║ - AUTH_TOKEN (long hex string) ║
 * ║ - TWILIO_FROM (your free Twilio number e.g. +15xxxxxxxx) ║
 * ║ 5. Paste them in the fields below ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * ⚠ TRIAL LIMITATION: SMS can only be sent to VERIFIED numbers.
 * Verify your number at: Console → Phone Numbers → Verified Caller IDs
 */
public class SmsService {

    // ── SMS ENABLE/DISABLE TOGGLE ──────────────────────────────────────────
    public static final boolean ENABLE_SMS = false; 
    // ────────────────────────────────────────────────────────────────────────

    // ── PASTE YOUR TWILIO CREDENTIALS HERE ──────────────────────────────────
    private static final String ACCOUNT_SID = "YOUR_ACCOUNT_SID"; 
    private static final String AUTH_TOKEN  = "YOUR_AUTH_TOKEN";
    private static final String TWILIO_FROM = "YOUR_TWILIO_NUMBER";
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Sends a 4-digit OTP to the given mobile number via Twilio.
     *
     * @param mobile 10-digit Indian mobile number (without +91)
     * @param otp    The 4-digit OTP to send
     * @return true if SMS was sent successfully, false otherwise
     */
    public static boolean sendOtp(String mobile, String otp) {

        // Validate inputs
        if (mobile == null || mobile.length() < 10) {
            System.err.println("[SmsService] Invalid mobile number: " + mobile);
            return false;
        }

        // Check if credentials are still placeholders
        if (ACCOUNT_SID.startsWith("YOUR_") || AUTH_TOKEN.startsWith("YOUR_") || TWILIO_FROM.startsWith("YOUR_")) {
            System.err.println("[SmsService] ❌ Twilio credentials not configured!");
            System.err.println("[SmsService]    Edit SmsService.java and set ACCOUNT_SID, AUTH_TOKEN, TWILIO_FROM.");
            return false;
        }

        try {
            // Build the recipient number with India country code
            String toNumber = "+91" + mobile;

            // Build the SMS message body
            String messageBody = "Your ParkSmart OTP is: " + otp
                    + ". Valid for 3 minutes. Do not share this with anyone.";

            // Twilio API URL
            String urlStr = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json";

            // Build POST body
            String postData = "From=" + URLEncoder.encode(TWILIO_FROM, StandardCharsets.UTF_8)
                    + "&To=" + URLEncoder.encode(toNumber, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(messageBody, StandardCharsets.UTF_8);

            // Basic Auth (Base64 of "AccountSID:AuthToken")
            String auth = ACCOUNT_SID + ":" + AUTH_TOKEN;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            // Open connection
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);

            // Write POST body
            try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
                out.write(postData.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }

            int responseCode = conn.getResponseCode();

            // Read response
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 200 && responseCode < 300
                                    ? conn.getInputStream()
                                    : conn.getErrorStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            conn.disconnect();

            String responseBody = response.toString();
            System.out.println("[SmsService] HTTP " + responseCode + " → " + responseBody);

            // Twilio returns HTTP 201 Created on success, with "sid" in JSON body
            if (responseCode == 201 && responseBody.contains("\"sid\"")) {
                System.out.println("[SmsService] ✅ OTP SMS sent via Twilio to +91-" + mobile);
                return true;
            } else if (responseCode == 400 && responseBody.contains("unverified")) {
                System.err.println("[SmsService] ❌ Number +91-" + mobile + " is not verified in Twilio trial.");
                System.err.println("[SmsService]    Verify it at: https://console.twilio.com → Verified Caller IDs");
                return false;
            } else {
                System.err.println("[SmsService] ❌ SMS failed. Response: " + responseBody);
                return false;
            }

        } catch (java.net.SocketTimeoutException e) {
            System.err.println("[SmsService] ❌ Request timed out. Check internet connection.");
            return false;
        } catch (Exception e) {
            System.err.println("[SmsService] ❌ Exception: " + e.getMessage());
            return false;
        }
    }
}
