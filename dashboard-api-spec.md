# Dashboard API 명세서

## 개요

Dashboard API는 홈 페이지에서 사용되는 4가지 주요 기능을 제공합니다:
1. **승인 대기 목록 조회**: 현재 사용자가 승인해야 하는 문서 목록
2. **진행중인 업무 목록 조회**: 현재 사용자가 승인한 배포 작업 중 진행 중인 항목
3. **알림 목록 조회**: 취소 및 반려 알림
4. **복구현황 목록 조회**: ROLLBACK 타입 Approval의 Deployment 목록

---

## 공통 사항

### Base URL
```
/api/dashboard
```

### 인증
모든 API는 JWT Bearer Token 인증이 필요합니다.

**헤더**:
```
Authorization: Bearer {accessToken}
```

### 공통 응답 형식

#### 성공 응답
- HTTP Status: `200 OK`
- Content-Type: `application/json`

#### 에러 응답

**401 Unauthorized**
```json
{
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다."
}
```

**500 Internal Server Error**
```json
{
  "error": "INTERNAL_SERVER_ERROR",
  "message": "서버 오류가 발생했습니다."
}
```

### 데이터 타입
- **날짜**: ISO 8601 형식 (`2025-10-27T10:30:00`)
- **날짜만**: `YYYY-MM-DD` 형식 (`2025-10-27`)
- **시간만**: `HH:mm` 형식 (`16:00`)

---

## 1. 승인 대기 목록 조회

현재 사용자가 승인해야 하는 Approval 리스트를 조회합니다.

### 엔드포인트
```
GET /api/dashboard/pending-approvals
```

### 요청

**헤더**:
```
Authorization: Bearer {accessToken}
```

**Query Parameters**: 없음

### 응답

**성공 (200 OK)**:
```json
{
  "data": [
    {
      "id": 101,
      "title": "결제 서비스 배포 작업 계획서",
      "docType": "작업계획서",
      "serviceName": ["결제 서비스", "결제 게이트웨이"],
      "requestedAt": "2025-10-27T10:30:00",
      "currentApprover": ["김리뷰", "이승인"],
      "registrant": "홍길동",
      "registrantDepartment": "IT",
      "description": "결제 서비스 신규 기능 배포를 위한 작업 계획서입니다.",
      "relatedServices": ["결제 서비스", "결제 게이트웨이"],
      "status": "승인 대기",
      "deployment": {
        "id": 1,
        "title": "결제 서비스 배포 작업 계획서",
        "status": "PENDING",
        "stage": "PLAN",
        "projectName": "결제 서비스",
        "scheduledDate": "2025-10-30",
        "scheduledTime": "16:00",
        "registrant": "홍길동",
        "registrantDepartment": "IT",
        "relatedServices": [
          {
            "id": 1,
            "name": "결제 서비스",
            "projectId": 1
          },
          {
            "id": 2,
            "name": "결제 게이트웨이",
            "projectId": 2
          }
        ]
      }
    }
  ]
}
```

### 응답 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | integer | 필수 | Approval ID |
| `title` | string | 필수 | 문서 제목 (deployment.title과 동일) |
| `docType` | string | 필수 | 문서 유형 (`작업계획서`, `결과보고`, `배포`, `재배포`, `복구`, `임시저장`) |
| `serviceName` | array[string] | 필수 | 서비스 이름 배열 (메인 프로젝트 + 관련 프로젝트) |
| `requestedAt` | datetime | 필수 | 요청 시각 (ISO 8601 형식, approval.createdAt) |
| `currentApprover` | array[string] | 필수 | 현재 승인 예정자 이름 배열 (is_approved=NULL인 approval_line의 account 이름) |
| `registrant` | string | 필수 | 등록자 이름 (deployment.issuer.name) |
| `registrantDepartment` | string | 필수 | 등록 부서명 (deployment.issuer.department.name) |
| `description` | string | 선택 | 설명 (deployment.content) |
| `relatedServices` | array[string] | 필수 | 연관 서비스 이름 배열 (serviceName과 동일) |
| `status` | string | 필수 | 상태 (`승인 대기`) |
| `deployment` | object | 필수 | Deployment 정보 (nested object) |
| `deployment.id` | integer | 필수 | Deployment ID |
| `deployment.title` | string | 필수 | 배포 제목 |
| `deployment.status` | enum | 필수 | Deployment 상태 (`PENDING`, `APPROVED`, `REJECTED`, `IN_PROGRESS`, `COMPLETED`, `CANCELED`) |
| `deployment.stage` | enum | 필수 | Deployment 단계 (`PLAN`, `DEPLOYMENT`, `REPORT`, `RETRY`, `ROLLBACK`, `DRAFT`) |
| `deployment.projectName` | string | 필수 | 프로젝트(서비스) 이름 |
| `deployment.scheduledDate` | date | 선택 | 예정 날짜 (YYYY-MM-DD, null 가능) |
| `deployment.scheduledTime` | time | 선택 | 예정 시간 (HH:mm, null 가능) |
| `deployment.registrant` | string | 필수 | 등록자 이름 |
| `deployment.registrantDepartment` | string | 필수 | 등록 부서명 |
| `deployment.relatedServices` | array[object] | 필수 | 연관 서비스 배열 |
| `deployment.relatedServices[].id` | integer | 필수 | 서비스 ID (project ID) |
| `deployment.relatedServices[].name` | string | 필수 | 서비스 이름 |
| `deployment.relatedServices[].projectId` | integer | 필수 | 프로젝트 ID |

