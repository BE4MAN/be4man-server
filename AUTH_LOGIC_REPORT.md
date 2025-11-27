# 인증 시스템 비즈니스 로직 요약 보고서

## 📋 PPT용 핵심 요약 (3~4개)

### 1️⃣ 인증 아키텍처
**GitHub OAuth2 + JWT Stateless 인증**
- OAuth2 Provider: GitHub
- 토큰 타입: JWT (Access/Refresh/SignToken)
- 세션: Stateless (서버 확장성)
- 저장소: Redis (토큰 검증)

### 2️⃣ 토큰 관리 시스템
**3단계 토큰 + Redis 캐싱**
- **Access Token**: API 인증 (accountId, role 포함)
- **Refresh Token**: 토큰 갱신 (2주, Redis JTI 저장)
- **SignToken**: 회원가입 임시 인증 (5분)
- **Redis 구조**: `refresh:{accountId}` → JTI, `sign:{githubId}` → 임시정보

### 3️⃣ 보안 메커니즘
**Token Rotation + JTI 검증**
- Token Rotation: Refresh 시 새 토큰 발급, 기존 토큰 무효화
- JTI 검증: Redis 저장값과 비교하여 탈취 방지
- 1회용 토큰: SignToken, Temporary Code (사용 후 삭제)
- TTL 관리: 자동 만료 (5분/2주)

### 4️⃣ 역할 기반 권한 (Role-Based Authorization)
**3단계 역할 체계**
- **역할 타입**: `DEVELOPER`, `MANAGER`, `HEAD`
- **JWT 페이로드**: Access Token에 `role` 포함
- **권한 설정**: `ROLE_DEVELOPER`, `ROLE_MANAGER`, `ROLE_HEAD` (Spring Security GrantedAuthority)
- **인증 컨텍스트**: `AccountPrincipal(accountId, role)` → `@AuthenticationPrincipal`로 접근
- **권한 검증**: JwtAuthenticationFilter에서 SecurityContext에 권한 설정

---

## 개요
BE4MAN 서버는 **GitHub OAuth2** 기반의 인증 시스템을 사용하며, **JWT 토큰**과 **Redis**를 활용하여 안전하고 확장 가능한 인증 메커니즘을 구현합니다.

---

## 주요 구성 요소

### 1. 인증 방식
- **OAuth2 Provider**: GitHub
- **토큰 타입**: JWT (JSON Web Token)
- **세션 관리**: Stateless (JWT 기반)
- **캐시 저장소**: Redis

### 2. 토큰 종류

#### Access Token
- **용도**: API 요청 시 인증에 사용
- **페이로드**: `accountId`, `role`
- **유효 기간**: 설정값에 따라 결정 (일반적으로 짧음)
- **저장 위치**: 클라이언트 (메모리 또는 로컬 스토리지)

#### Refresh Token
- **용도**: Access Token 갱신에 사용
- **페이로드**: `accountId`, `jti` (JWT ID), `type: "refresh"`
- **유효 기간**: 2주
- **저장 위치**: 
  - 클라이언트: JWT 형식으로 전달
  - 서버: Redis에 JTI만 저장 (`refresh:{accountId}` → `jti`)

#### SignToken
- **용도**: 신규 회원가입 시 임시 인증 토큰
- **페이로드**: `githubId`, `type: "sign"`
- **유효 기간**: 5분
- **저장 위치**: 클라이언트 (회원가입 완료 전까지)

---

## 인증 플로우

### 시나리오 1: 신규 사용자 회원가입

