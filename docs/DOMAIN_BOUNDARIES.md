# 도메인 경계 분석

> 패키지가 곧 도메인 경계다. 도메인 간 통신은 **공개 도메인 메서드** 또는 **ApplicationEvent** 두 가지뿐이다. 직접 다른 도메인의 Repository/Entity 내부 상태를 만지지 않는다.

## 도메인 지도

```
┌──────────┐
│   auth   │  외부 시스템 (Kakao) ↔ 자체 ID (JWT) 변환
│  (kakao, │
│   jwt)   │
└────┬─────┘
     │ MemberRepository (read/upsert)
     ▼
┌──────────┐         ┌──────────┐
│  member  │         │ product  │  서로 모름. 둘 다 wish의 building block.
└──────────┘         └──────────┘
     ▲                    ▲
     │ ManyToOne           │ ManyToOne
     │                    │
     └────┬───────────────┘
          │
       ┌──┴───┐
       │ wish │  member ↔ product 관계 + 수량
       └──┬───┘
          │ ApplicationEvent (WishAdded / WishQuantityChanged / WishRemoved)
          ▼
       ┌──────────┐
       │ activity │  AFTER_COMMIT, REQUIRES_NEW 로 별도 트랜잭션에서 기록
       └──────────┘
```

## 의존 방향 규칙

| from → to | 허용 | 메모 |
|---|---|---|
| `auth` → `member` | ✅ | `MemberRepository.findByKakaoId`, `Member.fromKakao()` |
| `auth` → `product` | ❌ | 인증은 상품을 모른다 |
| `auth` → `wish` | ❌ | |
| `member` → 다른 도메인 | ❌ | 가장 안쪽 — 누구도 의존 X |
| `product` → 다른 도메인 | ❌ | 가장 안쪽 |
| `wish` → `member`, `product` | ✅ | ManyToOne 연관 + Repository |
| `wish` → `auth` | ❌ | 위시는 인증을 모른다 (컨트롤러에서 `@LoginMember`로 주입받은 ID만 받는다) |
| `wish` → `activity` | ✅ (이벤트만) | 직접 호출 X. `ApplicationEventPublisher` 사용 |
| `activity` → `wish` | ✅ (이벤트 리스너) | 단방향: `wish` 가 publish, `activity` 가 listen |
| `activity` → 외부 다른 도메인 | ❌ | activity는 기록 전용 — 다른 도메인을 변경하지 않는다 |

> 위 규칙을 어기는 import 가 등장하면 도메인이 새는 신호다. 핵심: **member / product 는 어떤 다른 도메인도 import 하지 않는다**.

## 책임 분리 — "도메인 책임 되찾기"

| 책임 | 잘못된 위치 (서비스) | 올바른 위치 (도메인) |
|---|---|---|
| 재고 차감 + 음수 방지 | `service { product.stock -= qty }` | `Product.decreaseStock(qty)` |
| 위시 소유권 검증 | `service { wish.member.id == memberId }` | `Wish.isOwnedBy(memberId)` |
| 카카오 회원 식별 생성 | `service { Member(...kakaoId=k) }` | `Member.fromKakao(kakaoId, email, name)` |
| 위시 수량 합치기 (upsert) | 컨트롤러의 if/else | `Wish.increase(amount)` |

서비스는 **트랜잭션 + 도메인 호출 + 이벤트 발행**만 한다. 분기/검증/계산은 모두 도메인 메서드.

## 트랜잭션 경계

| 경계 | 위치 | 전파 | 설명 |
|---|---|---|---|
| 카카오 로그인 | `AuthService.loginWithKakao` | REQUIRED | 외부 호출 (kakao API) → 회원 upsert → JWT 발급. 한 트랜잭션 |
| 위시 추가/수정/삭제 | `WishService.add/changeQuantity/remove` | REQUIRED | upsert/소유권 검증/도메인 호출이 한 트랜잭션 |
| 위시 활동 기록 | `WishActivityListener.on*` | **REQUIRES_NEW** | 본 트랜잭션 commit 후 별도 트랜잭션 |
| 상품 CRUD | `ProductService.*` | REQUIRED | 단순 |

### 왜 `WishActivity` 만 `REQUIRES_NEW` 인가?

