package sys.be4man.domains.ban.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sys.be4man.domains.project.model.entity.Project;
import sys.be4man.global.model.entity.BaseEntity;

/**
 * 프로젝트-금지 작업 연관관계 엔티티
 */
@Entity
@Table(name = "project_ban")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectBan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_ban_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ban_id", nullable = false)
    private Ban ban;

    @Builder
    public ProjectBan(Project project, Ban ban) {
        this.project = project;
        this.ban = ban;
    }
}
