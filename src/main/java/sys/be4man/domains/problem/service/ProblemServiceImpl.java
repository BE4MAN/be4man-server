package sys.be4man.domains.problem.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.account.repository.AccountRepository;
import sys.be4man.domains.deployment.model.entity.Deployment;
import sys.be4man.domains.deployment.repository.DeploymentRepository;
import sys.be4man.domains.problem.dto.request.ProblemCreateRequest;
import sys.be4man.domains.problem.dto.response.ProblemResponse;
import sys.be4man.domains.problem.model.entity.Problem;
import sys.be4man.domains.problem.model.entity.ProblemCategory;
import sys.be4man.domains.problem.model.entity.ProblemDeployment;
import sys.be4man.domains.problem.repository.ProblemCategoryRepository;
import sys.be4man.domains.problem.repository.ProblemDeploymentRepository;
import sys.be4man.domains.problem.repository.ProblemRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class ProblemServiceImpl implements ProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemCategoryRepository problemCategoryRepository;
    private final ProblemDeploymentRepository problemDeploymentRepository;
    private final DeploymentRepository deploymentRepository;
    private final AccountRepository accountRepository;

    @Override
    public Long createProblem(ProblemCreateRequest request) {
        ProblemCategory category = problemCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("ProblemCategory not found. id=" + request.getCategoryId()));

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found. id=" + request.getAccountId()));

        Problem problem = Problem.builder()
                .category(category)
                .account(account)
                .title(request.getTitle())
                .description(request.getDescription())
                .importance(request.getImportance())
                .build();

        problemRepository.save(problem);

        List<Long> depIds = request.getDeploymentIds();
        if (depIds != null && !depIds.isEmpty()) {
            for (Long depId : depIds) {
                if (depId == null) continue;
                Deployment deployment = deploymentRepository.findById(depId)
                        .orElseThrow(() -> new IllegalArgumentException("Deployment not found. id=" + depId));

                ProblemDeployment link = ProblemDeployment.builder()
                        .deployment(deployment)
                        .problem(problem)
                        .build();

                problemDeploymentRepository.save(link);
                problem.addProblemDeployment(link);
            }
        }

        return problem.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemResponse getProblem(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Problem not found. id=" + id));
        return toResponse(problem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProblemResponse> getProblemsByCategory(Long categoryId) {
        List<Problem> list = problemRepository.findByCategory_IdOrderByIdAsc(categoryId);
        return list.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProblemResponse> getAllProblems() {
        return problemRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ProblemResponse toResponse(Problem p) {
        List<Long> depIds = (p.getProblemDeployments() == null)
                ? Collections.emptyList()
                : p.getProblemDeployments().stream()
                        .map(link -> link.getDeployment().getId())
                        .collect(Collectors.toList());

        return ProblemResponse.builder()
                .id(p.getId())
                .categoryId(p.getCategory().getId())
                .accountId(p.getAccount().getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .importance(p.getImportance())
                .deploymentIds(depIds)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
