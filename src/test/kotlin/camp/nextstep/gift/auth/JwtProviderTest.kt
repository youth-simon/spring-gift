package camp.nextstep.gift.auth

import camp.nextstep.gift.common.UnauthorizedException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals

class JwtProviderTest {
    private val objectMapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
    private val secret = "this-is-a-test-only-secret-and-should-be-long-enough"

    private fun providerAt(
        now: Instant,
        expiry: Duration = Duration.ofMinutes(30),
    ): JwtProvider {
        val properties = JwtProperties(secret = secret, expiry = expiry)
        val clock = Clock.fixed(now, ZoneId.of("UTC"))
        return JwtProvider(properties, objectMapper, clock)
    }

    @Test
    fun `issued token round-trips back to the member id`() {
        val provider = providerAt(Instant.parse("2026-01-01T00:00:00Z"))

        val token = provider.issue(memberId = 42)

        assertEquals(42, provider.parse(token))
    }

    @Test
    fun `expired token is rejected`() {
        val issuer = providerAt(Instant.parse("2026-01-01T00:00:00Z"), expiry = Duration.ofMinutes(1))
        val token = issuer.issue(memberId = 7)

        val verifier = providerAt(Instant.parse("2026-01-01T00:05:00Z"))

        assertThrows<UnauthorizedException> { verifier.parse(token) }
    }

    @Test
    fun `tampered signature is rejected`() {
        val provider = providerAt(Instant.parse("2026-01-01T00:00:00Z"))
        val token = provider.issue(memberId = 7)
        val tampered = token.dropLast(2) + "AA"

        assertThrows<UnauthorizedException> { provider.parse(tampered) }
    }

    @Test
    fun `malformed token is rejected`() {
        val provider = providerAt(Instant.parse("2026-01-01T00:00:00Z"))

        assertThrows<UnauthorizedException> { provider.parse("not-a-jwt") }
    }

    @Test
    fun `token with unexpected alg header is rejected even when signed correctly`() {
        val provider = providerAt(Instant.parse("2026-01-01T00:00:00Z"))
        val forged =
            forgeToken(
                header = mapOf("alg" to "HS512", "typ" to "JWT"),
                payload = mapOf("sub" to "7", "exp" to Instant.parse("2026-01-01T01:00:00Z").epochSecond),
            )

        assertThrows<UnauthorizedException> { provider.parse(forged) }
    }

    @Test
    fun `token with unexpected typ header is rejected even when signed correctly`() {
        val provider = providerAt(Instant.parse("2026-01-01T00:00:00Z"))
        val forged =
            forgeToken(
                header = mapOf("alg" to "HS256", "typ" to "JWE"),
                payload = mapOf("sub" to "7", "exp" to Instant.parse("2026-01-01T01:00:00Z").epochSecond),
            )

        assertThrows<UnauthorizedException> { provider.parse(forged) }
    }

    // 서명은 provider 의 secret 으로 올바르게 만든 뒤 헤더만 바꾼 토큰. signature 검증은 통과하고 alg/typ 검증에서 걸려야 한다.
    private fun forgeToken(
        header: Map<String, Any>,
        payload: Map<String, Any>,
    ): String {
        val encoder = java.util.Base64.getUrlEncoder().withoutPadding()
        val headerEncoded = encoder.encodeToString(objectMapper.writeValueAsBytes(header))
        val payloadEncoded = encoder.encodeToString(objectMapper.writeValueAsBytes(payload))
        val signingInput = "$headerEncoded.$payloadEncoded"
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val signature = encoder.encodeToString(mac.doFinal(signingInput.toByteArray()))
        return "$signingInput.$signature"
    }
}
