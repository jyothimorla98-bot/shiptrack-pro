package com.shiptrack.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;

public final class TrackingNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private TrackingNumberGenerator() {}

    /** Format: STP-YYMMDD-XXXXXX (ambiguous characters left out on purpose). */
    public static String next() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            suffix.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return String.format("STP-%02d%02d%02d-%s",
                today.getYear() % 100, today.getMonthValue(), today.getDayOfMonth(), suffix);
    }
}
