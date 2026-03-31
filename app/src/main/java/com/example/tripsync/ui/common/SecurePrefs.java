package com.example.tripsync.ui.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SecurePrefs {

    private static final String KEYSTORE_NAME = "AndroidKeyStore";
    private static final String KEY_ALIAS = "TripSyncSecurePrefsKey";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String VALUE_PREFIX = "enc::";

    private SecurePrefs() {
    }

    public static void putString(Context context, String prefName, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        if (value == null) {
            prefs.edit().remove(key).apply();
            return;
        }

        try {
            prefs.edit().putString(key, encrypt(value)).apply();
        } catch (Exception e) {
            prefs.edit().putString(key, value).apply();
        }
    }

    public static String getString(Context context, String prefName, String key, String defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        String storedValue = prefs.getString(key, null);
        if (storedValue == null) {
            return defaultValue;
        }

        if (!storedValue.startsWith(VALUE_PREFIX)) {
            putString(context, prefName, key, storedValue);
            return storedValue;
        }

        try {
            return decrypt(storedValue);
        } catch (Exception e) {
            prefs.edit().remove(key).apply();
            return defaultValue;
        }
    }

    public static void clear(Context context, String prefName) {
        context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().clear().apply();
    }

    private static String encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());

        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        String ivEncoded = Base64.encodeToString(iv, Base64.NO_WRAP);
        String encryptedEncoded = Base64.encodeToString(encrypted, Base64.NO_WRAP);
        return VALUE_PREFIX + ivEncoded + ":" + encryptedEncoded;
    }

    private static String decrypt(String cipherText) throws Exception {
        String payload = cipherText.substring(VALUE_PREFIX.length());
        String[] parts = payload.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid cipher payload");
        }

        byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), new GCMParameterSpec(128, iv));

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static SecretKey getOrCreateSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_NAME);
        keyStore.load(null);

        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_NAME);
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());

        return keyGenerator.generateKey();
    }
}
