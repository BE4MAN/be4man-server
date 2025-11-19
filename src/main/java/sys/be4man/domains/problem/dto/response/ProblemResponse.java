package sys.be4man.domains.problem.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import sys.be4man.domains.problem.model.type.Importance;

@Getter
@Builder
public class ProblemResponse {

    private Long id;
    private Long categoryId;
    private Long accountId;

    private String title;
    private String description;

    private Importance importance;

    private List<Long> deploymentIds;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
