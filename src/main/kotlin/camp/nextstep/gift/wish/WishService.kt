package camp.nextstep.gift.wish

import camp.nextstep.gift.common.NotFoundException
import camp.nextstep.gift.common.UnauthorizedException
import camp.nextstep.gift.member.Member
import camp.nextstep.gift.member.MemberRepository
import camp.nextstep.gift.product.Product
import camp.nextstep.gift.product.ProductRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
@Transactional(readOnly = true)
class WishService(
    private val wishRepository: WishRepository,
    private val memberRepository: MemberRepository,
    private val productRepository: ProductRepository,
    private val events: ApplicationEventPublisher,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun listOf(
        memberId: Long,
        pageable: Pageable,
    ): Page<Wish> = wishRepository.findAllByMemberId(memberId, pageable)

    @Transactional
    fun add(
        memberId: Long,
        productId: Long,
        quantity: Int,
    ): Wish {
        val member = loadMember(memberId)
        val product = loadProduct(productId)

        val existing = wishRepository.findByMemberIdAndProductId(memberId, productId)
        val saved =
            if (existing != null) {
                existing.increase(quantity)
                existing
            } else {
                wishRepository.save(Wish.create(member, product, quantity))
            }
        events.publishEvent(
            WishAdded(memberId, productId, quantity, isNew = existing == null, occurredAt = now()),
        )
        return saved
    }

    @Transactional
    fun changeQuantity(
        memberId: Long,
        wishId: Long,
        quantity: Int,
    ) {
        val wish = loadWish(wishId)
        if (!wish.isOwnedBy(memberId)) {
            throw UnauthorizedException("not owner of wish=$wishId")
        }
        wish.changeQuantityTo(quantity)
        events.publishEvent(
            WishQuantityChanged(memberId, wish.product.id!!, quantity, occurredAt = now()),
        )
    }

    @Transactional
    fun remove(
        memberId: Long,
        productId: Long,
    ) {
        val deleted = wishRepository.deleteByMemberIdAndProductId(memberId, productId)
        if (deleted == 0L) {
            throw NotFoundException("wish not found: member=$memberId, product=$productId")
        }
        events.publishEvent(WishRemoved(memberId, productId, occurredAt = now()))
    }

    private fun now(): Instant = Instant.now(clock)

    private fun loadMember(id: Long): Member = memberRepository.findById(id).orElseThrow { NotFoundException("member not found: $id") }

    private fun loadProduct(id: Long): Product = productRepository.findById(id).orElseThrow { NotFoundException("product not found: $id") }

    private fun loadWish(id: Long): Wish = wishRepository.findById(id).orElseThrow { NotFoundException("wish not found: $id") }
}
