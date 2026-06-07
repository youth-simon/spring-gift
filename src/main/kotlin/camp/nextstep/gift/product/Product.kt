package camp.nextstep.gift.product

import camp.nextstep.gift.common.BaseEntity
import camp.nextstep.gift.common.InvalidStateException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "products",
    indexes = [Index(name = "idx_products_name", columnList = "name")],
)
class Product protected constructor(
    name: String,
    price: Long,
    imageUrl: String,
    stock: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(nullable = false, length = 50)
    var name: String = name
        protected set

    @Column(nullable = false)
    var price: Long = price
        protected set

    @Column(name = "image_url", nullable = false, length = 1000)
    var imageUrl: String = imageUrl
        protected set

    @Column(nullable = false)
    var stock: Int = stock
        protected set

    fun rename(newName: String) {
        require(newName.isNotBlank()) { "name must not be blank" }
        require(newName.length <= MAX_NAME_LENGTH) { "name must be at most $MAX_NAME_LENGTH chars" }
        this.name = newName
    }

    fun changePrice(newPrice: Long) {
        require(newPrice >= 0) { "price must not be negative" }
        this.price = newPrice
    }

    fun replaceStock(newStock: Int) {
        require(newStock >= 0) { "stock must not be negative" }
        this.stock = newStock
    }

    fun changeImageUrl(newImageUrl: String) {
        require(newImageUrl.isNotBlank()) { "imageUrl must not be blank" }
        this.imageUrl = newImageUrl
    }

    fun decreaseStock(quantity: Int) {
        require(quantity > 0) { "quantity must be positive" }
        if (stock < quantity) {
            throw InvalidStateException("not enough stock for product=$id (stock=$stock, requested=$quantity)")
        }
        stock -= quantity
    }

    fun isAvailable(quantity: Int): Boolean = stock >= quantity

    companion object {
        const val MAX_NAME_LENGTH = 50

        fun create(
            name: String,
            price: Long,
            imageUrl: String,
            stock: Int,
        ): Product {
            require(name.isNotBlank()) { "name must not be blank" }
            require(name.length <= MAX_NAME_LENGTH) { "name must be at most $MAX_NAME_LENGTH chars" }
            require(price >= 0) { "price must not be negative" }
            require(stock >= 0) { "stock must not be negative" }
            require(imageUrl.isNotBlank()) { "imageUrl must not be blank" }
            return Product(name, price, imageUrl, stock)
        }
    }
}
