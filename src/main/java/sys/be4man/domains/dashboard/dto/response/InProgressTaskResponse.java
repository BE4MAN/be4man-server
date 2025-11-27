// 작성자 : 이원석
package sys.be4man.domains.dashboard.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 진행중인 업무 목록 응답 DTO
 *
 * @param id                   Deployment ID
 * @param title                배포 제목
 * @param date                 예정 날짜 (YYYY-MM-DD)
 * @param scheduledTime        예정 시간 (HH:mm)
 * @param status               Deployment 상태 (APPROVED, IN_PROGRESS)
 * @param stage                Deployment 단계 (PLAN, DEPLOYMENT)
 * @param isDeployed           Jenkins 배포 성공 여부 (null: 배포 전, true: 성공, false: 실패)
 * @param service              서비스 이름 (projectName)
 * @param registrant           등록자 이름
 * @param registrantDepartment 등록 부서명
 * @param description          설명
 * @param relatedServices      연관 서비스 이름 배열
 */
public record InProgressTaskResponse(
        Long id,
        String title,
        LocalDate date,
        LocalTime scheduledTime,
        String status,
        String stage,
        Boolean isDeployed,
        String service,
        String registrant,
        String registrantDepartment,
        String description,
        List<String> relatedServices
) {
}

