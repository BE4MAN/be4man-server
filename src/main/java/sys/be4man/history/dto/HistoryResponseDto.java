package sys.be4man.history.dto;

import lombok.*;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.model.type.ProcessingStatus;
import java.time.LocalDateTime;

/**
 * History 페이지 응답 DTO
 * - 화면에 표시할 데이터 형식
 *
 * [핵심 구조]
 * ProcessingStage (처리 단계): PLAN(계획서) / DEPLOYMENT(배포) / REPORT(레포트)
 * ProcessingStatus (처리 상태): 각 단계별로 다른 상태를 가짐
 *   - PLAN: PENDING(대기) / APPROVED(승인) / REJECTED(반려)
 *   - DEPLOYMENT: REJECTED(거절) / COMPLETED(완료)
 *   - REPORT: PENDING(대기) / APPROVED(승인) / REJECTED(반려)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryResponseDto {

    private Long id;                        // 작업 번호
    private String drafter;                 // 기안자
    private String department;              // 부서
    private String serviceName;             // 서비스명
    private String workTitle;               // 작업 제목

    private ProcessingStage processingStage;    // 처리 단계 (PLAN/DEPLOYMENT/REPORT)
    private ProcessingStatus processingStatus;  // 처리 상태 (단계별로 다름)
    private String deploymentResult;            // 배포 결과 (성공/실패/-)

    private LocalDateTime createdAt;        // 생성 일시

    /**
     * Deployment 엔티티로부터 DTO 생성
     *
     * [매핑 로직]
     * 1. DeploymentStatus (배포 작업 상태) → 처리 단계 결정
     * 2. ReportStatus (보고서 상태) → 처리 상태 결정
     * 3. 두 상태를 조합하여 최종 ProcessingStage & ProcessingStatus 도출
     */
    public HistoryResponseDto(Deployment deployment) {
        this.id = deployment.getId();
        this.drafter = deployment.getDrafter();
        this.department = deployment.getDepartment();
        this.serviceName = deployment.getServiceName();
        this.workTitle = deployment.getWorkTitle();

        // [핵심] 처리 단계와 상태를 Deployment의 두 가지 상태값으로부터 매핑
        this.processingStage = determineProcessingStage(
            deployment.getDeploymentStatus(),
            deployment.getReportStatus()
        );

        this.processingStatus = determineProcessingStatus(
            deployment.getDeploymentStatus(),
            deployment.getReportStatus(),
            this.processingStage
        );

        // 배포 결과 변환
        this.deploymentResult = convertDeploymentResult(deployment.getDeploymentStatus());

        this.createdAt = deployment.getCreatedAt();
    }

    /**
     * 처리 단계 결정 (PLAN / DEPLOYMENT / REPORT)
     *
     * [로직]
     * 1. STAGED, PENDING(배포 대기), APPROVED(배포 승인) → PLAN (계획서 단계)
     * 2. DEPLOYMENT(배포 중), SUCCESS, FAILURE → DEPLOYMENT (배포 단계)
     * 3. COMPLETED(배포 완료), REPORT_STATUS 존재 → REPORT (레포트 단계)
     */
    private ProcessingStage determineProcessingStage(
        DeploymentStatus deploymentStatus,
        ReportStatus reportStatus
    ) {
        if (deploymentStatus == null) {
            return null;
        }

        switch (deploymentStatus) {
            // 배포 전 단계: 계획서
            case PENDING:
            case APPROVED:
            case REJECTED:
            case CANCELED:
                return ProcessingStage.PLAN;

            // 배포 완료 후: 결과에 따라 분류
            case SUCCESS:
            case FAILURE:
                // ReportStatus가 있으면 REPORT 단계, 없으면 DEPLOYMENT 단계
                return (reportStatus != null)
                    ? ProcessingStage.REPORT
                    : ProcessingStage.DEPLOYMENT;

            default:
                return null;
        }
    }

    /**
     * 처리 상태 결정 (단계별로 다름)
     *
     * [PLAN 단계 (계획서)]
     *   - PENDING → PENDING (대기)
     *   - APPROVED → APPROVED (승인)
     *   - REJECTED/CANCELED → REJECTED (반려)
     *
     * [DEPLOYMENT 단계 (배포)]
     *   - REJECTED(배포 직전 거절) → REJECTED (거절)
     *   - SUCCESS/FAILURE(배포 실행 완료) → COMPLETED (완료)
     *
     * [REPORT 단계 (레포트)]
     *   - ReportStatus.PENDING → PENDING (대기)
     *   - ReportStatus.APPROVED → APPROVED (승인)
     *   - ReportStatus.REJECTED → REJECTED (반려)
     */
    private ProcessingStatus determineProcessingStatus(
        DeploymentStatus deploymentStatus,
        ReportStatus reportStatus,
        ProcessingStage processingStage
    ) {
        if (deploymentStatus == null || processingStage == null) {
            return null;
        }

        // [PLAN 단계] 배포 승인 대기 상태
        if (processingStage == ProcessingStage.PLAN) {
            switch (deploymentStatus) {
                case PENDING:
                    return ProcessingStatus.PENDING;     // 대기
                case APPROVED:
                    return ProcessingStatus.APPROVED;    // 승인
                case REJECTED:
                case CANCELED:
                    return ProcessingStatus.REJECTED;    // 반려
                default:
                    return ProcessingStatus.PENDING;
            }
        }

        // [DEPLOYMENT 단계] 배포 실행 상태
        if (processingStage == ProcessingStage.DEPLOYMENT) {
            switch (deploymentStatus) {
                case REJECTED:
                    return ProcessingStatus.REJECTED;    // 거절 (배포 전 관리자 거절)
                case SUCCESS:
                case FAILURE:
                    return ProcessingStatus.COMPLETED;   // 완료 (배포 실행 완료)
                default:
                    return ProcessingStatus.PENDING;
            }
        }

        // [REPORT 단계] 보고서 승인 상태
        if (processingStage == ProcessingStage.REPORT) {
            if (reportStatus == null) {
                return ProcessingStatus.PENDING;
            }

            switch (reportStatus) {
                case PENDING:
                    return ProcessingStatus.PENDING;     // 대기
                case APPROVED:
                    return ProcessingStatus.APPROVED;    // 승인
                case REJECTED:
                    return ProcessingStatus.REJECTED;    // 반려
                default:
                    return ProcessingStatus.PENDING;
            }
        }

        return null;
    }

    /**
     * 배포 결과 변환
     * DeploymentStatus를 화면 표시용 문자열로 변환
     */
    private String convertDeploymentResult(DeploymentStatus deploymentStatus) {
        if (deploymentStatus == null) {
            return "-";
        }

        switch (deploymentStatus) {
            case SUCCESS:
                return "성공";
            case FAILURE:
                return "실패";
            default:
                return "-";
        }
    }
}
