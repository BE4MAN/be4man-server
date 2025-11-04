package sys.be4man.domains.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.model.type.DeploymentStage;
import sys.be4man.domains.deployment.model.type.DeploymentStatus;
import sys.be4man.domains.deployment.model.type.DeploymentType;
import sys.be4man.domains.deployment.repository.DeploymentRepository;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.pullrequest.model.entity.PullRequest;
import sys.be4man.domains.schedule.dto.response.DeploymentScheduleResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService - 배포 작업 목록 조회 테스트")
class ScheduleServiceGetDeploymentSchedulesTest {

    @Mock
    private DeploymentRepository deploymentRepository;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private Account testAccount;
    private Project testProject;
    private PullRequest testPullRequest;
    private Deployment deployment1;
    private Deployment deployment2;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .githubId(12345L)
                .name("테스트 계정")
                .email("test@example.com")
                .role(Role.DEVELOPER)
                .position(JobPosition.STAFF)
                .department(JobDepartment.IT)
                .githubAccessToken("test-token")
                .build();
        ReflectionTestUtils.setField(testAccount, "id", 1L);

        testProject = Project.builder()
                .manager(testAccount)
                .name("테스트 프로젝트")
                .isRunning(true)
                .jenkinsIp("192.168.1.1")
                .build();
        ReflectionTestUtils.setField(testProject, "id", 1L);

        testPullRequest = PullRequest.builder()
                .repositoryDefaultBranch("main")
                .repositoryName("test-repo")
                .repositoryUrl("https://github.com/test/test-repo")
                .branch("feature-branch")
                .prNumber(123)
                .filesChanged(5)
                .title("테스트 PR 제목")
                .linesAdded(100)
                .linesRemoved(50)
                .commitCount(3)
                .build();
        ReflectionTestUtils.setField(testPullRequest, "id", 1L);

        deployment1 = Deployment.builder()
                .project(testProject)
                .issuer(testAccount)
                .pullRequest(testPullRequest)
                .title("배포 작업 1")
                .content("배포 내용 1")
                .type(DeploymentType.DEPLOY)
                .stage(DeploymentStage.PLAN)
                .status(DeploymentStatus.PENDING)
                .scheduledAt(LocalDateTime.of(2025, 1, 15, 10, 0))
                .build();
        ReflectionTestUtils.setField(deployment1, "id", 1L);

