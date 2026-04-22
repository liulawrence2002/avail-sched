package com.goblinscheduler.util;

import java.security.SecureRandom;
import java.util.Base64;

public class TokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private TokenGenerator() {}

    public static String generatePublicId() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes).substring(0, 16);
    }

    public static String generateHostToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes).substring(0, 32);
    }

    public static String generateParticipantToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes).substring(0, 32);
    }
}
