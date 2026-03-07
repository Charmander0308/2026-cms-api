## 프로젝트 실행 방법

### 요구 사항

| 항목 | 버전 |
|------|------|
| Java | 25 |
| Gradle | Wrapper 포함 (별도 설치 불필요) |

### 실행

```bash
# 개발 환경 실행 (기본 프로파일 — H2 인메모리 DB, p6spy SQL 로그 활성)
./gradlew bootRun

# 운영 환경 실행 (prod 프로파일 — p6spy 비활성, 풀 사이즈 확장)
java -jar build/libs/cms-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### 테스트

```bash
./gradlew test
```

### 주요 접속 정보

| 항목 | 값 |
|------|-----|
| API Base URL | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| H2 콘솔 | `http://localhost:8080/h2-console` |
| H2 JDBC URL | `jdbc:h2:mem:test` |
| 기본 관리자 | `admin` / `admin123` |
| 기본 사용자 | `user1` / `user123` |

---

## 구현 내용

### 사용 기술 및 추가 의존성

| 항목 | 내용 |
|------|------|
| 로그인 방식 | JWT Bearer Token (stateless) |
| 패스워드 암호화 | BCrypt |
| JWT 라이브러리 | `io.jsonwebtoken:jjwt-api:0.12.6` |
| 유효성 검사 | `spring-boot-starter-validation` |
| Soft Delete | `@SQLRestriction` (Hibernate 내장) — `deleted_at` 기반 논리 삭제 |
| API 접근 로그 | Logback RollingFileAppender — `logs/access.log` 일별 롤링 |
| 가상 스레드 | `spring.threads.virtual.enabled=on` — Tomcat·`@Async` 모두 적용 |
| 커넥션 풀 | HikariCP 7.0.2 — 가상 스레드 ThreadFactory 적용, 풀 파라미터 정밀 튜닝 |
| 응답 압축 | Tomcat 내장 Gzip — 1KB 이상 JSON 응답 자동 압축 |
| 테스트 | `spring-security-test` — MockMvc + Spring Security 통합 테스트 |
| API 문서 | `springdoc-openapi-starter-webmvc-ui:2.8.5` — Swagger UI (운영 환경에서 자동 비활성화) |

### 로그인 방식

**JWT (JSON Web Token) Bearer Token** 방식을 선택했습니다.

- 클라이언트가 `POST /api/auth/login`으로 로그인 요청
- 서버에서 자격증명 확인 후 JWT 토큰 발급 (유효기간 24시간)
- 이후 모든 요청에 `Authorization: Bearer <token>` 헤더 포함
- 서버는 Stateless로 동작 (세션 미사용)

### Soft Delete

`users`, `contents` 테이블 모두 물리 삭제 대신 `deleted_at` 타임스탬프를 기록하는 논리 삭제를 적용합니다.

- `@SQLRestriction("deleted_at IS NULL")` — 모든 JPA 조회에서 삭제된 레코드 자동 제외
- 삭제된 사용자는 로그인 불가, 삭제된 콘텐츠는 목록·상세 조회 불포함

### 초기 데이터 (자동 생성)

앱 최초 기동 시 `DataInitializer`가 아래 데이터를 자동으로 삽입합니다.

| 사용자명 | 비밀번호 | 역할 |
|----------|----------|------|
| admin | admin123 | ADMIN |
| user1 | user123 | USER |

### 접근 권한 규칙

| 기능 | 인증 필요 | 권한 제한 |
|------|-----------|-----------|
| 콘텐츠 목록/상세 조회 | O | 없음 (인증만 필요) |
| 콘텐츠 생성 | O | 없음 (인증만 필요) |
| 콘텐츠 수정 | O | 본인 작성 또는 ADMIN |
| 콘텐츠 삭제 | O | 본인 작성 또는 ADMIN |

### Swagger UI

`http://localhost:8080/swagger-ui.html`에서 모든 API를 브라우저로 직접 실행할 수 있습니다.

**사용 방법**

1. `POST /api/auth/login` 실행 → 응답의 `token` 값 복사
2. 우측 상단 **Authorize** 버튼 클릭 → 복사한 토큰 붙여넣기
3. 이후 모든 API에 JWT 인증이 자동 적용됨

> 운영 환경(`prod` 프로파일)에서는 Swagger UI가 자동으로 비활성화됩니다.

### 패키지 구조

