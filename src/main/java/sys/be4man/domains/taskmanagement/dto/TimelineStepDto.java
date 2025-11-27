// 작성자 : 허겸
package sys.be4man.domains.taskmanagement.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TimelineStepDto {
    private Integer stepNumber;
    private String stepName;
    private String status;        // "completed", "active", "pending", "rejected"
    private String result;         // "success", "failure", null (배포 결과)
    private String timestamp;
    private String description;
}