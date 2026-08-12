package com.magicstudios.magiccore.modules.shop;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

public record ItemFingerprint(String material, String sha256) {
    public ItemFingerprint {
        material = Objects.requireNonNull(material, "material").toUpperCase(Locale.ROOT);
        if (!sha256.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("sha256 must be lowercase hex");
    }

    public static ItemFingerprint of(String material, byte[] canonicalItemData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(material.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(canonicalItemData);
            return new ItemFingerprint(material, HexFormat.of().formatHex(digest.digest()));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
