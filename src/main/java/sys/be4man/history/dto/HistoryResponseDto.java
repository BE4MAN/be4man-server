package sys.be4man.history.dto;

import lombok.*;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeployStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * History 페이지 응답 DTO
 * - 화면에 표시할 데이터 형식
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryResponseDto {

    /**
     * 배포 ID
     */
    private Long id;

    /**
     * PR 번호
     * 예: 201, 247, 246
     */
    private Integer prNumber;

    /**
     * 브랜치명
     * 예: "feature/ui-update", "feature/auth-improvements"
     */
    private String branch;

    /**
     * 승인 여부 (한글)
     * - "승인대기중" (PENDING)
     * - "승인완료" (APPROVED)
     * - "반려" (REJECTED)
     * - "취소" (CANCELED)
     * - "배포" (SUCCESS, FAILURE인 경우)
     */
    private String approvalStatus;

    /**
     * 배포 시간 (포맷된 문자열)
     * 예: "2025.07.25 17:15"
     */
    private String deployTime;

    /**
     * 결과
     * - "성공" (SUCCESS)
     * - "실패" (FAILURE)
     * - "-" (나머지 상태)
     */
    private String result;


    /**
     * 원본 상태 (enum)
     * - 프론트엔드에서 조건 분기 시 사용
     */
    private DeployStatus status;

    /**
     * 생성 시간 (원본)
     * - 프론트엔드에서 추가 가공 시 사용
     */
    private LocalDateTime createdAt;

    /**
     * Entity → DTO 변환 생성자
     */
    public HistoryResponseDto(Deployment deployment) {
        this.id = deployment.getId();
        this.prNumber = deployment.getPrNumber();
        this.branch = deployment.getBranch();
        this.status = deployment.getStatus();
        this.createdAt = deployment.getCreatedAt();

        // 승인 여부 변환
        this.approvalStatus = convertApprovalStatus(deployment.getStatus());

        // 배포 시간 포맷
        this.deployTime = formatDeployTime(deployment.getCreatedAt());

        // 결과 변환
        this.result = convertResult(deployment.getStatus());
    }

    /**
     * 승인 여부 변환 로직
     */
    private String convertApprovalStatus(DeployStatus status) {
        if (status == null) return "-";

        switch (status) {
            case PENDING:
                return "승인대기";
            case APPROVED:
                return "승인완료";
            case REJECTED:
                return "반려";
            case CANCELED:
                return "취소";
            case SUCCESS:
            case FAILURE:
                return "배포";
            default:
                return "-";
        }
    }

    /**
     * 배포 시간 포맷 (yyyy.MM.dd HH:mm)
     */
    private String formatDeployTime(LocalDateTime dateTime) {
        if (dateTime == null) return "-";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        return dateTime.format(formatter);
    }

    /**
     * 결과 변환 로직
     */
    private String convertResult(DeployStatus status) {
        if (status == null) return "-";

        switch (status) {
            case SUCCESS:
                return "성공";
            case FAILURE:
                return "실패";
            default:
                return "-";
        }
    }

    /**
     * 승인 여부 뱃지 색상 클래스
     * - 프론트엔드에서 사용
     */
    public String getApprovalBadgeClass() {
        if (status == null) return "badge-secondary";

        switch (status) {
            case PENDING:
            case APPROVED:
                return "badge-warning";   // 노란색
            case REJECTED:
                return "badge-danger";    // 분홍색/빨간색
            case CANCELED:
                return "badge-secondary"; // 회색
            case SUCCESS:
            case FAILURE:
                return "badge-primary";   // 파란색 (배포)
            default:
                return "badge-secondary";
        }
    }

    /**
     * 결과 뱃지 색상 클래스
     * - 프론트엔드에서 사용
     */
    public String getResultBadgeClass() {
        if (status == null) return "";

        switch (status) {
            case SUCCESS:
                return "badge-success";  // 초록색
            case FAILURE:
                return "badge-danger";   // 빨간색
            default:
                return "";
        }
    }
}