```
1. 사용자가 GitHub OAuth 로그인 클릭
   ↓
2. GitHub 인증 완료 후 백엔드로 리다이렉트
   ↓
3. OAuth2SuccessHandler 실행
   - GitHub 사용자 정보 추출 (githubId, email, profileImageUrl, githubAccessToken)
   - DB에서 계정 조회 (githubId 기준)
   - 계정이 없으면 → 신규 사용자 처리
   ↓
4. 신규 사용자 처리
   - GitHub 정보를 Redis에 임시 저장 (5분 TTL)
     Key: "sign:{githubId}"
     Value: GitHubTempInfo (email, githubAccessToken, profileImageUrl)
   - SignToken 생성 (githubId 포함, 5분 유효)
   - 프론트엔드로 리다이렉트
     URL: {frontendUrl}/auth/callback#requires_signup=true&sign_token={signToken}
   ↓
5. 프론트엔드에서 회원가입 폼 표시
   ↓
6. 사용자가 회원가입 정보 입력 (name, department, position)
   ↓
7. POST /api/auth/signup 요청
   - Authorization 헤더: Bearer {signToken}
   - Request Body: { name, department, position }
   ↓
8. AuthService.signup() 실행
   - SignToken 검증
   - SignToken에서 githubId 추출
   - Redis에서 GitHub 정보 조회 및 삭제 (1회용)
   - 중복 계정 확인
   - 신규 Account 엔티티 생성 및 저장
   - Access Token + Refresh Token 발급
   ↓
9. 클라이언트에 토큰 전달
   Response: { accessToken, refreshToken }
```

### 시나리오 2: 기존 사용자 로그인

```
1. 사용자가 GitHub OAuth 로그인 클릭
   ↓
2. GitHub 인증 완료 후 백엔드로 리다이렉트
   ↓
3. OAuth2SuccessHandler 실행
   - GitHub 사용자 정보 추출
   - DB에서 계정 조회 (githubId 기준)
   - 계정이 있으면 → 기존 사용자 처리
   ↓
4. 기존 사용자 처리
   - GitHub Access Token 업데이트
   - Temporary Code 생성 (UUID, 5분 유효)
   - Redis에 저장: Key: "oauth:code:{tempCode}", Value: accountId
   - 프론트엔드로 리다이렉트
     URL: {frontendUrl}/auth/callback#requires_signup=false&code={tempCode}
   ↓
5. 프론트엔드에서 로그인 처리
   ↓
6. POST /api/auth/signin 요청
   - Request Body: { code: tempCode }
   ↓
7. AuthService.signin() 실행
   - Temporary Code로 accountId 조회 및 삭제 (1회용)
   - Account 조회
   - Access Token + Refresh Token 발급
   ↓
8. 클라이언트에 토큰 전달
   Response: { accessToken, refreshToken }
```

### 시나리오 3: 토큰 갱신 (Token Rotation)

```
1. Access Token 만료 또는 갱신 필요 시
   ↓
2. POST /api/auth/refresh 요청
   - Request Body: { refreshToken }
   ↓
3. AuthService.refresh() 실행
   - Refresh Token JWT 검증 (만료, 서명, 형식)
   - Refresh Token에서 accountId, jti 추출
   - Redis에서 저장된 JTI 조회
   - JTI 일치 여부 확인 (토큰 탈취 방지)
   - Account 조회
   - 새로운 Access Token + Refresh Token 발급 (Token Rotation)
   - 기존 Refresh Token은 무효화됨
   ↓
4. 클라이언트에 새 토큰 전달
   Response: { accessToken, refreshToken }
```

### 시나리오 4: API 요청 인증

```
1. 클라이언트가 API 요청
   - Authorization 헤더: Bearer {accessToken}
   ↓
2. JwtAuthenticationFilter 실행
   - 요청 경로가 화이트리스트에 있는지 확인
   - Authorization 헤더에서 토큰 추출
   - JWT 토큰 검증 (만료, 서명, 형식)
   - 토큰에서 accountId, role 추출
   - SecurityContext에 인증 정보 설정
   ↓
3. Controller에서 @AuthenticationPrincipal로 사용자 정보 접근
   - AccountPrincipal 객체로 accountId, role 사용 가능
```

### 시나리오 5: 로그아웃

