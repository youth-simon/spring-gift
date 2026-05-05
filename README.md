# spring-gift

카카오 선물하기 클론 — 과제5 (구조 리팩토링 + 트랜잭션/도메인 책임 + 카카오 로그인).

- Spring Boot 4.0.6 / Kotlin 2.2 / JDK 21
- Spring Data JPA + H2 (in-memory)
- 카카오 OAuth2 (Authorization Code) + 자체 발급 JWT
- 단위/통합 테스트 22개

## 빠르게 실행

```bash
./gradlew test          # 전체 테스트
./gradlew bootRun       # 8080 에서 기동
```

기동 후:
- `/h2-console` (JDBC URL: `jdbc:h2:mem:spring-gift;MODE=MYSQL;DB_CLOSE_DELAY=-1`)
- `GET /api/products`, `POST /api/products`, ...
- `GET /api/auth/kakao/callback?code=...`
- `GET /api/wishes` (헤더 `Authorization: Bearer <jwt>`)

## 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `KAKAO_LOGIN_CLIENT_ID` | `dummy-client-id` | 카카오 REST API 키 |
| `KAKAO_LOGIN_CLIENT_SECRET` | `` | 비활성화 시 빈 문자열 |
| `KAKAO_LOGIN_REDIRECT_URI` | `http://localhost:8080/api/auth/kakao/callback` | |
| `AUTH_JWT_SECRET` | `change-this-...` | HMAC-SHA256 키 (32+ bytes 권장) |

> 어드민 키 / 액세스 토큰 / 클라이언트 시크릿은 절대 Git 에 커밋하지 않는다. 로컬 환경 변수 또는 시크릿 매니저로 주입한다.

---

## 구현 기능 목록

### 도메인
- [x] `Product` — 이름/가격/재고를 캡슐화. `decreaseStock`, `isAvailable`, `rename`, `changePrice`, `replaceStock`
- [x] `Member` — 카카오 식별자(`kakaoId`) 기반 조회. `Member.fromKakao(kakaoId, email, name)` 정적 팩토리
- [x] `Wish` — 회원·상품·수량의 묶음. `increase`, `changeQuantityTo`, `isOwnedBy(memberId)`
- [x] `BaseEntity` — `@EnableJpaAuditing` 으로 `createdAt` / `updatedAt` 자동 설정

### Auth
- [x] 카카오 인가 코드 → 액세스 토큰 교환 (`/oauth/token`)
- [x] 카카오 사용자 정보 조회 (`/v2/user/me`)
- [x] 신규 회원 자동 가입 / 기존 회원 재사용
- [x] HMAC-SHA256 자체 JWT 발급 + 검증 (외부 라이브러리 없이)
- [x] `@LoginMember` 어노테이션 + `HandlerMethodArgumentResolver`
- [x] 만료 / 위변조 / 형식 오류 토큰 거부

### Product API
- [x] `GET /api/products` (페이지네이션)
- [x] `GET /api/products/{id}`
- [x] `POST /api/products` (Bean Validation)
- [x] `PUT /api/products/{id}`
- [x] `DELETE /api/products/{id}`

### Wish API
- [x] `GET /api/wishes` — 로그인 회원의 위시 페이지 조회 (`@EntityGraph` 로 N+1 방지)
- [x] `POST /api/wishes` — 동일 상품이면 수량 증가, 없으면 신규 생성 (upsert)
- [x] `PATCH /api/wishes/{id}` — 본인 소유만 수정 가능 (`UnauthorizedException`)
- [x] `DELETE /api/wishes/products/{productId}` — 멱등적 삭제

### 횡단 관심사
- [x] `GlobalExceptionHandler` — 도메인 예외를 HTTP 상태로 매핑 (404/401/409/400)
- [x] `application.properties` — 비밀 키는 환경 변수로 외부화

---

## 구현 전략 (과제5의 4가지 축)

### 1. 구조 변경 — 계층 추출 / 스타일 정리
- 컨트롤러는 `Request → Command → Service → Domain → Response` 만 수행한다. 비즈니스 분기는 모두 도메인/서비스에 있다.
- `*Request`, `*Command`, `*Response` 를 컨트롤러 옆에 같은 파일로 두어 팀 컨벤션의 변경 비용을 낮췄다 (서비스가 외부 DTO 를 모르도록).
- `@Transactional(readOnly = true)` 를 클래스에, 쓰기 메서드에만 `@Transactional` 을 다는 일관된 스타일.

