package sys.be4man.domains.problem.dto.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.problem.model.type.Importance;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemUpdateRequest {
    private Long categoryId;
    private Long accountId;
    private String title;
    private String description;
    private Importance importance;
    private List<Long> deploymentIds;
    private Boolean isSolved;
}
