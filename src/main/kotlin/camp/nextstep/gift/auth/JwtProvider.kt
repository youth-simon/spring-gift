package camp.nextstep.gift.auth

import camp.nextstep.gift.common.UnauthorizedException
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class JwtProvider(
    private val properties: JwtProperties,
    private val objectMapper: ObjectMapper,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun issue(memberId: Long): String {
        val now = Instant.now(clock)
        val header = mapOf("alg" to ALG, "typ" to TYP)
        val payload =
            mapOf(
                "sub" to memberId.toString(),
                "iat" to now.epochSecond,
                "exp" to now.plus(properties.expiry).epochSecond,
            )

        val headerEncoded = encode(objectMapper.writeValueAsBytes(header))
        val payloadEncoded = encode(objectMapper.writeValueAsBytes(payload))
        val signingInput = "$headerEncoded.$payloadEncoded"
        val signature = encode(sign(signingInput))
        return "$signingInput.$signature"
    }

    fun parse(token: String): Long {
        val parts = token.split(".")
        if (parts.size != 3) throw UnauthorizedException("malformed token")
        val (headerPart, payloadPart, signaturePart) = parts

        val expectedSignature = encode(sign("$headerPart.$payloadPart"))
        if (!constantTimeEquals(expectedSignature, signaturePart)) {
            throw UnauthorizedException("invalid signature")
        }

        @Suppress("UNCHECKED_CAST")
        val header = objectMapper.readValue(decode(headerPart), Map::class.java) as Map<String, Any>
        if (header["alg"] != ALG) throw UnauthorizedException("unexpected alg")
        if (header["typ"] != TYP) throw UnauthorizedException("unexpected typ")

        @Suppress("UNCHECKED_CAST")
        val payload = objectMapper.readValue(decode(payloadPart), Map::class.java) as Map<String, Any>

        val exp = (payload["exp"] as? Number)?.toLong() ?: throw UnauthorizedException("missing exp")
        if (exp < Instant.now(clock).epochSecond) {
            throw UnauthorizedException("token expired")
        }

        val sub = payload["sub"]?.toString() ?: throw UnauthorizedException("missing subject")
        return sub.toLongOrNull() ?: throw UnauthorizedException("invalid subject")
    }

    private fun sign(input: String): ByteArray {
        val mac = Mac.getInstance(MAC_ALG)
        mac.init(SecretKeySpec(properties.secret.toByteArray(), MAC_ALG))
        return mac.doFinal(input.toByteArray())
    }

    private fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun decode(text: String): ByteArray = Base64.getUrlDecoder().decode(text)

    private fun constantTimeEquals(
        a: String,
        b: String,
    ): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    companion object {
        private const val ALG = "HS256"
        private const val TYP = "JWT"
        private const val MAC_ALG = "HmacSHA256"
    }
}
