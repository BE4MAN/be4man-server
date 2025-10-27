package sys.be4man.domains.account.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.account.model.type.JobDepartment;
import sys.be4man.domains.account.model.type.JobPosition;
import sys.be4man.domains.account.model.type.Role;
import sys.be4man.global.model.entity.BaseEntity;

@Entity
@Table(name = "account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_id", unique = true, nullable = false)
    private Long githubId;

    @Column(name = "name", nullable = false, length = 52)
    private String name;

    @Column(name = "email", unique = true, length = 512)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false, length = 52)
    private JobPosition position;

    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false, length = 52)
    private JobDepartment department;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    @Column(name = "github_access_token", nullable = false, length = 512)
    private String githubAccessToken;

    @Builder
    public Account(Long githubId, String name, String email, Role role,
            JobPosition position, JobDepartment department, String profileImageUrl,
            String githubAccessToken) {
        this.githubId = githubId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.position = position;
        this.department = department;
        this.profileImageUrl = profileImageUrl;
        this.githubAccessToken = githubAccessToken;
    }

    /**
     * GitHub Access Token 업데이트
     */
    public void updateGitHubAccessToken(String githubAccessToken) {
        this.githubAccessToken = githubAccessToken;
    }

}
