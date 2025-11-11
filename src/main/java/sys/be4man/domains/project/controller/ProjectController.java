package sys.be4man.domains.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sys.be4man.domains.project.dto.response.ProjectResponse;
import sys.be4man.domains.project.service.ProjectService;

@Tag(name = "Project", description = "프로젝트 관리 API")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(
            summary = "프로젝트 단건 조회",
            description = "프로젝트 ID로 프로젝트 정보를 조회합니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    @Operation(
            summary = "프로젝트 전체 조회",
            description = "등록된 모든 프로젝트 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAll() {
        return ResponseEntity.ok(projectService.getAll());
    }
}
