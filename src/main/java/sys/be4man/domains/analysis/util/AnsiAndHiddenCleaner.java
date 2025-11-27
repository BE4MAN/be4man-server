// 작성자 : 조윤상
package sys.be4man.domains.analysis.util;


import java.util.regex.Pattern;

public final class AnsiAndHiddenCleaner {

    // 1) 숨김 블록: ESC[8m ... ESC[0m  (여러 줄 포함 가능하므로 DOTALL)
    private static final Pattern HIDDEN_BLOCK =
            Pattern.compile("\u001B\\[8m.*?\u001B\\[0m", Pattern.DOTALL);

    // 2) 일반 ANSI CSI 시퀀스(색상/커서 이동 등)
    //    \u001B[  +  (옵션들)  +  최종 명령 글자(@-~ 범위)
    private static final Pattern ANSI_CSI =
            Pattern.compile("\u001B\\[[0-9;?]*[ -/]*[@-~]");

    // (옵션) 캐리지리턴 제거
    private static final Pattern CR = Pattern.compile("\\r");

    /** progressiveText로 받은 원시 로그를 사람이 읽을 수 있게 정리 */
    public static String clean(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        // 순서 중요: 숨김 블록 먼저 지우고, 남은 ANSI 제어 시퀀스 제거
        String s = HIDDEN_BLOCK.matcher(raw).replaceAll("");
        s = ANSI_CSI.matcher(s).replaceAll("");
        s = CR.matcher(s).replaceAll(""); // 윈도 캐리지리턴 제거(선택)
        return s;
    }
}