        deployment2 = Deployment.builder()
                .project(testProject)
                .issuer(testAccount)
                .pullRequest(testPullRequest)
                .title("배포 작업 2")
                .content("배포 내용 2")
                .type(DeploymentType.DEPLOY)
                .stage(DeploymentStage.DEPLOYMENT)
                .status(DeploymentStatus.COMPLETED)
                .isDeployed(true)
                .scheduledAt(LocalDateTime.of(2025, 1, 16, 14, 30))
                .build();
        ReflectionTestUtils.setField(deployment2, "id", 2L);
    }

    @Test
    @DisplayName("배포 작업 목록 조회 - 성공")
    void getDeploymentSchedules_Success() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 15);
        LocalDate endDate = LocalDate.of(2025, 1, 17);
        List<Deployment> deployments = Arrays.asList(deployment1, deployment2);

        when(deploymentRepository.findScheduledDeployments(
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        )).thenReturn(deployments);

        // when
        List<DeploymentScheduleResponse> response = scheduleService.getDeploymentSchedules(startDate, endDate);

        // then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);

        // 첫 번째 배포 작업 검증 (PLAN-PENDING → "PLAN_PENDING")
        DeploymentScheduleResponse first = response.get(0);
        assertThat(first.id()).isEqualTo(1L);
        assertThat(first.title()).isEqualTo("배포 작업 1");
        assertThat(first.status()).isEqualTo("PLAN_PENDING");
        assertThat(first.projectName()).isEqualTo("테스트 프로젝트");
        assertThat(first.prTitle()).isEqualTo("테스트 PR 제목");
        assertThat(first.prBranch()).isEqualTo("feature-branch");
        assertThat(first.scheduledDate()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(first.scheduledTime()).isEqualTo(LocalDateTime.of(2025, 1, 15, 10, 0).toLocalTime());

        // 두 번째 배포 작업 검증 (DEPLOYMENT-COMPLETED + isDeployed=true → "DEPLOYMENT_SUCCESS")
        DeploymentScheduleResponse second = response.get(1);
        assertThat(second.id()).isEqualTo(2L);
        assertThat(second.title()).isEqualTo("배포 작업 2");
        assertThat(second.status()).isEqualTo("DEPLOYMENT_SUCCESS");
        assertThat(second.projectName()).isEqualTo("테스트 프로젝트");
        assertThat(second.prTitle()).isEqualTo("테스트 PR 제목");
        assertThat(second.prBranch()).isEqualTo("feature-branch");
        assertThat(second.scheduledDate()).isEqualTo(LocalDate.of(2025, 1, 16));
        assertThat(second.scheduledTime()).isEqualTo(LocalDateTime.of(2025, 1, 16, 14, 30).toLocalTime());
    }

    @Test
    @DisplayName("배포 작업 목록 조회 - 조회 결과가 없는 경우")
    void getDeploymentSchedules_Empty() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 15);
        LocalDate endDate = LocalDate.of(2025, 1, 17);

        when(deploymentRepository.findScheduledDeployments(
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        )).thenReturn(List.of());

        // when
        List<DeploymentScheduleResponse> response = scheduleService.getDeploymentSchedules(startDate, endDate);

        // then
        assertThat(response).isNotNull();
        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("배포 작업 목록 조회 - PLAN_REJECTED와 DEPLOYMENT_CANCELED 제외")
    void getDeploymentSchedules_ExcludeRejectedAndCanceled() {
        // given
        LocalDate startDate = LocalDate.of(2025, 1, 15);
        LocalDate endDate = LocalDate.of(2025, 1, 17);

        Deployment planRejected = Deployment.builder()
                .project(testProject)
                .issuer(testAccount)
                .pullRequest(testPullRequest)
                .title("반려된 작업계획서")
                .content("반려된 내용")
                .type(DeploymentType.DEPLOY)
                .stage(DeploymentStage.PLAN)
                .status(DeploymentStatus.REJECTED)
                .scheduledAt(LocalDateTime.of(2025, 1, 15, 11, 0))
                .build();
        ReflectionTestUtils.setField(planRejected, "id", 3L);

        Deployment deploymentCanceled = Deployment.builder()
                .project(testProject)
                .issuer(testAccount)
                .pullRequest(testPullRequest)
                .title("취소된 배포")
                .content("취소된 내용")
                .type(DeploymentType.DEPLOY)
                .stage(DeploymentStage.DEPLOYMENT)
                .status(DeploymentStatus.CANCELED)
                .scheduledAt(LocalDateTime.of(2025, 1, 15, 12, 0))
                .build();
        ReflectionTestUtils.setField(deploymentCanceled, "id", 4L);

        // PLAN_REJECTED와 DEPLOYMENT_CANCELED는 제외되고, deployment1, deployment2만 반환
        List<Deployment> deployments = Arrays.asList(deployment1, deployment2);

        when(deploymentRepository.findScheduledDeployments(
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        )).thenReturn(deployments);

        // when
        List<DeploymentScheduleResponse> response = scheduleService.getDeploymentSchedules(startDate, endDate);

        // then
        assertThat(response).isNotNull();
        assertThat(response).hasSize(2);
        assertThat(response).extracting(DeploymentScheduleResponse::title)
                .containsExactly("배포 작업 1", "배포 작업 2");
        assertThat(response).extracting(DeploymentScheduleResponse::title)
                .doesNotContain("반려된 작업계획서", "취소된 배포");
    }
}

