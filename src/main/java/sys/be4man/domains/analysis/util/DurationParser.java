// 작성자 : 조윤상
package sys.be4man.domains.analysis.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class DurationParser {

    // e.g. "1 hr 2 mins 3 sec and counting", "58 sec", "500 ms"
    private static final Pattern TOKEN = Pattern.compile(
            "(\\d+)\\s*(hours?|hrs?|h|minutes?|mins?|m|seconds?|secs?|s|milliseconds?|msecs?|ms)",
            Pattern.CASE_INSENSITIVE
    );


    /**
     * Jenkins duration string -> seconds (Long)
     * Examples:
     * "1 min 58 sec and counting" -> 118
     * "2 hrs 0 min" -> 7200
     * "45 sec" -> 45
     * "1500 ms" -> 1  (floor(1.5s) = 1)  // 필요 시 반올림 정책으로 변경 가능
     */
    public static long toSeconds(String input) {
        if (input == null || input.isBlank()) return 0L;

        long totalSeconds = 0L;
        Matcher m = TOKEN.matcher(input);
        while (m.find()) {
            long value = Long.parseLong(m.group(1));
            String unit = m.group(2).toLowerCase();

            if (unit.startsWith("h")) {                  // h, hr, hrs, hour, hours
                totalSeconds += value * 3600L;
            } else if (unit.startsWith("m") && !unit.equals("ms")) { // m, min, mins, minute, minutes
                totalSeconds += value * 60L;
            } else if (unit.startsWith("s")) {           // s, sec, secs, second, seconds
                totalSeconds += value;
            } else if (unit.equals("ms") || unit.startsWith("msec") || unit.startsWith("millis")) {
                totalSeconds += (value / 1000L);         // 정책: 내림(floor)
                // 반올림 원하면: totalSeconds += Math.round(value / 1000.0);
            }
        }
        return totalSeconds;
    }
}
