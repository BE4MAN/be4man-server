package sys.be4man.domains.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import sys.be4man.domains.analysis.model.entity.BuildRun;
import sys.be4man.domains.analysis.repository.BuildRunRepository;
import sys.be4man.domains.approval.repository.ApprovalRepository;
import sys.be4man.domains.dashboard.dto.response.PaginationResponse;
import sys.be4man.domains.dashboard.dto.response.RecoveryResponse;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.pullrequest.model.entity.PullRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService - 복구현황 목록 조회 테스트")
class DashboardServiceGetRecoveryTest {

    @Mock
    private ApprovalRepository approvalRepository;

    @Mock
    private BuildRunRepository buildRunRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Account testAccount;
    private Project testProject;
    private PullRequest testPullRequest;
    private Deployment completedDeployment;
    private Deployment inProgressDeployment;
    private Deployment pendingDeployment;
    private BuildRun firstBuildRun;
    private BuildRun lastBuildRun;

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

        // COMPLETED 상태
        completedDeployment = Deployment.builder()
                .project(testProject)
                .issuer(testAccount)
                .pullRequest(testPullRequest)
                .title("결제 서비스 DB 마이그레이션 작업")
                .content("DB 마이그레이션 작업")
                .stage(DeploymentStage.ROLLBACK)
                .status(DeploymentStatus.COMPLETED)
                .isDeployed(true)
                .build();
        ReflectionTestUtils.setField(completedDeployment, "id", 201L);
        ReflectionTestUtils.setField(completedDeployment, "createdAt", LocalDateTime.of(2025, 10, 29, 15, 0));
        ReflectionTestUtils.setField(completedDeployment, "updatedAt", LocalDateTime.of(2025, 10, 29, 16, 10));

        // IN_PROGRESS 상태
        inProgressDeployment = Deployment.builder()
                .project(testProject)
                .issuer(testAccount)
                .pullRequest(testPullRequest)
                .title("진행중인 복구 작업")
                .content("복구 작업 진행 중")
                .stage(DeploymentStage.ROLLBACK)
                .status(DeploymentStatus.IN_PROGRESS)
                .build();
        ReflectionTestUtils.setField(inProgressDeployment, "id", 202L);
        ReflectionTestUtils.setField(inProgressDeployment, "createdAt", LocalDateTime.of(2025, 10, 28, 10, 0));
        ReflectionTestUtils.setField(inProgressDeployment, "updatedAt", LocalDateTime.of(2025, 10, 28, 11, 0));

        // PENDING 상태
        pendingDeployment = Deployment.builder()
                .project(testProject)
                .issuer(testAccount)
                .pullRequest(testPullRequest)
                .title("대기중인 복구 작업")
                .content("복구 작업 대기 중")
                .stage(DeploymentStage.ROLLBACK)
                .status(DeploymentStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(pendingDeployment, "id", 203L);
        ReflectionTestUtils.setField(pendingDeployment, "createdAt", LocalDateTime.of(2025, 10, 27, 9, 0));
        ReflectionTestUtils.setField(pendingDeployment, "updatedAt", LocalDateTime.of(2025, 10, 27, 9, 30));

        // BuildRun 설정 (COMPLETED용)
        firstBuildRun = BuildRun.builder()
                .deployment(completedDeployment)
                .jenkinsJobName("test-job")
                .buildNumber(1L)
                .log("Build log")
                .duration(1000L) // 1초 (밀리초)
                .startedAt(LocalDateTime.of(2025, 10, 29, 15, 22))
                .endedAt(LocalDateTime.of(2025, 10, 29, 15, 30))
                .build();
        ReflectionTestUtils.setField(firstBuildRun, "id", 1L);

        lastBuildRun = BuildRun.builder()
                .deployment(completedDeployment)
                .jenkinsJobName("test-job")
                .buildNumber(2L)
                .log("Build log")
                .duration(120000L) // 120초 (밀리초) = 2분
                .startedAt(LocalDateTime.of(2025, 10, 29, 15, 50))
                .endedAt(LocalDateTime.of(2025, 10, 29, 16, 4))
                .build();
        ReflectionTestUtils.setField(lastBuildRun, "id", 2L);
    }

