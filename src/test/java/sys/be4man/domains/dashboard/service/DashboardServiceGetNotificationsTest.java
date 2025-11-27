// 작성자 : 이원석
package sys.be4man.domains.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.model.type.JobPosition;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.entity.ApprovalLine;
import sys.be4man.domains.approval.model.type.ApprovalStatus;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.approval.repository.ApprovalLineRepository;
import sys.be4man.domains.approval.repository.ApprovalRepository;
import sys.be4man.domains.dashboard.dto.response.NotificationResponse;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.repository.RelatedProjectRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService - 알림 목록 조회 테스트")
class DashboardServiceGetNotificationsTest {

    @Mock
    private ApprovalLineRepository approvalLineRepository;

    @Mock
    private ApprovalRepository approvalRepository;

    @Mock
    private RelatedProjectRepository relatedProjectRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Account testAccount;
    private Project testProject;
    private Deployment canceledDeployment;
    private Deployment rejectedDeploymentByIssuer;
    private Deployment rejectedDeploymentByApprover;
    private Approval canceledApproval;
    private Approval rejectedApproval;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .email("test@example.com")
                .name("테스트 사용자")
                .position(JobPosition.STAFF)
                .build();
        ReflectionTestUtils.setField(testAccount, "id", 1L);

        testProject = Project.builder()
                .name("테스트 서비스")
                .build();
        ReflectionTestUtils.setField(testProject, "id", 1L);

        canceledDeployment = Deployment.builder()
                .title("취소된 배포 작업")
                .status(DeploymentStatus.CANCELED)
                .stage(DeploymentStage.PLAN)
                .project(testProject)
                .issuer(testAccount)
                .build();
        ReflectionTestUtils.setField(canceledDeployment, "id", 101L);
        ReflectionTestUtils.setField(canceledDeployment, "updatedAt", LocalDateTime.now().minusHours(2));

        rejectedDeploymentByIssuer = Deployment.builder()
                .title("반려된 배포 작업 (요청자)")
                .status(DeploymentStatus.REJECTED)
                .stage(DeploymentStage.PLAN)
                .project(testProject)
                .issuer(testAccount)
                .build();
        ReflectionTestUtils.setField(rejectedDeploymentByIssuer, "id", 102L);
        ReflectionTestUtils.setField(rejectedDeploymentByIssuer, "updatedAt", LocalDateTime.now().minusHours(1));

        rejectedDeploymentByApprover = Deployment.builder()
                .title("반려된 배포 작업 (승인자)")
                .status(DeploymentStatus.REJECTED)
                .stage(DeploymentStage.PLAN)
                .project(testProject)
                .issuer(testAccount)
                .build();
        ReflectionTestUtils.setField(rejectedDeploymentByApprover, "id", 103L);
        ReflectionTestUtils.setField(rejectedDeploymentByApprover, "updatedAt", LocalDateTime.now());

        canceledApproval = Approval.builder()
                .deployment(canceledDeployment)
                .account(testAccount)
                .type(ApprovalType.DEPLOYMENT)
                .title("취소된 승인")
                .content("내용")
                .status(ApprovalStatus.APPROVED)
                .service("테스트 서비스")
                .build();
        ReflectionTestUtils.setField(canceledApproval, "id", 1L);

