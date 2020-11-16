package com.android.example.filelocker;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class Biometric {

    private static final int KEY_SIZE = 256;
    private static final String MASTER_KEY_ALIAS = "_androidx_security_master_key_biometric";

    public static LiveData<SharedPreferences> create(final Fragment fragment, final String fileName, final int userAuthenticationValidityDurationSeconds, BiometricPrompt.PromptInfo promptInfo) {
        final MutableLiveData<SharedPreferences> out = new MutableLiveData<>();
        new BiometricPrompt(fragment.requireActivity(), ContextCompat.getMainExecutor(fragment.requireContext()),
                new AuthenticationCallback(fragment.requireContext(), fileName, userAuthenticationValidityDurationSeconds, out)
        ).authenticate(promptInfo);
        return out;
    }


    private static SharedPreferences create(Context c, String fileName, int userAuthenticationValidityDurationSeconds) {
        try {
            return EncryptedSharedPreferences.create(
                    fileName,
                    MasterKeys.getOrCreate(new KeyGenParameterSpec.Builder(
                            MASTER_KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(KEY_SIZE)
                            .setUserAuthenticationValidityDurationSeconds(userAuthenticationValidityDurationSeconds)
                            .setUserAuthenticationRequired(true)
                            .build()),
                    c,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e("Biometric", "Error:" + e.toString());
            return null;
        }
    }


    private static class AuthenticationCallback extends BiometricPrompt.AuthenticationCallback {
        private final Context context;
        private final String fileName;
        private final int userAuthenticationValidityDurationSeconds;
        private final MutableLiveData<SharedPreferences> out;

        AuthenticationCallback(Context context, String fileName, int userAuthenticationValidityDurationSeconds, MutableLiveData<SharedPreferences> out) {
            this.context = context;
            this.fileName = fileName;
            this.userAuthenticationValidityDurationSeconds = userAuthenticationValidityDurationSeconds;
            this.out = out;
        }

        @Override
        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);

            Log.d("Biometric",  "Succeed");

            out.postValue(create(context, fileName, userAuthenticationValidityDurationSeconds));
        }

        @Override
        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);

            Log.d("Biometric", "Error:" + errString);

            out.postValue(null);
        }
    }
}
