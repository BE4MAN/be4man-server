package sys.be4man.domains.taskmanagement.dto;

import lombok.*;

/**
 * 타임라인 단계 DTO
 * - 6단계: 계획서작성 → 계획서승인 → 배포시작 → 배포종료 → 결과보고작성 → 결과보고승인
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineStepDto {

    private Integer stepNumber;             // 단계 번호 (1~6)
    private String stepName;                // 단계 이름 (계획서작성, 계획서승인, ...)
    private String status;                  // 상태 (completed/active/pending)
    private String timestamp;               // 완료 시각 (YYYY.MM.DD HH:mm)
    private String description;             // 부가 설명 (optional)
}