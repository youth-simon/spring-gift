package camp.nextstep.gift.auth

import camp.nextstep.gift.member.MemberRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
class AuthServiceTest {
    @TestConfiguration
    class StubKakaoConfig {
        @Bean
        @Primary
        fun stubKakaoClient(): KakaoOAuthClient = StubKakaoOAuthClient()
    }

    @Autowired private lateinit var authService: AuthService

    @Autowired private lateinit var memberRepository: MemberRepository

    @Autowired private lateinit var kakaoOAuthClient: KakaoOAuthClient

    @Test
    fun `first kakao login creates a new member`() {
        val stub = kakaoOAuthClient as StubKakaoOAuthClient
        stub.userId = 1001
        stub.email = "neo@kakao.com"
        stub.nickname = "neo"

        val result = authService.loginWithKakao("any-auth-code")

        assertNotNull(result.accessToken)
        assertEquals("neo@kakao.com", result.email)
        assertEquals("neo", result.name)
        assertNotNull(memberRepository.findByKakaoId(1001))
    }

    @Test
    fun `repeated kakao login reuses the existing member`() {
        val stub = kakaoOAuthClient as StubKakaoOAuthClient
        stub.userId = 2002
        stub.email = "morpheus@kakao.com"
        stub.nickname = "morpheus"

        val first = authService.loginWithKakao("code-1")
        val second = authService.loginWithKakao("code-2")

        assertEquals(first.memberId, second.memberId)
        assertTrue(memberRepository.findAll().count { it.kakaoId == 2002L } == 1)
    }
}

class StubKakaoOAuthClient : KakaoOAuthClient {
    var userId: Long = 0
    var email: String = ""
    var nickname: String = ""

    override fun exchangeToken(authorizationCode: String): KakaoTokenResponse =
        KakaoTokenResponse(accessToken = "stub-access", tokenType = "bearer")

    override fun loadUser(accessToken: String): KakaoUserResponse =
        KakaoUserResponse(
            id = userId,
            kakaoAccount =
                KakaoAccount(
                    email = email,
                    profile = KakaoProfile(nickname = nickname),
                ),
        )
}
