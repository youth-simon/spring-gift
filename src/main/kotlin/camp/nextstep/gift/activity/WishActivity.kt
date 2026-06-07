package camp.nextstep.gift.activity

import camp.nextstep.gift.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

enum class WishActivityType { ADDED, QUANTITY_INCREASED, QUANTITY_CHANGED, REMOVED }

@Entity
@Table(
    name = "wish_activities",
    indexes = [
        Index(name = "idx_wish_activities_member_occurred", columnList = "member_id, occurred_at DESC"),
        Index(name = "idx_wish_activities_product_occurred", columnList = "product_id, occurred_at DESC"),
    ],
)
class WishActivity protected constructor(
    memberId: Long,
    productId: Long,
    type: WishActivityType,
    quantity: Int?,
    occurredAt: Instant,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "member_id", nullable = false)
    var memberId: Long = memberId
        protected set

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var type: WishActivityType = type
        protected set

    @Column(name = "quantity")
    var quantity: Int? = quantity
        protected set

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: Instant = occurredAt
        protected set

    companion object {
        fun added(
            memberId: Long,
            productId: Long,
            quantity: Int,
            at: Instant,
        ): WishActivity = WishActivity(memberId, productId, WishActivityType.ADDED, quantity, at)

        /** 이미 위시에 있던 상품을 다시 추가해 수량이 누적된 경우. quantity 는 증가분이다. */
        fun quantityIncreased(
            memberId: Long,
            productId: Long,
            addedQuantity: Int,
            at: Instant,
        ): WishActivity = WishActivity(memberId, productId, WishActivityType.QUANTITY_INCREASED, addedQuantity, at)

        fun quantityChanged(
            memberId: Long,
            productId: Long,
            newQuantity: Int,
            at: Instant,
        ): WishActivity = WishActivity(memberId, productId, WishActivityType.QUANTITY_CHANGED, newQuantity, at)

        fun removed(
            memberId: Long,
            productId: Long,
            at: Instant,
        ): WishActivity = WishActivity(memberId, productId, WishActivityType.REMOVED, null, at)
    }
}
