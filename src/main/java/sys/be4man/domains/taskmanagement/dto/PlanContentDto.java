package sys.be4man.domains.taskmanagement.dto;

import lombok.*;

/**
 * 계획서 내용 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanContentDto {

    private String drafter;                 // 기안자
    private String department;              // 부서
    private String createdAt;               // 작성 시각 (YYYY.MM.DD HH:mm)
    private String serviceName;             // 서비스명
    private String taskTitle;               // 작업 제목
    private String deploymentType;          // 배포 유형
    private String scheduledAt;             // 예정 시각 (YYYY.MM.DD HH:mm)
    private String scheduledToEndedAt;      // 예정 종료 시각 (YYYY.MM.DD HH:mm)
    private String riskDescription;         // 위험도 설명
    private String expectedDuration;        // 예상 소요 시간
    private String version;                 // 버전
    private String strategy;                // 배포 전략
    private String content;                 // 계획서 상세 내용 (deployment.content)
    private String pullRequestUrl;          // PR URL
}