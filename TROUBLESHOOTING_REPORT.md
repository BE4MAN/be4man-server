# 백엔드 개발 트러블슈팅 리포트

## 프로젝트 개요

**프로젝트명**: BE4MAN (배포 관리 시스템)  
**역할**: 백엔드 아키텍처 설계 및 핵심 기능 개발  
**담당 영역**: 
- 백엔드 아키텍처 설계 및 설정
- 인증/인가 시스템 (JWT + OAuth2)
- 스케줄 관리 시스템
- 홈 페이지 (Dashboard) API

**기술 스택**:
- **Framework**: Spring Boot 3.5.6, Spring Security 6.3.x
- **Language**: Java 17
- **Database**: PostgreSQL 15.x, Redis
- **ORM**: JPA/Hibernate, QueryDSL 5.0.0
- **Build Tool**: Gradle
- **Testing**: JUnit 5, Mockito, AssertJ
- **Documentation**: Swagger/OpenAPI 3.0

---

## 1. 백엔드 아키텍처 설계

### 1.1 계층형 아키텍처 설계

**도전 과제**: 확장 가능하고 유지보수하기 쉬운 아키텍처 설계

**해결 방법**:
- **3계층 구조** (Controller → Service → Repository) 적용
- **도메인 중심 설계**: `domains/{domain}/controller|service|repository|model` 패키지 구조
- **SOLID 원칙** 준수:
  - **SRP**: Checker 패턴으로 검증 로직 분리 (`AccountChecker`, `BanChecker`)
  - **DIP**: 생성자 주입 (`@RequiredArgsConstructor`)으로 의존성 역전
  - **OCP**: 인터페이스 기반 설계 (RepositoryCustom 패턴)

**핵심 설계 결정**:
```java
// BaseEntity를 통한 공통 기능 상속
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate LocalDateTime createdAt;
    @LastModifiedDate LocalDateTime updatedAt;
    @Column Boolean isDeleted = false;
    
    public void softDelete() { this.isDeleted = true; }
}
```

**성과**:
- 모든 엔티티가 일관된 감사(Audit) 추적 및 Soft Delete 패턴 적용
- 코드 재사용성 향상 및 유지보수 용이성 확보

---

### 1.2 QueryDSL 기반 타입 안전 쿼리 설계

**도전 과제**: 복잡한 동적 쿼리 작성 및 타입 안전성 보장

**해결 방법**:
- **RepositoryCustom 패턴** 적용:
  ```
  Repository Interface
    ↓
  RepositoryCustom Interface
    ↓
  RepositoryImpl (QueryDSL 구현)
  ```
- QueryDSL Q클래스 자동 생성으로 컴파일 타임 쿼리 검증

**예시**:
```java
public class ApprovalLineRepositoryImpl implements ApprovalLineRepositoryCustom {
    public List<ApprovalLine> findPendingApprovalsByAccountId(Long accountId) {
        return queryFactory
            .selectFrom(approvalLine)
            .join(approvalLine.approval, approval).fetchJoin()
            .join(approval.deployment, deployment).fetchJoin()
            .where(
                approvalLine.approver.id.eq(accountId),
                approvalLine.isApproved.isNull(),
                deployment.status.in(DeploymentStatus.PENDING, DeploymentStatus.APPROVED)
            )
            .orderBy(deployment.updatedAt.desc())
            .fetch();
    }
}
```

**성과**:
- 런타임 SQL 오류 사전 방지
- IDE 자동완성 지원으로 개발 생산성 향상
- 복잡한 동적 쿼리 작성 용이

---

## 2. 인증/인가 시스템 구현

### 2.1 JWT + OAuth2 통합 인증

**도전 과제**: GitHub OAuth2 로그인과 JWT 토큰 기반 인증을 통합

**해결 방법**:
- **OAuth2 Flow**:
  1. 프론트엔드에서 GitHub OAuth2 인증
  2. `CustomOAuth2UserService`에서 GitHub 사용자 정보 수집
  3. `OAuth2SuccessHandler`에서 JWT 토큰 발급 및 Redis 저장
  4. 프론트엔드로 리다이렉트

- **JWT 인증 필터**:
  ```java
  @Component
  public class JwtAuthenticationFilter extends OncePerRequestFilter {
      @Override
      protected void doFilterInternal(...) {
          String token = extractTokenFromRequest(request);
          if (token != null && jwtProvider.validateToken(token)) {
              setAuthenticationToContext(request, token);
          }
      }
  }
  ```

