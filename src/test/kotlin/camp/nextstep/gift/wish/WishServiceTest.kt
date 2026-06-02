package camp.nextstep.gift.wish

import camp.nextstep.gift.common.UnauthorizedException
import camp.nextstep.gift.member.Member
import camp.nextstep.gift.member.MemberRepository
import camp.nextstep.gift.product.Product
import camp.nextstep.gift.product.ProductRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class WishServiceTest {
    @Autowired private lateinit var wishService: WishService

    @Autowired private lateinit var wishRepository: WishRepository

    @Autowired private lateinit var memberRepository: MemberRepository

    @Autowired private lateinit var productRepository: ProductRepository

    private var memberId: Long = 0
    private var productId: Long = 0

    @BeforeEach
    fun setUp() {
        wishRepository.deleteAll()
        memberRepository.deleteAll()
        productRepository.deleteAll()

        val member = memberRepository.save(Member.fromKakao(kakaoId = 999, email = "u@x", name = "user"))
        val product =
            productRepository.save(
                Product.create(name = "americano", price = 4500, imageUrl = "http://x", stock = 100),
            )
        memberId = member.id!!
        productId = product.id!!
    }

    @Test
    fun `add creates a new wish when none exists`() {
        val result = wishService.add(memberId, productId, quantity = 2)

        assertTrue(result.created)
        assertEquals(2, result.wish.quantity)
        assertEquals(memberId, result.wish.member.id)
    }

    @Test
    fun `add increases quantity when wish already exists for same product`() {
        val first = wishService.add(memberId, productId, quantity = 2)
        val second = wishService.add(memberId, productId, quantity = 3)

        assertTrue(first.created)
        assertFalse(second.created)

        val wishes = wishService.listOf(memberId, Pageable.unpaged()).content
        assertEquals(1, wishes.size)
        assertEquals(5, wishes.first().quantity)
    }

    @Test
    fun `changeQuantity is denied for a different member`() {
        val result = wishService.add(memberId, productId, quantity = 1)

        val otherMember = memberRepository.save(Member.fromKakao(kakaoId = 888, email = "v@x", name = "v"))

        assertThrows<UnauthorizedException> {
            wishService.changeQuantity(otherMember.id!!, result.wish.id!!, quantity = 9)
        }
    }

    @Test
    fun `remove deletes the wish for the given product`() {
        wishService.add(memberId, productId, quantity = 1)

        wishService.remove(memberId, productId)

        assertNull(wishRepository.findByMemberIdAndProductId(memberId, productId))
    }

    @Test
    fun `remove is idempotent when the wish does not exist`() {
        assertDoesNotThrow {
            wishService.remove(memberId, productId)
        }
    }
}