### 필터 조건

- `approval_line` 테이블에서 현재 사용자(`accountId`)가 승인 라인에 포함된 항목
- `approval_line.is_approved`가 `NULL`인 항목만 (아직 승인하지 않은 항목)
- `deployment.status`가 `PENDING` 또는 `APPROVED`인 항목
- `approval.isDeleted = false` AND `deployment.isDeleted = false`
- 정렬: `deployment.updatedAt DESC` (최신순)

---

## 2. 진행중인 업무 목록 조회

현재 사용자가 승인한 Deployment 중, 진행 중인 상태인 항목을 조회합니다.

### 엔드포인트
```
GET /api/dashboard/in-progress-tasks
```

### 요청

**헤더**:
```
Authorization: Bearer {accessToken}
```

**Query Parameters**: 없음

### 응답

**성공 (200 OK)**:
```json
{
  "data": [
    {
      "id": 201,
      "title": "사용자 서비스 신규 배포",
      "date": "2025-10-30",
      "scheduledTime": "16:00",
      "status": "APPROVED",
      "stage": "DEPLOYMENT",
      "isDeployed": null,
      "service": "사용자 서비스",
      "registrant": "김민호",
      "registrantDepartment": "IT",
      "description": "승인은 완료되었고, 배포 시간까지 대기 중입니다.",
      "relatedServices": ["사용자 서비스", "인증 서비스"]
    }
  ]
}
```

### 응답 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | integer | 필수 | Deployment ID |
| `title` | string | 필수 | 배포 제목 |
| `date` | date | 선택 | 예정 날짜 (YYYY-MM-DD, null 가능) |
| `scheduledTime` | time | 선택 | 예정 시간 (HH:mm, null 가능) |
| `status` | enum | 필수 | Deployment 상태 (`APPROVED`, `IN_PROGRESS`) |
| `stage` | enum | 필수 | Deployment 단계 (`PLAN`, `DEPLOYMENT`) |
| `isDeployed` | boolean | 선택 | Jenkins 배포 성공 여부 (null: 배포 전, true: 성공, false: 실패) |
| `service` | string | 필수 | 서비스 이름 (project.name) |
| `registrant` | string | 필수 | 등록자 이름 (deployment.issuer.name) |
| `registrantDepartment` | string | 필수 | 등록 부서명 (deployment.issuer.department.name) |
| `description` | string | 선택 | 설명 (deployment.content) |
| `relatedServices` | array[string] | 필수 | 연관 서비스 이름 배열 (메인 프로젝트 + 관련 프로젝트) |

### 필터 조건

- `approval_line` 테이블에서 현재 사용자가 승인한 항목 (`is_approved = true`)
- 다음 stage-status 조합 중 하나:
  - `deployment.stage = 'PLAN'` AND `deployment.status = 'APPROVED'`
  - `deployment.stage = 'DEPLOYMENT'` AND `deployment.status = 'PENDING'`
- `approval.isDeleted = false` AND `deployment.isDeleted = false`
- 정렬: `deployment.updatedAt DESC` (최신순)

---

## 3. 알림 목록 조회

현재 사용자와 관련된 "취소" 및 "반려" 알림을 조회합니다.

### 엔드포인트
```
GET /api/dashboard/notifications
```

### 요청

**헤더**:
```
Authorization: Bearer {accessToken}
```