**핵심 설계**:
- **Stateless 인증**: 세션 대신 JWT 사용 (`SessionCreationPolicy.STATELESS`)
- **Redis 토큰 관리**: Refresh Token, Sign Token을 Redis에 저장하여 보안 강화
- **필터 체인**: JWT 필터를 `UsernamePasswordAuthenticationFilter` 앞에 배치

**성과**:
- 무상태(Stateless) 인증으로 서버 확장성 확보
- OAuth2와 JWT의 안전한 통합

---

### 2.2 Redis를 활용한 토큰 관리

**도전 과제**: 토큰 무효화 및 보안 관리

**해결 방법**:
- **Redis Template 설정**:
  ```java
  @Bean
  public RedisTemplate<String, Object> redisTemplate(...) {
      // ObjectMapper 설정 (LocalDateTime 직렬화)
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      
      template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
      return template;
  }
  ```

- **토큰 서비스 분리**:
  - `RefreshTokenRedisService`: Refresh Token 관리
  - `SignTokenRedisService`: 회원가입용 임시 토큰 관리
  - `OAuthCodeRedisService`: OAuth2 인증 코드 임시 저장

**성과**:
- 토큰 무효화 및 만료 관리 용이
- 분산 환경에서도 일관된 토큰 관리

---

## 3. 스케줄 관리 시스템

### 3.1 복잡한 반복 패턴 계산

**도전 과제**: 주간/월간 반복되는 작업 금지 기간(Ban) 계산

**해결 방법**:
- **RecurrenceCalculator 유틸리티 클래스** 구현:
  ```java
  public static List<Period> calculateRecurrenceDates(
      RecurrenceType type,
      LocalDate startDate,
      LocalTime startTime,
      Integer durationMinutes,
      RecurrenceWeekday weekday,
      RecurrenceWeekOfMonth weekOfMonth,
      LocalDate recurrenceEndDate,
      LocalDate queryStartDate,
      LocalDate queryEndDate
  ) {
      // 복잡한 반복 로직 계산
  }
  ```

**핵심 로직**:
- 주간 반복: 매주 특정 요일
- 월간 반복: 매월 N번째 주의 특정 요일
- 기간 내 모든 발생 시점 계산

**성과**:
- 복잡한 반복 패턴을 정확하게 계산
- 재사용 가능한 유틸리티로 코드 중복 제거

---

### 3.2 Ban과 Deployment 충돌 감지 및 자동 취소

**도전 과제**: 작업 금지 기간 생성 시 겹치는 배포 작업 자동 취소

**해결 방법**:
```java
private void cancelOverlappingDeploymentsForBan(Ban ban, List<Long> projectIds) {
    // 1. 반복 패턴으로 모든 Ban 기간 계산
    List<Period> periods = RecurrenceCalculator.calculateRecurrenceDates(...);
    
    // 2. 각 기간별로 겹치는 Deployment 조회
    List<Deployment> overlappingDeployments = periods.stream()
        .flatMap(period -> 
            deploymentRepository.findOverlappingDeployments(
                period.startDateTime(),
                period.endDateTime(),
                projectIds
            ).stream()
        )
        .distinct()
        .toList();
    
    // 3. 겹치는 Deployment 상태를 CANCELED로 변경
    for (Deployment deployment : overlappingDeployments) {
        deployment.updateStatus(DeploymentStatus.CANCELED);
    }
}
```

**성과**:
- 작업 금지 기간 생성 시 자동으로 충돌하는 배포 작업 취소
- 비즈니스 규칙을 코드로 명확하게 표현

---

### 3.3 N+1 쿼리 문제 해결

**도전 과제**: 스케줄 조회 시 관련 프로젝트 정보를 조회하는 과정에서 N+1 쿼리 발생

**문제 상황**:
```java
// 문제: 각 Deployment마다 관련 프로젝트를 개별 조회
List<Deployment> deployments = deploymentRepository.findScheduledDeployments(...);
for (Deployment deployment : deployments) {
    List<RelatedProject> relatedProjects = relatedProjectRepository
        .findByProjectId(deployment.getProject().getId()); // N+1 발생!
}
```

**해결 방법**:
```java
// 1. 배치 조회: 모든 Deployment의 project_id 수집
List<Long> deploymentProjectIds = deployments.stream()
    .map(deployment -> deployment.getProject().getId())
    .distinct()
    .toList();

// 2. 한 번의 쿼리로 모든 관련 프로젝트 조회
Map<Long, List<String>> projectRelatedServicesMap = 
    buildProjectRelatedServicesMap(deploymentProjectIds);

// 3. 메모리에서 매핑
return deployments.stream()
    .map(deployment -> {
        Long projectId = deployment.getProject().getId();
        List<String> relatedServices = 
            projectRelatedServicesMap.getOrDefault(projectId, List.of());
        return DeploymentScheduleResponse.from(deployment, relatedServices);
    })
    .toList();
```