### 2. 트랜잭션 경계 세우기
- 모든 비즈니스 트랜잭션은 **서비스 메서드 1개 = 트랜잭션 1개**.
- 위시 추가 (`WishService.add`) 는 "조회 → 수량 증가 또는 신규 저장" 이 한 트랜잭션 안에서 일어난다 — 컨트롤러가 흩어 두면 동시 호출 시 중복 위시가 생길 수 있다.
- `WishService.changeQuantity` 는 소유권 검증 → 도메인 메서드 호출이 한 트랜잭션. 누락 시 다른 회원이 본인 위시를 수정할 수 있다.

### 3. 누락된 작동 구현
- 위시 upsert (동일 상품 재요청 시 수량 합치기) — 컨트롤러 레벨에서 빠지기 쉬운 작동.
- JWT 만료 / 위변조 / 형식 검증 — 단순 발급만으로는 불충분.
- `Product.decreaseStock` 의 음수 재고 방지 — `InvalidStateException` 으로 도메인 위반을 명시적으로 막음.

### 4. 도메인 책임 되찾기
- `Product` 의 재고 감소를 **서비스가 직접 빼지 않고** `product.decreaseStock(qty)` 으로 위임. 음수 재고 방지 / 검증 / 상태 변경이 한 곳.
- `Wish.isOwnedBy(memberId)` — 권한 검증을 도메인 메서드로. 서비스에서 `wish.member.id == memberId` 같은 가시성 노출을 막음.
- `Member.fromKakao(...)` 정적 팩토리 — 카카오에서 온 회원이라는 사실을 생성 시점에 강제.

---

## 검증

- 단위 테스트: `ProductTest`, `WishTest`, `JwtProviderTest`
  - 도메인 불변식, JWT round-trip / 만료 / 위변조 / 형식 오류
- 통합 테스트 (`@SpringBootTest` + H2): `AuthServiceTest`, `WishServiceTest`
  - `StubKakaoOAuthClient` 로 카카오 외부 호출 차단
  - 위시 upsert / 권한 분리 / 멱등 삭제 검증

```bash
./gradlew test
# 22 tests, 0 failed
```

작동 변경은 단순 "예외 미발생" 이 아니라 **상태 재조회** (`memberRepository.findByKakaoId`, `wishService.listOf`) 로 검증한다.

---

## ADR (간단)

### ADR-001 JWT 라이브러리 미사용
- **결정**: jjwt / nimbus-jose 같은 외부 라이브러리 대신 직접 HMAC-SHA256 으로 발급/검증.
- **이유**: 의존성 1개를 줄이는 것보다, 토큰 구조 (header.payload.signature) 와 `constant-time` 비교의 의미를 명시적으로 코드에 남기는 것이 학습 목적에 부합.
- **트레이드오프**: 회전 (kid), JWS/JWE 다양화가 필요해지면 라이브러리로 이주.

### ADR-002 위시 동일 상품 재추가 = 수량 증가
- **결정**: `POST /api/wishes` 가 동일 `(member, product)` 를 만나면 신규 row 가 아닌 기존 행의 수량을 증가시킨다.
- **이유**: `uk_wishes_member_product` 유니크 제약과 일치하는 의미. 클라이언트가 "이미 담겨 있나?" 를 매번 묻지 않아도 된다.
- **트레이드오프**: "동일 상품을 또 담는다" 는 의미가 도메인에 따라 다를 수 있다. 선물 묶음을 별도로 다뤄야 하는 정책이 생기면 별도 엔드포인트로 분리.

### ADR-003 트랜잭션 클래스 단위 readOnly
- **결정**: 서비스 클래스에 `@Transactional(readOnly = true)`, 변경 메서드에만 `@Transactional` 을 추가로 단다.
- **이유**: "기본은 읽기, 예외만 쓰기" 라는 정책을 코드의 모양으로 강제한다. 쓰기 메서드를 만들 때 `@Transactional` 추가를 잊으면 컴파일은 통과해도 의도가 드러나지 않는다.
- **트레이드오프**: Hibernate `FlushMode.MANUAL` 이 readOnly 트랜잭션에 적용되어 dirty checking 비용이 줄어드는 대신, 한 트랜잭션 안에서 읽기/쓰기를 섞기 어려워진다.