- **분리하지 않으면**: 활동 기록 INSERT 가 실패하면 위시 추가/삭제까지 롤백된다. 부수 효과 (audit) 가 본 작동을 막는다.
- **단순 분리만 하면**: `@Async` 로 빼는 방법도 있지만 트랜잭션 컨텍스트가 끊겨서 같은 DB 일관성 보장이 어렵다.
- **현재 선택**: `@TransactionalEventListener(AFTER_COMMIT) + @Transactional(REQUIRES_NEW)`
  - 본 트랜잭션이 commit 된 다음에만 listener 진입
  - listener 안에서 별도 트랜잭션을 새로 열고 그 안에서 INSERT
  - listener 가 실패해도 본 작동은 이미 commit. 활동 로그 INSERT 실패는 별도 알람 / 재시도로 처리할 문제

흐름:
```
WishService.add  ──[ tx#1: REQUIRED ]──┐
  load member, product                  │
  upsert wish                            │
  publish(WishAdded)                     │   <-- 이벤트는 보관, listener 미실행
                                          │
[ tx#1 COMMIT ]  <- 여기서 wish 가 영구화
                                          ▼
WishActivityListener.onAdded            ─┐
  [ tx#2: REQUIRES_NEW ]                  │
  insert wish_activities                   │
  [ tx#2 COMMIT ]                          │
                                            │ (실패해도 wish 는 살아있음)
```

## 인덱스 전략

| 테이블 | 인덱스 | 이유 |
|---|---|---|
| `members` | `uk_members_email` (UNIQUE) | 이메일 충돌 방지 + 동등 조회 |
| `members` | `uk_members_kakao_id` (UNIQUE) | 카카오 로그인 매번 `findByKakaoId` 호출 — equality lookup의 가장 흔한 경로 |
| `members` | `idx_members_created_at` (DESC) | 가입 순서 / 최근 가입자 조회 |
| `products` | `idx_products_name` | 검색/자동완성 prefix 매칭 (LIKE 'name%') |
| `wishes` | `uk_wishes_member_product` (UNIQUE) | upsert 의 기반. equality lookup도 동시에 커버 |
| `wishes` | `idx_wishes_member_created` (member_id, created_at DESC) | "내 위시 최근 순" — `findAllByMemberId` 의 기본 정렬 |
| `wishes` | `idx_wishes_product` | 상품 삭제 시 위시 일괄 정리 / 상품별 수요 통계 |
| `wish_activities` | `idx_wish_activities_member_occurred` (member_id, occurred_at DESC) | 회원별 활동 타임라인 |
| `wish_activities` | `idx_wish_activities_product_occurred` (product_id, occurred_at DESC) | 상품 인기도 분석 |

> covering index 가 되도록 정렬 컬럼을 함께 묶었다. 단일 컬럼만 인덱싱하면 sort 단계에서 다시 정렬 비용이 든다.

## 도메인 별 외부 의존성

| 도메인 | 외부 시스템 | 끊는 방법 |
|---|---|---|
| `auth` | Kakao OAuth (`kauth.kakao.com`, `kapi.kakao.com`) | `KakaoOAuthClient` 인터페이스 — 테스트에서 `StubKakaoClient` 주입 |
| 그 외 | 없음 | DB(JPA)만 |

테스트에서 `StubKakaoClient` 가 한 개라는 사실 → 외부 의존이 정확히 한 곳에만 있다는 신호. 더 늘어나면 (예: 이메일 발송) 같은 패턴으로 인터페이스 + Stub.

## 변경 영향 범위

| 변경 종류 | 영향받는 도메인 |
|---|---|
| 카카오 로그인 정책 변경 | `auth` 만 |
| 가격/재고 정책 변경 | `product` 만 (도메인 메서드로 캡슐화 → 호출자 안 바뀜) |
| 위시 수량 정책 변경 | `wish` 만 |
| 위시 활동 로그 포맷 변경 | `activity` 만 (event 인터페이스가 안 바뀌면) |
| 새 활동 로그 destination 추가 (예: Kafka) | `activity` 추가 listener — `wish` 는 무관 |

이게 이벤트 기반 분리의 핵심 보상이다.
