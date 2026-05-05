package camp.nextstep.gift.product

import camp.nextstep.gift.common.InvalidStateException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductTest {
    @Test
    fun `create rejects blank name`() {
        assertThrows<IllegalArgumentException> {
            Product.create(name = "", price = 1000, imageUrl = "http://x", stock = 1)
        }
    }

    @Test
    fun `create rejects negative price`() {
        assertThrows<IllegalArgumentException> {
            Product.create(name = "coffee", price = -1, imageUrl = "http://x", stock = 1)
        }
    }

    @Test
    fun `create rejects name longer than 50 chars`() {
        assertThrows<IllegalArgumentException> {
            Product.create(name = "a".repeat(51), price = 0, imageUrl = "http://x", stock = 0)
        }
    }

    @Test
    fun `decreaseStock reduces stock`() {
        val product = Product.create(name = "americano", price = 4500, imageUrl = "http://x", stock = 10)

        product.decreaseStock(3)

        assertEquals(7, product.stock)
    }

    @Test
    fun `decreaseStock fails when not enough stock`() {
        val product = Product.create(name = "americano", price = 4500, imageUrl = "http://x", stock = 2)

        assertThrows<InvalidStateException> {
            product.decreaseStock(3)
        }
    }

    @Test
    fun `isAvailable returns false when stock is insufficient`() {
        val product = Product.create(name = "americano", price = 4500, imageUrl = "http://x", stock = 2)

        assertTrue(product.isAvailable(2))
        assertFalse(product.isAvailable(3))
    }
}
