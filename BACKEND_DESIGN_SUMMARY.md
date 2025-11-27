# 백엔드 설계 요약 (PPT용)

## 1️⃣ 계층형 아키텍처 (Layered Architecture)
**Controller → Service → Repository 3계층 구조**

```
Controller (API 엔드포인트)
  ↓
Service (비즈니스 로직)
  ↓
Repository (데이터 접근)
```

**계층별 역할**
- **Controller**: HTTP 요청/응답, Validation, Swagger 문서화
- **Service**: 비즈니스 로직, 트랜잭션 관리 (`@Transactional`)
- **Repository**: JPA + QueryDSL (타입 안전 쿼리)

**도메인 중심 설계**
- 패키지 구조: `domains/{domain}/controller|service|repository|model`
- 도메인별 독립적 관리 (account, auth, schedule, deployment 등)

---

## 2️⃣ BaseEntity & Soft Delete
**JPA Auditing + 논리 삭제 패턴**

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate LocalDateTime createdAt;
    @LastModifiedDate LocalDateTime updatedAt;
    @Column Boolean isDeleted = false;
    
    public void softDelete() { this.isDeleted = true; }
}
```

**주요 기능**
- ✅ **JPA Auditing**: 생성일/수정일 자동 관리
- ✅ **Soft Delete**: 물리 삭제 대신 논리 삭제 (`isDeleted` 플래그)
- ✅ **상속 구조**: 모든 Entity가 BaseEntity 상속

**장점**
- 데이터 복구 가능
- 감사(Audit) 추적 용이
- 삭제 이력 관리

---

## 3️⃣ OOP 설계 원칙
**SOLID + 디자인 패턴 적용**

### Builder 패턴
```java
@Builder
public class Account {
    // Entity 생성 시 빌더 패턴 사용
}
```

### Record DTO
```java
public record SignupRequest(
    @NotBlank String name,
    JobDepartment department,
    @NotNull JobPosition position
) {}
```

### Checker 패턴
```java
@Component
public class AccountChecker {
    public Account checkAccountExists(Long accountId) { }
    public void checkConflictAccountExistsByGithubId(Long githubId) { }
}
```

**설계 원칙**
- ✅ **SRP**: 단일 책임 원칙 (Checker는 검증만 담당)
- ✅ **OCP**: 확장에 열려있음 (인터페이스 기반)
- ✅ **DIP**: 의존성 역전 (생성자 주입 `@RequiredArgsConstructor`)
- ✅ **불변성**: Record DTO로 불변 객체 사용

---

## 4️⃣ QueryDSL & 타입 안전 쿼리
**컴파일 타임 쿼리 검증**

```
Repository Interface
  ↓
RepositoryCustom Interface
  ↓
RepositoryImpl (QueryDSL 구현)
```

**주요 기능**
- ✅ **타입 안전**: 컴파일 타임 쿼리 검증
- ✅ **동적 쿼리**: 복잡한 조건문 쿼리 작성 용이
- ✅ **코드 생성**: Q클래스 자동 생성 (`QAccount`, `QBan` 등)

**예시**
```java
public class BanRepositoryImpl implements BanRepositoryCustom {
    public List<Ban> findBans(...) {
        return queryFactory
            .selectFrom(ban)
            .where(ban.isDeleted.eq(false))
            .fetch();
    }
}
```

---

## 5️⃣ 테스트 전략
**Mockito + JUnit 5 기반 단위 테스트**

```java
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {
    @Mock private ProjectRepository projectRepository;
    @InjectMocks private ScheduleServiceImpl scheduleService;
    
    @Test
    @DisplayName("스케줄 관리 메타데이터 조회 - 성공")
    void getScheduleMetadata_Success() {
        // given-when-then 패턴
    }
}
```

**테스트 특징**
- ✅ **Mock 기반**: 의존성 Mock 처리
- ✅ **Given-When-Then**: 명확한 테스트 구조
- ✅ **@DisplayName**: 한글 테스트 설명
- ✅ **AssertJ**: 유창한 검증 API

---

## 6️⃣ Swagger API 문서화
**SpringDoc OpenAPI 3.0 통합**

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .components(new Components()
            .addSecuritySchemes("Bearer Authentication", bearerScheme))
        .info(new Info()
            .title("배포맨 API 명세서")
            .version("v1.0.0"));
}
```

