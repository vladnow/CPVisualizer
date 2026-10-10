package io.github.cpvisualizer;

import java.util.Locale;
import java.util.regex.Pattern;

public final class TimeParser {
    private static final Pattern PART = Pattern.compile("(\\d+)([smhd])");
    private TimeParser() { }
    public static int seconds(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Specify a duration: 15m, 6h, 3d or 1d6h.");
        var matcher = PART.matcher(text.toLowerCase(Locale.ROOT));
        long total = 0;
        int end = 0;
        try {
            while (matcher.find()) {
                if (matcher.start() != end) throw new IllegalArgumentException("Invalid duration format.");
                int multiplier = switch (matcher.group(2)) { case "s" -> 1; case "m" -> 60; case "h" -> 3600; default -> 86400; };
                total = Math.addExact(total, Math.multiplyExact(Long.parseLong(matcher.group(1)), multiplier));
                end = matcher.end();
            }
        } catch (ArithmeticException ex) { throw new IllegalArgumentException("The duration is too large."); }
        if (end != text.length() || total < 1 || total > Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid duration or duration too large.");
        return (int) total;
    }
}