```
1. POST /api/auth/logout 요청
   - Authorization 헤더: Bearer {accessToken}
   ↓
2. AuthService.logout() 실행
   - SecurityContext에서 accountId 추출
   - Redis에서 Refresh Token 삭제
   ↓
3. 로그아웃 완료
   - Access Token은 클라이언트에서 삭제
   - Refresh Token은 서버에서 삭제되어 재사용 불가
```

---

## 보안 기능

### 1. Token Rotation
- Refresh Token 사용 시 새로운 Refresh Token도 함께 발급
- 기존 Refresh Token은 즉시 무효화되어 토큰 탈취 시 피해 최소화

### 2. JTI (JWT ID) 검증
- Refresh Token에 고유한 JTI 포함
- Redis에 JTI 저장하여 토큰 유효성 이중 검증
- JTI 불일치 시 인증 실패 (토큰 재사용 방지)

### 3. 1회용 토큰/코드
- **SignToken**: 회원가입 완료 후 Redis에서 삭제
- **Temporary Code**: 로그인 시 1회 사용 후 삭제
- 재사용 방지로 보안 강화

### 4. TTL (Time To Live) 관리
- **SignToken**: 5분
- **Temporary Code**: 5분
- **Refresh Token**: 2주
- Redis TTL로 자동 만료 관리

### 5. Stateless 인증
- 세션 없이 JWT만으로 인증 처리
- 서버 확장성 향상
- CSRF 공격 방어 (JWT 사용으로 CSRF 비활성화)

---

## Redis 저장 구조

### Refresh Token
```
Key: "refresh:{accountId}"
Value: "{jti}" (JWT ID)
TTL: 14일
```

### SignToken 임시 정보
```
Key: "sign:{githubId}"
Value: GitHubTempInfo (email, githubAccessToken, profileImageUrl)
TTL: 5분
```

### OAuth Temporary Code
```
Key: "oauth:code:{tempCode}"
Value: "{accountId}"
TTL: 5분
```

---

## Spring Security 설정

### SecurityConfig 주요 설정

1. **CSRF 비활성화**: JWT 사용으로 불필요
2. **CORS 설정**: 프론트엔드 도메인 허용
3. **Form Login 비활성화**: JWT 사용
4. **HTTP Basic 비활성화**: JWT 사용
5. **세션 관리**: STATELESS 모드
6. **OAuth2 로그인**: GitHub 연동
7. **JWT 필터**: 모든 요청에 JWT 검증 필터 적용

### 화이트리스트 경로
- `/api/health` - 헬스 체크
- `/swagger-ui/**`, `/api-docs/**` - API 문서
- `/oauth/**`, `/oauth2/**` - OAuth2 콜백
- `/api/auth/signup`, `/api/auth/signin`, `/api/auth/refresh` - 인증 API
- `/public/**` - 공개 API

---

## 역할 기반 권한 처리 (Role-Based Authorization)

### 역할 타입
```java
public enum Role {
    DEVELOPER,  // 개발자
    MANAGER,    // 관리자
    HEAD        // 책임자
}
```

### 권한 처리 흐름

#### 1. JWT 토큰에 역할 포함
- **Access Token 생성 시**: `JwtProvider.generateAccessToken(accountId, role)`
- **페이로드**: `{ accountId, role: "DEVELOPER|MANAGER|HEAD" }`

#### 2. 인증 필터에서 권한 설정
- **JwtAuthenticationFilter**: JWT에서 `role` 추출
- **Spring Security 권한 변환**: `"ROLE_" + role.name()` 형식으로 GrantedAuthority 생성
  - `DEVELOPER` → `ROLE_DEVELOPER`
  - `MANAGER` → `ROLE_MANAGER`
  - `HEAD` → `ROLE_HEAD`
- **SecurityContext 설정**: `UsernamePasswordAuthenticationToken`에 권한 포함

#### 3. 컨트롤러에서 권한 접근
```java
@GetMapping("/api/resource")
public ResponseEntity<?> getResource(
    @AuthenticationPrincipal AccountPrincipal principal
) {
    Long accountId = principal.accountId();
    Role role = principal.role();  // DEVELOPER, MANAGER, HEAD
    
    // 역할 기반 비즈니스 로직 처리
    // 예: MANAGER 이상만 접근 가능한 리소스
}
```