**주요 기능**
- ✅ **자동 문서화**: 어노테이션 기반 API 문서 생성
- ✅ **JWT 인증**: Swagger UI에서 토큰 테스트 가능
- ✅ **상세 설명**: `@Operation`, `@ApiResponse` 활용

**어노테이션**
- `@Tag`: API 그룹화
- `@Operation`: API 설명
- `@ApiResponse`: 응답 예시
- `@SecurityRequirement`: 인증 필요 표시

---

## 7️⃣ 예외 처리 전략
**GlobalExceptionHandler + BaseException 계층**

```
Exception
  ↓
BaseException (추상 클래스)
  ↓
도메인별 예외 (NotFoundException, ConflictException 등)
```

**예외 처리 흐름**
1. **커스텀 예외**: `BaseException` 상속
2. **GlobalExceptionHandler**: `@RestControllerAdvice`로 전역 처리
3. **표준 응답**: `ErrorResponse` DTO로 일관된 에러 응답

**예외 타입**
- `NotFoundException` (404)
- `ConflictException` (409)
- `BadRequestException` (400)
- `ForbiddenException` (403)

**Validation 예외**
- `MethodArgumentNotValidException`: `@Valid` 실패
- `ConstraintViolationException`: `@Validated` 실패

---

## 8️⃣ DTO & Validation
**Record DTO + Jakarta Validation**

```java
public record CreateBanRequest(
    @NotBlank String title,
    @NotNull LocalDate startDate,
    @Positive Integer durationMinutes,
    @NotEmpty List<Long> relatedProjectIds
) {
    @AssertTrue
    public boolean isWeeklyRecurrenceValid() { }
}
```

**DTO 패턴**
- ✅ **Record**: 불변 DTO (Java 14+)
- ✅ **Validation**: `@Valid`, `@NotNull`, `@NotBlank` 등
- ✅ **커스텀 검증**: `@AssertTrue` 메서드
- ✅ **패키지 구조**: `dto/request`, `dto/response` 분리

---

## 9️⃣ Lombok 활용
**보일러플레이트 코드 제거**

**주요 어노테이션**
- `@RequiredArgsConstructor`: 생성자 주입
- `@Getter`: Getter 자동 생성
- `@Builder`: 빌더 패턴
- `@Slf4j`: 로깅 (`log.info()`)

**장점**
- 코드 간결성 향상
- 가독성 개선
- 유지보수 용이

---

## 🔟 기술 스택

**프레임워크**
- Spring Boot 3.5.6
- Spring Security (JWT + OAuth2)
- Spring Data JPA

**데이터베이스**
- PostgreSQL 15.x (운영)
- H2 (테스트)

**캐시**
- Redis (토큰 관리)

**빌드 도구**
- Gradle
- QueryDSL 코드 생성

**문서화**
- Swagger (SpringDoc OpenAPI)

**테스트**
- JUnit 5
- Mockito
- AssertJ

---

## 핵심 키워드

**아키텍처**: 3계층 구조, 도메인 중심 설계  
**엔티티**: BaseEntity, Soft Delete, JPA Auditing  
**OOP**: Builder, Record, Checker 패턴, SOLID 원칙  
**쿼리**: QueryDSL, 타입 안전 쿼리  
**테스트**: Mockito, JUnit 5, Given-When-Then  
**문서화**: Swagger, OpenAPI 3.0  
**예외**: GlobalExceptionHandler, BaseException 계층  
**DTO**: Record, Validation, 불변 객체