    @Test
    @DisplayName("복구현황 목록 조회 성공 - COMPLETED 상태")
    void getRecovery_Completed_Success() {
        // given
        int page = 1;
        int pageSize = 5;
        List<Deployment> deployments = List.of(completedDeployment);
        List<BuildRun> buildRuns = List.of(firstBuildRun, lastBuildRun);

        when(approvalRepository.countRollbackDeployments()).thenReturn(1L);
        when(approvalRepository.findRollbackDeployments(0, pageSize)).thenReturn(deployments);
        when(buildRunRepository.findByDeploymentIdIn(List.of(201L))).thenReturn(buildRuns);

        // when
        PaginationResponse<RecoveryResponse> result = dashboardService.getRecovery(page, pageSize);

        // then
        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(1);
        assertThat(result.pagination().total()).isEqualTo(1L);
        assertThat(result.pagination().page()).isEqualTo(1);
        assertThat(result.pagination().pageSize()).isEqualTo(5);
        assertThat(result.pagination().totalPages()).isEqualTo(1);

        RecoveryResponse recovery = result.data().get(0);
        assertThat(recovery.id()).isEqualTo(201L);
        assertThat(recovery.title()).isEqualTo("결제 서비스 DB 마이그레이션 작업");
        assertThat(recovery.service()).isEqualTo("결제 서비스");
        assertThat(recovery.status()).isEqualTo("COMPLETED");
        assertThat(recovery.duration()).isEqualTo("42분"); // 15:22 ~ 16:04 = 42분
        assertThat(recovery.buildRunDuration()).isEqualTo(120); // 마지막 BuildRun의 duration: 120000ms / 1000 = 120초
        assertThat(recovery.recoveredAt()).isEqualTo(completedDeployment.getUpdatedAt());
        assertThat(recovery.updatedAt()).isEqualTo(completedDeployment.getUpdatedAt());
        assertThat(recovery.registrant()).isEqualTo("홍길동");
        assertThat(recovery.registrantDepartment()).isEqualTo("IT");
        assertThat(recovery.deploymentId()).isEqualTo(201L);
    }

    @Test
    @DisplayName("복구현황 목록 조회 성공 - IN_PROGRESS 상태")
    void getRecovery_InProgress_Success() {
        // given
        int page = 1;
        int pageSize = 5;
        List<Deployment> deployments = List.of(inProgressDeployment);

        when(approvalRepository.countRollbackDeployments()).thenReturn(1L);
        when(approvalRepository.findRollbackDeployments(0, pageSize)).thenReturn(deployments);
        when(buildRunRepository.findByDeploymentIdIn(List.of(202L))).thenReturn(List.of());

        // when
        PaginationResponse<RecoveryResponse> result = dashboardService.getRecovery(page, pageSize);

        // then
        assertThat(result.data()).hasSize(1);
        RecoveryResponse recovery = result.data().get(0);
        assertThat(recovery.status()).isEqualTo("IN_PROGRESS");
        assertThat(recovery.duration()).isNull();
        assertThat(recovery.buildRunDuration()).isNull();
        assertThat(recovery.recoveredAt()).isEqualTo(inProgressDeployment.getUpdatedAt());
        assertThat(recovery.updatedAt()).isEqualTo(inProgressDeployment.getUpdatedAt());
    }

    @Test
    @DisplayName("복구현황 목록 조회 성공 - PENDING 상태")
    void getRecovery_Pending_Success() {
        // given
        int page = 1;
        int pageSize = 5;
        List<Deployment> deployments = List.of(pendingDeployment);

        when(approvalRepository.countRollbackDeployments()).thenReturn(1L);
        when(approvalRepository.findRollbackDeployments(0, pageSize)).thenReturn(deployments);
        when(buildRunRepository.findByDeploymentIdIn(List.of(203L))).thenReturn(List.of());

        // when
        PaginationResponse<RecoveryResponse> result = dashboardService.getRecovery(page, pageSize);

        // then
        assertThat(result.data()).hasSize(1);
        RecoveryResponse recovery = result.data().get(0);
        assertThat(recovery.status()).isEqualTo("PENDING");
        assertThat(recovery.duration()).isNull();
        assertThat(recovery.buildRunDuration()).isNull();
        assertThat(recovery.recoveredAt()).isEqualTo(pendingDeployment.getUpdatedAt());
        assertThat(recovery.updatedAt()).isEqualTo(pendingDeployment.getUpdatedAt());
    }

