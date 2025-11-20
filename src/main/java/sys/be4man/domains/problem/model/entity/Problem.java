package sys.be4man.domains.problem.model.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.account.model.entity.Account;
import sys.be4man.domains.problem.model.type.Importance;
import sys.be4man.global.model.entity.BaseEntity;

@Entity
@Table(name = "problem")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Problem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProblemCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Importance importance;

    @Column(name = "is_solved", nullable = false)
    private boolean isSolved = false;

    @OneToMany(mappedBy = "problem")
    private List<ProblemDeployment> problemDeployments = new ArrayList<>();

    @Builder
    private Problem(
            ProblemCategory category,
            Account account,
            String title,
            String description,
            Importance importance,
            boolean isSolved
    ) {
        this.category = category;
        this.account = account;
        this.title = title;
        this.description = description;
        this.importance = importance;
        this.isSolved = isSolved;
    }

    public void addProblemDeployment(ProblemDeployment link) {
        this.problemDeployments.add(link);
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateImportance(Importance importance) {
        this.importance = importance;
    }

    public void changeCategory(ProblemCategory category) {
        this.category = category;
    }

    public void changeAccount(Account account) {
        this.account = account;
    }

    public void updateBasicInfo(String title, String description, Importance importance) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (importance != null) {
            this.importance = importance;
        }
    }

    public void markSolved() {
        this.isSolved = true;
    }

    public void markUnsolved() {
        this.isSolved = false;
    }
}
