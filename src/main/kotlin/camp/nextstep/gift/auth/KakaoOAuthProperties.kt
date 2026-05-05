package camp.nextstep.gift.auth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kakao.login")
data class KakaoOAuthProperties(
    val clientId: String,
    val clientSecret: String = "",
    val redirectUri: String,
    val authorizeUrl: String = "https://kauth.kakao.com/oauth/authorize",
    val tokenUrl: String = "https://kauth.kakao.com/oauth/token",
    val userInfoUrl: String = "https://kapi.kakao.com/v2/user/me",
)
