package camp.nextstep.gift.member

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/admin/members")
class AdminMemberController(
    private val memberRepository: MemberRepository,
) {
    @GetMapping
    fun list(model: Model): String {
        model.addAttribute("members", memberRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")))
        return "member/list"
    }
}
