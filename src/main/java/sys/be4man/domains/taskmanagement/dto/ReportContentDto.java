// 작성자 : 허겸
package sys.be4man.domains.taskmanagement.dto;

import lombok.*;

/**
 * 결과보고 내용 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportContentDto {

    private String deploymentResult;        // 배포 결과 (성공/실패)
    private String actualStartedAt;         // 실제 시작 시각 (YYYY.MM.DD HH:mm)
    private String actualEndedAt;           // 실제 종료 시각 (YYYY.MM.DD HH:mm)
    private String actualDuration;          // 실제 소요 시간
    private String reportContent;           // 결과보고 상세 내용 (deployment.content)
    private String reportCreatedAt;         // 결과보고 작성 시각 (YYYY.MM.DD HH:mm)
}