package camp.nextstep.gift.product

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/admin/products")
class AdminProductController(
    private val productService: ProductService,
) {
    @GetMapping
    fun list(
        pageable: Pageable,
        model: Model,
    ): String {
        model.addAttribute("products", productService.findAll(pageable).content)
        return "product/list"
    }

    @GetMapping("/new")
    fun newForm(): String = "product/new"

    @PostMapping
    fun create(
        @ModelAttribute form: ProductForm,
    ): String {
        productService.create(form.toCreateCommand())
        return "redirect:/admin/products"
    }

    @GetMapping("/{id}/edit")
    fun editForm(
        @PathVariable id: Long,
        model: Model,
    ): String {
        model.addAttribute("product", productService.findById(id))
        return "product/edit"
    }

    @PostMapping("/{id}/edit")
    fun update(
        @PathVariable id: Long,
        @ModelAttribute form: ProductForm,
    ): String {
        productService.update(id, form.toUpdateCommand())
        return "redirect:/admin/products"
    }

    @PostMapping("/{id}/delete")
    fun delete(
        @PathVariable id: Long,
    ): String {
        productService.delete(id)
        return "redirect:/admin/products"
    }
}

data class ProductForm(
    var name: String = "",
    var price: Long = 0,
    var imageUrl: String = "",
    var stock: Int = 0,
) {
    fun toCreateCommand() = CreateProductCommand(name, price, imageUrl, stock)

    fun toUpdateCommand() = UpdateProductCommand(name, price, imageUrl, stock)
}
