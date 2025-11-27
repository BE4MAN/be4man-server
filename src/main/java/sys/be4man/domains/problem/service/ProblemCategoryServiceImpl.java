// 작성자 : 김민호
package sys.be4man.domains.problem.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.repository.AccountRepository;
import sys.be4man.domains.problem.dto.request.ProblemCategoryCreateRequest;
import sys.be4man.domains.problem.dto.response.ProblemCategoryResponse;
import sys.be4man.domains.problem.model.entity.ProblemCategory;
import sys.be4man.domains.problem.repository.ProblemCategoryRepository;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.domains.project.repository.ProjectRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class ProblemCategoryServiceImpl implements ProblemCategoryService {

    private final ProblemCategoryRepository problemCategoryRepository;
    private final ProjectRepository projectRepository;
    private final AccountRepository accountRepository;

    @Override
    public Long createCategory(ProblemCategoryCreateRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found. id=" + request.getProjectId()));

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found. id=" + request.getAccountId()));

        ProblemCategory category = ProblemCategory.builder()
                .project(project)
                .account(account)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        problemCategoryRepository.save(category);
        return category.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemCategoryResponse getCategory(Long id) {
        ProblemCategory category = problemCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ProblemCategory not found. id=" + id));
        return toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProblemCategoryResponse> getCategoriesByProject(Long projectId) {
        List<ProblemCategory> list = problemCategoryRepository.findByProject_IdOrderByIdAsc(projectId);
        return list.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProblemCategoryResponse> getAllCategories() {
        return problemCategoryRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ProblemCategoryResponse toResponse(ProblemCategory c) {
        return ProblemCategoryResponse.builder()
                .id(c.getId())
                .projectId(c.getProject().getId())
                .accountId(c.getAccount().getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