---

## AI 도구 활용 기록

이번 과제는 다음 흐름으로 AI (Claude Code) 와 협업했다.

### 활용 방식
- **요구사항 분해 → 단계 정의**: 과제5 PDF 를 먼저 읽고, "프로젝트 생성 / 도메인 / 서비스·컨트롤러 / 카카오 로그인 / 테스트 / README" 6단계로 쪼갰다. 단계별 체크리스트를 두고, 한 단계가 끝나기 전엔 다음 단계로 넘어가지 않았다.
- **단계마다 `compileKotlin` 또는 `test` 실행**: AI 가 만든 코드를 그대로 신뢰하지 않고, 단계가 끝날 때마다 실제 컴파일 / 테스트로 검증했다 (예: Spring Boot 4 가 Jackson 3 (`tools.jackson.databind`) 로 이주한 사실은 컴파일 에러로 발견했다).
- **외부 호출은 stub 으로 차단**: `KakaoOAuthClient` 인터페이스를 두고 `@TestConfiguration` 의 `StubKakaoOAuthClient` 를 주입하도록 설계 — 실제 카카오 API 키가 없어도 통합 테스트가 돌아가도록.

### 학습 포인트
- **Spring Boot 4 의 변화** — `spring-boot-starter-web` 이 `spring-boot-starter-webmvc` 로 분리됐고, Jackson 의 패키지가 `com.fasterxml.jackson.databind` → `tools.jackson.databind` 로 이동했다. autoconfigured `RestClient.Builder` 빈이 빠진 케이스도 있어, `RestClient.create()` 직접 호출로 단순화했다.
- **도메인 책임 되찾기 패턴 적용 감각** — "서비스에서 product.price * quantity 를 계산하지 말고 도메인에 묻는다" 는 원칙을 위시 / 재고 / 권한 검증 3 곳에 일관되게 적용하니, 컨트롤러와 서비스가 정말로 얇아진다.
- **트랜잭션 경계의 의미** — `WishService.add` 의 "조회 → upsert" 가 한 트랜잭션이라는 사실을 테스트 (`add increases quantity when wish already exists`) 가 직접 검증한다. 검증 없는 트랜잭션 어노테이션은 신뢰할 근거가 없다.

### 반복 패턴
- AI 가 작성한 코드를 받으면 → `./gradlew compileKotlin` → 에러를 그대로 다시 AI 에 던지지 않고, 패키지/모듈명 하나만 고치는 식으로 직접 개입했다 (Boot 4 / Jackson 3 마이그레이션).
- 새 컴포넌트를 추가할 때마다 "이 컴포넌트가 의존하는 빈은 무엇인가? 테스트에서 어떻게 끊을 것인가?" 를 먼저 결정한 뒤 코드를 썼다 (예: `KakaoOAuthClient` 인터페이스화).

### 의도하지 않은 변경 제거
- `LoginMemberArgumentResolver.supportsParameter` 가 처음에 타입 비교까지 넣었다가 우선순위 버그가 있어, 어노테이션 검사만 남기는 쪽으로 단순화했다 — "더 안전해 보이는 코드" 가 항상 더 옳지 않다.

---

## 디렉토리

```
src/main/kotlin/camp/nextstep/gift/
├── SpringGiftApplication.kt
├── auth/         # 카카오 OAuth + JWT + @LoginMember
├── common/       # BaseEntity, JpaConfig, Exception, GlobalHandler
├── member/       # Member, MemberRepository
├── product/      # Product, Repository, Service, Controller
└── wish/         # Wish, Repository, Service, Controller
```

## Commit 컨벤션

[AngularJS Git Commit Message Conventions](https://gist.github.com/stephenparish/9941e89d80e2bc58a153) 참조.

```
<type>(<scope>): <subject>

feat(wish): upsert wish on duplicate product
fix(auth): reject expired JWT
refactor(product): move stock check into domain
test(wish): cover ownership check
```
