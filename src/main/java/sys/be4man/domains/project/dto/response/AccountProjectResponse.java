// 작성자 : 김민호
package sys.be4man.domains.project.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountProjectResponse {
    private Long accountProjectId;
    private Long accountId;
    private Long projectId;
    private String projectName;
}
