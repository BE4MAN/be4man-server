package sys.be4man.domains.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.model.type.JobDepartment;
import sys.be4man.domains.account.model.type.JobPosition;
import sys.be4man.domains.account.model.type.Role;
import sys.be4man.domains.approval.model.entity.Approval;
import sys.be4man.domains.approval.model.entity.ApprovalLine;
import sys.be4man.domains.approval.model.type.ApprovalStatus;
import sys.be4man.domains.approval.model.type.ApprovalType;
import sys.be4man.domains.approval.repository.ApprovalLineRepository;
import sys.be4man.domains.dashboard.dto.response.PendingApprovalResponse;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.repository.RelatedProjectRepository;
import sys.be4man.domains.pullrequest.model.entity.PullRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService - 승인 대기 목록 조회 테스트")
class DashboardServiceGetPendingApprovalsTest {

    @Mock
    private ApprovalLineRepository approvalLineRepository;

    @Mock
    private RelatedProjectRepository relatedProjectRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Account testAccount;
    private Account approverAccount;
    private Project testProject;
    private PullRequest testPullRequest;
    private Deployment testDeployment;
    private Approval testApproval;
    private ApprovalLine testApprovalLine;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .githubId(12345L)
                .name("홍길동")
                .email("hong@example.com")
                .role(Role.DEVELOPER)
                .position(JobPosition.STAFF)
                .department(JobDepartment.IT)
                .githubAccessToken("test-token")
                .build();
        ReflectionTestUtils.setField(testAccount, "id", 1L);

        approverAccount = Account.builder()
                .githubId(67890L)
                .name("김리뷰")
                .email("kim@example.com")
                .role(Role.MANAGER)
                .position(JobPosition.STAFF)
                .department(JobDepartment.IT)
                .githubAccessToken("test-token")
                .build();
        ReflectionTestUtils.setField(approverAccount, "id", 2L);

        testProject = Project.builder()
                .manager(testAccount)
                .name("결제 서비스")
                .isRunning(true)
                .jenkinsIp("192.168.1.1")
                .build();
        ReflectionTestUtils.setField(testProject, "id", 1L);

        testPullRequest = PullRequest.builder()
                .prNumber(123)
                .repositoryUrl("https://github.com/test/test-repo")
                .branch("feature-branch")
                .build();
        ReflectionTestUtils.setField(testPullRequest, "id", 1L);

        testDeployment = Deployment.builder()
                .project(testProject)
                .issuer(testAccount)
                .pullRequest(testPullRequest)
                .title("결제 서비스 배포 작업 계획서")
                .content("결제 서비스 신규 기능 배포를 위한 작업 계획서입니다.")
                .stage(DeploymentStage.PLAN)
                .status(DeploymentStatus.PENDING)
                .scheduledAt(LocalDateTime.of(2025, 10, 30, 16, 0))
                .build();
        ReflectionTestUtils.setField(testDeployment, "id", 1L);
        ReflectionTestUtils.setField(testDeployment, "createdAt", LocalDateTime.of(2025, 10, 27, 10, 30));

        testApproval = Approval.builder()
                .deployment(testDeployment)
                .account(testAccount)
                .type(ApprovalType.PLAN)
                .title("결제 서비스 배포 작업 계획서")
                .content("결제 서비스 신규 기능 배포를 위한 작업 계획서입니다.")
                .status(ApprovalStatus.PENDING)
                .service("결제 서비스")
                .build();
        ReflectionTestUtils.setField(testApproval, "id", 101L);
        ReflectionTestUtils.setField(testApproval, "createdAt", LocalDateTime.of(2025, 10, 27, 10, 30));

