package camp.nextstep.gift.product

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService,
) {
    @GetMapping
    fun list(pageable: Pageable): List<ProductResponse> = productService.findAll(pageable).content.map { ProductResponse.from(it) }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
    ): ProductResponse = ProductResponse.from(productService.findById(id))

    @PostMapping
    fun create(
        @Valid @RequestBody request: ProductRequest,
    ): ResponseEntity<ProductResponse> {
        val created = productService.create(request.toCreateCommand())
        val location = URI.create("/api/products/${created.id}")
        return ResponseEntity.created(location).body(ProductResponse.from(created))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProductRequest,
    ): ProductResponse = ProductResponse.from(productService.update(id, request.toUpdateCommand()))

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        productService.delete(id)
        return ResponseEntity.noContent().build()
    }
}

data class ProductRequest(
    @field:NotBlank
    @field:Size(max = Product.MAX_NAME_LENGTH)
    val name: String,
    @field:PositiveOrZero
    val price: Long,
    @field:NotBlank
    val imageUrl: String,
    @field:Min(0)
    val stock: Int,
) {
    fun toCreateCommand() = CreateProductCommand(name, price, imageUrl, stock)

    fun toUpdateCommand() = UpdateProductCommand(name, price, imageUrl, stock)
}

data class ProductResponse(
    val id: Long,
    val name: String,
    val price: Long,
    val imageUrl: String,
    val stock: Int,
) {
    companion object {
        fun from(product: Product) =
            ProductResponse(
                id = product.id!!,
                name = product.name,
                price = product.price,
                imageUrl = product.imageUrl,
                stock = product.stock,
            )
    }
}
