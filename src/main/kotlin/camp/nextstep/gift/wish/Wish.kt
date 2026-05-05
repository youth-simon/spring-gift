package camp.nextstep.gift.wish

import camp.nextstep.gift.common.BaseEntity
import camp.nextstep.gift.member.Member
import camp.nextstep.gift.product.Product
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "wishes",
    uniqueConstraints = [UniqueConstraint(name = "uk_wishes_member_product", columnNames = ["member_id", "product_id"])],
    indexes = [
        Index(name = "idx_wishes_member_created", columnList = "member_id, created_at DESC"),
        Index(name = "idx_wishes_product", columnList = "product_id"),
    ],
)
class Wish protected constructor(
    member: Member,
    product: Product,
    quantity: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member = member
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product = product
        protected set

    @Column(nullable = false)
    var quantity: Int = quantity
        protected set

    fun increase(amount: Int) {
        require(amount > 0) { "amount must be positive" }
        quantity += amount
    }

    fun changeQuantityTo(newQuantity: Int) {
        require(newQuantity > 0) { "quantity must be positive" }
        quantity = newQuantity
    }

    fun isOwnedBy(memberId: Long): Boolean = member.id == memberId

    companion object {
        fun create(
            member: Member,
            product: Product,
            quantity: Int,
        ): Wish {
            require(quantity > 0) { "quantity must be positive" }
            return Wish(member, product, quantity)
        }
    }
}