**성과**:
- 쿼리 수: N+1개 → 2개로 감소
- 응답 시간 대폭 개선 (수십 개의 Deployment 조회 시)

---

## 4. 홈 페이지 (Dashboard) API 구현

### 4.1 복잡한 다중 조인 쿼리 최적화

**도전 과제**: 승인 대기 목록 조회 시 여러 테이블 조인 및 필터링

**초기 문제**: Native Query 사용으로 타입 안전성 부족 및 유지보수 어려움

**해결 방법**: QueryDSL로 리팩토링
```java
public List<ApprovalLine> findPendingApprovalsByAccountId(Long accountId) {
    return queryFactory
        .selectFrom(approvalLine)
        .join(approvalLine.approval, approval).fetchJoin()
        .join(approval.deployment, deployment).fetchJoin()
        .join(deployment.project, project).fetchJoin()
        .join(deployment.issuer, issuer).fetchJoin()
        .where(
            approvalLine.approver.id.eq(accountId),
            approvalLine.isApproved.isNull(),
            deployment.status.in(DeploymentStatus.PENDING, DeploymentStatus.APPROVED),
            approval.isDeleted.eq(false),
            deployment.isDeleted.eq(false)
        )
        .orderBy(deployment.updatedAt.desc())
        .fetch();
}
```

**성과**:
- 타입 안전성 확보
- Fetch Join으로 N+1 문제 사전 방지
- 코드 가독성 및 유지보수성 향상

---

### 4.2 PostgreSQL DISTINCT + ORDER BY 오류 해결

**도전 과제**: `SELECT DISTINCT`와 `ORDER BY`를 함께 사용할 때 PostgreSQL 오류 발생

**에러 메시지**:
```
ERROR: for SELECT DISTINCT, ORDER BY expressions must appear in select list
```

**문제 코드**:
```java
return queryFactory
    .selectDistinct(approvalLine)
    .from(approvalLine)
    .join(...)
    .orderBy(
        deployment.updatedAt.desc(),
        approval.updatedAt.desc()  // 문제: DISTINCT와 함께 사용 불가
    )
    .fetch();
```

**해결 방법**:
```java
// ORDER BY에서 approval.updatedAt 제거
return queryFactory
    .selectDistinct(approvalLine)
    .from(approvalLine)
    .join(...)
    .orderBy(deployment.updatedAt.desc())  // DISTINCT된 컬럼만 사용
    .fetch();
```

**성과**:
- PostgreSQL 요구사항 준수
- 쿼리 정상 실행

---

### 4.3 복잡한 알림 로직 구현

**도전 과제**: "취소" 및 "반려" 알림을 다양한 조건으로 조회

**비즈니스 로직**:
1. **취소 알림**: 사용자가 승인한 Deployment가 CANCELED 상태
2. **반려 알림**: 
   - 사용자가 요청한 Deployment가 REJECTED
   - 사용자가 승인한 Approval이 다른 사람에 의해 반려됨

**해결 방법**:
```java
// 1. 취소 알림 조회
List<ApprovalLine> canceledLines = approvalLineRepository
    .findCanceledNotificationsByAccountId(accountId);

// 2. 반려 알림 조회 (두 가지 케이스)
List<Deployment> rejectedDeployments = approvalLineRepository
    .findRejectedDeploymentsByIssuerId(accountId);
List<ApprovalLine> rejectedApprovals = approvalLineRepository
    .findRejectedApprovalsByApproverId(accountId);

// 3. 통합 및 정렬
List<NotificationResponse> notifications = new ArrayList<>();
// ... 통합 로직
```

**특별한 챌린지**: "사용자가 승인한 Approval이 다른 사람에 의해 반려됨" 케이스
- 서브쿼리로 반려된 ApprovalLine 확인
- 반려 시각이 사용자 승인 시각보다 이후인지 확인

**성과**:
- 복잡한 비즈니스 로직을 명확한 쿼리로 구현
- 다양한 알림 케이스를 정확하게 처리

---

### 4.4 복구현황 목록 조회 및 Duration 계산

**도전 과제**: BuildRun 데이터를 기반으로 복구 소요 시간 계산

