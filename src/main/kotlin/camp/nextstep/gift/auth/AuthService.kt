package camp.nextstep.gift.auth

import camp.nextstep.gift.member.Member
import camp.nextstep.gift.member.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuthService(
    private val kakaoOAuthClient: KakaoOAuthClient,
    private val memberRepository: MemberRepository,
    private val jwtProvider: JwtProvider,
) {
    @Transactional
    fun loginWithKakao(authorizationCode: String): LoginResult {
        val token = kakaoOAuthClient.exchangeToken(authorizationCode)
        val kakaoUser = kakaoOAuthClient.loadUser(token.accessToken)

        val member =
            memberRepository.findByKakaoId(kakaoUser.id)
                ?: memberRepository.save(
                    Member.fromKakao(
                        kakaoId = kakaoUser.id,
                        email = kakaoUser.email(),
                        name = kakaoUser.nickname(),
                    ),
                )

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