    @Test
    @DisplayName("복구현황 목록 조회 성공 - 모든 상태 포함")
    void getRecovery_AllStatuses_Success() {
        // given
        int page = 1;
        int pageSize = 5;
        List<Deployment> deployments = List.of(
                completedDeployment, // 최신 (createdAt: 2025-10-29)
                inProgressDeployment, // 중간 (createdAt: 2025-10-28)
                pendingDeployment // 오래됨 (createdAt: 2025-10-27)
        );
        List<BuildRun> buildRuns = List.of(firstBuildRun, lastBuildRun);

        when(approvalRepository.countRollbackDeployments()).thenReturn(3L);
        when(approvalRepository.findRollbackDeployments(0, pageSize)).thenReturn(deployments);
        when(buildRunRepository.findByDeploymentIdIn(any())).thenReturn(buildRuns);

        // when
        PaginationResponse<RecoveryResponse> result = dashboardService.getRecovery(page, pageSize);

        // then
        assertThat(result.data()).hasSize(3);
        assertThat(result.pagination().total()).isEqualTo(3L);
        assertThat(result.pagination().totalPages()).isEqualTo(1);

        // 정렬 확인: createdAt DESC (최신순)
        assertThat(result.data().get(0).id()).isEqualTo(201L); // COMPLETED
        assertThat(result.data().get(1).id()).isEqualTo(202L); // IN_PROGRESS
        assertThat(result.data().get(2).id()).isEqualTo(203L); // PENDING
    }

    @Test
    @DisplayName("복구현황 목록 조회 성공 - 페이지네이션")
    void getRecovery_Pagination_Success() {
        // given
        int page = 2;
        int pageSize = 2;
        List<Deployment> deployments = List.of(pendingDeployment); // 3개 중 2페이지

        when(approvalRepository.countRollbackDeployments()).thenReturn(3L);
        when(approvalRepository.findRollbackDeployments(2, pageSize)).thenReturn(deployments);
        when(buildRunRepository.findByDeploymentIdIn(List.of(203L))).thenReturn(List.of());

        // when
        PaginationResponse<RecoveryResponse> result = dashboardService.getRecovery(page, pageSize);

        // then
        assertThat(result.data()).hasSize(1);
        assertThat(result.pagination().total()).isEqualTo(3L);
        assertThat(result.pagination().page()).isEqualTo(2);
        assertThat(result.pagination().pageSize()).isEqualTo(2);
        assertThat(result.pagination().totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("복구현황 목록 조회 - 빈 리스트")
    void getRecovery_Empty_Success() {
        // given
        int page = 1;
        int pageSize = 5;

        when(approvalRepository.countRollbackDeployments()).thenReturn(0L);
        when(approvalRepository.findRollbackDeployments(0, pageSize)).thenReturn(List.of());

        // when
        PaginationResponse<RecoveryResponse> result = dashboardService.getRecovery(page, pageSize);

        // then
        assertThat(result.data()).isEmpty();
        assertThat(result.pagination().total()).isEqualTo(0L);
        assertThat(result.pagination().totalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("복구현황 목록 조회 - COMPLETED 상태이지만 BuildRun이 없는 경우")
    void getRecovery_CompletedWithoutBuildRuns_Success() {
        // given
        int page = 1;
        int pageSize = 5;
        List<Deployment> deployments = List.of(completedDeployment);

        when(approvalRepository.countRollbackDeployments()).thenReturn(1L);
        when(approvalRepository.findRollbackDeployments(0, pageSize)).thenReturn(deployments);
        when(buildRunRepository.findByDeploymentIdIn(List.of(201L))).thenReturn(List.of());

        // when
        PaginationResponse<RecoveryResponse> result = dashboardService.getRecovery(page, pageSize);

        // then
        assertThat(result.data()).hasSize(1);
        RecoveryResponse recovery = result.data().get(0);
        assertThat(recovery.status()).isEqualTo("COMPLETED");
        assertThat(recovery.duration()).isNull(); // BuildRun이 없으면 duration도 null
        assertThat(recovery.buildRunDuration()).isNull(); // BuildRun이 없으면 buildRunDuration도 null
        assertThat(recovery.recoveredAt()).isEqualTo(completedDeployment.getUpdatedAt());
        assertThat(recovery.updatedAt()).isEqualTo(completedDeployment.getUpdatedAt());
    }
}

