package com.example.tripsync.ui.common;

import android.content.Context;

import java.util.Locale;

public final class LocalUserStore {

    public static final String SESSION_PREFS = "SessionPrefs";
    public static final String PROFILE_PREFS = "UserProfile";
    private static final String SESSION_EMAIL_KEY = "user_email";

    private LocalUserStore() {
    }

    public static void saveSessionEmail(Context context, String email) {
        SecurePrefs.putString(context, SESSION_PREFS, SESSION_EMAIL_KEY, email);
    }

    public static String getSessionEmail(Context context, String fallback) {
        return SecurePrefs.getString(context, SESSION_PREFS, SESSION_EMAIL_KEY, fallback);
    }

    public static void clearSession(Context context) {
        SecurePrefs.clear(context, SESSION_PREFS);
    }

    public static String getProfileName(Context context, String email, String fallback) {
        return SecurePrefs.getString(context, PROFILE_PREFS, buildProfileKey(email) + "_name", fallback);
    }

    public static void saveProfileName(Context context, String email, String name) {
        SecurePrefs.putString(context, PROFILE_PREFS, buildProfileKey(email) + "_name", name);
    }

    public static String getProfileImage(Context context, String email) {
        return SecurePrefs.getString(context, PROFILE_PREFS, buildProfileKey(email) + "_image", null);
    }

    public static void saveProfileImage(Context context, String email, String imageUri) {
        SecurePrefs.putString(context, PROFILE_PREFS, buildProfileKey(email) + "_image", imageUri);
    }

    public static String buildProfileKey(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "default_profile";
        }
        return email.trim().toLowerCase(Locale.US).replace(".", "_");
    }
}