#### 4. 권한 검증 방법
- **Spring Security 어노테이션** (구현 가능):
  - `@PreAuthorize("hasRole('MANAGER')")` - MANAGER 이상만 접근
  - `@PreAuthorize("hasRole('HEAD')")` - HEAD만 접근
- **수동 검증**:
  - `AccountPrincipal`에서 `role` 확인
  - 비즈니스 로직에서 역할별 분기 처리

### 권한 계층 구조
```
HEAD (최고 권한)
  ↓
MANAGER (관리 권한)
  ↓
DEVELOPER (기본 권한)
```

### 회원가입 시 기본 역할
- 신규 계정 생성 시 기본값: `Role.DEVELOPER`
- 이후 관리자가 역할 변경 가능 (구현 필요)

---

## 주요 클래스 역할

### Controller
- **AuthController**: 인증 관련 API 엔드포인트 제공
  - `POST /api/auth/signup` - 회원가입
  - `POST /api/auth/signin` - 로그인
  - `POST /api/auth/refresh` - 토큰 갱신
  - `POST /api/auth/logout` - 로그아웃

### Service
- **AuthService/AuthServiceImpl**: 인증 비즈니스 로직
- **RefreshTokenRedisService**: Refresh Token Redis 관리
- **SignTokenRedisService**: SignToken 임시 정보 Redis 관리
- **OAuthCodeRedisService**: OAuth Temporary Code Redis 관리

### JWT
- **JwtProvider**: JWT 토큰 생성 및 검증 (role 포함)
- **JwtAuthenticationFilter**: 요청마다 JWT 검증 및 권한 설정

### OAuth2
- **CustomOAuth2UserService**: GitHub 사용자 정보 조회 및 처리
- **OAuth2SuccessHandler**: OAuth2 성공 후 리다이렉트 및 토큰/코드 발급

### DTO
- **AccountPrincipal**: 인증된 사용자 정보 (accountId, role)

---

## 예외 처리

### AuthExceptionType
- `INVALID_SIGN_TOKEN` - 유효하지 않은 SignToken
- `SIGN_TOKEN_INFO_NOT_FOUND` - SignToken 정보 없음
- `INVALID_TEMP_CODE` - 유효하지 않은 임시 코드
- `INVALID_REFRESH_TOKEN` - 유효하지 않은 Refresh Token
- `REFRESH_TOKEN_NOT_FOUND` - Refresh Token 없음
- `REFRESH_TOKEN_MISMATCH` - Refresh Token JTI 불일치

모든 예외는 `401 Unauthorized` 상태 코드로 반환됩니다.

---

## 특징 및 장점

1. **OAuth2 기반**: GitHub 계정으로 간편 로그인
2. **JWT 기반 Stateless**: 서버 확장성 및 성능 향상
3. **Token Rotation**: 보안 강화
4. **Redis 활용**: 빠른 토큰 검증 및 관리
5. **1회용 토큰/코드**: 재사용 방지
6. **명확한 플로우**: 신규/기존 사용자 분리 처리
7. **Spring Security 통합**: 표준 보안 프레임워크 활용
8. **역할 기반 권한**: 3단계 역할 체계로 세밀한 접근 제어

---

## 개선 가능한 영역

1. **Access Token 만료 시간**: 현재 설정값 확인 필요
2. **Refresh Token 갱신 전략**: 자동 갱신 로직 고려
3. **토큰 블랙리스트**: 로그아웃된 Access Token 관리 (현재는 Refresh Token만 관리)
4. **다중 기기 로그인**: 동일 계정의 여러 기기에서 로그인 시 처리 전략
5. **보안 이벤트 로깅**: 의심스러운 인증 시도 추적

---

_작성일: 2025-01-27_
_버전: 1.0_

