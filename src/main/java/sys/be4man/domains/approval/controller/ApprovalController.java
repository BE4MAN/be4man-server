package sys.be4man.domains.approval.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sys.be4man.domains.approval.dto.request.ApprovalCreateRequest;
import sys.be4man.domains.approval.dto.request.ApprovalDecisionRequest;
import sys.be4man.domains.approval.dto.response.ApprovalDetailResponse;
import sys.be4man.domains.approval.dto.response.ApprovalSummaryResponse;
import sys.be4man.domains.approval.model.type.ApprovalStatus;
import sys.be4man.domains.approval.service.ApprovalService;

@Tag(name = "Approval", description = "전자 결재 관련 API")
@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @Operation(
            summary = "결재 목록 조회",
            description = """
            특정 사용자의 결재 문서 목록을 조회합니다.
            <br>- `accountId`는 필수이며, `status`는 선택적으로 필터링할 수 있습니다.
            <br>예: `/api/approvals?accountId=1&status=APPROVED`
            """
    )
    @GetMapping
    public ResponseEntity<List<ApprovalSummaryResponse>> getApprovals(
            @RequestParam Long accountId,
            @RequestParam(required = false) ApprovalStatus status
    ) {
        return ResponseEntity.ok(approvalService.getApprovals(accountId, status));
    }

    @Operation(
            summary = "결재 상세 조회",
            description = "결재 문서의 상세 정보를 조회합니다. 문서의 기안자, 결재선, 본문 내용 등이 포함됩니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApprovalDetailResponse> getApprovalDetail(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(approvalService.getApprovalDetail(id));
    }

    @Operation(
            summary = "임시 저장",
            description = "결재 문서를 임시 저장합니다. 결재선 지정이나 본문 작성 중 중간 저장할 때 사용합니다."
    )
    @PostMapping("/drafts")
    public ResponseEntity<Long> saveDraft(@RequestBody ApprovalCreateRequest request) {
        Long id = approvalService.saveDraft(request);
        return ResponseEntity.ok(id);
    }

    @Operation(
            summary = "기안 및 제출",
            description = "결재 문서를 생성하고 동시에 결재 요청 상태로 제출합니다."
    )
    @PostMapping("/submit")
    public ResponseEntity<Long> createAndSubmit(@RequestBody ApprovalCreateRequest request) {
        Long id = approvalService.createAndSubmit(request);
        return ResponseEntity.ok(id);
    }

    @Operation(
            summary = "결재 문서 제출",
            description = "임시 저장된 결재 문서를 결재 요청 상태로 변경합니다."
    )
    @PatchMapping("/{id}/submit")
    public ResponseEntity<Void> submit(@PathVariable Long id) {
        approvalService.submit(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "결재 문서 상신 취소",
            description = "기안자가 결재 요청을 취소할 때 사용합니다. 상태는 ‘취소됨’으로 변경됩니다."
    )
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        approvalService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "결재 승인 처리",
            description = """
            결재자가 결재 문서를 승인합니다.
            <br>- 요청 본문(`ApprovalDecisionRequest`)에는 결재 의견을 포함할 수 있습니다.
            """
    )
    @PatchMapping("/{id}/approve")
    public ResponseEntity<Void> approve(
            @PathVariable Long id,
            @RequestBody ApprovalDecisionRequest request
    ) {
        approvalService.approve(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "결재 반려 처리",
            description = """
            결재자가 결재 문서를 반려합니다.
            <br>- 요청 본문(`ApprovalDecisionRequest`)에는 반려 사유를 포함할 수 있습니다.
            """
    )
    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long id,
            @RequestBody ApprovalDecisionRequest request
    ) {
        approvalService.reject(id, request);
        return ResponseEntity.noContent().build();
    }
}