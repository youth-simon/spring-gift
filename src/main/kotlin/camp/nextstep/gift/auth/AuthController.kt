package camp.nextstep.gift.auth

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @GetMapping("/kakao/callback")
    fun kakaoCallback(
        @RequestParam("code") code: String,
    ): LoginResponse {
        val result = authService.loginWithKakao(code)
        return LoginResponse(
            accessToken = result.accessToken,
            memberId = result.memberId,
            email = result.email,
            name = result.name,
        )
    }
}

data class LoginResponse(
    val accessToken: String,
    val memberId: Long,
    val email: String,
    val name: String,
)