        rejectedApproval = Approval.builder()
                .deployment(rejectedDeploymentByIssuer)
                .account(testAccount)
                .type(ApprovalType.DEPLOYMENT)
                .title("반려된 승인")
                .content("내용")
                .status(ApprovalStatus.REJECTED)
                .service("테스트 서비스")
                .build();
        ReflectionTestUtils.setField(rejectedApproval, "id", 2L);
    }

    @Test
    @DisplayName("취소 알림 조회 성공")
    void getNotifications_Canceled_Success() {
        // given
        Long accountId = testAccount.getId();
        List<Deployment> canceledDeployments = List.of(canceledDeployment);

        when(approvalLineRepository.findCanceledNotificationsByAccountId(accountId))
                .thenReturn(canceledDeployments);
        when(approvalLineRepository.findRejectedDeploymentsByIssuerId(accountId))
                .thenReturn(List.of());
        when(approvalLineRepository.findRejectedApprovalsByApproverId(accountId))
                .thenReturn(List.of());

        // when
        List<NotificationResponse> result = dashboardService.getNotifications(accountId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).kind()).isEqualTo("취소");
        assertThat(result.get(0).deploymentId()).isEqualTo(101L);
        assertThat(result.get(0).canceledAt()).isNotNull();
        assertThat(result.get(0).rejectedAt()).isNull();
    }

    @Test
    @DisplayName("반려 알림 조회 성공 (케이스 1: 요청자가 반려된 경우)")
    void getNotifications_RejectedByIssuer_Success() {
        // given
        Long accountId = testAccount.getId();
        List<Deployment> rejectedDeployments = List.of(rejectedDeploymentByIssuer);

        ApprovalLine rejectedLine = ApprovalLine.builder()
                .approval(rejectedApproval)
                .account(testAccount)
                .isApproved(false)
                .comment("반려 사유: 테스트")
                .build();
        ReflectionTestUtils.setField(rejectedLine, "id", 1L);

        rejectedApproval.addApprovalLine(rejectedLine);

        when(approvalLineRepository.findCanceledNotificationsByAccountId(accountId))
                .thenReturn(List.of());
        when(approvalLineRepository.findRejectedDeploymentsByIssuerId(accountId))
                .thenReturn(rejectedDeployments);
        when(approvalLineRepository.findRejectedApprovalsByApproverId(accountId))
                .thenReturn(List.of());
        when(approvalRepository.findByDeploymentId(102L))
                .thenReturn(List.of(rejectedApproval));
        when(approvalRepository.findByIdWithLines(2L))
                .thenReturn(Optional.of(rejectedApproval));

        // when
        List<NotificationResponse> result = dashboardService.getNotifications(accountId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).kind()).isEqualTo("반려");
        assertThat(result.get(0).deploymentId()).isEqualTo(102L);
        assertThat(result.get(0).reason()).isEqualTo("반려 사유: 테스트");
        assertThat(result.get(0).canceledAt()).isNull();
        assertThat(result.get(0).rejectedAt()).isNotNull();
    }

    @Test
    @DisplayName("반려 알림 조회 성공 (케이스 3: 승인자가 반려된 경우)")
    void getNotifications_RejectedByApprover_Success() {
        // given
        Long accountId = testAccount.getId();
        List<Deployment> rejectedDeployments = List.of(rejectedDeploymentByApprover);

        Approval approvalForApprover = Approval.builder()
                .deployment(rejectedDeploymentByApprover)
                .account(testAccount)
                .type(ApprovalType.DEPLOYMENT)
                .title("승인자가 반려한 승인")
                .content("내용")
                .status(ApprovalStatus.REJECTED)
                .service("테스트 서비스")
                .build();
        ReflectionTestUtils.setField(approvalForApprover, "id", 3L);

        ApprovalLine approvedLine = ApprovalLine.builder()
                .approval(approvalForApprover)
                .account(testAccount)
                .isApproved(true)
                .approvedAt(LocalDateTime.now().minusHours(2))
                .build();
        ReflectionTestUtils.setField(approvedLine, "id", 2L);

        ApprovalLine rejectedLine = ApprovalLine.builder()
                .approval(approvalForApprover)
                .account(testAccount)
                .isApproved(false)
                .comment("승인자가 반려한 사유")
                .approvedAt(LocalDateTime.now().minusHours(1))
                .build();
        ReflectionTestUtils.setField(rejectedLine, "id", 3L);

        approvalForApprover.addApprovalLine(approvedLine);
        approvalForApprover.addApprovalLine(rejectedLine);

        when(approvalLineRepository.findCanceledNotificationsByAccountId(accountId))
                .thenReturn(List.of());
        when(approvalLineRepository.findRejectedDeploymentsByIssuerId(accountId))
                .thenReturn(List.of());
        when(approvalLineRepository.findRejectedApprovalsByApproverId(accountId))
                .thenReturn(rejectedDeployments);
        when(approvalRepository.findByDeploymentId(103L))
                .thenReturn(List.of(approvalForApprover));
        when(approvalRepository.findByIdWithLines(3L))
                .thenReturn(Optional.of(approvalForApprover));

        // when
        List<NotificationResponse> result = dashboardService.getNotifications(accountId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).kind()).isEqualTo("반려");
        assertThat(result.get(0).deploymentId()).isEqualTo(103L);
        assertThat(result.get(0).reason()).isEqualTo("승인자가 반려한 사유");
        assertThat(result.get(0).canceledAt()).isNull();
        assertThat(result.get(0).rejectedAt()).isNotNull();
    }

    @Test
    @DisplayName("모든 알림 조회 성공 (취소 + 반려)")
    void getNotifications_AllTypes_Success() {
        // given
        Long accountId = testAccount.getId();

        ApprovalLine rejectedLine = ApprovalLine.builder()
                .approval(rejectedApproval)
                .account(testAccount)
                .isApproved(false)
                .comment("반려 사유")
                .build();
        ReflectionTestUtils.setField(rejectedLine, "id", 1L);

        rejectedApproval.addApprovalLine(rejectedLine);

        when(approvalLineRepository.findCanceledNotificationsByAccountId(accountId))
                .thenReturn(List.of(canceledDeployment));
        when(approvalLineRepository.findRejectedDeploymentsByIssuerId(accountId))
                .thenReturn(List.of(rejectedDeploymentByIssuer));
        when(approvalLineRepository.findRejectedApprovalsByApproverId(accountId))
                .thenReturn(List.of(rejectedDeploymentByApprover));
        when(approvalRepository.findByDeploymentId(any()))
                .thenReturn(List.of(rejectedApproval));
        when(approvalRepository.findByIdWithLines(any()))
                .thenReturn(Optional.of(rejectedApproval));

        // when
        List<NotificationResponse> result = dashboardService.getNotifications(accountId);

        // then
        assertThat(result).hasSize(3);
        // 정렬 확인: 최신순 (updatedAt DESC)
        assertThat(result.get(0).deploymentId()).isEqualTo(103L); // 가장 최신
        assertThat(result.get(1).deploymentId()).isEqualTo(102L);
        assertThat(result.get(2).deploymentId()).isEqualTo(101L); // 가장 오래됨
    }

    @Test
    @DisplayName("알림이 없는 경우 빈 리스트 반환")
    void getNotifications_Empty_Success() {
        // given
        Long accountId = testAccount.getId();

        when(approvalLineRepository.findCanceledNotificationsByAccountId(accountId))
                .thenReturn(List.of());
        when(approvalLineRepository.findRejectedDeploymentsByIssuerId(accountId))
                .thenReturn(List.of());
        when(approvalLineRepository.findRejectedApprovalsByApproverId(accountId))
                .thenReturn(List.of());

        // when
        List<NotificationResponse> result = dashboardService.getNotifications(accountId);

        // then
        assertThat(result).isEmpty();
    }
}