**요구사항**:
- `duration`: 첫 번째 BuildRun.startedAt ~ 마지막 BuildRun.endedAt
- `recoveredAt`: 마지막 BuildRun.endedAt
- `status`가 COMPLETED이고 BuildRun이 존재할 때만 계산

**해결 방법**:
```java
List<BuildRun> buildRuns = buildRunsMap.getOrDefault(deployment.getId(), List.of());

if ("COMPLETED".equals(status) && !buildRuns.isEmpty()) {
    BuildRun firstBuildRun = buildRuns.stream()
        .min((a, b) -> a.getStartedAt().compareTo(b.getStartedAt()))
        .orElse(null);
    
    BuildRun lastBuildRun = buildRuns.stream()
        .max((a, b) -> a.getEndedAt().compareTo(b.getEndedAt()))
        .orElse(null);
    
    if (firstBuildRun != null && lastBuildRun != null) {
        Duration durationBetween = Duration.between(
            firstBuildRun.getStartedAt(),
            lastBuildRun.getEndedAt()
        );
        duration = durationBetween.toMinutes() + "분";
        recoveredAt = lastBuildRun.getEndedAt();
    }
}
```

**성과**:
- 정확한 복구 소요 시간 계산
- 배치 조회로 N+1 문제 방지

---

## 5. 테스트 전략

### 5.1 단위 테스트 작성

**도전 과제**: 복잡한 비즈니스 로직의 신뢰성 보장

**해결 방법**:
- **Mockito + JUnit 5** 기반 단위 테스트
- **Given-When-Then** 패턴 적용
- **@DisplayName**으로 한글 테스트 설명

**예시**:
```java
@Test
@DisplayName("승인 대기 목록 조회 성공")
void getPendingApprovals_Success() {
    // given
    Long accountId = 1L;
    List<ApprovalLine> approvalLines = createMockApprovalLines();
    when(approvalLineRepository.findPendingApprovalsByAccountId(accountId))
        .thenReturn(approvalLines);
    
    // when
    List<PendingApprovalResponse> result = 
        dashboardService.getPendingApprovals(accountId);
    
    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).status()).isEqualTo("승인 대기");
}
```

**성과**:
- 각 API별로 포괄적인 테스트 커버리지 확보
- 리팩토링 시 안전성 보장

---

## 6. 주요 기술적 성과

### 6.1 성능 최적화
- **N+1 쿼리 문제 해결**: 배치 조회 및 Fetch Join 활용
- **쿼리 최적화**: QueryDSL로 효율적인 쿼리 작성

### 6.2 코드 품질
- **타입 안전성**: QueryDSL로 컴파일 타임 쿼리 검증
- **유지보수성**: 도메인 중심 설계 및 SOLID 원칙 준수
- **가독성**: 명확한 네이밍 및 계층 구조

### 6.3 보안
- **JWT + OAuth2 통합**: 안전한 인증/인가 시스템
- **Redis 토큰 관리**: 토큰 무효화 및 보안 강화

### 6.4 확장성
- **Stateless 아키텍처**: 서버 확장 용이
- **도메인 중심 설계**: 새로운 기능 추가 용이

---

## 7. 학습 및 성장

### 7.1 기술적 역량
- **Spring Security 심화**: JWT, OAuth2, Filter Chain 이해
- **QueryDSL 마스터**: 복잡한 동적 쿼리 작성 능력
- **성능 최적화**: N+1 문제 해결 및 쿼리 최적화 경험
- **아키텍처 설계**: 확장 가능한 백엔드 구조 설계

### 7.2 문제 해결 능력
- **디버깅**: PostgreSQL 오류 분석 및 해결
- **리팩토링**: Native Query → QueryDSL 전환
- **최적화**: 성능 문제 식별 및 해결

---

## 8. 향후 개선 사항

1. **캐싱 전략**: Redis를 활용한 API 응답 캐싱
2. **비동기 처리**: 복잡한 쿼리를 비동기로 처리
3. **모니터링**: 쿼리 성능 모니터링 및 로깅 강화
4. **API 문서화**: Swagger를 통한 상세한 API 문서화

---

## 결론

이 프로젝트를 통해 백엔드 아키텍처 설계부터 복잡한 비즈니스 로직 구현, 성능 최적화까지 전 과정을 경험했습니다. 특히 **QueryDSL을 활용한 타입 안전 쿼리 작성**, **N+1 문제 해결**, **JWT + OAuth2 통합** 등 실무에서 자주 마주치는 문제들을 직접 해결하며 실전 역량을 크게 향상시킬 수 있었습니다.






