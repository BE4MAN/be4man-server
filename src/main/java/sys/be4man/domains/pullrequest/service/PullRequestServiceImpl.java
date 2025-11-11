package sys.be4man.domains.pullrequest.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.pullrequest.dto.request.PullRequestCreateRequest;
import sys.be4man.domains.pullrequest.dto.response.PullRequestResponse;
import sys.be4man.domains.pullrequest.dto.request.PullRequestUpdateRequest;
import sys.be4man.domains.pullrequest.model.entity.PullRequest;
import sys.be4man.domains.pullrequest.repository.PullRequestRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class PullRequestServiceImpl implements PullRequestService {

    private final PullRequestRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<PullRequestResponse> getAllByGithubEmail(String githubEmail) {
        return repository.findByGithubEmail(githubEmail).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PullRequestResponse getById(Long id) {
        PullRequest pr = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PR not found id=" + id));
        return toDto(pr);
    }

    @Override
    public PullRequestResponse create(PullRequestCreateRequest request) {
        if (repository.existsByPrNumber(request.getPrNumber())) {
            throw new IllegalStateException("이미 존재하는 PR 번호입니다.");
        }

        PullRequest pr = PullRequest.builder()
                .prNumber(request.getPrNumber())
                .repositoryUrl(request.getRepositoryUrl())
                .branch(request.getBranch())
                .build();

        repository.save(pr);
        return toDto(pr);
    }

    @Override
    public PullRequestResponse update(Long id, PullRequestUpdateRequest request) {
        PullRequest pr = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PR not found id=" + id));

        pr.update(request.getRepositoryUrl(), request.getBranch());
        return toDto(pr);
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("PR not found id=" + id);
        }
        repository.deleteById(id);
    }

    private PullRequestResponse toDto(PullRequest pr) {
        return PullRequestResponse.builder()
                .id(pr.getId())
                .prNumber(pr.getPrNumber())
                .repositoryUrl(pr.getRepositoryUrl())
                .branch(pr.getBranch())
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                .build();
    }
}
