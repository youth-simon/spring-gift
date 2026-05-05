package camp.nextstep.gift.wish

import camp.nextstep.gift.auth.LoginMember
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/wishes")
class WishController(
    private val wishService: WishService,
) {
    @GetMapping
    fun list(
        @LoginMember memberId: Long,
        pageable: Pageable,
    ): List<WishResponse> = wishService.listOf(memberId, pageable).content.map { WishResponse.from(it) }

    @PostMapping
    fun add(
        @LoginMember memberId: Long,
        @Valid @RequestBody request: WishCreateRequest,
    ): ResponseEntity<WishResponse> {
        val wish = wishService.add(memberId, request.productId, request.quantity)
        val location = URI.create("/api/wishes/${wish.id}")
        return ResponseEntity.created(location).body(WishResponse.from(wish))
    }

    @PatchMapping("/{id}")
    fun changeQuantity(
        @LoginMember memberId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody request: WishQuantityRequest,
    ): ResponseEntity<Void> {
        wishService.changeQuantity(memberId, id, request.quantity)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/products/{productId}")
    fun delete(
        @LoginMember memberId: Long,
        @PathVariable productId: Long,
    ): ResponseEntity<Void> {
        wishService.remove(memberId, productId)
        return ResponseEntity.noContent().build()
    }
}

data class WishCreateRequest(
    @field:Positive val productId: Long,
    @field:Min(1) val quantity: Int,
)

data class WishQuantityRequest(
    @field:Min(1) val quantity: Int,
)

data class WishResponse(
    val id: Long,
    val productId: Long,
    val productName: String,
    val price: Long,
    val imageUrl: String,
    val quantity: Int,
) {
    companion object {
        fun from(wish: Wish) =
            WishResponse(
                id = wish.id!!,
                productId = wish.product.id!!,
                productName = wish.product.name,
                price = wish.product.price,
                imageUrl = wish.product.imageUrl,
                quantity = wish.quantity,
            )
    }
}
