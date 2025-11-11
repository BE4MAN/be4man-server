package sys.be4man.domains.deployment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sys.be4man.domains.deployment.dto.request.DeploymentCreateRequest;
import sys.be4man.domains.deployment.dto.response.DeploymentResponse;
import sys.be4man.domains.deployment.service.DeploymentService;

@Tag(name = "Deployment", description = "배포 관련 API")
@RestController
@RequestMapping("/api/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    @Operation(
            summary = "배포 작업 생성",
            description = """
            새로운 배포 작업을 생성합니다.<br>
            주로 결재 상신 또는 임시 저장 시 호출되며, 관련 프로젝트/브랜치/PR 정보를 포함합니다.<br>
            요청 본문(`DeploymentCreateRequest`)에는 배포 대상 서비스, 환경, 버전 정보 등이 포함됩니다.
            """
    )
    @PostMapping
    public ResponseEntity<DeploymentResponse> createDeployment(@RequestBody DeploymentCreateRequest request) {
        return ResponseEntity.ok(deploymentService.createDeployment(request));
    }

    @Operation(
            summary = "배포 작업 단건 조회",
            description = """
            특정 배포 작업의 상세 정보를 조회합니다.<br>
            배포 상태, 생성 시간, 배포자, 관련 결재 정보 등이 포함됩니다.
            """
    )
    @GetMapping("/{id}")
    public ResponseEntity<DeploymentResponse> getDeployment(@PathVariable Long id) {
        return ResponseEntity.ok(deploymentService.getDeployment(id));
    }

    @Operation(
            summary = "배포 작업 전체 조회",
            description = """
            모든 배포 작업 목록을 조회합니다.<br>
            최근 배포 이력, 진행 중인 배포 상태 등을 확인할 수 있습니다.
            """
    )
    @GetMapping
    public ResponseEntity<List<DeploymentResponse>> getAllDeployments() {
        return ResponseEntity.ok(deploymentService.getAllDeployments());
    }
}