**Query Parameters**: 없음

### 응답

**성공 (200 OK)**:
```json
{
  "data": [
    {
      "id": 301,
      "kind": "취소",
      "reason": "작업 금지 기간에 해당되어 자동 취소되었습니다.",
      "serviceName": "결제 서비스",
      "deploymentId": 101,
      "deploymentTitle": "결제 서비스 배포 작업",
      "canceledAt": "2025-10-27T11:00:00",
      "rejectedAt": null
    },
    {
      "id": 302,
      "kind": "반려",
      "reason": "모니터링 계획이 부족하여 팀장에 의해 반려되었습니다.",
      "serviceName": "알림 서비스",
      "deploymentId": 102,
      "deploymentTitle": "알림 서비스 배포 작업",
      "canceledAt": null,
      "rejectedAt": "2025-10-28T16:20:00"
    }
  ]
}
```

### 응답 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | integer | 필수 | Deployment ID (알림 고유 ID가 없으므로 deployment ID 사용) |
| `kind` | enum | 필수 | 알림 종류 (`취소`, `반려`) |
| `reason` | string | 필수 | 사유 (취소/반려 사유, 반려의 경우 ApprovalLine.comment) |
| `serviceName` | string | 필수 | 서비스 이름 (deployment.project.name) |
| `deploymentId` | integer | 필수 | Deployment ID |
| `deploymentTitle` | string | 필수 | 배포 제목 |
| `canceledAt` | datetime | 선택 | 취소 시각 (kind가 '취소'일 때만 제공, deployment.updatedAt) |
| `rejectedAt` | datetime | 선택 | 반려 시각 (kind가 '반려'일 때만 제공, deployment.updatedAt) |

### 필터 조건

**"취소" 알림**:
- `deployment.status = 'CANCELED'`
- 현재 사용자가 `approval_line`에서 승인한 deployment (`is_approved = true`)
- `canceledAt` 기준 최신순 정렬

**"반려" 알림**:
다음 두 가지 경우를 포함합니다:

1. **현재 사용자가 요청한 deployment가 반려된 경우**:
   - `deployment.status = 'REJECTED'`
   - `deployment.issuer.id = current_user_id`

2. **현재 사용자가 승인한 approval이 다른 사람에 의해 반려된 경우**:
   - `deployment.status = 'REJECTED'` 또는 `approval.status = 'REJECTED'`
   - `approval_line`에서 현재 사용자가 승인한 항목 (`is_approved = true`)
   - 해당 approval의 approval_lines 중 `is_approved = false`인 항목이 존재
   - 반려 시각이 현재 사용자의 승인 시각보다 이후

**정렬**: `canceledAt` 또는 `rejectedAt` 기준 최신순 (DESC)

---

## 4. 복구현황 목록 조회

`approval.type = 'ROLLBACK'`인 Approval과 연결된 Deployment 리스트를 페이지네이션으로 조회합니다.

### 엔드포인트
```
GET /api/dashboard/recovery
```

### 요청

**헤더**:
```
Authorization: Bearer {accessToken}
```

**Query Parameters**:

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | 선택 | `1` | 페이지 번호 (1부터 시작) |
| `pageSize` | integer | 선택 | `5` | 페이지당 항목 수 |

### 응답

**성공 (200 OK)**:
```json
{
  "data": [
    {
      "id": 201,
      "title": "결제 서비스 DB 마이그레이션 작업",
      "service": "결제 서비스",
      "status": "COMPLETED",
      "duration": "42분",
      "recoveredAt": "2025-10-29T16:04:00",
      "registrant": "홍길동",
      "registrantDepartment": "IT",
      "deploymentId": 201
    },
    {
      "id": 202,
      "title": "진행중인 복구 작업",
      "service": "결제 서비스",
      "status": "IN_PROGRESS",
      "duration": null,
      "recoveredAt": null,
      "registrant": "홍길동",
      "registrantDepartment": "IT",
      "deploymentId": 202
    },
    {
      "id": 203,
      "title": "대기중인 복구 작업",
      "service": "결제 서비스",
      "status": "PENDING",
      "duration": null,
      "recoveredAt": null,
      "registrant": "홍길동",
      "registrantDepartment": "IT",
      "deploymentId": 203
    }
  ],
  "pagination": {
    "total": 12,
    "page": 1,
    "pageSize": 5,
    "totalPages": 3
  }
}
```