```
src/main/java/com/malgn/
├── Application.java
├── configure/
│   ├── AppConfiguration.java              # @EnableAsync, 가상 스레드 TaskExecutor
│   ├── DataSourceConfiguration.java       # HikariCP 가상 스레드 ThreadFactory 설정
│   ├── SwaggerConfiguration.java          # OpenAPI 정의, JWT 보안 스킴
│   ├── filter/
│   │   └── ApiAccessLogFilter.java        # API 접근 로그 필터 (method/uri/status/duration/user)
│   └── security/
│       ├── SecurityConfiguration.java     # JWT 필터, 권한 설정
│       ├── H2DbSecurityConfiguration.java
│       ├── ActuatorSecurityConfiguration.java
│       ├── JwtTokenProvider.java          # JWT 생성/검증
│       ├── JwtAuthenticationFilter.java   # JWT 인증 필터
│       └── CustomUserDetailsService.java  # 사용자 인증 서비스
├── domain/
│   ├── auth/
│   │   ├── controller/AuthController.java
│   │   ├── dto/{LoginRequest, TokenResponse}.java
│   │   └── service/AuthService.java
│   ├── contents/
│   │   ├── controller/ContentsController.java
│   │   ├── dto/{ContentsCreateRequest, ContentsUpdateRequest, ContentsResponse}.java
│   │   ├── entity/Contents.java
│   │   ├── repository/ContentsRepository.java
│   │   └── service/ContentsService.java
│   └── user/
│       ├── entity/{User, Role}.java
│       └── repository/UserRepository.java
└── common/
    ├── exception/{ErrorCode, BusinessException, GlobalExceptionHandler}.java
    ├── response/{ApiResponse, PageResponse}.java
    └── init/DataInitializer.java
```

---

## REST API Docs

### 기본 정보

- Base URL: `http://localhost:8080`
- 인증: `Authorization: Bearer <JWT_TOKEN>` 헤더
- 응답 형식: `{"success": boolean, "data": object|null, "message": string|null}`

---

### 1. 인증

#### 로그인

```
POST /api/auth/login
```

**Request Body**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response 200**
```json
{
  "success": true,
  "data": {
    "tokenType": "Bearer",
    "token": "eyJhbGciOiJIUzM4NCJ9..."
  },
  "message": null
}
```

**Response 401** - 인증 실패
```json
{
  "success": false,
  "data": null,
  "message": "아이디 또는 비밀번호가 올바르지 않습니다."
}
```

---

### 2. 콘텐츠 관리

#### 콘텐츠 목록 조회 (페이징)

```
GET /api/contents?page=0&size=10&sort=createdDate,desc
```

**Query Parameters**

| 파라미터 | 기본값 | 설명 |
|----------|--------|------|
| page | 0 | 페이지 번호 (0부터 시작) |
| size | 10 | 페이지 당 항목 수 |
| sort | createdDate,desc | 정렬 기준 |

**Response 200**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "제목",
        "description": "내용",
        "viewCount": 0,
        "createdDate": "2026-03-07T12:00:00",
        "createdBy": "admin",
        "lastModifiedDate": null,
        "lastModifiedBy": null
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "message": null
}
```

#### 콘텐츠 상세 조회

```
GET /api/contents/{id}
```

- 조회 시 `view_count` 1 증가

**Response 200** - 성공 (목록 조회와 동일한 단건 구조)

**Response 404**
```json
{
  "success": false,
  "data": null,
  "message": "콘텐츠를 찾을 수 없습니다."
}
```

#### 콘텐츠 생성

```
POST /api/contents
```

**Request Body**
```json
{
  "title": "제목 (필수, 최대 100자)",
  "description": "내용 (선택)"
}
```

**Response 201** - 생성된 콘텐츠 반환

**Response 400** - 유효성 검사 실패
```json
{
  "success": false,
  "data": null,
  "message": "제목은 필수입니다."
}
```

#### 콘텐츠 수정

```
PUT /api/contents/{id}
```

- **권한**: 본인이 생성한 콘텐츠 또는 ADMIN

**Request Body**
```json
{
  "title": "수정된 제목 (필수, 최대 100자)",
  "description": "수정된 내용 (선택)"
}
```

**Response 200** - 수정된 콘텐츠 반환 (`lastModifiedBy`, `lastModifiedDate` 갱신)

**Response 403**
```json
{
  "success": false,
  "data": null,
  "message": "접근 권한이 없습니다."
}
```

#### 콘텐츠 삭제

```
DELETE /api/contents/{id}
```

- **권한**: 본인이 생성한 콘텐츠 또는 ADMIN
- **처리 방식**: Soft Delete — 물리 삭제 없이 `deleted_at` 기록

**Response 200**
```json
{
  "success": true,
  "data": null,
  "message": null
}
```

---

### 공통 에러 응답

| HTTP 상태코드 | 설명 |
|--------------|------|
| 400 | 요청 유효성 검사 실패 |
| 401 | 인증 필요 (토큰 없음 또는 만료) |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 500 | 서버 내부 오류 |

---

### 사용한 AI 도구

- Claude Code (claude-sonnet-4-6) : 프로젝트 셋업, 구조 설계에 활용
- Gemini Code Assist (Gemini 3 Pro Preview) : .gitignore 교차 검증 등 부수적인 작업에 활용