        testApprovalLine = ApprovalLine.builder()
                .approval(testApproval)
                .account(approverAccount)
                .comment("")
                .isApproved(null)
                .build();
        ReflectionTestUtils.setField(testApprovalLine, "id", 1L);
    }

    @Test
    @DisplayName("승인 대기 목록 조회 - 성공")
    void getPendingApprovals_Success() {
        // given
        Long accountId = 2L; // approverAccount ID
        List<ApprovalLine> approvalLines = Arrays.asList(testApprovalLine);

        when(approvalLineRepository.findPendingApprovalsByAccountId(accountId))
                .thenReturn(approvalLines);
        when(approvalLineRepository.findByApprovalIdIn(Arrays.asList(101L)))
                .thenReturn(approvalLines);
        when(relatedProjectRepository.findByProjectIdIn(Arrays.asList(1L)))
                .thenReturn(List.of());

        // when
        List<PendingApprovalResponse> response = dashboardService.getPendingApprovals(accountId);

        // then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);

        PendingApprovalResponse first = response.get(0);
        assertThat(first.id()).isEqualTo(101L);
        assertThat(first.title()).isEqualTo("결제 서비스 배포 작업 계획서");
        assertThat(first.docType()).isEqualTo("작업계획서");
        assertThat(first.serviceName()).containsExactly("결제 서비스");
        assertThat(first.requestedAt()).isEqualTo(LocalDateTime.of(2025, 10, 27, 10, 30));
        assertThat(first.currentApprover()).containsExactly("김리뷰");
        assertThat(first.registrant()).isEqualTo("홍길동");
        assertThat(first.registrantDepartment()).isEqualTo("IT");
        assertThat(first.description()).isEqualTo("결제 서비스 신규 기능 배포를 위한 작업 계획서입니다.");
        assertThat(first.status()).isEqualTo("승인 대기");

        // Deployment 정보 검증
        assertThat(first.deployment()).isNotNull();
        assertThat(first.deployment().id()).isEqualTo(1L);
        assertThat(first.deployment().title()).isEqualTo("결제 서비스 배포 작업 계획서");
        assertThat(first.deployment().status()).isEqualTo("PENDING");
        assertThat(first.deployment().stage()).isEqualTo("PLAN");
        assertThat(first.deployment().projectName()).isEqualTo("결제 서비스");
        assertThat(first.deployment().scheduledDate()).isEqualTo(java.time.LocalDate.of(2025, 10, 30));
        assertThat(first.deployment().scheduledTime()).isEqualTo(java.time.LocalTime.of(16, 0));
        assertThat(first.deployment().registrant()).isEqualTo("홍길동");
        assertThat(first.deployment().registrantDepartment()).isEqualTo("IT");
        assertThat(first.deployment().relatedServices()).isEmpty();
    }

    @Test
    @DisplayName("승인 대기 목록 조회 - 빈 목록 반환")
    void getPendingApprovals_Empty() {
        // given
        Long accountId = 2L;

        when(approvalLineRepository.findPendingApprovalsByAccountId(accountId))
                .thenReturn(List.of());

        // when
        List<PendingApprovalResponse> response = dashboardService.getPendingApprovals(accountId);

        // then
        assertThat(response).isNotNull();
        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("승인 대기 목록 조회 - is_approved가 NULL인 항목만 반환")
    void getPendingApprovals_OnlyNullIsApproved() {
        // given
        Long accountId = 2L;

        // is_approved가 true인 ApprovalLine (제외되어야 함)
        ApprovalLine approvedLine = ApprovalLine.builder()
                .approval(testApproval)
                .account(approverAccount)
                .comment("")
                .isApproved(true)
                .build();
        ReflectionTestUtils.setField(approvedLine, "id", 2L);

        // is_approved가 NULL인 ApprovalLine (포함되어야 함)
        List<ApprovalLine> approvalLines = Arrays.asList(testApprovalLine);

        when(approvalLineRepository.findPendingApprovalsByAccountId(accountId))
                .thenReturn(approvalLines);
        when(approvalLineRepository.findByApprovalIdIn(Arrays.asList(101L)))
                .thenReturn(approvalLines);
        when(relatedProjectRepository.findByProjectIdIn(Arrays.asList(1L)))
                .thenReturn(List.of());

        // when
        List<PendingApprovalResponse> response = dashboardService.getPendingApprovals(accountId);

        // then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        // is_approved가 NULL인 항목만 반환되었는지 확인
        assertThat(response.get(0).id()).isEqualTo(101L);
    }

    @Test
    @DisplayName("승인 대기 목록 조회 - deployment.status가 PENDING 또는 APPROVED인 항목만 반환")
    void getPendingApprovals_OnlyPendingOrApprovedDeployment() {
        // given
        Long accountId = 2L;

        // APPROVED 상태의 Deployment
        Deployment approvedDeployment = Deployment.builder()
                .project(testProject)
                .issuer(testAccount)
                .pullRequest(testPullRequest)
                .title("승인된 배포")
                .content("내용")
                .stage(DeploymentStage.PLAN)
                .status(DeploymentStatus.APPROVED)
                .scheduledAt(LocalDateTime.of(2025, 10, 30, 16, 0))
                .build();
        ReflectionTestUtils.setField(approvedDeployment, "id", 2L);
        ReflectionTestUtils.setField(approvedDeployment, "createdAt", LocalDateTime.of(2025, 10, 27, 10, 30));

        Approval approvedApproval = Approval.builder()
                .deployment(approvedDeployment)
                .account(testAccount)
                .type(ApprovalType.PLAN)
                .title("승인된 배포")
                .content("내용")
                .status(ApprovalStatus.APPROVED)
                .service("결제 서비스")
                .build();
        ReflectionTestUtils.setField(approvedApproval, "id", 102L);
        ReflectionTestUtils.setField(approvedApproval, "createdAt", LocalDateTime.of(2025, 10, 27, 10, 30));

        ApprovalLine approvedApprovalLine = ApprovalLine.builder()
                .approval(approvedApproval)
                .account(approverAccount)
                .comment("")
                .isApproved(null)
                .build();
        ReflectionTestUtils.setField(approvedApprovalLine, "id", 3L);

        List<ApprovalLine> approvalLines = Arrays.asList(testApprovalLine, approvedApprovalLine);

        when(approvalLineRepository.findPendingApprovalsByAccountId(accountId))
                .thenReturn(approvalLines);
        when(approvalLineRepository.findByApprovalIdIn(Arrays.asList(101L, 102L)))
                .thenReturn(approvalLines);
        when(relatedProjectRepository.findByProjectIdIn(Arrays.asList(1L)))
                .thenReturn(List.of());

        // when
        List<PendingApprovalResponse> response = dashboardService.getPendingApprovals(accountId);

        // then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);
        // PENDING과 APPROVED 상태의 Deployment만 포함되었는지 확인
        assertThat(response).extracting(r -> r.deployment().status())
                .containsExactlyInAnyOrder("PENDING", "APPROVED");
    }
}

