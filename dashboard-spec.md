# 홈(Dashboard) 페이지 API 스펙

## 개요

홈 페이지는 사용자에게 승인 대기 문서, 진행중인 업무, 알림, 주간 일정, 복구현황을 표시합니다.

---

## 1. 상단 3개 섹션

### 1-1. 승인 대기 목록 조회

**GET** `/api/dashboard/pending-approvals`

#### 기능

현재 사용자가 승인해야 하는 approval 리스트를 조회합니다. `approval_line`에서 현재 사용자가 있고, `is_approved`가 `NULL`인 항목만 반환합니다.

#### Query Parameters

없음 (인증된 사용자 정보를 기반으로 자동 필터링)

#### Response

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
      "registrantDepartment": "DevOps팀",
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
        "registrantDepartment": "DevOps팀",
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

#### Response 필드 설명

| 필드                                     | 타입     | 필수 | 설명                                                                                        |
| ---------------------------------------- | -------- | ---- | ------------------------------------------------------------------------------------------- |
| `id`                                     | integer  | 필수 | Approval ID                                                                                 |
| `title`                                  | string   | 필수 | 문서 제목 (deployment title과 동일)                                                         |
| `docType`                                | string   | 필수 | 문서 유형 (`작업계획서`, `결과보고`)                                                        |
| `serviceName`                            | array    | 필수 | 서비스 이름 배열 (deployment.relatedServices에서 추출)                                      |
| `requestedAt`                            | datetime | 필수 | 요청 시각 (ISO 8601 형식)                                                                   |
| `currentApprover`                        | array    | 필수 | 현재 승인 예정자 이름 배열 (approval_line에서 is_approved=NULL인 사용자 이름)               |
| `registrant`                             | string   | 필수 | 등록자 이름                                                                                 |
| `registrantDepartment`                   | string   | 필수 | 등록 부서명                                                                                 |
| `description`                            | string   | 선택 | 설명 (deployment description 또는 계획서 내용)                                              |
| `relatedServices`                        | array    | 필수 | 연관 서비스 이름 배열                                                                       |
| `status`                                 | string   | 필수 | 상태 (`승인 대기`)                                                                          |
| `deployment`                             | object   | 필수 | Deployment 정보 (nested object)                                                             |
| `deployment.id`                          | integer  | 필수 | Deployment ID                                                                               |
| `deployment.title`                       | string   | 필수 | 배포 제목                                                                                   |
| `deployment.status`                      | enum     | 필수 | Deployment 상태 (`PENDING`, `APPROVED`, `REJECTED`, `IN_PROGRESS`, `COMPLETED`, `CANCELED`) |
| `deployment.stage`                       | enum     | 필수 | Deployment 단계 (`PLAN`, `DEPLOYMENT`, `REPORT`, `RETRY`, `ROLLBACK`, `DRAFT`)              |
| `deployment.projectName`                 | string   | 필수 | 프로젝트(서비스) 이름                                                                       |
| `deployment.scheduledDate`               | string   | 필수 | 예정 날짜 (YYYY-MM-DD)                                                                      |
| `deployment.scheduledTime`               | string   | 필수 | 예정 시간 (HH:mm)                                                                           |
| `deployment.registrant`                  | string   | 필수 | 등록자 이름                                                                                 |
| `deployment.registrantDepartment`        | string   | 필수 | 등록 부서명                                                                                 |
| `deployment.relatedServices`             | array    | 필수 | 연관 서비스 배열 (nested object, relation table에서 조인)                                   |
| `deployment.relatedServices[].id`        | integer  | 필수 | 서비스 ID (relation table의 projectId)                                                      |
| `deployment.relatedServices[].name`      | string   | 필수 | 서비스 이름 (project.name)                                                                  |
| `deployment.relatedServices[].projectId` | integer  | 필수 | 프로젝트 ID (relation table의 projectId, 중복 데이터)                                       |

#### 필터 조건

- `approval_line` 테이블에서 현재 사용자(`current_user_id`)가 승인 라인에 포함된 항목
- `approval_line.is_approved`가 `NULL`인 항목만 (아직 승인하지 않은 항목)
- `deployment.status`가 `PENDING` 또는 `APPROVED`인 항목
- 최신순 정렬 (`requestedAt DESC`)

---

### 1-2. 진행중인 업무 목록 조회

**GET** `/api/dashboard/in-progress-tasks`

#### 기능

현재 사용자가 승인한 deployment 중, 진행중인 상태인 항목을 조회합니다.

