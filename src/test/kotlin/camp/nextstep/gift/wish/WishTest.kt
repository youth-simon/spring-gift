package camp.nextstep.gift.wish

import camp.nextstep.gift.member.Member
import camp.nextstep.gift.product.Product
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.reflect.Field
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WishTest {
    private fun aMember(id: Long): Member {
        val member = Member.fromKakao(kakaoId = id, email = "u$id@x", name = "user-$id")
        member.assignId(id)
        return member
    }

    private fun aProduct(id: Long): Product {
        val product = Product.create(name = "americano", price = 4500, imageUrl = "http://x", stock = 10)
        product.assignId(id)
        return product
    }

    @Test
    fun `create rejects non-positive quantity`() {
        val member = aMember(1)
        val product = aProduct(2)

        assertThrows<IllegalArgumentException> { Wish.create(member, product, 0) }
        assertThrows<IllegalArgumentException> { Wish.create(member, product, -1) }
    }

    @Test
    fun `increase adds to current quantity`() {
        val wish = Wish.create(aMember(1), aProduct(2), quantity = 1)

        wish.increase(2)

        assertEquals(3, wish.quantity)
    }

    @Test
    fun `changeQuantityTo replaces quantity`() {
        val wish = Wish.create(aMember(1), aProduct(2), quantity = 1)

        wish.changeQuantityTo(5)

        assertEquals(5, wish.quantity)
    }

    @Test
    fun `isOwnedBy reflects member id`() {
        val wish = Wish.create(aMember(1), aProduct(2), quantity = 1)

        assertTrue(wish.isOwnedBy(1))
        assertFalse(wish.isOwnedBy(99))
    }
}

private fun Any.assignId(value: Long) {
    val field: Field =
        generateSequence(this::class.java) { it.superclass }
            .mapNotNull { runCatching { it.getDeclaredField("id") }.getOrNull() }
            .first()
    field.isAccessible = true
    field.set(this, value)
}
