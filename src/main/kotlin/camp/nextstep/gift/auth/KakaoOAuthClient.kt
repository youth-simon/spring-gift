package camp.nextstep.gift.auth

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

interface KakaoOAuthClient {
    fun exchangeToken(authorizationCode: String): KakaoTokenResponse

    fun loadUser(accessToken: String): KakaoUserResponse
}

@Component
class DefaultKakaoOAuthClient(
    private val properties: KakaoOAuthProperties,
) : KakaoOAuthClient {
    private val restClient: RestClient = RestClient.create()

    override fun exchangeToken(authorizationCode: String): KakaoTokenResponse {
        val form =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "authorization_code")
                add("client_id", properties.clientId)
                add("redirect_uri", properties.redirectUri)
                add("code", authorizationCode)
                if (properties.clientSecret.isNotBlank()) {
                    add("client_secret", properties.clientSecret)
                }
            }

        return restClient
            .post()
            .uri(properties.tokenUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(KakaoTokenResponse::class.java)
            ?: error("kakao token response is null")
    }

    override fun loadUser(accessToken: String): KakaoUserResponse =
        restClient
            .get()
            .uri(properties.userInfoUrl)
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .body(KakaoUserResponse::class.java)
            ?: error("kakao user response is null")
}

data class KakaoTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("refresh_token") val refreshToken: String? = null,
    @JsonProperty("expires_in") val expiresIn: Long? = null,
    val scope: String? = null,
)

data class KakaoUserResponse(
    val id: Long,
    @JsonProperty("kakao_account") val kakaoAccount: KakaoAccount?,
) {
    fun email(): String = kakaoAccount?.email ?: "kakao_$id@spring-gift.local"

    fun nickname(): String = kakaoAccount?.profile?.nickname ?: "kakao_$id"
}

data class KakaoAccount(
    val email: String?,
    val profile: KakaoProfile?,
)

data class KakaoProfile(
    val nickname: String?,
)