#### Query Parameters

없음 (인증된 사용자 정보를 기반으로 자동 필터링)

#### Response

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
      "registrantDepartment": "DevOps팀",
      "description": "승인은 완료되었고, 배포 시간까지 대기 중입니다.",
      "relatedServices": ["사용자 서비스", "인증 서비스"]
    }
  ]
}
```

#### Response 필드 설명

| 필드                   | 타입    | 필수 | 설명                                                            |
| ---------------------- | ------- | ---- | --------------------------------------------------------------- |
| `id`                   | integer | 필수 | Deployment ID                                                   |
| `title`                | string  | 필수 | 배포 제목                                                       |
| `date`                 | string  | 필수 | 예정 날짜 (YYYY-MM-DD)                                          |
| `scheduledTime`        | string  | 필수 | 예정 시간 (HH:mm)                                               |
| `status`               | enum    | 필수 | Deployment 상태 (`APPROVED`, `IN_PROGRESS`)                     |
| `stage`                | enum    | 필수 | Deployment 단계 (`PLAN`, `DEPLOYMENT`)                          |
| `isDeployed`           | boolean | 선택 | Jenkins 배포 성공 여부 (null: 배포 전, true: 성공, false: 실패) |
| `service`              | string  | 필수 | 서비스 이름 (projectName)                                       |
| `registrant`           | string  | 필수 | 등록자 이름                                                     |
| `registrantDepartment` | string  | 필수 | 등록 부서명                                                     |
| `description`          | string  | 선택 | 설명                                                            |
| `relatedServices`      | array   | 필수 | 연관 서비스 이름 배열                                           |

#### 필터 조건

- `approval_line` 테이블에서 현재 사용자가 승인한 항목 (`is_approved = true`)
- 다음 stage-status 조합 중 하나:
  - `deployment.stage = 'PLAN'` AND `deployment.status = 'APPROVED'`
  - `deployment.stage = 'DEPLOYMENT'` AND `deployment.status = 'PENDING'`
- 최신순 정렬 (`deployment.updatedAt DESC`)

---

### 1-3. 알림 목록 조회

**GET** `/api/dashboard/notifications`

#### 기능

현재 사용자와 관련된 "취소" 및 "반려" 알림을 조회합니다. 알림 테이블이 없으므로 deployment 데이터에서 추출합니다.

#### Query Parameters

없음 (인증된 사용자 정보를 기반으로 자동 필터링)

#### Response

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
      "canceledAt": "2025-10-27T11:00:00"
    },
    {
      "id": 302,
      "kind": "반려",
      "reason": "모니터링 계획이 부족하여 팀장에 의해 반려되었습니다.",
      "serviceName": "알림 서비스",
      "deploymentId": 102,
      "deploymentTitle": "알림 서비스 배포 작업",
      "rejectedAt": "2025-10-28T16:20:00"
    }
  ]
}
```

#### Response 필드 설명

| 필드              | 타입     | 필수 | 설명                                                       |
| ----------------- | -------- | ---- | ---------------------------------------------------------- |
| `id`              | integer  | 필수 | Deployment ID (알림 고유 ID가 없으므로 deployment ID 사용) |
| `kind`            | enum     | 필수 | 알림 종류 (`취소`, `반려`)                                 |
| `reason`          | string   | 필수 | 사유 (취소/반려 사유, 반려의 경우 ApprovalLine.comment)    |
| `serviceName`     | string   | 필수 | 서비스 이름 (deployment.projectName)                       |
| `deploymentId`    | integer  | 필수 | Deployment ID                                              |
| `deploymentTitle` | string   | 필수 | 배포 제목                                                  |
| `canceledAt`      | datetime | 선택 | 취소 시각 (kind가 '취소'일 때만 제공, deployment.updatedAt) |
| `rejectedAt`      | datetime | 선택 | 반려 시각 (kind가 '반려'일 때만 제공, deployment.updatedAt 또는 approval.updatedAt) |

#### 필터 조건

**"취소" 알림:**

- `deployment.status = 'CANCELED'`
- 현재 사용자가 `approval_line`에서 승인한 deployment (`is_approved = true`)
- 새로운 ban 등록으로 인해 자동 취소된 경우 포함
- `canceledAt` 기준 최신순 정렬

**"반려" 알림:**

