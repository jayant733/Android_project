package com.example.tripsync.ui.common;

import android.content.Context;

import java.util.Locale;

public final class LocalUserStore {

    public static final String SESSION_PREFS = "SessionPrefs";
    public static final String PROFILE_PREFS = "UserProfile";
    private static final String SESSION_EMAIL_KEY = "user_email";
    private static final String LEGACY_PROFILE_NAME_KEY = "profile_name";
    private static final String LEGACY_PROFILE_IMAGE_KEY = "profile_image";
    private static final String LEGACY_DEFAULT_PROFILE_NAME_KEY = "default_profile_name";
    private static final String LEGACY_DEFAULT_PROFILE_IMAGE_KEY = "default_profile_image";

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
        String profileKey = buildProfileKey(email) + "_name";
        String savedName = SecurePrefs.getString(context, PROFILE_PREFS, profileKey, null);

        if (savedName != null && !savedName.trim().isEmpty()) {
            return savedName;
        }

        String legacyName = readLegacyProfileValue(context, LEGACY_PROFILE_NAME_KEY);
        if (legacyName == null || legacyName.trim().isEmpty()) {
            legacyName = readLegacyProfileValue(context, LEGACY_DEFAULT_PROFILE_NAME_KEY);
        }

        if (legacyName != null && !legacyName.trim().isEmpty()) {
            saveProfileName(context, email, legacyName);
            return legacyName;
        }

        return fallback;
    }

    public static void saveProfileName(Context context, String email, String name) {
        SecurePrefs.putString(context, PROFILE_PREFS, buildProfileKey(email) + "_name", name);
    }

    public static String getProfileImage(Context context, String email) {
        String profileKey = buildProfileKey(email) + "_image";
        String savedImage = SecurePrefs.getString(context, PROFILE_PREFS, profileKey, null);

        if (savedImage != null && !savedImage.trim().isEmpty()) {
            return savedImage;
        }

        String legacyImage = readLegacyProfileValue(context, LEGACY_PROFILE_IMAGE_KEY);
        if (legacyImage == null || legacyImage.trim().isEmpty()) {
            legacyImage = readLegacyProfileValue(context, LEGACY_DEFAULT_PROFILE_IMAGE_KEY);
        }

        if (legacyImage != null && !legacyImage.trim().isEmpty()) {
            saveProfileImage(context, email, legacyImage);
            return legacyImage;
        }

        return null;
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

    private static String readLegacyProfileValue(Context context, String key) {
        return context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
                .getString(key, null);
    }
}
