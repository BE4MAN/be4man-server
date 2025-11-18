package sys.be4man.domains.taskmanagement.dto;

import lombok.*;

/**
 * Jenkins 빌드 로그 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JenkinsLogDto {

    private String jenkinsJobName;          // Jenkins Job 이름
    private Long buildNumber;               // 빌드 번호
    private String buildStatus;             // 빌드 상태 (SUCCESS/FAILURE/IN_PROGRESS)
    private String startedAt;               // 시작 시각 (YYYY.MM.DD HH:mm)
    private String endedAt;                 // 종료 시각 (YYYY.MM.DD HH:mm)
    private Long duration;                  // 소요 시간 (밀리초)
    private String durationFormatted;       // 소요 시간 (포맷팅: "3분 25초")
    private String log;                     // 빌드 로그 (raw console output)
}