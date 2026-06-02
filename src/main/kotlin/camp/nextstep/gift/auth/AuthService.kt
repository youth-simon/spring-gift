package camp.nextstep.gift.auth

import org.springframework.stereotype.Service

@Service
class AuthService(
    private val kakaoOAuthClient: KakaoOAuthClient,
    private val memberRegistry: MemberRegistry,
    private val jwtProvider: JwtProvider,
) {
    fun loginWithKakao(authorizationCode: String): LoginResult {
        val token = kakaoOAuthClient.exchangeToken(authorizationCode)
        val kakaoUser = kakaoOAuthClient.loadUser(token.accessToken)

        val member = memberRegistry.findOrRegister(kakaoUser)

        return LoginResult(
            accessToken = jwtProvider.issue(member.id!!),
            memberId = member.id!!,
            email = member.email,
            name = member.name,
        )
    }
}

data class LoginResult(
    val accessToken: String,
    val memberId: Long,
    val email: String,
    val name: String,
)
