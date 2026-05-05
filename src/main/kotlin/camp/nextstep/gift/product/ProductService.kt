package camp.nextstep.gift.product

import camp.nextstep.gift.common.NotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository,
) {
    fun findAll(pageable: Pageable): Page<Product> = productRepository.findAll(pageable)

    fun findById(id: Long): Product = productRepository.findById(id).orElseThrow { NotFoundException("product not found: $id") }

    @Transactional
    fun create(command: CreateProductCommand): Product {
        val product =
            Product.create(
                name = command.name,
                price = command.price,
                imageUrl = command.imageUrl,
                stock = command.stock,
            )
        return productRepository.save(product)
    }

    @Transactional
    fun update(
        id: Long,
        command: UpdateProductCommand,
    ): Product {
        val product = findById(id)
        product.rename(command.name)
        product.changePrice(command.price)
        product.replaceStock(command.stock)
        return product
    }

    @Transactional
    fun delete(id: Long) {
        if (!productRepository.existsById(id)) {
            throw NotFoundException("product not found: $id")
        }
        productRepository.deleteById(id)
    }
}

data class CreateProductCommand(
    val name: String,
    val price: Long,
    val imageUrl: String,
    val stock: Int,
)

data class UpdateProductCommand(
    val name: String,
    val price: Long,
    val imageUrl: String,
    val stock: Int,
)
