package sys.be4man.domains.analysis.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class IsoLocalDateTimeParser {
    private static final DateTimeFormatter KST_LABELED_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static LocalDateTime parseKstLikeUtc(String isoWithLiteralZ) {
        if (isoWithLiteralZ == null || isoWithLiteralZ.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(isoWithLiteralZ, KST_LABELED_UTC);
    }
}
