# 인증 시스템 핵심 요약 (PPT용)

## 1️⃣ 인증 아키텍처
**GitHub OAuth2 + JWT Stateless**

```
GitHub OAuth2 → JWT 토큰 발급 → Stateless 인증
- OAuth2 Provider: GitHub
- 토큰: JWT (Access/Refresh/SignToken)
- 세션: Stateless (서버 확장성)
- 저장소: Redis
```

---

## 2️⃣ 토큰 관리 시스템
**3단계 토큰 + Redis 캐싱**

| 토큰 타입 | 용도 | 유효기간 | 저장 위치 |
|---------|------|---------|----------|
| **Access Token** | API 인증 | 짧음 | 클라이언트 |
| **Refresh Token** | 토큰 갱신 | 2주 | 클라이언트 + Redis |
| **SignToken** | 회원가입 | 5분 | 클라이언트 |

**Redis 구조**
- `refresh:{accountId}` → JTI (토큰 검증)
- `sign:{githubId}` → 임시 정보

---

## 3️⃣ 보안 메커니즘
**Token Rotation + JTI 검증**

- ✅ **Token Rotation**: Refresh 시 새 토큰 발급, 기존 무효화
- ✅ **JTI 검증**: Redis 저장값과 비교 (탈취 방지)
- ✅ **1회용 토큰**: 사용 후 즉시 삭제
- ✅ **TTL 관리**: 자동 만료 (5분/2주)

---

## 4️⃣ 역할 기반 권한
**3단계 역할 체계**

```
HEAD (최고 권한)
  ↓
MANAGER (관리 권한)
  ↓
DEVELOPER (기본 권한)
```

**권한 처리 흐름**
1. JWT Access Token에 `role` 포함
2. JwtAuthenticationFilter에서 `ROLE_*` 형식으로 변환
3. SecurityContext에 권한 설정
4. `@AuthenticationPrincipal`로 컨트롤러에서 접근

**역할 타입**
- `DEVELOPER` → `ROLE_DEVELOPER`
- `MANAGER` → `ROLE_MANAGER`
- `HEAD` → `ROLE_HEAD`

---

## 인증 플로우 요약

### 신규 사용자
```
GitHub OAuth → SignToken 발급 → 회원가입 → Access/Refresh Token
```

### 기존 사용자
```
GitHub OAuth → Temporary Code → 로그인 → Access/Refresh Token
```

### 토큰 갱신
```
Refresh Token → JTI 검증 → Token Rotation → 새 토큰 발급
```

---

## 핵심 키워드

**인증**: GitHub OAuth2, JWT, Stateless  
**토큰**: Access/Refresh/SignToken, Token Rotation  
**보안**: JTI 검증, 1회용 토큰, TTL 관리  
**권한**: Role-Based (DEVELOPER/MANAGER/HEAD)  
**저장소**: Redis (토큰 검증)







