package sys.be4man.domains.pullrequest.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrMergedBody {

    @JsonProperty("pr_number")
    @NotNull
    private Integer prNumber;

    @JsonProperty("branch")
    private String branch;

    @JsonProperty("repository_url")
    @NotNull
    private String repositoryUrl;

    @JsonProperty("github_email")
    private String githubEmail;

    private Long githubId;
}
