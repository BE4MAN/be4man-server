package sys.be4man.domains.problem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sys.be4man.domains.problem.dto.request.ProblemCategoryCreateRequest;
import sys.be4man.domains.problem.dto.request.ProblemCreateRequest;
import sys.be4man.domains.problem.dto.response.ProblemCategoryResponse;
import sys.be4man.domains.problem.dto.response.ProblemResponse;
import sys.be4man.domains.problem.service.ProblemCategoryService;
import sys.be4man.domains.problem.service.ProblemService;

@Tag(name = "Problem", description = "문제 및 문제 유형 관리 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemCategoryService problemCategoryService;
    private final ProblemService problemService;

    @Operation(summary = "문제 유형 생성")
    @PostMapping("/problem-categories")
    public ResponseEntity<Long> createCategory(@RequestBody ProblemCategoryCreateRequest request) {
        Long id = problemCategoryService.createCategory(request);
        return ResponseEntity.ok(id);
    }

    @Operation(summary = "문제 유형 단건 조회")
    @GetMapping("/problem-categories/{id}")
    public ResponseEntity<ProblemCategoryResponse> getCategory(@PathVariable Long id) {
        ProblemCategoryResponse res = problemCategoryService.getCategory(id);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "프로젝트 기준 문제 유형 목록 조회")
    @GetMapping("/problem-categories")
    public ResponseEntity<List<ProblemCategoryResponse>> getCategoriesByProject(
            @RequestParam("projectId") Long projectId
    ) {
        List<ProblemCategoryResponse> list = problemCategoryService.getCategoriesByProject(projectId);
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "전체 문제 유형 목록 조회")
    @GetMapping("/problem-categories/all")
    public ResponseEntity<List<ProblemCategoryResponse>> getAllCategories() {
        List<ProblemCategoryResponse> list = problemCategoryService.getAllCategories();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "문제 생성")
    @PostMapping("/problems")
    public ResponseEntity<Long> createProblem(@RequestBody ProblemCreateRequest request) {
        Long id = problemService.createProblem(request);
        return ResponseEntity.ok(id);
    }

    @Operation(summary = "문제 단건 조회")
    @GetMapping("/problems/{id}")
    public ResponseEntity<ProblemResponse> getProblem(@PathVariable Long id) {
        ProblemResponse res = problemService.getProblem(id);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "카테고리 기준 문제 목록 조회")
    @GetMapping("/problems")
    public ResponseEntity<List<ProblemResponse>> getProblemsByCategory(
            @RequestParam("categoryId") Long categoryId
    ) {
        List<ProblemResponse> list = problemService.getProblemsByCategory(categoryId);
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "전체 문제 목록 조회")
    @GetMapping("/problems/all")
    public ResponseEntity<List<ProblemResponse>> getAllProblems() {
        List<ProblemResponse> list = problemService.getAllProblems();
        return ResponseEntity.ok(list);
    }
}
