package sys.be4man.domains.analysis.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jenkins Console Output를 스테이지 단위로 파싱하는 유틸리티.
 * - ANSI 숨김 블록(ESC[8m ... ESC[0m)과 일반 ANSI CSI 시퀀스를 제거 후 파싱
 * - [Pipeline] { (StageName)  ~  [Pipeline] // stage  를 스테이지 경계로 인식
 * - 각 스테이지의 로그를 기반으로 단순 휴리스틱으로 성공/실패 판단
 * - 파이프라인 전역 실패 신호( "Finished: FAILURE", "ERROR: script returned exit code" )가 있는데
 *   어떤 스테이지도 실패가 아닐 경우, 마지막 스테이지를 실패로 보정
 *
 * 사용 예:
 *   List<StageBlock> stages = JenkinsConsoleParser.parse(fullConsoleText);
 */
public final class JenkinsConsoleLogParser {

    private JenkinsConsoleLogParser() {}

    /** [Pipeline] { (Stage Name)  라인의 오픈 패턴 */
    private static final Pattern STAGE_OPEN =
            Pattern.compile("^\\Q[Pipeline]\\E\\s*\\{\\s*\\((?<stage>.*)\\)\\s*$");

    /** [Pipeline] // stage  라인의 클로즈 패턴 */
    private static final Pattern STAGE_CLOSE =
            Pattern.compile("^\\Q[Pipeline]\\E\\s*//\\s*stage\\s*$");

    /** Jenkins가 줄 앞에 붙이는 숨김 ANSI 블록: ESC[8m ... ESC[0m */
    private static final Pattern HIDDEN_BLOCK =
            Pattern.compile("\\u001B\\[8m.*?\\u001B\\[0m");

    /** 일반 ANSI CSI 시퀀스 제거(색상/커서 제어 등) */
    private static final Pattern ANSI_CSI =
            Pattern.compile("\\u001B\\[[0-9;?]*[ -/]*[@-~]");

    /** 결과 모델 */
    public record StageBlock(int orderIndex, String name, String log, boolean success) {}

    /**
     * 전체 콘솔 로그를 스테이지 단위로 파싱한다.
     */
    public static List<StageBlock> parse(String fullLog) {
        if (fullLog == null || fullLog.isEmpty()) return List.of();

        // 라인 단위 분리(모든 개행 \R) 및 후처리
        String[] lines = fullLog.split("\\R", -1);

        List<StageBlock> result = new ArrayList<>();
        String currentName = null;
        List<String> buf = new ArrayList<>();
        int orderIndex = 0;

        for (String raw : lines) {
            String line = sanitize(raw);

            // 빈 줄만 남으면 스킵(잡음 감소)
            if (line.isEmpty() && currentName == null) continue;

            Matcher open = STAGE_OPEN.matcher(line);
            Matcher close = STAGE_CLOSE.matcher(line);

            if (open.find()) {
                // 이전 스테이지가 닫히지 않은 채로 새 스테이지가 열리면 강제 마감
                if (currentName != null) {
                    result.add(buildStage(++orderIndex, currentName, buf));
                    buf.clear();
                }
                currentName = open.group("stage").trim();
                continue;
            }

            if (close.find()) {
                if (currentName != null) {
                    result.add(buildStage(++orderIndex, currentName, buf));
                    currentName = null;
                    buf.clear();
                }
                continue;
            }

            if (currentName != null) {
                buf.add(line);
            }
        }

        // 파일 끝에서 열린 스테이지가 남아있으면 마감
        if (currentName != null) {
            result.add(buildStage(++orderIndex, currentName, buf));
        }

        // 파이프라인 전역 실패 신호가 있는데 스테이지에 실패가 하나도 없다면, 마지막 스테이지를 실패로 보정
        applyGlobalFailureHeuristicIfNeeded(fullLog, result);

        return result;
    }

    /** 한 줄 정리: 숨김 ANSI 블록 + 일반 ANSI CSI 제거, trim */
    private static String sanitize(String line) {
        if (line == null || line.isEmpty()) return "";
        String s = HIDDEN_BLOCK.matcher(line).replaceAll("");
        s = ANSI_CSI.matcher(s).replaceAll("");
        return s.trim();
    }

    /** StageBlock 생성 시 성공/실패 휴리스틱 평가 */
    private static StageBlock buildStage(int orderIndex, String name, List<String> buf) {
        String log = String.join("\n", buf);
        boolean success = inferSuccess(buf);
        return new StageBlock(orderIndex, name, log, success);
    }

    /**
     * 간단한 휴리스틱:
     * - 명시적 성공 문구가 있으면 성공
     * - 전형적 에러 키워드가 있으면 실패(단, 직후 '+ true'로 무시한 케이스는 성공 처리)
     * - JSON 페이로드의 "status":"FAILURE" 같은 값은 실패로 간주하지 않음(웹훅 테스트 등 오탐 방지)
     */
    private static boolean inferSuccess(List<String> lines) {
        String joinedLower = String.join("\n", lines).toLowerCase();

        // 1) 성공 신호
        if (joinedLower.contains("build successful")
                || joinedLower.contains("finished: success")) {
            return true;
        }

        // 2) JSON 페이로드상의 "status":"FAILURE"는 실패로 치지 않음(오탐 방지)
        boolean containsPayloadFailure =
                joinedLower.contains("\"status\"") && joinedLower.contains("\"failure\"");

        // 3) 실패 키워드(확장 가능)
        String[] failKeys = {
                " finished: failure",
                "error response from daemon",
                "error:", "error ",
                " exception",
                "returned non-zero exit status",
                "script returned exit code",
                "fatal:"
        };

        boolean hasError = false;
        for (String k : failKeys) {
            if (joinedLower.contains(k)) {
                hasError = true;
                break;
            }
        }
        if (!hasError) return true;

        if (containsPayloadFailure) {
            // JSON 내 상태 텍스트만으로는 실패 처리하지 않음
            // (진짜 실패 키워드가 있다면 위에서 이미 hasError=true임)
        }

        // 4) '+ true'로 에러 무시한 경우 성공 처리
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i).toLowerCase();
            if (s.contains("error") || s.contains("exception") || s.contains("script returned exit code")) {
                for (int j = i + 1; j < Math.min(i + 6, lines.size()); j++) {
                    if (lines.get(j).trim().equals("+ true")) {
                        return true; // 의도적으로 무시
                    }
                }
                return false;
            }
        }
        return false;
    }

    /**
     * 파이프라인 전역 실패 신호가 있는데, 개별 스테이지는 모두 성공으로 나온 경우
     * 마지막 스테이지를 실패로 보정한다.
     */
    private static void applyGlobalFailureHeuristicIfNeeded(String fullLog, List<StageBlock> stages) {
        if (stages.isEmpty()) return;

        String lower = fullLog == null ? "" : fullLog.toLowerCase();
        boolean pipelineFailed =
                lower.contains("finished: failure") || lower.contains("error: script returned exit code");

        if (!pipelineFailed) return;

        boolean anyFailed = stages.stream().anyMatch(sb -> !sb.success());
        if (anyFailed) return;

        // 마지막 스테이지를 실패로 덮어쓰기
        StageBlock last = stages.get(stages.size() - 1);
        stages.set(stages.size() - 1, new StageBlock(last.orderIndex(), last.name(), last.log(), false));
    }
}