### 응답 필드 설명

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `data` | array[object] | 필수 | 복구현황 목록 |
| `data[].id` | integer | 필수 | 복구현황 고유 ID (deployment ID) |
| `data[].title` | string | 필수 | 배포작업명 (deployment.title) |
| `data[].service` | string | 필수 | 서비스명 (deployment.project.name) |
| `data[].status` | enum | 필수 | 복구 상태 (`PENDING`, `IN_PROGRESS`, `COMPLETED`) |
| `data[].duration` | string | 선택 | 소요시간 (예: "42분") - status가 `COMPLETED`이고 BuildRun이 존재할 때만 제공 |
| `data[].recoveredAt` | datetime | 선택 | 복구 완료 시각 (ISO 8601 형식) - status가 `COMPLETED`이고 BuildRun이 존재할 때만 제공 |
| `data[].registrant` | string | 필수 | 등록자 이름 (deployment.issuer.name) |
| `data[].registrantDepartment` | string | 필수 | 등록 부서명 (deployment.issuer.department.koreanName) |
| `data[].deploymentId` | integer | 필수 | 원본 Deployment ID |
| `pagination` | object | 필수 | 페이지네이션 정보 |
| `pagination.total` | integer | 필수 | 전체 항목 수 |
| `pagination.page` | integer | 필수 | 현재 페이지 번호 |
| `pagination.pageSize` | integer | 필수 | 페이지당 항목 수 |
| `pagination.totalPages` | integer | 필수 | 전체 페이지 수 |

### 필터 조건

- `approval.type = 'ROLLBACK'`
- `approval.isDeleted = false` AND `deployment.isDeleted = false`
- 정렬: `deployment.createdAt DESC` (최신순)
- 페이지네이션: 기본 `pageSize = 5`, `page` 파라미터로 페이지 번호 지정

### 상태값 결정 로직

복구 상태(`status`)는 deployment의 현재 상태에 따라 결정됩니다:

- **`COMPLETED`**: `deployment.status = 'COMPLETED'` 또는 `deployment.isDeployed = true`
- **`IN_PROGRESS`**: `deployment.status = 'IN_PROGRESS'`
- **`PENDING`**: 그 외 (기본값)

### duration 및 recoveredAt 계산 로직

- `status`가 `COMPLETED`이고 해당 deployment의 BuildRun이 존재하는 경우에만 계산됩니다.
- **`duration`**: 첫 번째 BuildRun.startedAt ~ 마지막 BuildRun.endedAt의 차이를 분 단위로 계산 (예: "42분")
- **`recoveredAt`**: 마지막 BuildRun.endedAt 사용
- `status`가 `COMPLETED`가 아니거나 BuildRun이 없는 경우, `duration`과 `recoveredAt`은 `null`입니다.

---

## API 엔드포인트 요약

| 엔드포인트 | 메서드 | 설명 | 페이지네이션 |
|-----------|--------|------|-------------|
| `/api/dashboard/pending-approvals` | GET | 승인 대기 목록 조회 | 없음 |
| `/api/dashboard/in-progress-tasks` | GET | 진행중인 업무 목록 조회 | 없음 |
| `/api/dashboard/notifications` | GET | 알림 목록 조회 | 없음 |
| `/api/dashboard/recovery` | GET | 복구현황 목록 조회 | 있음 (기본 5개) |

---

## 참고 사항

1. **승인 대기**: `approval_line` 테이블에서 현재 사용자가 다음 승인자로 지정되어 있고 아직 승인하지 않은 항목만 조회합니다.

2. **진행중인 업무**: 현재 사용자가 승인한 deployment 중, 아직 완료되지 않은 항목을 조회합니다. `PLAN + APPROVED` 또는 `DEPLOYMENT + PENDING` 조합만 포함됩니다.

3. **알림**: 알림 테이블이 없으므로 deployment의 상태 변경 이력을 기반으로 알림을 생성합니다. 취소 알림과 반려 알림을 통합하여 최신순으로 정렬합니다.

4. **복구현황**: `approval.type = 'ROLLBACK'`인 Approval과 연결된 deployment를 조회합니다. 복구 상태는 deployment의 현재 상태에 따라 결정되며, BuildRun 데이터를 기반으로 duration과 recoveredAt을 계산합니다.

5. **N+1 쿼리 최적화**: 모든 API에서 관련 프로젝트, BuildRun 등의 데이터를 배치 조회하여 N+1 쿼리 문제를 방지합니다.