- 다음 두 가지 경우:
  1. 현재 사용자가 요청한 deployment가 반려된 경우:
     - `deployment.status = 'REJECTED'`
     - `deployment.issuer.id = current_user_id`
  2. 현재 사용자가 승인한 approval(deployment)이 다른 사람에 의해 반려된 경우:
     - `deployment.status = 'REJECTED'` 또는 `approval.status = 'REJECTED'`
     - `approval_line`에서 현재 사용자가 승인한 항목 (`is_approved = true`)
     - 해당 approval의 approval_lines 중 `is_approved = false`인 항목이 존재
     - 반려 시각이 현재 사용자의 승인 시각보다 이후
- `rejectedAt` 또는 `updatedAt` 기준 최신순 정렬

---

## 2. 주간 캘린더 섹션

주간 캘린더는 스케줄 관리 페이지와 동일한 데이터를 사용합니다. 기존 API를 재사용합니다.

### 2-1. 배포 작업 목록 조회 (기존 API 재사용)

**GET** `/api/schedules/deployments`

#### Query Parameters

| 파라미터    | 타입   | 필수 | 설명                |
| ----------- | ------ | ---- | ------------------- |
| `startDate` | string | 필수 | 시작일 (YYYY-MM-DD) |
| `endDate`   | string | 필수 | 종료일 (YYYY-MM-DD) |

#### Response

스케줄 관리 페이지의 배포 작업 목록 조회 API와 동일한 응답 구조를 사용합니다.

#### 참고

- 주간 캘린더는 현재 표시된 주의 시작일(월요일) ~ 종료일(일요일)을 기준으로 조회합니다.

---

### 2-2. 작업 금지 기간 목록 조회 (기존 API 재사용)

**GET** `/api/schedules/bans`

#### Query Parameters

| 파라미터    | 타입   | 필수 | 설명                |
| ----------- | ------ | ---- | ------------------- |
| `startDate` | string | 필수 | 시작일 (YYYY-MM-DD) |
| `endDate`   | string | 필수 | 종료일 (YYYY-MM-DD) |

#### Response

스케줄 관리 페이지의 작업 금지 기간 목록 조회 API와 동일한 응답 구조를 사용합니다.

---

### 2-3. 공휴일 목록 조회

**참고**: 공휴일은 백엔드 엔드포인트가 없습니다. 프론트엔드에서 `src/api/holidays.js`를 사용하여 공공데이터 포털 한국천문연구원\_특일 정보 API를 직접 호출합니다.

#### 구현 방식

- 프론트엔드에서 `getHolidays(year)` 함수를 호출하여 해당 연도의 공휴일 데이터를 조회합니다.
- 응답 형식: `Array<{date: string, name: string}>`
  - `date`: 공휴일 날짜 (YYYY-MM-DD 형식)
  - `name`: 공휴일 이름

---

## 3. 복구현황 섹션

### 3-1. 복구현황 목록 조회

**GET** `/api/dashboard/recovery`

#### 기능

`approval.type = 'ROLLBACK'`인 Approval과 연결된 deployment 리스트를 조회합니다.

#### Query Parameters

| 파라미터   | 타입    | 필수 | 설명                         |
| ---------- | ------- | ---- | ---------------------------- |
| `page`     | integer | 선택 | 페이지 번호 (기본값: 1)      |
| `pageSize` | integer | 선택 | 페이지당 항목 수 (기본값: 5) |

#### Response

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

#### Response 필드 설명

| 필드                   | 타입     | 필수 | 설명                                                                                    |
| ---------------------- | -------- | ---- | --------------------------------------------------------------------------------------- |
| `id`                   | integer  | 필수 | 복구현황 고유 ID (deployment ID)                                                        |
| `title`                | string   | 필수 | 배포작업명 (deployment.title)                                                           |
| `service`              | string   | 필수 | 서비스명 (deployment.project.name)                                                      |
| `status`               | enum     | 필수 | 복구 상태 (`PENDING`, `IN_PROGRESS`, `COMPLETED`)                                      |
| `duration`             | string   | 선택 | 소요시간 (예: "42분") - status가 `COMPLETED`이고 BuildRun이 존재할 때만 제공            |
| `recoveredAt`          | datetime | 선택 | 복구 완료 시각 (ISO 8601 형식) - status가 `COMPLETED`이고 BuildRun이 존재할 때만 제공    |
| `registrant`           | string   | 필수 | 등록자 이름 (deployment.issuer.name)                                                    |
| `registrantDepartment` | string   | 필수 | 등록 부서명 (deployment.issuer.department.koreanName)                                   |
| `deploymentId`         | integer  | 필수 | 원본 Deployment ID                                                                      |

