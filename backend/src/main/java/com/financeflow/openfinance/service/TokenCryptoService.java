package com.financeflow.openfinance.service;

import com.financeflow.openfinance.config.OpenFinanceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class TokenCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int TAG_LENGTH = 128;
    private final OpenFinanceProperties properties;

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] key = buildKey(properties.getCryptoKey());
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criptografar token Open Finance", e);
        }
    }

    private byte[] buildKey(String source) throws Exception {
        String safeSource = source == null ? "financeflow-open-finance-key-32chars" : source;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(safeSource.getBytes(StandardCharsets.UTF_8));
    }
}
