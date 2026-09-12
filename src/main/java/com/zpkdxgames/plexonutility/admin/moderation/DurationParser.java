package com.zpkdxgames.plexonutility.admin.moderation;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure parser for bounded administrative ban durations. */
public final class DurationParser {
    private static final Pattern DURATION = Pattern.compile("^(\\d+)([mhd])$", Pattern.CASE_INSENSITIVE);
    private static final BigInteger SECONDS_PER_MINUTE = BigInteger.valueOf(60L);
    private static final BigInteger SECONDS_PER_HOUR = BigInteger.valueOf(3_600L);
    private static final BigInteger SECONDS_PER_DAY = BigInteger.valueOf(86_400L);

    private DurationParser() {
    }

    public static ParsedDuration parse(String raw, int maxDays) {
        if (maxDays <= 0) throw new IllegalArgumentException("Maximum duration must be positive");
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Duration is required");
        String token = raw.trim().toLowerCase(Locale.ROOT);
        if (token.equals("perm") || token.equals("permanent")) {
            return new ParsedDuration(true, null, "permanent");
        }

        Matcher matcher = DURATION.matcher(token);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid duration. Use perm, 30m, 2h, 1d, 7d, or 30d");
        }

        BigInteger amount;
        try {
            amount = new BigInteger(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid duration", exception);
        }
        if (amount.signum() <= 0) throw new IllegalArgumentException("Duration must be greater than zero");

        BigInteger multiplier = switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
            case "m" -> SECONDS_PER_MINUTE;
            case "h" -> SECONDS_PER_HOUR;
            case "d" -> SECONDS_PER_DAY;
            default -> throw new IllegalArgumentException("Invalid duration unit");
        };
        BigInteger seconds = amount.multiply(multiplier);
        BigInteger maximum = BigInteger.valueOf(maxDays).multiply(SECONDS_PER_DAY);
        if (seconds.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("Duration exceeds the configured maximum of " + maxDays + " days");
        }

        long boundedSeconds;
        try {
            boundedSeconds = seconds.longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Duration is too large", exception);
        }
        return new ParsedDuration(false, Duration.ofSeconds(boundedSeconds), token);
    }

    public static boolean looksLikeDurationToken(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String token = raw.trim().toLowerCase(Locale.ROOT);
        if (token.equals("perm") || token.equals("permanent")) return true;
        char first = token.charAt(0);
        return Character.isDigit(first) || first == '+' || first == '-';
    }

    public record ParsedDuration(boolean permanent, Duration duration, String display) {
        public ParsedDuration {
            if (permanent && duration != null) throw new IllegalArgumentException("Permanent duration must not carry a finite duration");
            if (!permanent && (duration == null || duration.isZero() || duration.isNegative())) {
                throw new IllegalArgumentException("Temporary duration must be positive");
            }
        }
    }
}
