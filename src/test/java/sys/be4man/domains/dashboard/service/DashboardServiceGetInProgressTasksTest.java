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
import sys.be4man.domains.approval.repository.ApprovalLineRepository;
import sys.be4man.domains.dashboard.dto.response.InProgressTaskResponse;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.repository.RelatedProjectRepository;
import sys.be4man.domains.pullrequest.model.entity.PullRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService - 진행중인 업무 목록 조회 테스트")
class DashboardServiceGetInProgressTasksTest {

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
    private Deployment planApprovedDeployment;
    private Deployment deploymentPendingDeployment;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .githubId(12345L)
                .name("김민호")
                .email("kim@example.com")
                .role(Role.DEVELOPER)
                .position(JobPosition.STAFF)
                .department(JobDepartment.IT)
                .githubAccessToken("test-token")
                .build();
        ReflectionTestUtils.setField(testAccount, "id", 1L);

        approverAccount = Account.builder()
                .githubId(67890L)
                .name("승인자")
                .email("approver@example.com")
                .role(Role.MANAGER)
                .position(JobPosition.STAFF)
                .department(JobDepartment.IT)
                .githubAccessToken("test-token")
                .build();
        ReflectionTestUtils.setField(approverAccount, "id", 2L);

        testProject = Project.builder()
                .manager(testAccount)
                .name("사용자 서비스")
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

        // PLAN + APPROVED
        planApprovedDeployment = Deployment.builder()
                .project(testProject)
                .issuer(testAccount)
                .pullRequest(testPullRequest)
                .title("사용자 서비스 신규 배포")
                .content("승인은 완료되었고, 배포 시간까지 대기 중입니다.")
                .stage(DeploymentStage.PLAN)
                .status(DeploymentStatus.APPROVED)
                .scheduledAt(LocalDateTime.of(2025, 10, 30, 16, 0))
                .build();
        ReflectionTestUtils.setField(planApprovedDeployment, "id", 201L);
        ReflectionTestUtils.setField(planApprovedDeployment, "updatedAt", LocalDateTime.of(2025, 10, 28, 10, 0));

        // DEPLOYMENT + PENDING
        deploymentPendingDeployment = Deployment.builder()
                .project(testProject)
                .issuer(testAccount)
                .pullRequest(testPullRequest)
                .title("배포 대기 중인 작업")
                .content("배포 대기 중입니다.")
                .stage(DeploymentStage.DEPLOYMENT)
                .status(DeploymentStatus.PENDING)
                .scheduledAt(LocalDateTime.of(2025, 10, 31, 14, 0))
                .build();
        ReflectionTestUtils.setField(deploymentPendingDeployment, "id", 202L);
        ReflectionTestUtils.setField(deploymentPendingDeployment, "updatedAt", LocalDateTime.of(2025, 10, 29, 11, 0));

    }

    @Test
    @DisplayName("진행중인 업무 목록 조회 - 성공 (모든 stage-status 조합 포함)")
    void getInProgressTasks_Success() {
        // given
        Long accountId = 2L; // approverAccount ID
        List<Deployment> deployments = Arrays.asList(
                deploymentPendingDeployment, // updatedAt이 가장 최신
                planApprovedDeployment
        );

        when(approvalLineRepository.findInProgressTasksByAccountId(accountId))
                .thenReturn(deployments);
        when(relatedProjectRepository.findByProjectIdIn(Arrays.asList(1L)))
                .thenReturn(List.of());

        // when
        List<InProgressTaskResponse> response = dashboardService.getInProgressTasks(accountId);

        // then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);

        // updatedAt DESC 정렬 확인 (가장 최신이 첫 번째)
        InProgressTaskResponse first = response.get(0);
        assertThat(first.id()).isEqualTo(202L);
        assertThat(first.title()).isEqualTo("배포 대기 중인 작업");
        assertThat(first.status()).isEqualTo("PENDING");
        assertThat(first.stage()).isEqualTo("DEPLOYMENT");
        assertThat(first.service()).isEqualTo("사용자 서비스");
        assertThat(first.registrant()).isEqualTo("김민호");
        assertThat(first.registrantDepartment()).isEqualTo("IT");
        assertThat(first.description()).isEqualTo("배포 대기 중입니다.");
        assertThat(first.relatedServices()).containsExactly("사용자 서비스");
        assertThat(first.date()).isEqualTo(java.time.LocalDate.of(2025, 10, 31));
        assertThat(first.scheduledTime()).isEqualTo(java.time.LocalTime.of(14, 0));

        // 두 번째 항목
        InProgressTaskResponse second = response.get(1);
        assertThat(second.id()).isEqualTo(201L);
        assertThat(second.status()).isEqualTo("APPROVED");
        assertThat(second.stage()).isEqualTo("PLAN");
    }

    @Test
    @DisplayName("진행중인 업무 목록 조회 - 빈 목록 반환")
    void getInProgressTasks_Empty() {
        // given
        Long accountId = 2L;

        when(approvalLineRepository.findInProgressTasksByAccountId(accountId))
                .thenReturn(List.of());

        // when
        List<InProgressTaskResponse> response = dashboardService.getInProgressTasks(accountId);

        // then
        assertThat(response).isNotNull();
        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("진행중인 업무 목록 조회 - stage-status 조합 필터링 검증")
    void getInProgressTasks_StageStatusFiltering() {
        // given
        Long accountId = 2L;

        // 허용되는 조합들만 포함
        List<Deployment> deployments = Arrays.asList(
                planApprovedDeployment,        // PLAN + APPROVED
                deploymentPendingDeployment    // DEPLOYMENT + PENDING
        );

        when(approvalLineRepository.findInProgressTasksByAccountId(accountId))
                .thenReturn(deployments);
        when(relatedProjectRepository.findByProjectIdIn(Arrays.asList(1L)))
                .thenReturn(List.of());

        // when
        List<InProgressTaskResponse> response = dashboardService.getInProgressTasks(accountId);

        // then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);

        // 각 조합이 포함되었는지 확인
        assertThat(response).extracting(InProgressTaskResponse::stage)
                .containsExactlyInAnyOrder("PLAN", "DEPLOYMENT");
        assertThat(response).extracting(InProgressTaskResponse::status)
                .containsExactlyInAnyOrder("APPROVED", "PENDING");
    }

    @Test
    @DisplayName("진행중인 업무 목록 조회 - 정렬 순서 검증 (updatedAt DESC)")
    void getInProgressTasks_SortOrder() {
        // given
        Long accountId = 2L;

        // updatedAt이 다른 순서로 설정
        ReflectionTestUtils.setField(planApprovedDeployment, "updatedAt", LocalDateTime.of(2025, 10, 27, 10, 0));
        ReflectionTestUtils.setField(deploymentPendingDeployment, "updatedAt", LocalDateTime.of(2025, 10, 28, 11, 0));

        List<Deployment> deployments = Arrays.asList(
                deploymentPendingDeployment, // 가장 최신
                planApprovedDeployment      // 가장 오래됨
        );

        when(approvalLineRepository.findInProgressTasksByAccountId(accountId))
                .thenReturn(deployments);
        when(relatedProjectRepository.findByProjectIdIn(Arrays.asList(1L)))
                .thenReturn(List.of());

        // when
        List<InProgressTaskResponse> response = dashboardService.getInProgressTasks(accountId);

        // then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);

        // updatedAt DESC 순서 확인
        assertThat(response.get(0).id()).isEqualTo(202L); // 가장 최신
        assertThat(response.get(1).id()).isEqualTo(201L); // 가장 오래됨
    }
}

