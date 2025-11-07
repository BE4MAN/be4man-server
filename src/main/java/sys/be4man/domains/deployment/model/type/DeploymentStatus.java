package sys.be4man.domains.deployment.model.type;

import lombok.Getter;

/**
 * 배포 작업 상태 종류
 *
 * 작업 흐름:
 * 1. 계획서 단계:
 *    - PENDING(승인대기): 작업 관리 내역에 표시
 *    - 반려 시: 작업 관리 내역에 표시 안 함 (모든 승인자)
 *    - 모든 승인자 승인 시 자동으로 배포 단계로 이동
 *
 * 2. 배포 단계:
 *    - IN_PROGRESS(배포중): scheduledAt 시간에 자동 시작
 *    - COMPLETED(종료): 배포 정상 완료
 *    - CANCELED(취소): 배포 중 취소
 *
 * 3. 결과보고 단계:
 *    - PENDING(승인대기): 작업 관리 내역에 표시
 *    - REJECTED(반려): 누구 한 명이라도 반려 시 모든 승인자의 내역에 표시
 *    - APPROVED(완료): 모든 승인자 승인 시
 */
@Getter
public enum DeploymentStatus {

    PENDING("대기"),      // 계획서/결과보고 승인 대기 중
    APPROVED("승인"),
    REJECTED("반려"),         // 결과보고 반려 (모든 승인자의 내역에 표시)
    CANCELED("취소"),         // 배포 중 취소
    IN_PROGRESS("진행중"),    // 배포 진행 중
    COMPLETED("완료");        // 배포 종료     // 결과보고 최종 완료

    private final String koreanName;

    DeploymentStatus(String koreanName) {
        this.koreanName = koreanName;
    }
}