#### 필터 조건

- `approval.type = 'ROLLBACK'`
- `approval.isDeleted = false` AND `deployment.isDeleted = false`
- 정렬: `deployment.createdAt DESC` (최신순)
- 페이지네이션: 기본 `pageSize = 5`, `page` 파라미터로 페이지 번호 지정

#### 상태값 결정 로직

복구 상태(`status`)는 deployment의 현재 상태에 따라 결정됩니다:

- `COMPLETED`: `deployment.status = 'COMPLETED'` 또는 `deployment.isDeployed = true`
- `IN_PROGRESS`: `deployment.status = 'IN_PROGRESS'`
- `PENDING`: 그 외 (기본값)

#### duration 및 recoveredAt 계산 로직

- `status`가 `COMPLETED`이고 해당 deployment의 BuildRun이 존재하는 경우에만 계산됩니다.
- `duration`: 첫 번째 BuildRun.startedAt ~ 마지막 BuildRun.endedAt의 차이를 분 단위로 계산 (예: "42분")
- `recoveredAt`: 마지막 BuildRun.endedAt 사용
- `status`가 `COMPLETED`가 아니거나 BuildRun이 없는 경우, `duration`과 `recoveredAt`은 `null`입니다.

---

## 에러 응답

### 401 Unauthorized

```json
{
  "error": "UNAUTHORIZED",
  "message": "인증이 필요합니다."
}
```

### 403 Forbidden

```json
{
  "error": "FORBIDDEN",
  "message": "접근 권한이 없습니다."
}
```

### 500 Internal Server Error

```json
{
  "error": "INTERNAL_SERVER_ERROR",
  "message": "서버 오류가 발생했습니다."
}
```

---

## 공통 사항

### 인증

모든 API는 인증된 사용자의 정보를 기반으로 자동 필터링됩니다. 현재 사용자 ID는 JWT 토큰에서 추출됩니다.

### 정렬

- 명시되지 않은 경우 기본 정렬은 최신순(created DESC 또는 updatedAt DESC)입니다.

### 페이지네이션

- 기본 페이지 크기: 5 (복구현황)
- 페이지 번호는 1부터 시작합니다.

### 데이터 타입

- 날짜: ISO 8601 형식 (예: `2025-10-27T10:30:00`)
- 날짜만: `YYYY-MM-DD` 형식 (예: `2025-10-27`)
- 시간만: `HH:mm` 형식 (예: `16:00`)

---

## 참고 사항

1. **승인 대기**: `approval_line` 테이블에서 현재 사용자가 다음 승인자로 지정되어 있고 아직 승인하지 않은 항목만 조회합니다.
2. **진행중인 업무**: 현재 사용자가 승인한 deployment 중, 아직 완료되지 않은 항목을 조회합니다.
3. **알림**: 알림 테이블이 없으므로 deployment의 상태 변경 이력을 기반으로 알림을 생성합니다.
4. **복구현황**: `approval.type = 'ROLLBACK'`인 Approval과 연결된 deployment를 조회합니다. 복구 상태는 deployment의 현재 상태에 따라 결정됩니다.

---

## API 엔드포인트 요약

### 상단 3개 섹션

| 엔드포인트                          | 메서드 | 설명                 | 페이지네이션 |
| ----------------------------------- | ------ | -------------------- | ------------ |
| `/api/dashboard/pending-approvals`  | GET    | 승인 대기 목록 조회   | 없음         |
| `/api/dashboard/in-progress-tasks`  | GET    | 진행중인 업무 목록 조회 | 없음         |
| `/api/dashboard/notifications`      | GET    | 알림 목록 조회       | 없음         |

### 복구현황 섹션

| 엔드포인트              | 메서드 | 설명             | 페이지네이션 |
| ----------------------- | ------ | ---------------- | ------------ |
| `/api/dashboard/recovery` | GET    | 복구현황 목록 조회 | 있음 (기본 5개) |

### 주간 캘린더 섹션

| 엔드포인트                    | 메서드 | 설명                   | 페이지네이션 |
| ----------------------------- | ------ | ---------------------- | ------------ |
| `/api/schedules/deployments`  | GET    | 배포 작업 목록 조회     | 없음         |
| `/api/schedules/bans`         | GET    | 작업 금지 기간 목록 조회 | 없음         |

**참고**: 공휴일은 프론트엔드에서 공공데이터 포털 API를 직접 호출합니다.
