package sys.be4man.domains.taskmanagement.dto;

import lombok.*;

import java.util.List;

/**
 * 작업 상세 페이지 응답 DTO
 * - 프론트엔드 TaskDetail 컴포넌트에 맞춘 데이터 구조
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDetailResponseDto {

    // 기본 정보
    private Long taskId;                    // 작업 번호
    private String serviceName;             // 서비스명
    private String taskTitle;               // 작업 제목
    private String currentStage;            // 현재 단계 (계획서/배포/결과보고)
    private String currentStatus;           // 현재 상태 (승인대기/배포중/완료 등)
    private String initialTab;              // 초기 활성화할 탭

    // 타임라인 정보
    private List<TimelineStepDto> timeline;

    // 승인 정보 (계획서 승인 + 결과보고 승인)
    private ApprovalInfoDto planApproval;   // 계획서 승인 정보
    private ApprovalInfoDto reportApproval; // 결과보고 승인 정보

    // Jenkins 빌드 정보
    private JenkinsLogDto jenkinsLog;

    // 탭별 컨텐츠
    private PlanContentDto planContent;     // 계획서 내용
    private ReportContentDto reportContent; // 결과보고 내용
}