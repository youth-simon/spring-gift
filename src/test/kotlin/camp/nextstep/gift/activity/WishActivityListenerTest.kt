package camp.nextstep.gift.activity

import camp.nextstep.gift.member.Member
import camp.nextstep.gift.member.MemberRepository
import camp.nextstep.gift.product.Product
import camp.nextstep.gift.product.ProductRepository
import camp.nextstep.gift.wish.WishRepository
import camp.nextstep.gift.wish.WishService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
class WishActivityListenerTest {
    @Autowired private lateinit var wishService: WishService

    @Autowired private lateinit var wishRepository: WishRepository

    @Autowired private lateinit var memberRepository: MemberRepository

    @Autowired private lateinit var productRepository: ProductRepository

    @Autowired private lateinit var activityRepository: WishActivityRepository

    private var memberId: Long = 0
    private var productId: Long = 0

    @BeforeEach
    fun setUp() {
        activityRepository.deleteAll()
        wishRepository.deleteAll()
        memberRepository.deleteAll()
        productRepository.deleteAll()

        memberId = memberRepository.save(Member.fromKakao(kakaoId = 999, email = "u@x", name = "u")).id!!
        productId =
            productRepository
                .save(
                    Product.create(name = "americano", price = 4500, imageUrl = "http://x", stock = 100),
                ).id!!
    }

    @Test
    fun `add publishes WishAdded which is recorded after commit`() {
        wishService.add(memberId, productId, quantity = 2)

        val activities = activityRepository.findAllByMemberIdOrderByOccurredAtDesc(memberId)
        assertEquals(1, activities.size)
        assertEquals(WishActivityType.ADDED, activities.first().type)
        assertEquals(2, activities.first().quantity)
    }

    @Test
    fun `re-adding an existing product records QUANTITY_INCREASED, not ADDED`() {
        wishService.add(memberId, productId, quantity = 2)
        wishService.add(memberId, productId, quantity = 3)

        val activities = activityRepository.findAllByMemberIdOrderByOccurredAtDesc(memberId)
        assertEquals(2, activities.size)
        assertEquals(WishActivityType.QUANTITY_INCREASED, activities.first().type)
        assertEquals(3, activities.first().quantity)
    }

    @Test
    fun `change quantity publishes WishQuantityChanged`() {
        val result = wishService.add(memberId, productId, quantity = 1)
        wishService.changeQuantity(memberId, result.wish.id!!, quantity = 9)

        val activities = activityRepository.findAllByMemberIdOrderByOccurredAtDesc(memberId)
        assertEquals(2, activities.size)
        assertEquals(WishActivityType.QUANTITY_CHANGED, activities.first().type)
        assertEquals(9, activities.first().quantity)
    }

    @Test
    fun `remove publishes WishRemoved`() {
        wishService.add(memberId, productId, quantity = 1)
        wishService.remove(memberId, productId)

        val activities = activityRepository.findAllByMemberIdOrderByOccurredAtDesc(memberId)
        val types = activities.map { it.type }
        assertTrue(types.contains(WishActivityType.REMOVED))
    }
}
